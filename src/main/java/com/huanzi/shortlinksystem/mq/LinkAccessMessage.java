package com.huanzi.shortlinksystem.mq;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 短链访问埋点消息。
 * 这里只保留访问日志落库和 PV/UV 更新真正需要的字段，
 * 避免把整个请求对象或过多无关字段塞进 MQ 消息体。
 */
@Data
public class LinkAccessMessage {

    // 关联到哪条短链，consumer 用它更新 pv_count / uv_count。
    private Long linkId;
    // 既用于访问日志留痕，也用于 Redis UV key 组装。
    private String shortCode;
    private String originUrl;
    // 来自 Cookie 的访客标识，是当前基础版 UV 去重的核心字段。
    private String visitorId;
    private String userIp;
    private String userAgent;
    private String referer;
    // accessTime 用于访问明细，visitDate 用于按天统计 UV/PV 趋势。
    private LocalDateTime accessTime;
    private LocalDate visitDate;
}
