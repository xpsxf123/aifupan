package com.jiuyu.replay.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邀请进度
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
@Data
@TableName("tb_client_invite_progress")
public class ClientInviteProgressEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 关联的邀请活动id
	 */
	private Long inviteActivityId;
	/**
	 * 邀请进度类型 0：被邀请人进度 1：邀请人进度
	 */
	private Integer inviteProgressType;
	/**
	 * 邀请进度code
	 */
	private String inviteProgressCode;
	/**
	 * 邀请进度值
	 */
	private Integer inviteProgressValue;
	/**
	 * 邀请进度标题
	 */
	private String inviteProgressTitle;
	/**
	 * 邀请进度要求
	 */
	private String inviteProgressRequire;
	/**
	 * 启用状态 0：停用 1：启用中
	 */
	private Integer inviteProgressStatus;
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
