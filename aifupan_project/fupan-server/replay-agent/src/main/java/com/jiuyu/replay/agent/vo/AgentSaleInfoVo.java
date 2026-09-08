package com.jiuyu.replay.agent.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 代理商销售信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Data
@Schema(description = "代理商销售信息项")
public class AgentSaleInfoVo extends AgentSaleVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 推广渠道
	 */
	@Schema(description = "推广渠道")
	private String agentPromotionStr;
	/**
	 * 推广渠道信息列表
	 */
	@Schema(description = "推广渠道信息列表")
	private List<AgentPromotionInfoVo> agentPromotionList;
}
