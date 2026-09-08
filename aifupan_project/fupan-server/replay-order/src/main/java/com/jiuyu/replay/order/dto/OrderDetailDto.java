package com.jiuyu.replay.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 订单-明细
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-16 09:48:08
 */
@Data
public class OrderDetailDto implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	private Long id;
	/**
	 * 订单id
	 */
	private Long orderId;
	/**
	 * 监控位数量
	 */
	private Integer monitorNum;
	/**
	 * 拥有的ai语音分析时长，单位：分钟
	 */
	private Double aiAnalysisTime;
	/**
	 * 监控位数量
	 */
	private Integer anchorNum;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;


}
