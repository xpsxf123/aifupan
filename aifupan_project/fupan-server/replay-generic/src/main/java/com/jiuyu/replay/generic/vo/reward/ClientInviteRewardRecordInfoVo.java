package com.jiuyu.replay.generic.vo.reward;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 进度奖励记录信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 17:03:13
 */
@Data
@Schema(description = "进度奖励记录信息项")
public class ClientInviteRewardRecordInfoVo extends ClientInviteRewardRecordVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 邀请人名称
	 */
	@Schema(description = "邀请人名称")
	private String inviterName;
	/**
	 * 被邀请人名称
	 */
	@Schema(description = "被邀请人名称")
	private String inviteeName;
	/**
	 * 奖励
	 */
	@Schema(description = "奖励")
	private String reward;
	/**
	 * 奖励id集合
	 */
	@Schema(description = "奖励id集合")
	private List<Long> progressRewardIds;
	/**
	 * 奖励集合
	 */
	@Schema(description = "奖励集合")
	private List<String> rewardList;
	/**
	 * 进度code Label
	 */
	@Schema(description = "进度code Label")
	private String progressCodeStr;
}
