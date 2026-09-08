package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 第三方数据平台发送记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-22 14:21:09
 */
@Data
@TableName("tb_chanmama_send_record")
public class ChanmamaSendRecordEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 租户id
	 */
	private Long tenantId;
	/**
	 * 视频id
	 */
	private String videoId;
	/**
	 * 请求id
	 */
	private String requestId;
	/**
	 * 主播抖音号
	 */
	private String anchorNumber;
	/**
	 * 请求体
	 */
	private String requestBody;
	/**
	 * 响应状态
	 */
	private String responseStatus;
	/**
	 * 响应体
	 */
	private String responseBody;
	/**
	 * 回调体
	 */
	private String callbackBody;
	/**
	 * 回调状态
	 */
	private String callbackStatus;
	/**
	 * 直播场次号
	 */
	private String batchNumber;
	/**
	 * 数据状态 0：正常数据 1：异常数据但已校验完成 2：异常数据，未校验 3：未校验
	 */
	private Integer dataStatus;
	/**
	 * 数据来源 0：蝉妈妈 1：考古家
	 */
	private Integer accountType;
	/**
	 * 蝉妈妈/考古家的直播间id
	 */
	private String roomId;
	/**
	 * 修正请求的请求体
	 */
	private String revisionRequestBody;
	/**
	 * 修正请求的响应体
	 */
	private String revisionResponseBody;
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
