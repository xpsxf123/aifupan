package com.jiuyu.replay.generic.vo.activity;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 邀请进度信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
@Data
@Schema(description = "邀请进度信息")
public class ClientInviteProgressVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 关联的邀请活动id
	 */
	@Schema(description = "关联的邀请活动id")
	private Long inviteActivityId;
	/**
	 * 进度类型 0：被邀请人进度 1：邀请人进度
	 */
	@Schema(description = "进度类型 0：被邀请人进度 1：邀请人进度")
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
