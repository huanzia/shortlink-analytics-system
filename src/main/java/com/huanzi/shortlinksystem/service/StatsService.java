package com.huanzi.shortlinksystem.service;

import com.huanzi.shortlinksystem.vo.HotLinkVO;
import com.huanzi.shortlinksystem.vo.LinkTrendVO;
import com.huanzi.shortlinksystem.vo.LinkDimensionsVO;
import com.huanzi.shortlinksystem.vo.ShortLinkStatsVO;
import com.huanzi.shortlinksystem.vo.LinkAccessRecordVO;

import java.util.List;

/**
 * 统计查询服务接口。
 * 只负责“查”，不负责累加统计；
 * PV/UV 的更新发生在访问埋点消息的消费端。
 */
public interface StatsService {

    /**
     * 查询短链基础统计。
     */
    ShortLinkStatsVO getShortLinkStats(Long shortLinkId);

    /**
     * 查询最近访问记录。
     */
    List<LinkAccessRecordVO> getAccessRecords(Long shortLinkId);

    /**
     * 查询热门短链排行。
     */
    List<HotLinkVO> getHotLinks();

    /**
     * 查询最近 7 天趋势。
     */
    List<LinkTrendVO> getLinkTrend(Long shortLinkId);

    /**
     * 查询基础维度聚合。
     */
    LinkDimensionsVO getLinkDimensions(Long shortLinkId);
}
