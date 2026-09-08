package com.jiuyu.replay.common.utils;

import java.util.Map;

/**
 * 订单的Utils
 */
public class OrderUtils {

    public static String getCreateOrderBeanName(Integer commodityType) {
        Map<Integer, String> map = Map.of(
                0, "createOrderIncremental",
                1, "createOrderPackage",
                2, "createOrderActivity",
                3, "createOrderCommodityActivity"
        );
        return map.get(commodityType);
    }

}