package com.jiuyu.replay.agent.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 邀请奖励明细记录信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
@Data
@Schema(description = "邀请奖励明细记录信息")
public class ClientInviteRewardRecordDetailVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 奖励记录id
	 */
	@Schema(description = "奖励记录id")
	private Long inviteRewardRecordId;
	/**
	 * 进度奖励id
	 */
	@Schema(description = "进度奖励id")
	private Long inviteProgressRewardId;
	/**
	 * 奖励内容
	 */
	@Schema(description = "奖励内容")
	private String rewardContent;
	/**
	 * 租户id
	 */
	@Schema(description = "租户id")
	private Long tenantId;
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
