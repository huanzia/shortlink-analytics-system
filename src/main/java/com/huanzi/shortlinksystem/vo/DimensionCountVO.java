package com.huanzi.shortlinksystem.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
/**
 * 单个维度项的计数结果。
 * 适用于浏览器、操作系统、referer 等聚合结果里的“名称 + 数量”结构。
 */
public class DimensionCountVO {

    /** 维度名称，例如 Chrome、Windows、DIRECT。 */
    private String name;
    /** 当前维度值对应的访问次数。 */
    private Long count;
}
