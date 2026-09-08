package com.jiuyu.replay.agent.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 代理商平台销售信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Data
@Schema(description = "代理商平台销售信息")
public class AgentPlatformSaleBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 代理商id
	 */
	@Schema(description = "代理商id")
	private Long agentId;
	/**
	 * 销售人员id
	 */
	@Schema(description = "销售人员id")
	private Long saleId;
	/**
	 * 渠道二维码图片文件id
	 */
	@Schema(description = "渠道二维码图片文件id")
	private Long channelQrcodeImgId;


}
