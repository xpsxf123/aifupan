package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邀请码
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Data
@TableName("tb_invitation_code")
public class InvitationCodeEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 关联的邀请码批次id
	 */
	private Long batchId;
	/**
	 * 邀请码
	 */
	private String code;
	/**
	 * 使用状态 0：未使用 1：已使用
	 */
	private Integer useStatus;
	/**
	 * 使用时间
	 */
	private Date useDate;
	/**
	 * 使用的用户id，未使用时为0
	 */
	private Long userId;
	/**
	 * 有效期开始时间
	 */
	private Date validityStartDate;
	/**
	 * 有效期结束时间
	 */
	private Date validityEndDate;
	/**
	 * 状态 0：正常 1：禁用
	 */
	private Integer status;
	/**
	 * 使用后关联的订单id，未使用时为0
	 */
	private Long orderId;
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
	 * 是否已下发
	 */
	private Integer isLssued;

}
