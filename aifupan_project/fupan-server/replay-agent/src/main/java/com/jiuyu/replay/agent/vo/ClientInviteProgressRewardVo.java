package com.jiuyu.replay.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邀请进度奖励信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
@Data
@Schema(description = "邀请进度奖励信息")
public class ClientInviteProgressRewardVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 进度id
	 */
	@Schema(description = "进度id")
	private Long progressId;
	/**
	 * 奖励类型 0：版本 1：增量包
	 */
	@Schema(description = "奖励类型 0：版本 1：增量包")
	private Long rewardType;
	/**
	 * 版本id（当奖励类型为版本时有）
	 */
	@Schema(description = "版本id（当奖励类型为版本时有）")
	private Long packageId;
	/**
	 * 版本价格id（当奖励类型为版本时有）
	 */
	@Schema(description = "版本价格id（当奖励类型为版本时有）")
	private Long packagePriceId;
	/**
	 * 商品类型id
	 */
	@Schema(description = "商品类型id")
	private Long commodityTypeId;
	/**
	 * 商品数据量
	 */
	@Schema(description = "商品数据量")
	private Long commodityNumber;
	/**
	 * 商品有效期值，跟validity_unit结合使用，如：3年
	 */
	@Schema(description = "商品有效期值，跟validity_unit结合使用，如：3年")
	private Integer validityNum;
	/**
	 * 商品有效期单位，0：小时 1：天 2：月 3：季度 4：半年，5：年
	 */
	@Schema(description = "商品有效期单位，0：小时 1：天 2：月 3：季度 4：半年，5：年")
	private Integer validityUnit;
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
