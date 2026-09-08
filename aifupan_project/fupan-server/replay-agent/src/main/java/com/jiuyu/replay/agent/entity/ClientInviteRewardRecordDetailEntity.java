package com.jiuyu.replay.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邀请奖励明细记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
@Data
@TableName("tb_client_invite_reward_record_detail")
public class ClientInviteRewardRecordDetailEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 奖励记录id
	 */
	private Long inviteRewardRecordId;
	/**
	 * 进度奖励id
	 */
	private Long inviteProgressRewardId;
	/**
	 * 奖励内容
	 */
	private String rewardContent;
	/**
	 * 租户id
	 */
	private Long tenantId;
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


}
