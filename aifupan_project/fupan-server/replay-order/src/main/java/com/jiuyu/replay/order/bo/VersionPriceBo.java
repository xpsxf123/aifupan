package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 版本价格体系信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "版本价格体系信息")
public class VersionPriceBo implements Serializable {
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
    private Long commodityId;

    /**
     * 有效期值
     */
    @Schema(description = "有效期值 [必填]")
    @NotNull(message = "有效期值不能为空")
    private Integer validityNum;

    /**
     * 有效期单位 0：小时 1：天 2：月 3：季度 4：半年，5：年
     */
    @Schema(description = "有效期单位 0：小时 1：天 2：月 3：季度 4：半年，5：年 [必填]")
    @NotNull(message = "有效期单位不能为空")
    private Integer validityUnit;

    /**
     * 原价
     */
    @Schema(description = "原价 [必填]")
    @NotNull(message = "原价不能为空")
    private Integer originalPrice;

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
    private Integer realPrice;

    /**
     * 官网显示 0：不显示 1：显示
     */
    @Schema(description = "官网显示 0：不显示 1：显示 [必填]")
    @NotNull(message = "官网显示状态不能为空")
    private Integer showStatus;

    /**
     * 试用 0：不是 1：是
     */
    @Schema(description = "试用 0：不是 1：是 [必填]")
    @NotNull(message = "试用状态不能为空")
    private Integer trialVersion;
}

