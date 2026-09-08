package com.jiuyu.replay.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 代理商销售-渠道关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Data
@TableName("tb_agent_sale_promotion_channel")
public class AgentSalePromotionChannelEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 推广渠道id
	 */
	private Long promotionChannelId;
	/**
	 * 代理商销售id
	 */
	private Long agentSaleId;
	/**
	 * 销售渠道邀请链接的code
	 */
	private String saleUrlCode;
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
