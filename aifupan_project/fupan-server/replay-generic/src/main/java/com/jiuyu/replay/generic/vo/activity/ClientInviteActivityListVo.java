package com.jiuyu.replay.generic.vo.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 邀请活动列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
@Data
@Schema(description = "邀请活动列表项")
public class ClientInviteActivityListVo extends ClientInviteActivityVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
