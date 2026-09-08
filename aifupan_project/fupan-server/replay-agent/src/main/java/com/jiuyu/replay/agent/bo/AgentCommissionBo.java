package com.jiuyu.replay.agent.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 代理商佣金信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@Schema(description = "代理商佣金信息")
public class AgentCommissionBo implements Serializable {
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
	 * 代理商推广渠道id
	 */
	@Schema(description = "代理商推广渠道id")
	private Long promotionId;
	/**
	 * 代理商销售id
	 */
	@Schema(description = "代理商销售id")
	private Long agentSaleId;
	/**
	 * 订单id
	 */
	@Schema(description = "订单id")
	private Long orderId;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 订单总金额，单位：分
	 */
	@Schema(description = "订单总金额，单位：分")
	private Integer orderTotalMoney;
	/**
	 * 佣金比例
	 */
	@Schema(description = "佣金比例")
	private Double commission;
	/**
	 * 佣金，单位：分
	 */
	@Schema(description = "佣金，单位：分")
	private Integer commissionMoney;
	/**
	 * 分佣类型 0：新签佣金 1：续费佣金
	 */
	@Schema(description = "分佣类型 0：新签佣金 1：续费佣金")
	private Integer commissionType;
	/**
	 * 分佣模式：0:代理商分佣, 1:推广渠道分佣
	 */
	@Schema(description = "分佣模式：0:代理商分佣, 1:推广渠道分佣")
	private Integer commissionMode;
	/**
	 * 分佣时间
	 */
	@Schema(description = "分佣时间")
	private Date commissionTime;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remarks;
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
