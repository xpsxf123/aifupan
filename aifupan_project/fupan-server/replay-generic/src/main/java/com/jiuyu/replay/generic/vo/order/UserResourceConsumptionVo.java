package com.jiuyu.replay.generic.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户资源消耗统计VO
 *
 * @author AI Assistant
 */
@Data
@Schema(description = "用户资源消耗统计VO")
public class UserResourceConsumptionVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 商品类型ID
     */
    @Schema(description = "商品类型ID")
    private Long commodityTypeId;

    /**
     * 商品类型key
     */
    @Schema(description = "商品类型key")
    private String commodityTypeCode;

    /**
     * 商品名称
     */
    @Schema(description = "商品名称")
    private String commodityTypeName;

    /**
     * 商品单位
     */
    @Schema(description = "商品单位")
    private String commodityTypeUnit;

    /**
     * 消耗数量
     */
    @Schema(description = "消耗数量")
    private String consumptionQuantity;
}
