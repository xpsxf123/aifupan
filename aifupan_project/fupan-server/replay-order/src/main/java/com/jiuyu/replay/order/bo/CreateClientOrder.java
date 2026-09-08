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
@Schema(description = "创建客户端订单bo")
public class CreateClientOrder  implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "商品id(购买的是套餐：packageId，购买的是增量包：incrementId)")
    private Long commodityId;

    @Schema(description = "商品价格id，选择套餐要传")
    private Long commodityPriceId;

    @Schema(description = "商品类型 0增量包，1正常版本(月底资源重置)，2活动版本(月底资源不重置)，3邀请活动订单")
    private Integer commodityType;

    @Schema(description = "优惠价格（主要用于套餐升级产生的差价，和折扣没有关系）(单个商品)")
    private Integer discountRate;

    @Schema(description = "支付方式 0微信，1支付宝")
    private Integer payType;

    @Schema(description = "升级前的订单id(用于套餐升级)")
    private Long beforeUpgrading;

    @Schema(description = "是否是试用订单 0否，1是")
    private Integer trialOrder;

    @Schema(description = "支付凭证图片, 多个图片用逗号分开")
    private String payPictures;
}
