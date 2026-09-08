package com.jiuyu.replay.agent.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 邀请进度信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
@Data
@Schema(description = "邀请进度信息")
public class ClientInviteProgressBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 关联的邀请活动id
	 */
	@Schema(description = "关联的邀请活动id")
	private Long inviteActivityId;
	/**
	 * 邀请进度类型 0：被邀请人进度 1：邀请人进度
	 */
	@Schema(description = "邀请进度类型 0：被邀请人进度 1：邀请人进度")
	private Integer inviteProgressType;
	/**
	 * 邀请进度code
	 */
	@Schema(description = "邀请进度code")
	private String inviteProgressCode;
	/**
	 * 邀请进度值
	 */
	@Schema(description = "邀请进度值")
	private Integer inviteProgressValue;
	/**
	 * 邀请进度标题
	 */
	@Schema(description = "邀请进度标题")
	private String inviteProgressTitle;
	/**
	 * 邀请进度要求
	 */
	@Schema(description = "邀请进度要求")
	private String inviteProgressRequire;
	/**
	 * 启用状态 0：停用 1：启用中
	 */
	@Schema(description = "启用状态 0：停用 1：启用中")
	private Integer inviteProgressStatus;
	/**
	 * 奖励列表
	 */
	@Schema(description = "奖励列表")
	private List<ClientInviteProgressRewardBo> rewardList;

}
