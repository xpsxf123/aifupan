package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 *  商品列表项(含类型)
 * @author lyw
 */
@Data
@AllArgsConstructor
@Schema(description = "商品列表项(含类型)")
public class CommodityTypeDataVo implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 商品类型id
     */
    @Schema(description = "商品类型id")
    private Long commodityTypeId;
    /**
     * 商品类型code
     */
    @Schema(description = "商品类型code")
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
     * 商品列表
     */
    @Schema(description = "商品列表")
    private List<CommodityInfoVo> commodityListVo;
}
