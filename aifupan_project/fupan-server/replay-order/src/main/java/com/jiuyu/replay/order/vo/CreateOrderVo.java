package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建订单成功后返回
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "创建订单成功后返回")
public class CreateOrderVo {

    @Schema(description = "商品名称")
    private String title;

    @Schema(description = "订单id")
    private Long orderId;

    @Schema(description = "支付订单id")
    private Long orderPayId;

    @Schema(description = "支付二维码")
    private String urlCode;

    @Schema(description = "支付金额，单位：分")
    private Integer payMoney;

    @Schema(description = "支付类型 0：微信支付 1：支付宝支付")
    private Integer payType;
}
