package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户资产消费记录表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Data
@TableName("tb_user_property_details")
public class UserPropertyDetailsEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 用户名称
	 */
	private String userName;
	/**
	 * 父级用户id
	 */
	private Long parentUserId;
	/**
	 * 租户id(消费=当时active_tenant_id；清零/reset=订单固化租户)。算力汇总按此列聚合
	 */
	private Long tenantId;
	/**
	 * 订单详情id
	 */
	private Long orderDetailId;
	/**
	 * 用户资产id
	 */
	private Long propertyId;
	/**
	 * 使用的资产剩余id
	 */
	private Long typeSurplusId;
	/**
	 * 商品类型id
	 */
	private Long commodityTypeId;
	/**
	 * 商品类型key
	 */
	private String commodityTypeCode;
	/**
	 * 商品名称
	 */
	private String commodityTypeName;
	/**
	 * 商品单位
	 */
	private String commodityTypeUnit;
	/**
	 * 数量
	 */
	private Long quantity;
	/**
	 * 操作类型 0：减 1：加
	 */
	private Integer signs;
	/**
	 * 备注
	 */
	private String remarks;
	/**
	 * 创建时间
	 */
	private Date createDate;

    /**
     * 资产创建方式 0：用户创建，1：系统创建
     */
    private Integer assetCreationType;

}
