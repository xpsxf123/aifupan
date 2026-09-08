package com.jiuyu.replay.order.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "order")
public class OrderProperties {

    /**
     * 订单超时标识redis前缀
     */
    private String orderTimeoutRedisKey;
}
