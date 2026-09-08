package com.jiuyu.replay.order.bo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 邀请码信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Data
@Schema(description = "邀请码信息")
public class InvitationCodeBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 关联的邀请码批次id
	 */
	@Schema(description = "关联的邀请码批次id")
	private Long batchId;
	/**
	 * 邀请码
	 */
	@Schema(description = "邀请码")
	private String code;
	/**
	 * 使用状态 0：未使用 1：已使用
	 */
	@Schema(description = "使用状态 0：未使用 1：已使用")
	private Integer useStatus;
	/**
	 * 有效期开始时间
	 */
	@Schema(description = "有效期开始时间")
	private Date validityStartDate;
	/**
	 * 有效期结束时间
	 */
	@Schema(description = "有效期结束时间")
	private Date validityEndDate;
	/**
	 * 状态 0：正常 1：禁用
	 */
	@Schema(description = "状态 0：正常 1：禁用")
	private Integer status;
	/**
	 * 使用后关联的订单id，未使用时为0
	 */
	@Schema(description = "使用后关联的订单id，未使用时为0")
	private Long orderId;
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
