package com.jiuyu.replay.generic.vo.activity;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户-邀请关联信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-27 15:15:49
 */
@Data
@Schema(description = "用户-邀请关联信息")
public class UserInviteVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 被邀请的用户id
	 */
	@Schema(description = "被邀请的用户id")
	private Long userId;
	/**
	 * 邀请链接的code
	 */
	@Schema(description = "邀请链接的code")
	private String inviteCode;
	/**
	 * 邀请类型 0：代理商链接 1：推广渠道链接 2：用户链接 3：代理商销售链接
	 */
	@Schema(description = "邀请类型 0：代理商链接 1：推广渠道链接 2：用户链接 3：代理商销售链接")
	private Integer inviteType;
	/**
	 * 状态 0：生效中 1：已作废
	 */
	@Schema(description = "状态 0：生效中 1：已作废")
	private Integer inviteStatus;
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
