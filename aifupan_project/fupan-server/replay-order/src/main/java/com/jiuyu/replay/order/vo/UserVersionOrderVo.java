package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户资产类型总明细信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Data
@Schema(description = "用户资产类型总明细信息")
public class UserVersionOrderVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品名称")
    private String commodityName;

    @Schema(description = "订单生效时间")
    private Date startDate;

    @Schema(description = "订单过期时间")
    private Date endDate;

    @Schema(description = "总订单的天数")
    private Integer totalDay;

    @Schema(description = "剩余的天数")
    private Integer surplusDay;

    @Schema(description = "版本能抵扣的价格")
    private Integer surplusAmount;

    @Schema(description = "升级等级")
    private Integer level;

    @Schema(description = "原价格，单位：分(单个商品)")
    private Integer originalPrice;

    @Schema(description = "优惠价格（主要用于套餐升级产生的差价，和折扣没有关系）(单个商品)")
    private Integer discountRate;

    @Schema(description = "真实价格，单位：分")
    private Integer realPrice;

    @Schema(description = "版本在官网的图标")
    private String packageLogoUrl;
}
