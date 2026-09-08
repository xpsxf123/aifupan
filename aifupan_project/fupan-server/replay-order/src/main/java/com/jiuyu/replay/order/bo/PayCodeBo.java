package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "获取支付二维码参数")
public class PayCodeBo {

    /**
     * 支付方式 0：微信 1：支付宝
     */
    @Schema(description = "支付方式 0：微信 1：支付宝")
    private Integer payType;
    /**
     * 订单id
     */
    @Schema(description = "订单id")
    private Long orderId;
}
