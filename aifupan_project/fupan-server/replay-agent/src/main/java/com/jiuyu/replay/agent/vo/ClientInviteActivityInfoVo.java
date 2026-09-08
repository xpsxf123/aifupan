package com.jiuyu.replay.agent.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;

/**
 * 邀请活动信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-23 16:45:49
 */
@Data
@Schema(description = "邀请活动信息项")
public class ClientInviteActivityInfoVo extends ClientInviteActivityVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 活动的进度和奖励列表
	 */
	@Schema(description = "活动的进度和奖励列表")
	private List<ClientInviteProgressInfoVo> progressList;

}
