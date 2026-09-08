package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 直播场次的websocket记录统计
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-01-04 15:15:15
 */
@Data
@TableName("tb_total_socket_message")
public class TotalSocketMessageEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
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
	 * 主播secUid
	 */
	private String secUid;
	/**
	 * 直播场次号
	 */
	private String batchNumber;
	/**
	 * 直播开始时间
	 */
	private Date startDate;
	/**
	 * 直播结束时间
	 */
	private Date endDate;
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
	 * 累计观看人数
	 */
	private String totalOnlineNum;
	/**
	 * 弹幕总数
	 */
	private String totalBulletChatNum;
}
