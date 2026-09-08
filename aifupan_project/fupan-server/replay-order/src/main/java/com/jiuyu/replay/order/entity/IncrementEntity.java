package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 增量包表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-09 16:13:50
 */
@Data
@TableName("tb_increment")
public class IncrementEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 商品id
	 */
	private Long commodityId;
	/**
	 * 版本id
	 */
	private Long packageId;
	/**
	 * 商品价格id
	 */
	private Long commodityPriceId;
	/**
	 * 折扣
	 */
	private BigDecimal discount;
	/**
	 * 真实价格
	 */
	private BigDecimal realPrice;
	/**
	 * 状态 0未上架，1已上架
	 */
	private Integer status;
	/**
	 * 创建时间
	 */
	private Date createDate;
}
