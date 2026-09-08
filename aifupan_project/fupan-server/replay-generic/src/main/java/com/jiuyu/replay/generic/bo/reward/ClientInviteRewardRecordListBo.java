package com.jiuyu.replay.generic.bo.reward;


import com.jiuyu.replay.generic.bo.common.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 进度奖励记录列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 17:03:13
 */
@Data
@Schema(description = "进度奖励记录列表查询参数")
public class ClientInviteRewardRecordListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 按进度code类型筛选
	 */
	@Schema(description = "按进度code类型筛选")
	private String progressCode;
	/**
	 * 奖励的状态 0：待发放 1：已发放
	 */
	@Schema(description = "奖励的状态 0：待发放 1：已发放")
	private String rewardStatus;
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
	 * 邀请人用户id集合
	 */
	@Schema(description = "邀请人用户id集合")
	private List<Long> inviterUserIds;
	/**
	 * 被邀请人用户id集合
	 */
	@Schema(description = "被邀请人用户id集合")
	private List<Long> inviteeUserIds;
	/**
	 * 奖励时间-开始 yyyy-MM-dd
	 */
	@Schema(description = "奖励时间-开始 yyyy-MM-dd")
	private String startTime;
	/**
	 * 奖励时间-结束 yyyy-MM-dd
	 */
	@Schema(description = "奖励时间-结束 yyyy-MM-dd")
	private String endTime;

}
