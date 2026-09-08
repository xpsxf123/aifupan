package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 订单-支付信息信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@Schema(description = "订单-支付信息信息")
public class OrderPayBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 订单id
	 */
	@Schema(description = "订单id")
	private Long orderId;
	/**
	 * 支付状态 0：未支付 1：已支付 2：支付失败 3：已退款 4：取消支付
	 */
	@Schema(description = "支付状态 0：未支付 1：已支付 2：支付失败 3：已退款 4：取消支付")
	private Integer payStatus;
	/**
	 * 支付类型 0：微信支付 1：支付宝支付
	 */
	@Schema(description = "支付类型 0：微信支付 1：支付宝支付")
	private Integer payType;
	/**
	 * 支付金额，单位：分
	 */
	@Schema(description = "支付金额，单位：分")
	private Integer payMoney;
	/**
	 * 第三方支付订单号
	 */
	@Schema(description = "第三方支付订单号")
	private String thirdOrderNum;
	/**
	 * 支付完成时间
	 */
	@Schema(description = "支付完成时间")
	private Date payDate;
	/**
	 * 支付二维码
	 */
	@Schema(description = "支付二维码")
	private String payCode;
	/**
	 * 第三方支付回调内容
	 */
	@Schema(description = "第三方支付回调内容")
	private String thirdCallbackContent;
	/**
	 * 系统退款号
	 */
	@Schema(description = "系统退款号")
	private String refundOrderNum;
	/**
	 * 第三方退款号
	 */
	@Schema(description = "第三方退款号")
	private String refundThirdOrderNum;
	/**
	 * 退款金额
	 */
	@Schema(description = "退款金额")
	private Integer refundMoney;
	/**
	 * 退款状态 0：未申请退款 1：申请退款中 2：退款成功 3：退款失败
	 */
	@Schema(description = "退款状态 0：未申请退款 1：申请退款中 2：退款成功 3：退款失败")
	private Integer refundStatus;
	/**
	 * 退款失败原因
	 */
	@Schema(description = "退款失败原因")
	private String refundErrorReason;
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


}
