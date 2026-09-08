package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 版本增量包信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-09 16:13:50
 */
@Data
@Schema(description = "版本增量包信息")
public class VersionIncrementBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Schema(description = "id")
    private Long id;

    /**
     * 版本id
     */
    @Schema(description = "版本id")
    private Long packageId;

    /**
     * 商品id
     */
    @Schema(description = "商品id [必填]")
    @NotNull(message = "商品id不能为空")
    private Long commodityId;

    /**
     * 商品价格id
     */
    @Schema(description = "商品价格id")
    private Long commodityPriceId;

    /**
     * 价格
     */
    @Schema(description = "价格 [必填]")
    @NotNull(message = "价格不能为空")
    private BigDecimal originalPrice;

    /**
     * 折扣
     */
    @Schema(description = "折扣 [必填]")
    @NotNull(message = "折扣不能为空")
    private BigDecimal discount;

    /**
     * 折扣价
     */
    @Schema(description = "折扣价 [必填]")
    @NotNull(message = "折扣价不能为空")
    private BigDecimal realPrice;

    /**
     * 状态 0未上架，1已上架
     */
    @Schema(description = "状态 0未上架，1已上架")
    private Integer status;
}

