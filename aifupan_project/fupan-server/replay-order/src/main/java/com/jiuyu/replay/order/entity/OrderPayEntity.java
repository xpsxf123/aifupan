package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 订单-支付信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@TableName("tb_order_pay")
public class OrderPayEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 订单id
	 */
	private Long orderId;
	/**
	 * 支付状态 0：未支付 1：已支付 2：支付失败 3：已退款 4：取消支付
	 */
	private Integer payStatus;
	/**
	 * 支付类型 0：微信支付 1：支付宝支付
	 */
	private Integer payType;
	/**
	 * 支付金额，单位：分
	 */
	private Integer payMoney;
	/**
	 * 第三方支付订单号
	 */
	private String thirdOrderNum;
	/**
	 * 支付完成时间
	 */
	private Date payDate;
	/**
	 * 支付二维码
	 */
	private String payCode;
	/**
	 * 第三方支付回调内容
	 */
	private String thirdCallbackContent;
	/**
	 * 系统退款号
	 */
	private String refundOrderNum;
	/**
	 * 第三方退款号
	 */
	private String refundThirdOrderNum;
	/**
	 * 退款金额
	 */
	private Integer refundMoney;
	/**
	 * 退款状态 0：未申请退款 1：申请退款中 2：退款成功 3：退款失败
	 */
	private Integer refundStatus;
	/**
	 * 退款失败原因
	 */
	private String refundErrorReason;
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
