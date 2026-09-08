package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 邀请码-批次列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Data
@Schema(description = "邀请码-批次列表项")
public class InvitationCodeBatchListVo extends InvitationCodeBatchVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 版本名称
	 */
	@Schema(description = "版本名称")
	private String packageName;
	/**
	 * 邀请码总数
	 */
	@Schema(description = "邀请码总数")
	private Integer codeCount = 0;
	/**
	 * 已使用邀请码数量
	 */
	@Schema(description = "已使用邀请码数量")
	private Integer useCodeCount = 0;
	/**
	 * 未使用邀请码数量
	 */
	@Schema(description = "未使用邀请码数量")
	private Integer notUseCodeCount = 0;

    @Schema(description = "渠道名称")
    private String channelName;
}
