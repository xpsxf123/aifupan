package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 主播url
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-10 16:47:37
 */
@Data
@TableName("tb_anchor_url")
public class AnchorUrlEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 主播唯一标识
	 */
	private String secUid;
	/**
	 * 主页url
	 */
	private String homeUrl;
	/**
	 * 直播间url
	 */
	private String liveUrl;
	/**
	 * 主播名称
	 */
	private String anchorName;
	/**
	 * 主播头像
	 */
	private String anchorAvatar;
	/**
	 * 平台类型 0：抖音 1：快手 2：视频号
	 */
	private Integer platform;
	/**
	 * DouYinHomeLive(个人主页地址),DouYinLive（直播地址） 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手   其他待定
	 */
	private String platformResource;
	/**
	 * 主播userId
	 */
	private String anchorUserId;
	/**
	 * webSocketId
	 */
	private String webSocketId;
	/**
	 * 主播抖音号
	 */
	private String anchorNumber;
	/**
	 * 第三方数据平台是否已经收录 0:否 1:是
	 */
	private Integer chanmamaInclude;
	/**
	 * 系统行业id
	 */
	private Long systemTradeId;
	/**
	 * AI纠正后的行业id
	 */
	private Long aiCorrectTradeId;
	/**
	 * 添加AI纠正行业的时间
	 */
	private Date addAiTradeDate;
	/**
	 * 最后修改系统行业的时间
	 */
	private Date updateSystemTradeDate;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;

}
