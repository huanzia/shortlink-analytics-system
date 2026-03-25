package com.huanzi.shortlinksystem.controller;

import com.huanzi.shortlinksystem.common.result.Result;
import com.huanzi.shortlinksystem.service.StatsService;
import com.huanzi.shortlinksystem.vo.HotLinkVO;
import com.huanzi.shortlinksystem.vo.LinkAccessRecordVO;
import com.huanzi.shortlinksystem.vo.LinkDimensionsVO;
import com.huanzi.shortlinksystem.vo.LinkTrendVO;
import com.huanzi.shortlinksystem.vo.ShortLinkStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计查询接口。
 * 这里承载的是“读统计结果”，不参与 PV/UV 的写入更新；
 * 真正的统计累加是在 MQ 消费链路中完成的。
 */
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * 查询单条短链的基础统计快照，主要来自主表聚合字段。
     */
    @GetMapping("/link/{id}")
    public Result<ShortLinkStatsVO> getStats(@PathVariable Long id) {
        return Result.success(statsService.getShortLinkStats(id));
    }

    /**
     * 查询最近访问记录，便于直观看到访问日志是否已落库。
     */
    @GetMapping("/link/{id}/access-records")
    public Result<List<LinkAccessRecordVO>> getAccessRecords(@PathVariable Long id) {
        return Result.success(statsService.getAccessRecords(id));
    }

    /**
     * 查询最近 7 天的 PV/UV 趋势。
     */
    @GetMapping("/link/{id}/trend")
    public Result<List<LinkTrendVO>> getLinkTrend(@PathVariable Long id) {
        return Result.success(statsService.getLinkTrend(id));
    }

    /**
     * 查询 browser / os / referer 基础维度聚合结果。
     */
    @GetMapping("/link/{id}/dimensions")
    public Result<LinkDimensionsVO> getLinkDimensions(@PathVariable Long id) {
        return Result.success(statsService.getLinkDimensions(id));
    }

    /**
     * 查询热门短链排行。
     */
    @GetMapping("/hot-links")
    public Result<List<HotLinkVO>> getHotLinks() {
        return Result.success(statsService.getHotLinks());
    }
}
