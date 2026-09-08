package com.jiuyu.replay.generic.vo.activity;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 用户-邀请关联信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-27 15:15:49
 */
@Data
@Schema(description = "用户-邀请关联信息项")
public class UserInviteInfoVo extends UserInviteVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
