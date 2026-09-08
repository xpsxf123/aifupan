package com.jiuyu.replay.generic.vo.activity;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;

/**
 * 邀请进度信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
@Data
@Schema(description = "邀请进度信息项")
public class ClientInviteProgressInfoVo extends ClientInviteProgressVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 奖励列表
	 */
	@Schema(description = "奖励列表")
	List<ClientInviteProgressRewardInfoVo> rewardList;

}
