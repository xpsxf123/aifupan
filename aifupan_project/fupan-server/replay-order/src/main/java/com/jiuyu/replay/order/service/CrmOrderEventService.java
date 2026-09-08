package com.jiuyu.replay.order.service;

import com.jiuyu.replay.order.bo.CrmOrderChangedBo;

/**
 * CRM 订单事件发布服务
 * 负责在订单关键节点发布 Spring 事件，供出站监听器消费。
 */
public interface CrmOrderEventService {

    /**
     * 发布订单变化事件
     *
     * @param bo 订单变化参数
     */
    void publishOrderChangedEvent(CrmOrderChangedBo bo);
}

