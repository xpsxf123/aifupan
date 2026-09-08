package com.jiuyu.replay.agent.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 代理商推广渠道信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@Schema(description = "代理商推广渠道信息")
public class AgentPromotionBo implements Serializable {
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
	 * 推广渠道名称
	 */
	@Schema(description = "推广渠道名称")
	private String promotionName;
	/**
	 * 海报图片文件id，多个用_隔开
	 */
	@Schema(description = "海报图片文件id，多个用_隔开")
	private String posterImgIds;
	/**
	 * 按钮颜色
	 */
	@Schema(description = "按钮颜色")
	private String btnBgColor;

	/**
	 * 按钮文案
	 */
	@Schema(description = "按钮文案")
	private String btContent;

	/**
	 * 新签佣金比例
	 */
	@Schema(description = "新签佣金比例")
	private Double commissionRate;
	/**
	 * 续费佣金比例
	 */
	@Schema(description = "续费佣金比例")
	private Double renewalCommissionRate;
	/**
	 * 渠道URL链接code码
	 */
	@Schema(description = "渠道URL链接code码")
	private String promotionUrlCode;
	/**
	 * 状态 0：未启用 1：启用中
	 */
	@Schema(description = "状态 0：未启用 1：启用中")
	private Integer promotionStatus;


}
