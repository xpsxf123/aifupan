package com.jiuyu.replay.agent.bo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 邀请奖励记录信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
@Data
@Schema(description = "邀请奖励记录信息")
public class ClientInviteRewardRecordBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 邀请人用户id
	 */
	@Schema(description = "邀请人用户id")
	private Long inviteUserId;
	/**
	 * 被邀请人用户id
	 */
	@Schema(description = "被邀请人用户id")
	private Long beInviteUserId;
	/**
	 * 邀请进度id
	 */
	@Schema(description = "邀请进度id")
	private Long inviteProgressId;
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
