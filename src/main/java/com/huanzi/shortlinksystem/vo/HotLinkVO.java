package com.huanzi.shortlinksystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 热门短链排行项返回对象。
 * 对应 /stats/hot-links，当前按 pv_count 倒序返回 Top N 短链。
 */
public class HotLinkVO {

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
    /** 累计访问量 PV，用作当前热门排行主排序字段。 */
    private Long pvCount;
    /** 累计独立访客 UV。 */
    private Long uvCount;

    /** 创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
