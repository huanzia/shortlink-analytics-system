package com.huanzi.shortlinksystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
/**
 * 最近访问记录返回对象。
 * 对应 /stats/link/{id}/access-records，用于查看短链最近的访问明细。
 */
public class LinkAccessRecordVO {

    /** 访问日志主键ID。 */
    private Long id;
    /** 访客标识，用于区分是否为同一浏览器/访客。 */
    private String visitorId;
    /** 访问来源IP。 */
    private String userIp;
    /** 原始 User-Agent。 */
    private String userAgent;
    /** 来源页面。 */
    private String referer;

    /** 精确访问时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime accessTime;

    /** 访问所属日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;
}
