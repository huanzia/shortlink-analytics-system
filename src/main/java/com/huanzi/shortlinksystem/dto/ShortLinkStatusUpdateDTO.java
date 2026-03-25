package com.huanzi.shortlinksystem.dto;

import lombok.Data;

@Data
/**
 * 短链状态更新入参对象。
 * 当前只支持启用和禁用两种显式状态变更。
 */
public class ShortLinkStatusUpdateDTO {

    /** 目标状态值：1启用，0禁用。 */
    private Integer status;
}
