package com.jiuyu.replay.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 代理商佣金
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@TableName("tb_agent_commission")
public class AgentCommissionEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 代理商id
	 */
	private Long agentId;
	/**
	 * 代理商推广渠道id
	 */
	private Long promotionId;
	/**
	 * 代理商销售id
	 */
	private Long agentSaleId;
	/**
	 * 订单id
	 */
	private Long orderId;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 订单总金额，单位：分
	 */
	private Integer orderTotalMoney;
	/**
	 * 佣金比例
	 */
	private Double commission;
	/**
	 * 佣金，单位：分
	 */
	private Integer commissionMoney;
	/**
	 * 分佣类型 0：新签佣金 1：续费佣金
	 */
	private Integer commissionType;
	/**
	 * 分佣模式：0：代理商 1：推广渠道 2：用户
	 */
	private Integer commissionMode;
	/**
	 * 分佣时间
	 */
	private Date commissionTime;
	/**
	 * 备注
	 */
	private String remarks;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;


}
