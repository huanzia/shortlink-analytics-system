package com.huanzi.shortlinksystem.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huanzi.shortlinksystem.entity.LinkAccessLog;
import com.huanzi.shortlinksystem.entity.ShortLink;
import com.huanzi.shortlinksystem.manager.StatsManager;
import com.huanzi.shortlinksystem.mapper.LinkAccessLogMapper;
import com.huanzi.shortlinksystem.mapper.ShortLinkMapper;
import com.huanzi.shortlinksystem.mq.LinkAccessMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.huanzi.shortlinksystem.constant.MqConstants.LINK_ACCESS_QUEUE;

/**
 * 访问埋点消息消费者。
 * 当前消费流程是：
 * 1. 写入访问日志
 * 2. pv_count + 1
 * 3. 基于 Redis Set 判断是否为当天首次访问
 * 4. 如果是首次访问，再 uv_count + 1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LinkAccessConsumer {

    private final LinkAccessLogMapper linkAccessLogMapper;
    private final ShortLinkMapper shortLinkMapper;
    private final StatsManager statsManager;

    /**
     * 消费访问消息，落访问日志并更新短链 PV/UV。
     * 当前不做复杂幂等，后续可基于消息日志表继续扩展。
     */
    @RabbitListener(queues = LINK_ACCESS_QUEUE)
    public void consume(LinkAccessMessage payload) {
        // 先落访问明细，后续趋势统计、访问记录查询、维度聚合都依赖这张表。
        LinkAccessLog accessLog = new LinkAccessLog();
        accessLog.setLinkId(payload.getLinkId());
        accessLog.setShortCode(payload.getShortCode());
        accessLog.setVisitorId(payload.getVisitorId());
        accessLog.setUserIp(payload.getUserIp());
        accessLog.setUserAgent(payload.getUserAgent());
        accessLog.setReferer(payload.getReferer());
        accessLog.setAccessTime(payload.getAccessTime());
        accessLog.setVisitDate(payload.getVisitDate());
        linkAccessLogMapper.insert(accessLog);

        // PV 是总访问次数，每次有效跳转消费到这里都应累加。
        LambdaUpdateWrapper<ShortLink> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ShortLink::getId, payload.getLinkId())
                .setSql("pv_count = pv_count + 1");
        shortLinkMapper.update(null, updateWrapper);

        // UV 是按“短码 + 日期 + visitorId”去重后的独立访客数，
        // 只有 Redis Set 判断为当天首次访问时才累加。
        if (statsManager.recordUv(payload.getShortCode(), payload.getVisitorId(), payload.getVisitDate())) {
            LambdaUpdateWrapper<ShortLink> uvUpdateWrapper = new LambdaUpdateWrapper<>();
            uvUpdateWrapper.eq(ShortLink::getId, payload.getLinkId())
                    .setSql("uv_count = uv_count + 1");
            shortLinkMapper.update(null, uvUpdateWrapper);
        }

        log.info("Consumed link access message, linkId={}, shortCode={}, visitorId={}",
                payload.getLinkId(), payload.getShortCode(), payload.getVisitorId());
    }
}
