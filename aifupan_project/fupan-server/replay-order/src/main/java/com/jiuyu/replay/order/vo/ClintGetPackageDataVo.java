package com.jiuyu.replay.order.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "详细资产")
public class ClintGetPackageDataVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Schema(description = "id")
    private Long id;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
    /**
     * 父资产类型总明细id
     */
    @Schema(description = "父资产类型总明细id")
    private Long parentId;
    /**
     * 用户资产id
     */
    @Schema(description = "用户资产id")
    private Long propertyId;
    /**
     * 商品类型id
     */
    @Schema(description = "商品类型id")
    private Long commodityTypeId;
    /**
     * 商品类型key
     */
    @Schema(description = "商品类型key")
    private String commodityTypeCode;
    /**
     * 商品类型名称
     */
    @Schema(description = "商品类型名称")
    private String commodityTypeName;
    /**
     * 商品类型单位
     */
    @Schema(description = "商品类型单位")
    private String commodityTypeUnit;

    /**
     * 商品是否要月底清零 0否，1是
     */
    @Schema(description = "商品是否要月底清零 0否，1是")
    private Integer commodityTypeReset;

    /**
     * 使用数量
     */
    @Schema(description = "使用数量")
    private BigDecimal useQuantity;
    /**
     * 剩余使用数量
     */
    @Schema(description = "剩余使用数量")
    private BigDecimal remUseQuantity;
    /**
     * 总数量
     */
    @Schema(description = "总数量")
    private BigDecimal totalQuantity;
    @Schema(description = "增量包使用数量")
    private BigDecimal incUseQuantity;
    @Schema(description = "增量包剩余使用数量")
    private BigDecimal incRemUseQuantity;
    @Schema(description = "增量包总数量")
    private BigDecimal incTotalQuantity;
    @Schema(description = "增量包")
    private List<ClintGetDataVo>  incPackageList;

    @Schema(description = "套餐使用数量")
    private BigDecimal setMenuUseQuantity;
    @Schema(description = "套餐剩余使用数量")
    private BigDecimal setMenuRemUseQuantity;
    @Schema(description = "套餐总数量")
    private BigDecimal setMenuTotalQuantity;
    @Schema(description = "套餐")
    private List<ClintGetDataVo>  setMenuList;

    @Schema(description = "总数量")
    private List<ClintGetDataVo>  allInnerList;
}
