package com.jiuyu.replay.generic.vo.activity;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 邀请进度奖励信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
@Data
@Schema(description = "邀请进度奖励信息项")
public class ClientInviteProgressRewardInfoVo extends ClientInviteProgressRewardVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 增量包类型（当奖励类型为增量包时有值）
	 */
	@Schema(description = "增量包类型（当奖励类型为增量包时有值）")
	private String commodityTypeName;
	/**
	 * 增量包展示单位（当奖励类型为增量包时有值）
	 */
	@Schema(description = "增量包展示单位（当奖励类型为增量包时有值）")
	private String commodityTypeUnit;
	/**
	 * 增量包有效期（当奖励类型为增量包时有值）
	 */
	@Schema(description = "增量包有效期（当奖励类型为增量包时有值）")
	private String commodityTypeValidity;

	/**
	 * 版本名称（当奖励类型为版本时有值）
	 */
	@Schema(description = "版本名称（当奖励类型为版本时有值）")
	private String packageName;
	/**
	 * 版本价格（当奖励类型为版本时有值）
	 */
	@Schema(description = "版本价格（当奖励类型为版本时有值）")
	private Integer packagePrice;
	/**
	 * 版本时长（当奖励类型为版本时有值）
	 */
	@Schema(description = "版本时长（当奖励类型为版本时有值）")
	private String packageDuration;

}
