package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 商品类型资产剩余表信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Data
@Schema(description = "商品类型资产剩余表信息")
public class TypeSurplusVo implements Serializable {
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
	 * 商品类型key
	 */
	@Schema(description = "商品类型key")
	private String commodityTypeCode;
	/**
	 * 资产id
	 */
	@Schema(description = "资产id")
	private Long propertyId;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 订单详情id
	 */
	@Schema(description = "订单详情id")
	private Long orderDetailId;
	/**
	 * 使用数量
	 */
	@Schema(description = "使用数量")
	private Long useNumber;
	/**
	 * 总数量
	 */
	@Schema(description = "总数量")
	private Long totalNumber;
	/**
	 * 开始时间
	 */
	@Schema(description = "开始时间")
	private Date startTime;
	/**
	 * 结束时间
	 */
	@Schema(description = "结束时间")
	private Date endTime;
	/**
	 * 使用状态 0在使用，1已用完，2弃用(当前订单已经升级)
	 */
	@Schema(description = "使用状态 0在使用，1已用完，2弃用(当前订单已经升级)")
	private Integer useStatus;
	/**
	 * 时间状态 0生效中，1已过期
	 */
	@Schema(description = "时间状态 0生效中，1已过期")
	private Integer timeStatus;


}
