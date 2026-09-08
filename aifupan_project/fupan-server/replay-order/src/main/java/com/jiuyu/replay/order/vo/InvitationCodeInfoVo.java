package com.jiuyu.replay.order.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 邀请码信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Data
@Schema(description = "邀请码信息项")
public class InvitationCodeInfoVo extends InvitationCodeVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 使用人的名称
	 */
	@Schema(description = "使用人的名称")
	private String userName;
	/**
	 * 使用人的手机号
	 */
	@Schema(description = "使用人的手机号")
	private String userPhone;
	/**
	 * 批次信息
	 */
	@Schema(description = "批次信息")
	private InvitationCodeBatchVo invitationCodeBatchVo;

}
