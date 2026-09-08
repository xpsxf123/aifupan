package com.jiuyu.replay.generic.vo.words;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 主播url信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-10 16:47:37
 */
@Data
@Schema(description = "主播url信息项")
public class AnchorUrlInfoVo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
//	@TableId(value = "id", type = IdType.INPUT)
	@Schema(description = "ID")
	private Long id;
	/**
	 * 主播唯一标识
	 */
	@Schema(description = "主播唯一标识")
	private String secUid;
	/**
	 * 主页url
	 */
	@Schema(description = "主页url")
	private String homeUrl;
	/**
	 * 直播间url
	 */
	@Schema(description = "直播间url")
	private String liveUrl;
	/**
	 * 主播名称
	 */
	@Schema(description = "主播名称")
	private String anchorName;
	/**
	 * 主播头像
	 */
	@Schema(description = "主播头像")
	private String anchorAvatar;
	/**
	 * 平台类型 0：抖音 1：快手 2：视频号
	 */
	@Schema(description = "平台类型 0：抖音 1：快手 2：视频号")
	private Integer platform;
	/**
	 * DouYinHomeLive(个人主页地址),DouYinLive（直播地址） 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手   其他待定
	 */
	@Schema(description = "DouYinHomeLive(个人主页地址),DouYinLive（直播地址） 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手   其他待定")
	private String platformResource;
	/**
	 * 主播userId
	 */
	@Schema(description = "主播userId")
	private String anchorUserId;
	/**
	 * webSocketId
	 */
	@Schema(description = "webSocketId")
	private String webSocketId;
	/**
	 * 主播抖音号
	 */
	@Schema(description = "主播抖音号")
	private String anchorNumber;
	/**
	 * 第三方数据平台是否已经收录 0:否 1:是
	 */
	@Schema(description = "第三方数据平台是否已经收录 0:否 1:是")
	private Integer chanmamaInclude;
	/**
	 * 系统行业id
	 */
	@Schema(description = "系统行业id")
	private Long systemTradeId;
	/**
	 * AI纠正后的行业id
	 */
	@Schema(description = "AI纠正后的行业id")
	private Long aiCorrectTradeId;

	@Schema(description = "关联统计")
	private Integer UCounts;

	@Schema(description = "关联统计用户ids")
	private List<Long> UUserIds;

	@Schema(description = "视频统计")
	private Integer VideCounts;

	@Schema(description = "白名单人数")
	private Integer UserCounts;
}
