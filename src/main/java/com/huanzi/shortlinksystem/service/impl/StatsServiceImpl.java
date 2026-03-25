package com.huanzi.shortlinksystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huanzi.shortlinksystem.common.exception.BizException;
import com.huanzi.shortlinksystem.entity.LinkAccessLog;
import com.huanzi.shortlinksystem.entity.ShortLink;
import com.huanzi.shortlinksystem.enums.LinkStatusEnum;
import com.huanzi.shortlinksystem.mapper.LinkAccessLogMapper;
import com.huanzi.shortlinksystem.mapper.ShortLinkMapper;
import com.huanzi.shortlinksystem.service.StatsService;
import com.huanzi.shortlinksystem.vo.DimensionCountVO;
import com.huanzi.shortlinksystem.vo.HotLinkVO;
import com.huanzi.shortlinksystem.vo.LinkAccessRecordVO;
import com.huanzi.shortlinksystem.vo.LinkDimensionsVO;
import com.huanzi.shortlinksystem.vo.LinkTrendVO;
import com.huanzi.shortlinksystem.vo.ShortLinkStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * 统计查询核心实现。
 * 统计口径说明：
 * - 基础统计和热门排行主要看 tb_short_link 上已经累加好的 pv_count / uv_count
 * - 趋势和维度统计主要基于 tb_link_access_log 明细数据聚合
 * - 当前统一按“固定 userId=1 且排除删除态”控制可见范围
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private static final Long DEFAULT_USER_ID = 1L;
    private static final String UNKNOWN = "UNKNOWN";
    private static final String DIRECT = "DIRECT";

    private final ShortLinkMapper shortLinkMapper;
    private final LinkAccessLogMapper linkAccessLogMapper;

    @Override
    public ShortLinkStatsVO getShortLinkStats(Long shortLinkId) {
        // 先做可见性校验，避免统计接口绕过管理侧边界。
        ShortLink shortLink = getVisibleLink(shortLinkId);

        ShortLinkStatsVO statsVO = new ShortLinkStatsVO();
        statsVO.setId(shortLink.getId());
        statsVO.setShortCode(shortLink.getShortCode());
        statsVO.setShortUrl(shortLink.getShortUrl());
        statsVO.setOriginUrl(shortLink.getOriginUrl());
        statsVO.setTitle(shortLink.getTitle());
        statsVO.setStatus(shortLink.getStatus());
        statsVO.setExpireTime(shortLink.getExpireTime());
        statsVO.setPvCount(shortLink.getPvCount());
        statsVO.setUvCount(shortLink.getUvCount());
        statsVO.setCreateTime(shortLink.getCreateTime());
        return statsVO;
    }

    @Override
    public List<LinkAccessRecordVO> getAccessRecords(Long shortLinkId) {
        // 访问日志虽然来自明细表，但仍以“当前用户可见短链”为前置条件。
        getVisibleLink(shortLinkId);

        LambdaQueryWrapper<LinkAccessLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LinkAccessLog::getLinkId, shortLinkId)
                .orderByDesc(LinkAccessLog::getAccessTime)
                .last("limit 10");

        return linkAccessLogMapper.selectList(queryWrapper)
                .stream()
                .map(this::toAccessRecordVO)
                .toList();
    }

    @Override
    public List<HotLinkVO> getHotLinks() {
        // 当前排行口径：
        // 1. 仅看当前 userId=1
        // 2. 过滤删除态
        // 3. 禁用态和过期态仍参与排行，便于管理侧查看历史数据
        LambdaQueryWrapper<ShortLink> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShortLink::getUserId, DEFAULT_USER_ID)
                .ne(ShortLink::getStatus, LinkStatusEnum.DELETED.getCode())
                .orderByDesc(ShortLink::getPvCount)
                .orderByDesc(ShortLink::getId)
                .last("limit 10");

        return shortLinkMapper.selectList(queryWrapper)
                .stream()
                .map(this::toHotLinkVO)
                .toList();
    }

    @Override
    public List<LinkTrendVO> getLinkTrend(Long shortLinkId) {
        getVisibleLink(shortLinkId);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);

        LambdaQueryWrapper<LinkAccessLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LinkAccessLog::getLinkId, shortLinkId)
                .ge(LinkAccessLog::getVisitDate, startDate)
                .le(LinkAccessLog::getVisitDate, endDate)
                .orderByAsc(LinkAccessLog::getVisitDate);

        List<LinkAccessLog> accessLogs = linkAccessLogMapper.selectList(queryWrapper);
        Map<LocalDate, List<LinkAccessLog>> dailyLogs = accessLogs.stream()
                .collect(Collectors.groupingBy(LinkAccessLog::getVisitDate));

        // 即使某天没有数据，也补 0 返回，方便前端画连续趋势图。
        return LongStream.range(0, 7)
                .mapToObj(startDate::plusDays)
                .map(date -> toTrendVO(date, dailyLogs.getOrDefault(date, List.of())))
                .toList();
    }

    @Override
    public LinkDimensionsVO getLinkDimensions(Long shortLinkId) {
        getVisibleLink(shortLinkId);

        LambdaQueryWrapper<LinkAccessLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LinkAccessLog::getLinkId, shortLinkId);
        List<LinkAccessLog> accessLogs = linkAccessLogMapper.selectList(queryWrapper);

        // 这里是基础版聚合实现，直接基于明细表在内存里做分组统计，
        // 优点是直观；缺点是数据量更大时要下推到 SQL 或离线统计链路。
        LinkDimensionsVO dimensionsVO = new LinkDimensionsVO();
        dimensionsVO.setBrowsers(groupCounts(accessLogs.stream()
                .map(LinkAccessLog::getBrowser)
                .map(this::normalizeUnknown)
                .toList()));
        dimensionsVO.setOsList(groupCounts(accessLogs.stream()
                .map(LinkAccessLog::getOs)
                .map(this::normalizeUnknown)
                .toList()));
        dimensionsVO.setReferers(groupCounts(accessLogs.stream()
                .map(LinkAccessLog::getReferer)
                .map(this::normalizeReferer)
                .toList()));
        return dimensionsVO;
    }

    private LinkAccessRecordVO toAccessRecordVO(LinkAccessLog accessLog) {
        LinkAccessRecordVO recordVO = new LinkAccessRecordVO();
        recordVO.setId(accessLog.getId());
        recordVO.setVisitorId(accessLog.getVisitorId());
        recordVO.setUserIp(accessLog.getUserIp());
        recordVO.setUserAgent(accessLog.getUserAgent());
        recordVO.setReferer(accessLog.getReferer());
        recordVO.setAccessTime(accessLog.getAccessTime());
        recordVO.setVisitDate(accessLog.getVisitDate());
        return recordVO;
    }

    private HotLinkVO toHotLinkVO(ShortLink shortLink) {
        HotLinkVO hotLinkVO = new HotLinkVO();
        hotLinkVO.setId(shortLink.getId());
        hotLinkVO.setShortCode(shortLink.getShortCode());
        hotLinkVO.setShortUrl(shortLink.getShortUrl());
        hotLinkVO.setOriginUrl(shortLink.getOriginUrl());
        hotLinkVO.setTitle(shortLink.getTitle());
        hotLinkVO.setPvCount(shortLink.getPvCount());
        hotLinkVO.setUvCount(shortLink.getUvCount());
        hotLinkVO.setCreateTime(shortLink.getCreateTime());
        return hotLinkVO;
    }

    private LinkTrendVO toTrendVO(LocalDate date, List<LinkAccessLog> dailyLogs) {
        LinkTrendVO trendVO = new LinkTrendVO();
        trendVO.setDate(date);
        trendVO.setPvCount((long) dailyLogs.size());
        // 趋势里的 UV 口径是“当天 visitorId 去重后的人数”。
        trendVO.setUvCount(dailyLogs.stream()
                .map(LinkAccessLog::getVisitorId)
                .filter(visitorId -> visitorId != null && !visitorId.isBlank())
                .distinct()
                .count());
        return trendVO;
    }

    private List<DimensionCountVO> groupCounts(List<String> values) {
        // 统一按数量倒序、名称次排序，保证接口返回顺序稳定，便于演示和对比。
        return values.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new DimensionCountVO(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String normalizeUnknown(String value) {
        return value == null || value.isBlank() ? UNKNOWN : value;
    }

    private String normalizeReferer(String value) {
        // referer 为空通常表示直接访问，因此单独归并成 DIRECT，更方便理解。
        return value == null || value.isBlank() ? DIRECT : value;
    }

    private ShortLink getVisibleLink(Long shortLinkId) {
        // 统计接口当前和管理接口保持同一可见范围：
        // 固定 userId=1，并且排除删除态。
        LambdaQueryWrapper<ShortLink> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShortLink::getId, shortLinkId)
                .eq(ShortLink::getUserId, DEFAULT_USER_ID)
                .ne(ShortLink::getStatus, LinkStatusEnum.DELETED.getCode())
                .last("limit 1");
        ShortLink shortLink = shortLinkMapper.selectOne(queryWrapper);
        if (shortLink == null) {
            throw new BizException("short link not found");
        }
        return shortLink;
    }
}
