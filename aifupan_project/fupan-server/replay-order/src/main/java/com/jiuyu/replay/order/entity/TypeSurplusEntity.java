package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 商品类型资产剩余表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Data
@TableName("tb_type_surplus")
public class TypeSurplusEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 商品类型id
	 */
	private Long commodityTypeId;
	/**
	 * 商品类型key
	 */
	private String commodityTypeCode;
	/**
	 * 资产id
	 */
	private Long propertyId;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 订单详情id
	 */
	private Long orderDetailId;
	/**
	 * 使用数量
	 */
	private Long useNumber;
	/**
	 * 总数量
	 */
	private Long totalNumber;
	/**
	 * 开始时间
	 */
	private Date startTime;
	/**
	 * 结束时间
	 */
	private Date endTime;
	/**
	 * 使用状态 0在使用，1已用完，2弃用(当前订单已经升级)，3时间过期， 4冻结
	 */
	private Integer useStatus;
	/**
	 * 时间状态  0生效中，1已过期，2停用，3冻结
	 */
	private Integer timeStatus;


}
