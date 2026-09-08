package com.jiuyu.replay.third.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
/**
 * qps调用记录表信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-08 16:27:43
 */
@Data
@Schema(description = "查询订单支付状态")
public class QueryOrderStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单id")
    private Long orderId;

    @Schema(description = "支付状态， 0支付成功, 1转入退款， 2未支付， 3已关闭， 4已撤销（刷卡支付），5用户支付中，6支付失败(其他原因，如银行返回失败)")
    private Integer status;

    @Schema(description = "支付流水号")
    private String transactionId;

    @Schema(description = "支付的信息")
    private String jsonString;
}
