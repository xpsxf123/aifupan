package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品类型用量关联表信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "商品类型用量关联表信息")
public class TypeConsumptionVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
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
	 * 商品类型是否重置用量 0否，1是
	 */
	@Schema(description = "商品类型是否重置用量 0否，1是")
	private Integer commodityTypeReset;
	/**
	 * 来源id type为0字段为商品id，为1字段为订单详情id
	 */
	@Schema(description = "来源id type为0字段为商品id，为1字段为订单详情id")
	private Long sourceId;
	/**
	 * 来源类型 0商品，1订单详情
	 */
	@Schema(description = "来源类型 0商品，1订单详情")
	private Integer type;
	/**
	 * 数量
	 */
	@Schema(description = "数量")
	private Long number;


}
