package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品价格
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@TableName("tb_commodity_price")
public class CommodityPriceEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 类型 0商品，1套餐
	 */
	private Integer type;
	/**
	 * 商品id
	 */
	private Long commodityId;
	/**
	 * 原价格，单位：分
	 */
	private Integer originalPrice;
	/**
	 * 折扣，有0-1之间
	 */
	private BigDecimal discount;
	/**
	 * 真实价格，单位：分
	 */
	private Integer realPrice;
	/**
	 * 有效期值，跟validity_unit结合使用，如：3年
	 */
	private Integer validityNum;
	/**
	 * 有效期单位，0：小时 1：天 2：月 3：季度 4：半年，5：年
	 */
	private Integer validityUnit;
	/**
	 * 显示状态 0官网不显示，1官网显示
	 */
	private Integer showStatus;
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
    /**
     * 试用 0：不是，1：是
     */
    private Integer trialVersion;

}
