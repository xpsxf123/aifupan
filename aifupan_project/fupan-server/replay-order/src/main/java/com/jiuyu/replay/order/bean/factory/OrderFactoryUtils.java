package com.jiuyu.replay.order.bean.factory;

import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.order.bean.CreateOrder;
import com.jiuyu.replay.order.bean.impl.createOrder.CreateOrderActivity;
import com.jiuyu.replay.order.bean.impl.createOrder.CreateOrderCommodityActivity;
import com.jiuyu.replay.order.bean.impl.createOrder.CreateOrderIncremental;
import com.jiuyu.replay.order.bean.impl.createOrder.CreateOrderPackage;

import java.util.Map;
import java.util.function.Supplier;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/7/16 上午10:16
 */
public class OrderFactoryUtils {

    public static CreateOrder getCreateOrder(Integer commodityType) {
        Map<Integer, Supplier<CreateOrder>> map = Map.of(
                0, CreateOrderIncremental::new,
                1, CreateOrderPackage::new,
                2, CreateOrderActivity::new,
                3, CreateOrderCommodityActivity::new
        );

        Supplier<CreateOrder> supplier = map.get(commodityType);
        if (supplier == null) {
            throw new BusinessException(StatusCode.TYPE_NOT_EXIST);
        }
        return supplier.get();  // 调用无参构造方法创建实例
    }

}
