package com.jiuyu.replay.words.bo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 视频的详情信息
 *
 * @author lj
 * @email 
 * @date 2025-05-26 16:34:59
 */
@Data
@Schema(description = "视频的详情信息")
public class AnchorVideoDetailBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 视频唯一标识
	 */
	@Schema(description = "视频唯一标识")
	private String videoId;
	/**
	 * 自然原文生成状态 0待生成，1生成中，2生成成功，3生成失败
	 */
	@Schema(description = "自然原文生成状态 0待生成，1生成中，2生成成功，3生成失败")
	private Integer natureContentStatus;
	/**
	 * 优化原文生成状态 0待生成，1生成中，2生成成功，3生成失败
	 */
	@Schema(description = "优化原文生成状态 0待生成，1生成中，2生成成功，3生成失败")
	private Integer optimizeContentStatus;
	/**
	 * 是否有诊断报告
	 */
	@Schema(description = "是否有诊断报告")
	private Integer hasDiagnosisReport;
	/**
	 * 最新诊断报告文件名称
	 */
	@Schema(description = "最新诊断报告文件名称")
	private String  diagnosisOssName;
    /**
     * 是否有数据诊断报告
     */
    @Schema(description = "是否有数据诊断报告")
    private Integer hasDataDiagnosisReport;
    /**
     * 最新数据诊断报告文件名称
     */
    @Schema(description = "最新数据诊断报告文件名称")
    private String dataDiagnosisOssName;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
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
	 * 更新时间
	 */
	@Schema(description = "更新时间")
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;
	/**
	 * 自然原文 xxjob处理状态 0否  1扫
	 */
	@Schema(description = "自然原文 xxjob处理状态 0否  1扫")
	private Integer natSetJob;

	/**
	 * 优化原文 xxjob处理状态 0否  1扫
	 */
	@Schema(description = "优化原文 xxjob处理状态 0否  1扫")
	private Integer optSetJob;

	/**
	 * 自然原文生成来源 0服务器，1客户端
	 */
	@Schema(description = "自然原文生成来源 0服务器，1客户端")
	private Integer natureSourceType;

	/**
	 * 优化原文生成来源 0服务器，1客户端
	 */
	@Schema(description = "优化原文生成来源 0服务器，1客户端")
	private Integer optimizeSourceType;

    /**
     * 是否已推荐行业 0未推荐，1已推荐
     */
    @Schema(description = "是否已推荐行业 0未推荐，1已推荐")
    private Integer suggestTrade;

    /**
     * 推荐行业Id
     */
    @Schema(description = "推荐行业Id")
    private Long suggestTradeId;

    /**
     * 重要弹幕生成状态：0未获取，1获取中，2获取成功，3获取失败
     */
    @Schema(description = "重要弹幕生成状态：0未获取，1获取中，2获取成功，3获取失败")
    private Integer importantBarrageStatus;

    /**
     * 重要弹幕生成开始时间
     */
    @Schema(description = "重要弹幕生成开始时间")
    private Date importantBarrageTime;

    /**
     * 重要弹幕生成错误原因
     */
    @Schema(description = "重要弹幕生成错误原因")
    private String importantBarrageError;
}
