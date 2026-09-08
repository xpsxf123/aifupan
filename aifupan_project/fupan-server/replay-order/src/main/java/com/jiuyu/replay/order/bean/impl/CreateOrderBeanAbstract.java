package com.jiuyu.replay.order.bean.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.order.bean.CreateOrder;
import com.jiuyu.replay.order.bo.CreateOrderBo;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;

public abstract class CreateOrderBeanAbstract implements CreateOrder {
    // 套餐重置时间
    public static int resetNum = 1;
    // 套餐重置时间单位 [0小时，1天，2月，3季度，4半年，5年](取下标)
    public static int resetUnit = 2;

    @Override
    public void checkOrder(CreateOrderBo createOrderBo) {
        if (ObjectUtil.isEmpty(createOrderBo.getUserId())) RRException.create("用户不能为空");
        if (ObjectUtil.isEmpty(createOrderBo.getOrderType())) RRException.create("订单类型不能为空");
        if (ObjectUtil.isEmpty(createOrderBo.getCommodityType())) RRException.create("商品类型不能为空");
        if (ObjectUtil.isEmpty(createOrderBo.getSource())) RRException.create("来源不能为空");
        if (ObjectUtil.isEmpty(createOrderBo.getDiscountRate())) createOrderBo.setDiscountRate(0);
        if (ObjectUtil.isEmpty(createOrderBo.getPayType())) createOrderBo.setPayType(-1);
    }

    @Override
    public boolean isUseByOrder(OrderInfoVo order) {
        return true;
    }
}
