package com.huanzi.shortlinksystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huanzi.shortlinksystem.common.exception.BizException;
import com.huanzi.shortlinksystem.dto.ShortLinkCreateDTO;
import com.huanzi.shortlinksystem.dto.ShortLinkStatusUpdateDTO;
import com.huanzi.shortlinksystem.dto.ShortLinkUpdateDTO;
import com.huanzi.shortlinksystem.entity.ShortLink;
import com.huanzi.shortlinksystem.enums.LinkStatusEnum;
import com.huanzi.shortlinksystem.manager.BloomFilterManager;
import com.huanzi.shortlinksystem.manager.ShortLinkCacheManager;
import com.huanzi.shortlinksystem.mapper.ShortLinkMapper;
import com.huanzi.shortlinksystem.service.ShortLinkService;
import com.huanzi.shortlinksystem.vo.PageResultVO;
import com.huanzi.shortlinksystem.vo.ShortLinkCreateVO;
import com.huanzi.shortlinksystem.vo.ShortLinkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 短链管理核心实现。
 * 这一层负责把“短链管理规则”真正落到代码里，包括：
 * - 固定 userId=1 的当前阶段约束
 * - 创建短链时的唯一短码生成
 * - 编辑 / 启用禁用 / 删除后的缓存处理
 * - 启用后补充布隆过滤器
 */
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl implements ShortLinkService {

    private static final Long DEFAULT_USER_ID = 1L;

    private final BloomFilterManager bloomFilterManager;
    private final ShortLinkCacheManager shortLinkCacheManager;
    private final ShortLinkMapper shortLinkMapper;

    @Value("${short-link.base-url}")
    private String shortLinkBaseUrl;

    @Override
    public ShortLinkCreateVO createShortLink(ShortLinkCreateDTO createDTO) {
        if (!StringUtils.hasText(createDTO.getOriginUrl())) {
            throw new BizException("originUrl can not be blank");
        }

        // 当前阶段先固定 userId=1，等登录态接入后再替换成真实用户上下文。
        // 创建流程的关键点是：生成短码 -> 写库 -> 清理旧缓存 -> 加入布隆过滤器。
        String shortCode = generateUniqueShortCode();
        ShortLink shortLink = new ShortLink();
        shortLink.setUserId(DEFAULT_USER_ID);
        shortLink.setShortCode(shortCode);
        shortLink.setShortUrl(buildShortUrl(shortCode));
        shortLink.setOriginUrl(createDTO.getOriginUrl());
        shortLink.setTitle(createDTO.getTitle());
        shortLink.setDescription(createDTO.getDescription());
        shortLink.setExpireTime(createDTO.getExpireTime());
        shortLink.setStatus(LinkStatusEnum.ENABLED.getCode());
        shortLink.setPvCount(0L);
        shortLink.setUvCount(0L);
        // 插入数据库后由 MyBatis-Plus 回填主键，后续直接用于响应。
        shortLinkMapper.insert(shortLink);
        // 如果该短码之前命中过不存在场景，这里要先清掉空值缓存，避免新短链在 TTL 内仍返回 404。
        shortLinkCacheManager.delete(shortLink.getShortCode());
        bloomFilterManager.add(shortLink.getShortCode());

        ShortLinkCreateVO createVO = new ShortLinkCreateVO();
        createVO.setId(shortLink.getId());
        createVO.setShortCode(shortLink.getShortCode());
        createVO.setShortUrl(shortLink.getShortUrl());
        createVO.setTitle(shortLink.getTitle());
        createVO.setOriginUrl(shortLink.getOriginUrl());
        return createVO;
    }

    @Override
    public PageResultVO<ShortLinkVO> listMyLinks(Long pageNum, Long pageSize) {
        long currentPage = normalizePageNum(pageNum);
        long currentSize = normalizePageSize(pageSize);

        // 当前按固定用户查询，并且明确过滤删除态。
        // 禁用态、过期态仍然保留在列表中，便于管理侧继续查看和操作。
        LambdaQueryWrapper<ShortLink> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShortLink::getUserId, DEFAULT_USER_ID)
                .ne(ShortLink::getStatus, LinkStatusEnum.DELETED.getCode())
                .orderByDesc(ShortLink::getId);

        IPage<ShortLink> page = shortLinkMapper.selectPage(new Page<>(currentPage, currentSize), queryWrapper);

        PageResultVO<ShortLinkVO> result = new PageResultVO<>();
        result.setRecords(page.getRecords()
                .stream()
                .map(this::toVO)
                .toList());
        result.setTotal(page.getTotal());
        result.setPageNum(currentPage);
        result.setPageSize(currentSize);
        return result;
    }

    @Override
    public ShortLinkVO getLinkDetail(Long id) {
        return toVO(getCurrentUserLink(id));
    }

    @Override
    public ShortLinkVO updateLink(Long id, ShortLinkUpdateDTO updateDTO) {
        ShortLink shortLink = getCurrentUserLink(id);
        shortLink.setTitle(updateDTO.getTitle());
        shortLink.setDescription(updateDTO.getDescription());
        shortLink.setExpireTime(updateDTO.getExpireTime());
        shortLinkMapper.updateById(shortLink);
        // 编辑后统一删除缓存，让下一次跳转按数据库最新状态重新回源。
        shortLinkCacheManager.delete(shortLink.getShortCode());
        return toVO(shortLinkMapper.selectById(id));
    }

    @Override
    public ShortLinkVO updateLinkStatus(Long id, ShortLinkStatusUpdateDTO statusUpdateDTO) {
        if (statusUpdateDTO.getStatus() == null || (statusUpdateDTO.getStatus() != 0 && statusUpdateDTO.getStatus() != 1)) {
            throw new BizException("status must be 0 or 1");
        }

        ShortLink shortLink = getCurrentUserLink(id);
        Integer oldStatus = shortLink.getStatus();
        shortLink.setStatus(statusUpdateDTO.getStatus());
        shortLinkMapper.updateById(shortLink);
        // 状态变化可能直接影响跳转结果，因此简单可靠的做法是直接删缓存。
        shortLinkCacheManager.delete(shortLink.getShortCode());
        if (!LinkStatusEnum.ENABLED.getCode().equals(oldStatus)
                && LinkStatusEnum.ENABLED.getCode().equals(statusUpdateDTO.getStatus())) {
            // 应用启动时只会把启用态短链加载进布隆过滤器，
            // 所以“禁用 -> 启用”场景需要做一次增量补充。
            bloomFilterManager.add(shortLink.getShortCode());
        }
        return toVO(shortLinkMapper.selectById(id));
    }

    @Override
    public void deleteLink(Long id) {
        ShortLink shortLink = getCurrentUserLink(id);
        shortLink.setStatus(LinkStatusEnum.DELETED.getCode());
        shortLinkMapper.updateById(shortLink);
        // 删除后立即删缓存，避免旧缓存继续让短链可跳转。
        shortLinkCacheManager.delete(shortLink.getShortCode());
    }

    /**
     * 把数据库实体转换成对外返回对象，避免 controller 直接暴露 entity。
     */
    private ShortLinkVO toVO(ShortLink shortLink) {
        ShortLinkVO shortLinkVO = new ShortLinkVO();
        shortLinkVO.setId(shortLink.getId());
        shortLinkVO.setUserId(shortLink.getUserId());
        shortLinkVO.setShortCode(shortLink.getShortCode());
        shortLinkVO.setShortUrl(shortLink.getShortUrl());
        shortLinkVO.setOriginUrl(shortLink.getOriginUrl());
        shortLinkVO.setTitle(shortLink.getTitle());
        shortLinkVO.setDescription(shortLink.getDescription());
        shortLinkVO.setStatus(shortLink.getStatus());
        shortLinkVO.setPvCount(shortLink.getPvCount());
        shortLinkVO.setUvCount(shortLink.getUvCount());
        shortLinkVO.setExpireTime(shortLink.getExpireTime());
        shortLinkVO.setCreateTime(shortLink.getCreateTime());
        shortLinkVO.setUpdateTime(shortLink.getUpdateTime());
        return shortLinkVO;
    }

    private ShortLink getCurrentUserLink(Long id) {
        // 这里统一收敛“当前用户可操作短链”的查询规则，
        // 避免详情、编辑、状态更新、删除各自写一套条件导致边界不一致。
        LambdaQueryWrapper<ShortLink> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShortLink::getId, id)
                .eq(ShortLink::getUserId, DEFAULT_USER_ID)
                .ne(ShortLink::getStatus, LinkStatusEnum.DELETED.getCode())
                .last("limit 1");
        ShortLink shortLink = shortLinkMapper.selectOne(queryWrapper);
        if (shortLink == null) {
            throw new BizException("short link not found");
        }
        return shortLink;
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        // 这里只做最基础的分页边界保护，避免 pageSize 过大直接把整表拖出来。
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100L);
    }

    private String buildShortUrl(String shortCode) {
        return shortLinkBaseUrl + shortCode;
    }

    /**
     * 生成唯一短码。
     * 当前先用 UUID 截断做简化实现，并在数据库层做重复检查。
     * 这是一个偏演示型实现，适合基础版本，后续如果追求更高可控性可以换成更稳定的发号策略。
     */
    private String generateUniqueShortCode() {
        for (int i = 0; i < 10; i++) {
            String shortCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            LambdaQueryWrapper<ShortLink> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ShortLink::getShortCode, shortCode);
            if (shortLinkMapper.selectCount(queryWrapper) == 0) {
                return shortCode;
            }
        }
        throw new BizException("generate short code failed");
    }
}
