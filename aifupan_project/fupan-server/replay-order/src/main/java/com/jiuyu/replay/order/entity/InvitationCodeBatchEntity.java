package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邀请码-批次
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Data
@TableName("tb_invitation_code_batch")
public class InvitationCodeBatchEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 批次名称
	 */
	private String name;
	/**
	 * 关联的商品id
	 */
	private Long commodityId;
	/**
	 * 关联的商品名称
	 */
	private String commodityName;
	/**
	 * 关联的商品等级
	 */
	private Integer commodityLevel;
	/**
	 * 关联的商品价格id
	 */
	private Long commodityPriceId;
	/**
	 * 关联的商品价格，单位：分
	 */
	private Integer commodityRealPrice;
	/**
	 * 有效期值，跟validity_unit结合使用，如：3年
	 */
	private Integer commodityValidityNum;
	/**
	 * 有效期单位，0：小时 1：天 2：月 3：季度 4：半年，5：年
	 */
	private Integer commodityValidityUnit;
	/**
	 * 邀请码单价
	 */
	private Integer price;
	/**
	 * 版本形式 0：正常包月形式 1：到期资源失效
	 */
	@Schema(description = "版本形式 0：正常包月形式 1：到期资源失效")
	private Integer commodityType;
	/**
	 * 是否是无限次 0：否 1：是
	 */
	private Integer isInfinite;
	/**
	 * 来源类型 0：后台创建
	 */
	private Integer resourceType;
	/**
	 * 创建人的用户id
	 */
	private Long userId;
	/**
	 * 类型 0：机构码 1：个人码  2：激活码
	 */
	private Integer type;
	/**
	 * 邀请码数量
	 */
	private Integer quantity;
	/**
	 * 有效期开始时间
	 */
	private Date validityStartDate;
	/**
	 * 有效期结束时间
	 */
	private Date validityEndDate;
	/**
	 * 状态 0：正常 1：禁用
	 */
	private Integer status;
	/**
	 * 是否已下发 0：否 1：是
	 */
	private Integer isLssued;
	/**
	 * 是否免费 0：否 1：是
	 */
	private Integer isGratis;
	/**
	 * 备注
	 */
	private String remarks;
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

    /**
     * 渠道id
     */
    private Long channelId;


}
