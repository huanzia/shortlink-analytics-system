package com.huanzi.shortlinksystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huanzi.shortlinksystem.common.exception.BizException;
import com.huanzi.shortlinksystem.common.result.ResultCode;
import com.huanzi.shortlinksystem.common.utils.IpUtils;
import com.huanzi.shortlinksystem.entity.ShortLink;
import com.huanzi.shortlinksystem.enums.LinkStatusEnum;
import com.huanzi.shortlinksystem.manager.BloomFilterManager;
import com.huanzi.shortlinksystem.manager.ShortLinkCacheManager;
import com.huanzi.shortlinksystem.manager.ShortLinkCacheValue;
import com.huanzi.shortlinksystem.mapper.ShortLinkMapper;
import com.huanzi.shortlinksystem.mq.LinkAccessMessage;
import com.huanzi.shortlinksystem.mq.producer.LinkAccessProducer;
import com.huanzi.shortlinksystem.service.RedirectResult;
import com.huanzi.shortlinksystem.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.huanzi.shortlinksystem.constant.RedisKeyConstants.SHORT_LINK_CACHE_TTL_MINUTES;
import static com.huanzi.shortlinksystem.constant.RedisKeyConstants.SHORT_LINK_NULL_CACHE_TTL_MINUTES;

/**
 * 短链跳转核心实现。
 *
 * 当前主流程：
 * 1. 可选的 BloomFilter 前置判断
 * 2. 可选的 Redis 缓存命中
 * 3. Redis 未命中时回源 MySQL
 * 4. 对不存在短码写空值缓存，降低缓存穿透风险
 * 5. 对删除 / 禁用 / 过期做统一校验
 * 6. 只有真正跳转成功时才异步发送访问埋点
 *
 * 这也是整个项目最值得优先阅读的主链路之一。
 */
