package com.jiuyu.replay.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 代理商销售-渠道关联表信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Data
@Schema(description = "代理商销售-渠道关联表信息")
public class AgentSalePromotionChannelVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 推广渠道id
	 */
	@Schema(description = "推广渠道id")
	private Long promotionChannelId;
	/**
	 * 代理商销售id
	 */
	@Schema(description = "代理商销售id")
	private Long agentSaleId;
	/**
	 * 销售渠道邀请链接的code
	 */
	@Schema(description = "销售渠道邀请链接的code")
	private String saleUrlCode;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;


}
