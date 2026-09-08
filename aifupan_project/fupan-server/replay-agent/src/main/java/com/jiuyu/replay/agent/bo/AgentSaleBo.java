package com.jiuyu.replay.agent.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 代理商销售信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Data
@Schema(description = "代理商销售信息")
public class AgentSaleBo implements Serializable {
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
	 * 销售人员名称
	 */
	@Schema(description = "销售人员名称")
	private String saleName;
	/**
	 * 销售人员手机号
	 */
	@Schema(description = "销售人员手机号")
	private String salePhone;
	/**
	 * 状态 0：未启用 1：启用中
	 */
	@Schema(description = "状态 0：未启用 1：启用中")
	private Integer saleStatus;
	/**
	 * 创建人用户id
	 */
	@Schema(description = "创建人用户id")
	private Long createUserId;
	/**
	 * 推广渠道id集合
	 */
	@Schema(description = "推广渠道id集合")
	private List<Long> promotionIds;


}
