package com.huanzi.shortlinksystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
/**
 * 短链趋势统计返回对象。
 * 对应 /stats/link/{id}/trend，表示某一天的 PV / UV 统计值。
 */
public class LinkTrendVO {

    /** 趋势日期，格式为 yyyy-MM-dd。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    /** 当天访问量 PV。 */
    private Long pvCount;
    /** 当天独立访客 UV。 */
    private Long uvCount;
}
