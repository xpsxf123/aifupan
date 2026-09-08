package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户资产类型总明细
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Data
@TableName("tb_user_property_type")
public class UserPropertyTypeEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 父资产类型总明细id
	 */
	private Long parentId;
	/**
	 * 用户资产id
	 */
	private Long propertyId;
	/**
	 * 商品类型id
	 */
	private Long commodityTypeId;
	/**
	 * 商品类型key
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
	 * 商品是否要月底清零 0否，1是
	 */
	private Integer commodityTypeReset;
	/**
	 * 使用数量
	 */
	private Long useQuantity;
	/**
	 * 总数量
	 */
	private Long totalQuantity;
	/**
	 * 总的使用数量
	 */
	private Long totalUseQuantity;
	/**
	 * 创建记录时间
	 */
	private Date createDate;
	/**
	 * 修改记录时间
	 */
	private Date updateDate;
	/**
	 * 是否删除
	 */
	private Integer isDeleted;


}
