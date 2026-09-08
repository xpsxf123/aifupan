package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品类型用量关联表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@TableName("tb_type_consumption")
public class TypeConsumptionEntity implements Serializable {
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
	 * 商品类型code
	 */
	private String commodityTypeCode;
	/**
	 * 商品类型名称
	 */
	private String commodityTypeName;
	/**
	 * 商品类型单位
	 */
	private String commodityTypeUnit;
	/**
	 * 商品类型是否重置用量 0否，1是
	 */
	private Integer commodityTypeReset;
	/**
	 * 来源id type为0字段为商品id，为1字段为订单详情id
	 */
	private Long sourceId;
	/**
	 * 来源类型 0商品，1套餐， 2邀请码
	 */
	private Integer type;
	/**
	 * 数量
	 */
	private Long number;


}
