package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 商品类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-19 20:52:26
 */
@Data
@TableName("tb_commodity_type")
public class CommodityTypeEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 商品名称
	 */
	private String name;
	/**
	 * 商品类型bean的名称  vipLevel：会员等级  analysisTime：分析时长 monitorNum：监控位 anchorNum：主播数量  wordMarkTime：词语标注时长  storageNum：空间容量
	 */
	private String code;
	/**
	 * 单位
	 */
	private String unit;
	/**
	 * 是否可以重置清零
 	 */
	private Integer isReset;
	/**
	 * 重置时间
	 */
	private Integer resetNum;
	/**
	 * 重置时间单位(0小时，1天，2月，3季度，4半年，5年)
	 */
	private Integer resetUnit;
	/**
	 * 当前类型子账号是否拥有，0没有，1有
	 */
	private Integer subAccountHave;
	/**
	 * 是否删除
	 */
	private Integer isDeleted;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;


}
