package com.huanzi.shortlinksystem.manager;

import com.huanzi.shortlinksystem.common.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

import static com.huanzi.shortlinksystem.constant.RedisKeyConstants.SHORT_LINK_CACHE_KEY;
import static com.huanzi.shortlinksystem.constant.RedisKeyConstants.SHORT_LINK_NULL_CACHE_STATUS;
import static com.huanzi.shortlinksystem.constant.RedisKeyConstants.SHORT_LINK_NULL_CACHE_VALUE;

/**
 * 短链缓存管理器。
 * 负责统一封装 Redis 中与短链跳转相关的读写规则，尤其是：
 * - 正常短链缓存
 * - 空值缓存
 * - 缓存删除
 *
 * 这样业务层只需要表达“我要查缓存 / 写缓存 / 删缓存”，
 * 不需要关心 key 拼接和空值标记细节。
 */
@Component
@RequiredArgsConstructor
public class ShortLinkCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 查询短链缓存。
     * 返回约定：
     * - Optional.empty()：Redis 未命中
     * - status = -1：命中了空值缓存
     * - 其他：命中了正常短链缓存
     */
    public Optional<ShortLinkCacheValue> get(String shortCode) {
        Object cacheValue = redisTemplate.opsForValue().get(buildKey(shortCode));
        if (cacheValue == null) {
            return Optional.empty();
        }
        String value = String.valueOf(cacheValue);
        if (SHORT_LINK_NULL_CACHE_VALUE.equals(value)) {
            ShortLinkCacheValue emptyValue = new ShortLinkCacheValue();
            emptyValue.setStatus(SHORT_LINK_NULL_CACHE_STATUS);
            return Optional.of(emptyValue);
        }
        return Optional.of(JsonUtils.parseObject(value, ShortLinkCacheValue.class));
    }

    /**
     * 写入正常短链缓存。
     */
    public void put(String shortCode, ShortLinkCacheValue cacheValue, Duration duration) {
        redisTemplate.opsForValue().set(buildKey(shortCode), JsonUtils.toJson(cacheValue), duration);
    }

    /**
     * 写入空值缓存，用于拦截不存在短码的重复访问。
     */
    public void putNull(String shortCode, Duration duration) {
        redisTemplate.opsForValue().set(buildKey(shortCode), SHORT_LINK_NULL_CACHE_VALUE, duration);
    }

    /**
     * 判断是否为空值缓存命中。
     * 空值缓存本质上是“短码不存在”的特殊标记，用来防止同一个无效短码重复穿透到数据库。
     */
    public boolean isNullValue(ShortLinkCacheValue cacheValue) {
        return cacheValue != null && Integer.valueOf(SHORT_LINK_NULL_CACHE_STATUS).equals(cacheValue.getStatus());
    }

    /**
     * 删除短链缓存，适用于短链被编辑、启用/禁用、删除后的缓存失效。
     */
    public void delete(String shortCode) {
        redisTemplate.delete(buildKey(shortCode));
    }

    private String buildKey(String shortCode) {
        return SHORT_LINK_CACHE_KEY + shortCode;
    }
}
