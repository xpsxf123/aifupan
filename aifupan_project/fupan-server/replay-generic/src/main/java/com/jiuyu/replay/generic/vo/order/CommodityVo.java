package com.jiuyu.replay.generic.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 商品信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "商品信息")
public class CommodityVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 商品名
	 */
	@Schema(description = "商品名")
	private String name;
	/**
	 * 商品状态 0：未上架 1：已上架
	 */
	@Schema(description = "商品状态 0：未上架 1：已上架")
	private Integer status;
	/**
	 * 是否是赠送商品  0：否  1：是
	 */
	@Schema(description = "是否是赠送商品  0：否  1：是")
	private Integer isGive;
	/**
	 * 商品类型id
	 */
	@Schema(description = "商品类型id")
	private Long commodityTypeId;
	/**
	 * 商品数量
	 */
	@Schema(description = "商品数量")
	private Long number;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;


}
