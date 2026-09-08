package com.jiuyu.replay.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 代理商销售
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Data
@TableName("tb_agent_sale")
public class AgentSaleEntity implements Serializable {
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
	 * 销售人员名称
	 */
	private String saleName;
	/**
	 * 销售人员手机号
	 */
	private String salePhone;
	/**
	 * 状态 0：未启用 1：启用中
	 */
	private Integer saleStatus;
	/**
	 * 创建人用户id
	 */
	private Long createUserId;
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
