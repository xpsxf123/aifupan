package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 增量包表信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-09 16:13:50
 */
@Data
@Schema(description = "增量包表信息")
public class IncrementBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * 商品id
	 */
	@Schema(description = "商品id")
	private Long commodityId;
	/**
	 * 版本id
	 */
	@Schema(description = "版本id")
	private Long packageId;
	/**
	 * 商品价格id
	 */
	@Schema(description = "商品价格id")
	private Long commodityPriceId;
	/**
	 * 折扣
	 */
	@Schema(description = "折扣")
	private BigDecimal discount;
	/**
	 * 真实价格
	 */
	@Schema(description = "真实价格")
	private BigDecimal realPrice;
	/**
	 * 状态 0未上架，1已上架
	 */
	@Schema(description = "状态 0未上架，1已上架")
	private Integer status;

	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;


}
