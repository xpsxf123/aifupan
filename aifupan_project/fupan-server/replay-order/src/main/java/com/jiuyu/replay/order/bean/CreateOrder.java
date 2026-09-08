package com.jiuyu.replay.order.bean;

import com.jiuyu.replay.order.bo.CreateOrderBo;
import com.jiuyu.replay.order.bo.OrderBo;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;

/**
 * 创建订单
 */
public interface CreateOrder {

    /**
     * 创建订单--没有保存到数据库
     * @param createOrderBo
     * @return
     */
    OrderBo saveOrder(CreateOrderBo createOrderBo);

    /**
     * 校验订单
     * @param createOrderBo
     */
    void checkOrder(CreateOrderBo createOrderBo);

    /**
     * 判断订单是否可以使用
     * @param order
     * @return
     */
    boolean isUseByOrder(OrderInfoVo order);
}
