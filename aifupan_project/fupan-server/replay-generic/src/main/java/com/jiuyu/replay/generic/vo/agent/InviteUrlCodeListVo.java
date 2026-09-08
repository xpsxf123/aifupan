package com.jiuyu.replay.generic.vo.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 邀请链接的code列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@Schema(description = "邀请链接的code列表项")
public class InviteUrlCodeListVo extends InviteUrlCodeVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
