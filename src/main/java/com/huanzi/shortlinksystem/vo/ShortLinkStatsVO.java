package com.huanzi.shortlinksystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 短链基础统计返回对象。
 * 对应 /stats/link/{id}，用于展示单条短链当前的基础信息和累计 PV/UV 结果。
 */
public class ShortLinkStatsVO {

    /** 短链主键ID。 */
    private Long id;
    /** 短码。 */
    private String shortCode;
    /** 完整短链地址。 */
    private String shortUrl;
    /** 原始长链接。 */
    private String originUrl;
    /** 短链标题。 */
    private String title;
    /** 当前状态值。 */
    private Integer status;

    /** 过期时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;

    /** 累计访问量 PV。 */
    private Long pvCount;
    /** 累计独立访客 UV。 */
    private Long uvCount;

    /** 创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
