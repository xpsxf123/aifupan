package com.jiuyu.replay.words.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 客户端对比数据信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-26 14:51:59
 */
@Data
@Schema(description = "客户端对比数据信息")
public class SyncContrastVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 视频1的视频ID
	 */
	@Schema(description = "视频1的视频ID")
	private String videoOneId;
	/**
	 * 视频2的视频ID
	 */
	@Schema(description = "视频2的视频ID")
	private String videoTwoId;
	/**
	 * 对比时间
	 */
	@Schema(description = "对比时间")
	private String contrastTime;
	/**
	 * 主播1的secUid
	 */
	@Schema(description = "主播1的secUid")
	private String anchorOneId;
	/**
	 * 主播2的secUid
	 */
	@Schema(description = "主播2的secUid")
	private String anchorTwoId;
	/**
	 * 文件1的文件id
	 */
	@Schema(description = "文件1的文件id")
	private String fileOneId;
	/**
	 * 文件2的文件id
	 */
	@Schema(description = "文件2的文件id")
	private String fileTwoId;
	/**
	 * 对比的唯一标识
	 */
	@Schema(description = "对比的唯一标识")
	private String contrastId;
	/**
	 * 切片对比类型 0：原视频 1：复盘切片视频 2：短视频切片视频
	 */
	@Schema(description = "切片对比类型 0：原视频 1：复盘切片视频 2：短视频切片视频")
	private Integer sliceContrastType;
	/**
	 * 对比行业1
	 */
	@Schema(description = "对比行业1")
	private Long tradeOneId;
	/**
	 * 对比行业2
	 */
	@Schema(description = "对比行业2")
	private Long tradeTwoId;
	/**
	 * 是否已分享 0：否 1：是
	 */
	@Schema(description = "是否已分享 0：否 1：是")
	private Integer isShard;
	/**
	 * 在线复盘的url
	 */
	@Schema(description = "在线复盘的url")
	private String shareUrl;
	/**
	 * 删除状态 0：未删除 1：已从对比复盘列表删除 2：已从对比复盘列表和云空间删除
	 */
	@Schema(description = "删除状态 0：未删除 1：已从对比复盘列表删除 2：已从对比复盘列表和云空间删除")
	private Integer deleteStatus;
	/**
	 * 租户id
	 */
	@Schema(description = "租户id")
	private Long tenantId;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;
	/**
	 * 云空间备注
	 */
	@Schema(description ="云空间备注")
	@JsonProperty("cloudRemarks")
	private String cloudRemarks;
	/**
	 * 对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比
	 */
	@Schema(description = "对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比")
	@JsonProperty("syncScene")
	private Integer syncScene;
	/**
	 * 对比类型 0：视频对比 1：文件对比
	 */
	@Schema(description = "对比类型 0：视频对比 1：文件对比")
	@JsonProperty("contrastType")
	private Integer contrastType;

}
