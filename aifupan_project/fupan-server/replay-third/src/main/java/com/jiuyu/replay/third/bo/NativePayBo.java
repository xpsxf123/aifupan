package com.jiuyu.replay.third.bo;

import lombok.Data;

@Data
public class NativePayBo {

    /**
     * 支付方式 0：微信 1：支付宝
     */
    private Integer payType;
    /**
     * 订单id
     */
    private Long orderId;
    /**
     * 金额，单位：分
     */
    private Integer money;
    /**
     * 订单标题
     */
    private String title;

    /**
     * 回调地址
     */
    private String callbackAddress;
}
