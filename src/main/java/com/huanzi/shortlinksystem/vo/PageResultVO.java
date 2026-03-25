package com.huanzi.shortlinksystem.vo;

import lombok.Data;

import java.util.List;

@Data
/**
 * 通用分页返回对象。
 * 当前主要用于“我的短链”分页查询，后续其他列表接口也可以复用。
 */
public class PageResultVO<T> {

    /** 当前页数据列表。 */
    private List<T> records;
    /** 满足条件的总记录数。 */
    private Long total;
    /** 当前页码，从 1 开始。 */
    private Long pageNum;
    /** 当前每页大小。 */
    private Long pageSize;
}
