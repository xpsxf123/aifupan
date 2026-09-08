package com.jiuyu.replay.order.service.impl;

import com.jiuyu.replay.order.bo.CrmOrderChangedBo;
import com.jiuyu.replay.order.event.CrmOrderChangedEvent;
import com.jiuyu.replay.order.service.CrmOrderEventService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * CRM 订单事件发布服务实现
 */
@Service
@AllArgsConstructor
@Slf4j
public class CrmOrderEventServiceImpl implements CrmOrderEventService {

    private static final String LOG_PREFIX = "[CRM-ORDER-EVENT]";

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发布订单变化事件
     *
     * @param bo 订单变化参数
     */
    @Override
    public void publishOrderChangedEvent(CrmOrderChangedBo bo) {
        CrmOrderChangedEvent event = new CrmOrderChangedEvent(
                bo.getEventType(),
                bo.getOccurredAt(),
                bo.getUserId(),
                bo.getSalesId(),
                bo.getSalesPhone(),
                bo.getOrderId()
        );
        log.info("{} publish order changed event, eventType={}, userId={}, orderId={}",
                LOG_PREFIX, bo.getEventType(), bo.getUserId(), bo.getOrderId());
        applicationEventPublisher.publishEvent(event);
    }
}

