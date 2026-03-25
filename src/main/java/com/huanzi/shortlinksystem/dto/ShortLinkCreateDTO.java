package com.huanzi.shortlinksystem.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
/**
 * 短链创建入参对象。
 * 用于 /link/create，请求方传入原始链接和基础描述信息，由系统生成 shortCode 和 shortUrl。
 */
public class ShortLinkCreateDTO {

    /** 原始长链接，是最终跳转目标。 */
    private String originUrl;
    /** 短链标题，便于管理列表展示。 */
    private String title;
    /** 短链描述，用于区分业务用途。 */
    private String description;

    /** 过期时间，达到该时间后短链不再允许跳转。 */
    @Schema(description = "过期时间，格式为 yyyy-MM-dd HH:mm:ss", example = "2026-12-31 23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