@Service
@RequiredArgsConstructor
public class RedirectServiceImpl implements RedirectService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(SHORT_LINK_CACHE_TTL_MINUTES);
    private static final Duration NULL_CACHE_TTL = Duration.ofMinutes(SHORT_LINK_NULL_CACHE_TTL_MINUTES);

    private final BloomFilterManager bloomFilterManager;
    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkCacheManager shortLinkCacheManager;
    private final LinkAccessProducer linkAccessProducer;

    @Value("${short-link.features.redis-cache-enabled:true}")
    private boolean redisCacheEnabled;

    @Value("${short-link.features.bloom-filter-enabled:true}")
    private boolean bloomFilterEnabled;

    @Value("${short-link.features.mq-access-log-enabled:true}")
    private boolean mqAccessLogEnabled;

    @Override
    public RedirectResult getRedirectUrl(String shortCode, HttpServletRequest request, String visitorId) {
        // 开关关闭时退化成普通链路；开启时用布隆过滤器先拦一层明显不存在的短码。
        if (bloomFilterEnabled && !bloomFilterManager.mightContain(shortCode)) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "short link not found");
        }

        // 开启 Redis 后优先查缓存。
        // 这里不仅缓存正常短链，也缓存不存在短链的空值标记。
        if (redisCacheEnabled) {
            Optional<ShortLinkCacheValue> cacheOptional = shortLinkCacheManager.get(shortCode);
            if (cacheOptional.isPresent()) {
                ShortLinkCacheValue cacheValue = cacheOptional.get();
                if (shortLinkCacheManager.isNullValue(cacheValue)) {
                    // 空值缓存用于快速拦截不存在短码，避免恶意短码持续打库。
                    throw new BizException(ResultCode.NOT_FOUND.getCode(), "short link not found");
                }
                // 兼容旧缓存结构：如果缺少 linkId，则回源数据库补齐埋点所需字段。
                if (cacheValue.getLinkId() != null) {
                    validateStatus(cacheValue.getStatus(), cacheValue.getExpireTime());
                    // 只有通过状态校验的请求才算“真正跳转成功”，因此也只有这里才发送埋点。
                    sendAccessMessage(cacheValue.getLinkId(), shortCode, cacheValue.getOriginUrl(), visitorId, request);
                    return new RedirectResult(cacheValue.getOriginUrl(), visitorId);
                }
            }
        }

        // 缓存关闭或未命中时，才回源数据库。
        LambdaQueryWrapper<ShortLink> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShortLink::getShortCode, shortCode)
                .last("limit 1");
        ShortLink shortLink = shortLinkMapper.selectOne(queryWrapper);
        if (shortLink == null) {
            if (redisCacheEnabled) {
                shortLinkCacheManager.putNull(shortCode, NULL_CACHE_TTL);
            }
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "short link not found");
        }
        if (LinkStatusEnum.DELETED.getCode().equals(shortLink.getStatus())) {
            if (redisCacheEnabled) {
                // 删除态对外统一表现为 not found。
                // 这里顺便写一层空值缓存，避免删除后的短码继续频繁打库。
                shortLinkCacheManager.putNull(shortCode, NULL_CACHE_TTL);
            }
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "short link not found");
        }

        if (redisCacheEnabled) {
            ShortLinkCacheValue cacheValue = new ShortLinkCacheValue();
            cacheValue.setLinkId(shortLink.getId());
            cacheValue.setStatus(shortLink.getStatus());
            cacheValue.setExpireTime(shortLink.getExpireTime());
            if (isAvailable(shortLink.getStatus(), shortLink.getExpireTime())) {
                // 只有可正常跳转的短链才缓存 originUrl。
                // 异常状态只缓存状态信息，不缓存可跳转地址，避免脏数据继续被复用。
                cacheValue.setOriginUrl(shortLink.getOriginUrl());
            }
            shortLinkCacheManager.put(shortCode, cacheValue, CACHE_TTL);
        }

        validateStatus(shortLink.getStatus(), shortLink.getExpireTime());
        sendAccessMessage(shortLink.getId(), shortCode, shortLink.getOriginUrl(), visitorId, request);
        return new RedirectResult(shortLink.getOriginUrl(), visitorId);
    }

    /**
     * 统一校验短链是否允许跳转。
     * 规则说明：
     * - 删除态：对外表现为 404，更接近“资源不存在”
     * - 非启用态：按 disabled 处理
     * - expireTime 早于当前时间：按 expired 处理
     */
    private void validateStatus(Integer status, LocalDateTime expireTime) {
        if (LinkStatusEnum.DELETED.getCode().equals(status)) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "short link not found");
        }
        if (status == null || status != 1) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "short link is disabled");
        }
        if (expireTime != null && expireTime.isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "short link is expired");
        }
    }

    /**
     * 判断短链是否可作为“正常可跳转”数据进入缓存。
     * 这里只决定是否缓存 originUrl，不决定最终是否允许跳转；
     * 最终还是以 validateStatus 的结果为准。
     */
    private boolean isAvailable(Integer status, LocalDateTime expireTime) {
        return LinkStatusEnum.ENABLED.getCode().equals(status)
                && (expireTime == null || !expireTime.isBefore(LocalDateTime.now()));
    }

    /**
     * 跳转成功后异步发送访问埋点，不阻塞主链路。
     * MQ 开关关闭时直接跳过，方便做朴素版 / 缓存版 / 最终版压测对比。
     */
    private void sendAccessMessage(Long linkId, String shortCode, String originUrl, String visitorId,
                                   HttpServletRequest request) {
        if (!mqAccessLogEnabled) {
            return;
        }
        LinkAccessMessage message = new LinkAccessMessage();
        message.setLinkId(linkId);
        message.setShortCode(shortCode);
        message.setOriginUrl(originUrl);
        message.setVisitorId(visitorId);
        message.setUserIp(IpUtils.getClientIp(request));
        message.setUserAgent(request.getHeader("User-Agent"));
        message.setReferer(request.getHeader("Referer"));
        message.setAccessTime(LocalDateTime.now());
        message.setVisitDate(LocalDate.now());
        linkAccessProducer.send(message);
    }
}
