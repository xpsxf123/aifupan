package com.jiuyu.replay.generic.vo.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邀请活动信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
@Data
@Schema(description = "邀请活动信息")
public class ClientInviteActivityVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 代理商id
	 */
	@Schema(description = "代理商id")
	private Long agentId;
	/**
	 * 邀请活动名称
	 */
	@Schema(description = "邀请活动名称")
	private String activityName;
	/**
	 * 活动开始时间
	 */
	@Schema(description = "活动开始时间")
	private Date activityStartTime;
	/**
	 * 活动结束时间
	 */
	@Schema(description = "活动结束时间")
	private Date activityEndTime;
	/**
	 * 活动状态 0：未启用 1：启用中
	 */
	@Schema(description = "活动状态 0：未启用 1：启用中")
	private Integer activityStatus;
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
