package com.jiuyu.replay.order.bean.impl.createOrder;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.order.bo.CreateOrderBo;
import com.jiuyu.replay.order.bo.OrderBo;

/**
 * 活动的订单-也是版本，只是当前版本月底不会重置资源
 */
public class CreateOrderActivity extends CreateOrderPackage{

    @Override
    public OrderBo saveOrder(CreateOrderBo createOrderBo) {
        OrderBo orderBo = super.saveOrder(createOrderBo);
        if (ObjectUtil.isNotEmpty(orderBo) && ObjectUtil.isNotEmpty(orderBo.getOrderDetailList())){
            orderBo.getOrderDetailList().forEach(orderDetailBo -> {
                orderDetailBo.setResetDate(orderBo.getStartDate());
//                orderDetailBo.setResetNum(resetNum);
//                orderDetailBo.setResetUnit(resetUnit);
                orderDetailBo.setNextReset(null);
            });
        }
        return orderBo;
    }

    @Override
    public void checkOrder(CreateOrderBo createOrderBo) {
        super.checkOrder(createOrderBo);
    }
}
