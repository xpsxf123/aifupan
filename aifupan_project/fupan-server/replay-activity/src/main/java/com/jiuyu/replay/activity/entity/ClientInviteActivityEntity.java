package com.jiuyu.replay.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邀请活动
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
@Data
@TableName("tb_client_invite_activity")
public class ClientInviteActivityEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 代理商id
	 */
	private Long agentId;
	/**
	 * 邀请活动名称
	 */
	private String activityName;
	/**
	 * 活动开始时间
	 */
	private Date activityStartTime;
	/**
	 * 活动结束时间
	 */
	private Date activityEndTime;
	/**
	 * 活动状态 0：未启用 1：启用中
	 */
	private Integer activityStatus;
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
