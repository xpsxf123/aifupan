package com.jiuyu.replay.agent.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 代理商销售信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Data
@Schema(description = "代理商销售信息")
public class AgentSaleVo implements Serializable {
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
