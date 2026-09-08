package com.jiuyu.replay.generic.vo.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邀请链接的code信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@Schema(description = "邀请链接的code信息")
public class InviteUrlCodeVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * url链接code
	 */
	@Schema(description = "url链接code")
	private String urlCode;
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
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;

	/**
	 * 子账户用户id
	 */
	@Schema(description = "子账户用户id")
	private Long subUserId;
	/**
	 * 活动id
	 */
	@Schema(description = "活动id")
	private Long activityId;
	/**
	 * code类型 0：代理商链接 1：推广渠道链接 2：用户链接 3：代理商销售链接
	 */
	@Schema(description = "code类型 0：代理商链接 1：推广渠道链接 2：用户链接 3：代理商销售链接")
	private Integer codeType;
	/**
	 * 租户id
	 */
	@Schema(description = "租户id")
	private Long tenantId;
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
