package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 商品
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@TableName("tb_commodity")
public class CommodityEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 商品名
	 */
	private String name;
	/**
	 * 商品状态 0：未上架 1：已上架
	 */
	private Integer status;
	/**
	 * 是否是赠送商品  0：否  1：是
	 */
	private Integer isGive;
	/**
	 * 商品类型id
	 */
	private Long commodityTypeId;
	/**
	 * 商品数量
	 */
	private Long number;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;


}
