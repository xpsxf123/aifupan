package com.jiuyu.replay.words.bo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(name = "直播商品信息")
public class LiveProductBo implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 商品价格
     */
    @Schema(description = "商品价格")
    private BigDecimal price;

    /**
     * 每单金额
     */
    @Schema(description = "每单金额")
    private BigDecimal perOrderAmount;

    /**
     * 购买次数
     */
    @Schema(description = "购买次数")
    private Integer purchaseCount;

    /**
     * 销量范围文本
     */
    @Schema(description = "销量范围文本")
    private String volumeText;

    /**
     * 金额范围文本
     */
    @Schema(description = "金额范围文本")
    private String amountText;
    /**
     * 客单价范围文本
     */
    @Schema(description = "客单价范围文本")
    private String perOrderAmountText;
}
