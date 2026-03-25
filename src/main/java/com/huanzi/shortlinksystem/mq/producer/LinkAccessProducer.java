package com.huanzi.shortlinksystem.mq.producer;

import com.huanzi.shortlinksystem.mq.LinkAccessMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.huanzi.shortlinksystem.constant.MqConstants.LINK_ACCESS_EXCHANGE;
import static com.huanzi.shortlinksystem.constant.MqConstants.LINK_ACCESS_ROUTING_KEY;

/**
 * 访问埋点消息生产者。
 * 作用是把“跳转成功后的统计工作”异步化，避免主链路同步写日志和改统计字段。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LinkAccessProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送访问埋点消息。
     * 发送失败时只记录日志，不能反向影响短链跳转主流程。
     */
    public void send(LinkAccessMessage payload) {
        try {
            rabbitTemplate.convertAndSend(LINK_ACCESS_EXCHANGE, LINK_ACCESS_ROUTING_KEY, payload);
        } catch (Exception exception) {
            log.warn("Failed to send link access message, shortCode={}", payload.getShortCode(), exception);
        }
    }
}
