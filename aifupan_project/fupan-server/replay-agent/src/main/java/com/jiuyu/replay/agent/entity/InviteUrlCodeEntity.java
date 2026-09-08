package com.jiuyu.replay.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邀请链接的code
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@TableName("tb_invite_url_code")
public class InviteUrlCodeEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * url链接code
	 */
	private String urlCode;
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
	 * 用户id
	 */
	private Long userId;

	/**
	 * 子账号用户id
	 */
	private Long subUserId;
	/**
	 * 活动id
	 */
	private Long activityId;
	/**
	 * code类型 0：代理商链接 1：推广渠道链接 2：用户链接 3：代理商销售链接
	 */
	private Integer codeType;
	/**
	 * 租户id
	 */
	private Long tenantId;
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
