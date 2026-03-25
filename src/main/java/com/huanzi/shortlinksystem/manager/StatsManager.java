package com.huanzi.shortlinksystem.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.huanzi.shortlinksystem.constant.RedisKeyConstants.SHORT_LINK_STATS_KEY;
import static com.huanzi.shortlinksystem.constant.RedisKeyConstants.SHORT_LINK_UV_KEY;
import static com.huanzi.shortlinksystem.constant.RedisKeyConstants.SHORT_LINK_UV_SET_TTL_DAYS;

@Component
@RequiredArgsConstructor
public class StatsManager {

    private static final DateTimeFormatter UV_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisTemplate<String, Object> redisTemplate;

    public void incrementPv(Long shortLinkId) {
        redisTemplate.opsForValue().increment(SHORT_LINK_STATS_KEY + shortLinkId + ":pv");
    }

    /**
     * 按“短码 + 日期”维度做 UV 去重。
     * 只有当天首次访问该短链时才返回 true。
     */
    public boolean recordUv(String shortCode, String visitorId, LocalDate visitDate) {
        String uvKey = buildUvKey(shortCode, visitDate);
        Long added = redisTemplate.opsForSet().add(uvKey, visitorId);
        redisTemplate.expire(uvKey, Duration.ofDays(SHORT_LINK_UV_SET_TTL_DAYS));
        return added != null && added > 0;
    }

    private String buildUvKey(String shortCode, LocalDate visitDate) {
        return SHORT_LINK_UV_KEY + shortCode + ":" + visitDate.format(UV_DATE_FORMATTER);
    }
}
