package com.jiuyu.replay.order.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商品列表项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "商品列表项")
public class CommodityListVo extends CommodityVo implements Serializable {
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


}
