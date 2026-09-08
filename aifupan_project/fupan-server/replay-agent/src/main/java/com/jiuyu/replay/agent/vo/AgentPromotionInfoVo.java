package com.jiuyu.replay.agent.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 代理商推广渠道信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@Schema(description = "代理商推广渠道信息项")
public class AgentPromotionInfoVo extends AgentPromotionVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 渠道URL链接
	 */
	@Schema(description = "渠道URL链接")
	private String url;

}
