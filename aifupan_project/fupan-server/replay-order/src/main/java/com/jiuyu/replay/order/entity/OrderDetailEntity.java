package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@TableName("tb_order_detail")
public class OrderDetailEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键，自增长
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 订单ID
	 */
	private Long orderId;
	/**
	 * 商品ID
	 */
	private Long commodityId;
	/**
	 * 商品名称
	 */
	private String commodityName;
	/**
	 * 商品类型ID
	 */
	private Long commodityTypeId;
	/**
	 * 商品类型Code
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
	 * 总数量
	 */
	private Long totalNumber;
	/**
	 * 开始使用时间
	 */
	private Date startDate;
	/**
	 * 多少时间重置一次
	 */
	private Integer resetNum;
	/**
	 * 重置时间单位（0小时，1天，2月，3季度，4半年，5年）
	 */
	private Integer resetUnit;
	/**
	 * 清零时间
	 */
	private Date resetDate;
	/**
	 * 下次清零时间
	 */
	private Date nextReset;
	/**
	 * 最终过期时间
	 */
	private Date expirationDate;
	/**
	 * 状态 0未开始，1生效中，3已失效 4冻结
	 */
	private Integer status;
	/**
	 * 创建时间，默认当前时间
	 */
	private Date createDate;
	/**
	 * 更新时间，默认当前时间，并在更新时自动修改
	 */
	private Date updateDate;
	/**
	 * 删除标志，0 表示未删除，1 表示已删除，默认未删除
	 */
	private Integer isDeleted;


}
