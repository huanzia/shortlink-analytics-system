package com.huanzi.shortlinksystem.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huanzi.shortlinksystem.entity.ShortLink;
import com.huanzi.shortlinksystem.mapper.ShortLinkMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.huanzi.shortlinksystem.constant.RedisKeyConstants.SHORT_LINK_BLOOM_EXPECTED_INSERTIONS;
import static com.huanzi.shortlinksystem.constant.RedisKeyConstants.SHORT_LINK_BLOOM_FALSE_PROBABILITY;
import static com.huanzi.shortlinksystem.constant.RedisKeyConstants.SHORT_LINK_BLOOM_KEY;

/**
 * 布隆过滤器管理器。
 * 这里的职责很单一：维护“哪些 shortCode 可能存在”的快速判断能力，
 * 让明显无效的短码请求可以在进入 Redis / MySQL 前被拦掉。
 */
@Component
@RequiredArgsConstructor
public class BloomFilterManager {

    private final RedissonClient redissonClient;
    private final ShortLinkMapper shortLinkMapper;

    private RBloomFilter<String> bloomFilter;

    @PostConstruct
    public void init() {
        // BloomFilter 存在 Redis 中，应用重启后仍能复用已有结构。
        bloomFilter = redissonClient.getBloomFilter(SHORT_LINK_BLOOM_KEY);
        bloomFilter.tryInit(SHORT_LINK_BLOOM_EXPECTED_INSERTIONS, SHORT_LINK_BLOOM_FALSE_PROBABILITY);

        // 当前启动时只加载启用态短链，删除态和禁用态不纳入跳转前置判断。
        LambdaQueryWrapper<ShortLink> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShortLink::getStatus, 1)
                .select(ShortLink::getShortCode);
        List<ShortLink> shortLinks = shortLinkMapper.selectList(queryWrapper);
        shortLinks.stream()
                .map(ShortLink::getShortCode)
                .filter(shortCode -> shortCode != null && !shortCode.isBlank())
                .forEach(bloomFilter::add);
    }

    /**
     * 增量添加 shortCode。
     * 典型场景是新建短链成功后，或者禁用态切换回启用态时。
     */
    public void add(String value) {
        if (value != null && !value.isBlank()) {
            bloomFilter.add(value);
        }
    }

    /**
     * 判断 shortCode 是否“可能存在”。
     * 注意：布隆过滤器只能保证“不存在一定返回 false”，
     * 不能保证“返回 true 就一定存在”。
     */
    public boolean mightContain(String value) {
        return value != null && !value.isBlank() && bloomFilter.contains(value);
    }
}
