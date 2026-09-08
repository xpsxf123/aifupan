package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 相似主播发送记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-10-16 15:40:25
 */
@Data
@TableName("tb_similar_send_record")
public class SimilarSendRecordEntity implements Serializable {
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
	 * 主播抖音号
	 */
	private String anchorNumber;
	/**
	 * 主播secUid
	 */
	private String secUid;
	/**
	 * 请求ID（用于请求和回调的唯一标识）
	 */
	private String requestId;
	/**
	 * 请求体
	 */
	private String requestBody;
	/**
	 * 请求响应状态
	 */
	private String responseStatus;
	/**
	 * 请求响应体
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
	 * 备注（用于保存错误信息或其他信息）
	 */
	private String remark;
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
