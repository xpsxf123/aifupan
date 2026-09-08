package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 查询可以购买的套餐列表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "商品信息")
public class CanPurchasePackageBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "最小等级")
    private Integer minLevel;

    @Schema(description = "等级")
    private Integer level;

    @Schema(description = "套餐类型(用户只能购买主要套餐)：1主要套餐，2次要套餐")
    private Integer packageType;

    @Schema(description = "套餐id")
    private Long packageId;

    @Schema(description = "商品状态 0：未上架 1：已上架")
    private Integer status;
}
