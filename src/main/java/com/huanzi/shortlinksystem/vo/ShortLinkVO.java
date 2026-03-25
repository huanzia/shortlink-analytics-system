package com.huanzi.shortlinksystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 短链详情返回对象。
 * 主要用于管理侧列表、详情、编辑后返回，包含短链基础信息和累计统计信息。
 */
public class ShortLinkVO {

    /** 短链主键ID。 */
    private Long id;
    /** 创建该短链的用户ID。 */
    private Long userId;
    /** 短码本体。 */
    private String shortCode;
    /** 完整短链地址。 */
    private String shortUrl;
    /** 原始长链接。 */
    private String originUrl;
    /** 短链标题。 */
    private String title;
    /** 短链描述。 */
    private String description;
    /** 状态值：1启用，0禁用，2预留过期态，3删除态。 */
    private Integer status;
    /** 累计访问量 PV。 */
    private Long pvCount;
    /** 累计独立访客 UV。 */
    private Long uvCount;

    /** 过期时间，为空表示不过期。 */
    @Schema(description = "过期时间", example = "2026-12-31 23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;

    /** 创建时间。 */
    @Schema(description = "创建时间", example = "2026-03-24 22:37:55")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间。 */
    @Schema(description = "更新时间", example = "2026-03-24 22:37:55")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
