package com.huanzi.shortlinksystem.vo;

import lombok.Data;

import java.util.List;

@Data
/**
 * 短链维度聚合返回对象。
 * 对应 /stats/link/{id}/dimensions，当前包含浏览器、操作系统、来源页面三个维度。
 */
public class LinkDimensionsVO {

    /** 按 browser 聚合后的统计列表。 */
    private List<DimensionCountVO> browsers;
    /** 按 os 聚合后的统计列表。 */
    private List<DimensionCountVO> osList;
    /** 按 referer 聚合后的统计列表。 */
    private List<DimensionCountVO> referers;
}
