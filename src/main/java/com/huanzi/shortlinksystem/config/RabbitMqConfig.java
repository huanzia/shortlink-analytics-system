package com.huanzi.shortlinksystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.huanzi.shortlinksystem.constant.MqConstants.LINK_ACCESS_EXCHANGE;
import static com.huanzi.shortlinksystem.constant.MqConstants.LINK_ACCESS_QUEUE;
import static com.huanzi.shortlinksystem.constant.MqConstants.LINK_ACCESS_ROUTING_KEY;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange linkAccessExchange() {
        return new DirectExchange(LINK_ACCESS_EXCHANGE, true, false);
    }

    @Bean
    public Queue linkAccessQueue() {
        return new Queue(LINK_ACCESS_QUEUE, true);
    }

    @Bean
    public Binding linkAccessBinding(Queue linkAccessQueue, DirectExchange linkAccessExchange) {
        return BindingBuilder.bind(linkAccessQueue).to(linkAccessExchange).with(LINK_ACCESS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter(new ObjectMapper().findAndRegisterModules());
    }
}
