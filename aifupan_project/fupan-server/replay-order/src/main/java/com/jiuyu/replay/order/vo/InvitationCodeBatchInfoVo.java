package com.jiuyu.replay.order.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 邀请码-批次信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Data
@Schema(description = "邀请码-批次信息项")
public class InvitationCodeBatchInfoVo extends InvitationCodeBatchVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 邀请码列表
	 */
	private List<InvitationCodeInfoVo> codeList;

}
