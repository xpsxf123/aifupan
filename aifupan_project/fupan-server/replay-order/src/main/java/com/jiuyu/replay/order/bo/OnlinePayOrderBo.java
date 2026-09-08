package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建客户端订单bo
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@Schema(description = "创建在线支付订单bo")
public class OnlinePayOrderBo  implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品id(购买的是套餐：packageId，购买的是增量包：incrementId)")
    private Long commodityId;

    @Schema(description = "商品价格id，选择套餐要传")
    private Long commodityPriceId;

    @Schema(description = "支付方式 0微信，1支付宝")
    private Integer payType;

    @Schema(description = "当前用户的版本等级")
    private Integer level;
}