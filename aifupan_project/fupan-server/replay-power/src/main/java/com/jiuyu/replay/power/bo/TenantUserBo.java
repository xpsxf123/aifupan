package com.jiuyu.replay.power.bo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 租户-用户-关联表信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
@Data
@Schema(description = "租户-用户-关联表信息")
public class TenantUserBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 租户id
	 */
	@Schema(description = "租户id")
	private Long tenantId;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
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
