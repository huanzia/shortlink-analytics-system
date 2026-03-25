package com.huanzi.shortlinksystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 短链编辑入参对象。
 * 当前只开放对展示信息和过期时间的修改，不允许改 shortCode、shortUrl、originUrl。
 */
public class ShortLinkUpdateDTO {

    /** 目标短链ID，主要用于管理侧识别当前操作对象。 */
    private Long id;
    /** 修改后的标题。 */
    private String title;
    /** 修改后的描述。 */
    private String description;
    /** 修改后的过期时间。 */
    private LocalDateTime expireTime;
}
