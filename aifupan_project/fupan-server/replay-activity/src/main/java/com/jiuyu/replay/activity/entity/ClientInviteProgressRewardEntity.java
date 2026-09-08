package com.jiuyu.replay.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 邀请进度奖励
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
@Data
@TableName("tb_client_invite_progress_reward")
public class ClientInviteProgressRewardEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 进度id
	 */
	private Long progressId;
	/**
	 * 奖励类型 0：版本 1：增量包
	 */
	private Integer rewardType;
	/**
	 * 版本id（当奖励类型为版本时有）
	 */
	private Long packageId;
	/**
	 * 版本价格id（当奖励类型为版本时有）
	 */
	private Long packagePriceId;
	/**
	 * 商品类型id
	 */
	private Long commodityTypeId;
	/**
	 * 商品数据量
	 */
	private Long commodityNumber;
	/**
	 * 商品有效期值，跟validity_unit结合使用，如：3年
	 */
	private Integer validityNum;
	/**
	 * 商品有效期单位，0：小时 1：天 2：月 3：季度 4：半年，5：年
	 */
	private Integer validityUnit;
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
