package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邀请码列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Data
@Schema(description = "邀请码列表项")
public class InvitationCodeListVo extends InvitationCodeVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 批次名称
	 */
	@Schema(description = "批次名称")
	private String batchName;

	/**
	 * 版本名称
	 */
	@Schema(description = "版本名称")
	private String packageName;

	@Schema(description = "用户名")
	private String userName;
	/**
	 * 有效期值，跟validity_unit结合使用，如：3年
	 */
	@Schema(description = "有效期值，跟validity_unit结合使用，如：3年")
	private Integer commodityValidityNum;
	/**
	 * 有效期单位，0：小时 1：天 2：月 3：季度 4：半年，5：年
	 */
	@Schema(description = "有效期单位，0：小时 1：天 2：月 3：季度 4：半年，5：年")
	private Integer commodityValidityUnit;

	/**
	 * 有效期开始时间
	 */
	@Schema(description = "有效期开始时间")
	private Date validityStartDate;
	/**
	 * 有效期结束时间
	 */
	@Schema(description = "有效期结束时间")
	private Date validityEndDate;

    @Schema(description = "渠道名称")
    private String channelName;
}
