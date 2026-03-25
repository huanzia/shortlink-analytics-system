package com.huanzi.shortlinksystem.manager;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Redis 中保存的短链精简缓存对象。
 * 只保留跳转链路需要的字段，避免把整张表直接放入缓存。
 */
@Data
public class ShortLinkCacheValue {

    private Long linkId;
    private String originUrl;
    private Integer status;
    private LocalDateTime expireTime;
}
