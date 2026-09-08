package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 行业
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:10:10
 */
@Data
@TableName("tb_trade")
public class TradeEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 行业名称
	 */
	private String name;
	/**
	 * 行业描述
	 */
	private String remarks;
	/**
	 * 默认的通用模型id
	 */
	private Long defaultGeneralModelId;
	/**
	 * 行业模型id，为0表示没有
	 */
	private Long tradeModelId;
	/**
	 * 父行业id
	 */
	private Long parentId;
	/**
	 * 排序
	 */
	private Integer sort;
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
	 * 行业热榜生效状态（0:不生效, 1:生效）
	 */
	private Integer rankEnabled;

	/**
	 * 第三方采集开关
	 */
	private Boolean thirdCollect;
}
