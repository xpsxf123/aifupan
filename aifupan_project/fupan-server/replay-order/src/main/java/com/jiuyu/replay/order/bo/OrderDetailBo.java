package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@Schema(description = "信息")
public class OrderDetailBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键，自增长
	 */
	@Schema(description = "主键，自增长")
	private Long id;
	/**
	 * 订单ID
	 */
	@Schema(description = "订单ID")
	private Long orderId;
	/**
	 * 商品ID
	 */
	@Schema(description = "商品ID")
	private Long commodityId;
	/**
	 * 商品名称
	 */
	@Schema(description = "商品名称")
	private String commodityName;
	/**
	 * 商品类型ID
	 */
	@Schema(description = "商品类型ID")
	private Long commodityTypeId;
	/**
	 * 商品类型Code
	 */
	@Schema(description = "商品类型Code")
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
	 * 总数量
	 */
	@Schema(description = "总数量")
	private Long totalNumber;
	/**
	 * 开始使用时间
	 */
	@Schema(description = "开始使用时间")
	private Date startDate;
	/**
	 * 多少时间重置一次
	 */
	@Schema(description = "多少时间重置一次")
	private Integer resetNum;
	/**
	 * 重置时间单位（0小时，1天，2月，3季度，4半年，5年）
	 */
	@Schema(description = "重置时间单位（0小时，1天，2月，3季度，4半年，5年）")
	private Integer resetUnit;
	/**
	 * 清零时间
	 */
	@Schema(description = "清零时间")
	private Date resetDate;
	/**
	 * 下次清零时间
	 */
	@Schema(description = "下次清零时间")
	private Date nextReset;
	/**
	 * 最终过期时间
	 */
	@Schema(description = "最终过期时间")
	private Date expirationDate;
	/**
	 * 状态 0未开始，1已开始
	 */
	@Schema(description = "状态 0未开始，1生效中，2已过期，3已失效")
	private Integer status;
	/**
	 * 创建时间，默认当前时间
	 */
	@Schema(description = "创建时间，默认当前时间")
	private Date createDate;
	/**
	 * 更新时间，默认当前时间，并在更新时自动修改
	 */
	@Schema(description = "更新时间，默认当前时间，并在更新时自动修改")
	private Date updateDate;
	/**
	 * 删除标志，0 表示未删除，1 表示已删除，默认未删除
	 */
	@Schema(description = "删除标志，0 表示未删除，1 表示已删除，默认未删除")
	private Integer isDeleted;


}
