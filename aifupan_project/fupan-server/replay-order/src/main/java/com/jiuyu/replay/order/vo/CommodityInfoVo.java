package com.jiuyu.replay.order.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;

/**
 * 商品信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "商品信息项")
public class CommodityInfoVo extends CommodityVo implements Serializable {
	private static final long serialVersionUID = 1L;

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

//	@Schema(description = "套餐类型集合")
//	private List<TypeConsumptionVo> typeConsumptionList;

	@Schema(description = "商品类型信息")
	private CommodityTypeVo commodityType;

	@Schema(description = "商品价格列表")
	private List<CommodityPriceVo> commodityPriceList;

}
