package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@Schema(description = "订单信息")
public class OrderBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 用户名称
	 */
	@Schema(description = "用户名称")
	private String userName;
	/**
	 * 商品id
	 */
	@Schema(description = "商品id")
	private Long commodityId;
	/**
	 * 商品名称
	 */
	@Schema(description = "商品名称")
	private String commodityName;
	/**
	 * 订单标题
	 */
	@Schema(description = "订单标题")
	private String title;
	/**
	 * 状态（0：未支付 1：未开始，2：生效中 3：已过期 4：已退款 5：已结束(升级了，当前订单失效) 6：超时未支付关闭 7冻结 8手动取消订单）
	 */
	@Schema(description = "状态（0：未支付 1：未开始，2：生效中 3：已过期 4：已退款 5：已结束(升级了，当前订单失效) 6：超时未支付关闭 7冻结 8手动取消订单）")
	private Integer status;
	/**
	 * 订单类型 0免费订单，1升级订单，2免费版换收费版，3订单续费，4增量包订单，5版本活动订单，6编辑订单，7商品活动订单
	 */
	@Schema(description = "订单类型 0免费订单，1升级订单，2免费版换收费版，3订单续费，4增量包订单，5版本活动订单，6编辑订单，7商品活动订单")
	private Integer orderType;
	/**
	 * 商品类型 0增量包，1正常版本(月底资源重置)，2活动版本(月底资源不重置)，3邀请活动订单
	 */
	@Schema(description = "商品类型 0增量包，1正常版本(月底资源重置)，2活动版本(月底资源不重置)，3邀请活动订单")
	private Integer commodityType;
	/**
	 * 升级等级
	 */
	@Schema(description = "升级等级")
	private Integer level;
	/**
	 * 商品总数量
	 */
	@Schema(description = "商品总数量")
	private Integer quantity;
	/**
	 * 原价格，单位：分(单个商品)
	 */
	@Schema(description = "原价格，单位：分(单个商品)")
	private Integer originalPrice;
	/**
	 * 折扣，有0-1之间(单个商品)
	 */
	@Schema(description = "折扣，有0-1之间(单个商品)")
	private BigDecimal discount;
	/**
	 * 优惠价格（主要用于套餐升级产生的差价，和折扣没有关系）(单个商品)
	 */
	@Schema(description = "优惠价格（主要用于套餐升级产生的差价，和折扣没有关系）(单个商品)")
	private Integer discountRate;
	/**
	 * 真实价格，单位：分(单个商品)
	 */
	@Schema(description = "真实价格，单位：分(单个商品)")
	private Integer realPrice;
	/**
	 * 订单总价，单位：分
	 */
	@Schema(description = "订单总价，单位：分")
	private Integer totalPrice;
	/**
	 * 有效时间
	 */
	@Schema(description = "有效时间")
	private Integer expiration;
	/**
	 * 有效时间单位(0小时，1天，2月，3季度，4半年，5年)
	 */
	@Schema(description = "有效时间单位(0小时，1天，2月，3季度，4半年，5年)")
	private Integer expirationUnit;
	/**
	 * 付款时间
	 */
	@Schema(description = "付款时间")
	private Date payDate;
	/**
	 * 订单生效时间
	 */
	@Schema(description = "订单生效时间")
	private Date startDate;
	/**
	 * 订单过期时间
	 */
	@Schema(description = "订单过期时间")
	private Date endDate;
	/**
	 * 真实订单过期时间
	 */
	@Schema(description = "真实订单过期时间")
	private Date realEndDate;
	/**
	 * 是否分佣：0否，1是
	 */
	@Schema(description = "是否分佣：0否，1是")
	private Integer isCommission;
	/**
	 * 升级前的订单id(用于套餐升级)
	 */
	@Schema(description = "升级前的订单id(用于套餐升级)")
	private Long beforeUpgrading;
	/**
	 * 升级后的订单id(用于套餐升级)
	 */
	@Schema(description = "升级后的订单id(用于套餐升级)")
	private Long afterUpgrading;
	/**
	 * 来源 0正常下单，1手动添加，2邀请用户成功，3邀请码赠送
	 */
	@Schema(description = "来源 0正常下单，1手动添加，2邀请码赠送，3活动赠送")
	private Integer source;
	/**
	 * 是否是试用订单 0否，1是
	 */
	@Schema(description = "是否是试用订单 0否，1是")
	private Integer trialOrder;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;

	@Schema(description = "订单详情")
	private List<OrderDetailBo> orderDetailList;

}
