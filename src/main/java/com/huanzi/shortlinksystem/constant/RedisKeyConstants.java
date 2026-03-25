package com.huanzi.shortlinksystem.constant;

/**
 * 统一维护 Redis key 和相关参数。
 * 这里的常量主要服务于三条链路：
 * - 短链跳转缓存
 * - 布隆过滤器
 * - UV 去重
 */
public final class RedisKeyConstants {

    // 短链缓存：key 形如 shortlink:cache:abcd1234
    public static final String SHORT_LINK_CACHE_KEY = "shortlink:cache:";
    // 布隆过滤器在 Redis 中对应的逻辑名称。
    public static final String SHORT_LINK_BLOOM_KEY = "shortlink:bloom";
    public static final String SHORT_LINK_STATS_KEY = "shortlink:stats:";
    // UV Set：key 形如 shortlink:uv:abcd1234:20260325
    public static final String SHORT_LINK_UV_KEY = "shortlink:uv:";
    // 使用固定字符串作为空值缓存标记，便于快速判断“短码不存在”。
    public static final String SHORT_LINK_NULL_CACHE_VALUE = "__NULL__";
    // 用特殊状态标记空值缓存，避免和数据库中的真实状态混淆。
    public static final int SHORT_LINK_NULL_CACHE_STATUS = -1;
    // 正常短链缓存 TTL，当前先写死为 30 分钟。
    public static final long SHORT_LINK_CACHE_TTL_MINUTES = 30L;
    // 空值缓存 TTL 更短，既要防穿透，也要避免错误状态保持太久。
    public static final long SHORT_LINK_NULL_CACHE_TTL_MINUTES = 5L;
    // UV 去重按天统计，保留 2 天足够覆盖“今天 + 次日查询”场景。
    public static final long SHORT_LINK_UV_SET_TTL_DAYS = 2L;
    // 布隆过滤器参数当前是基础版配置，优先追求最小可用。
    public static final long SHORT_LINK_BLOOM_EXPECTED_INSERTIONS = 100_000L;
    public static final double SHORT_LINK_BLOOM_FALSE_PROBABILITY = 0.01D;

    private RedisKeyConstants() {
    }
}
