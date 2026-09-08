package com.jiuyu.replay.generic.vo.words;

import com.jiuyu.replay.generic.vo.words.anchor.AnchorUrlTradeVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 主播url信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-10 16:47:37
 */
@Data
@Schema(description = "主播url信息")
public class AnchorUrlVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 主播唯一标识
	 */
	@Schema(description = "")
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

	@Schema(description = "修改时间")
	private Date updateDate;

	@Schema(description = "添加时间")
	private Date createDate;
	/**
	 * 主播抖音号
	 */
	@Schema(description = "主播抖音号")
	private String anchorNumber;


	@Schema(description = "状态 0监控   1 删除 ")
	private Integer isDeleted;

	@Schema(description = "用户名称 ")
	private String userName;

	@Schema(description = "账号名称 ")
	private String nickName;

	@Schema(description = "账号名称 ")
	private Long userId;

	@Schema(description = "行业Id ")
	private Long tradeId;

	/**
	 * 拥有百名单书
	 */
	@Schema(description = "拥有百名单书 ")
	private Integer userCounts;

	/**
	 * 已被账号添加数
	 */
	@Schema(description = "已被账号添加数 ")
	private Integer uCounts;
	/**
	 * 已被录制数
	 */
	@Schema(description = "已被录制数 ")
	private Integer videCounts;

	/**
	 * 账号归属类型 0：自由账号 1：同行账号
	 */
	@Schema(description = "账号归属类型 0：自由账号 1：同行账号 ")
	private Integer accountType;

	/**
	 * 主播账号情况描述
	 */
	@Schema(description = "主播账号情况描述")
	private String anchorSituation;

    @Schema(description = "行业")
    private List<AnchorUrlTradeVo> anchorUrlTradeList;

    @Schema(description = "系统行业id")
    private Long systemTradeId;

    @Schema(description = "系统行业名称")
    private String systemTradeName;

    @Schema(description = "AI纠正行业id")
    private Long aiCorrectTradeId;

    @Schema(description = "AI纠正行业名称")
    private String aiCorrectTradeName;

}
