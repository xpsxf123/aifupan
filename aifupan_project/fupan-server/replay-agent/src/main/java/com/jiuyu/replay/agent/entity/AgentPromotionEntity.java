package com.jiuyu.replay.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 代理商推广渠道
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@TableName("tb_agent_promotion")
public class AgentPromotionEntity implements Serializable {
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
	 * 推广渠道名称
	 */
	private String promotionName;
	/**
	 * 海报图片文件id，多个用_隔开
	 */
	private String posterImgIds;
	/**
	 * 按钮颜色
	 */
	private String btnBgColor;

	/**
	 * 按钮文案
	 */
	private String btContent;

	/**
	 * 新签佣金比例
	 */
	private Double commissionRate;
	/**
	 * 续费佣金比例
	 */
	private Double renewalCommissionRate;
	/**
	 * 渠道URL链接code码
	 */
	private String promotionUrlCode;
	/**
	 * 状态 0：未启用 1：启用中
	 */
	private Integer promotionStatus;
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
