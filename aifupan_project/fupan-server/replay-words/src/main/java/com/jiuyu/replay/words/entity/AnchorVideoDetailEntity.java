package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 视频的详情
 *
 * @author lj
 * @email 
 * @date 2025-05-26 16:34:59
 */
@Data
@TableName("tb_anchor_video_detail")
public class AnchorVideoDetailEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 视频唯一标识
	 */
	private String videoId;
	/**
	 * 自然原文生成状态 0待生成，1生成中，2生成成功，3生成失败
	 */
	private Integer natureContentStatus;
	/**
	 * 优化原文生成状态 0待生成，1生成中，2生成成功，3生成失败
	 */
	private Integer optimizeContentStatus;
	/**
	 * 是否有诊断报告
	 */
	private Integer hasDiagnosisReport;
	/**
	 * 最新诊断报告文件名称
	 */
	private String  diagnosisOssName;

    /**
     * 是否有数据诊断报告
     */
    private Integer hasDataDiagnosisReport;
    /**
     * 最新数据诊断报告文件名称
     */
    private String dataDiagnosisOssName;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 租户id
	 */
	private Long tenantId;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 更新时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;

	/**
	 * 自然原文上次发送时间
	 */
	private Long natSetTime;
	/**
	 * 优化原文上次发送时间
	 */
	private Long optSetTime;

	/**
	 * 自然原文 xxjob处理状态 0否  1扫
	 */
	private Integer natSetJob;

	/**
	 * 优化原文 xxjob处理状态 0否  1扫
	 */
	private Integer optSetJob;

	/**
	 * 自然原文生成来源 0服务器，1客户端
	 */
	private Integer natureSourceType;

	/**
	 * 优化原文生成来源 0服务器，1客户端
	 */
	private Integer optimizeSourceType;

    /**
     * 是否已推荐行业 0未推荐，1已推荐
     */
    private Integer suggestTrade;
    /**
     * 推荐行业Id
     */
    private Long suggestTradeId;

    /**
     * 重要弹幕生成状态：0未获取，1获取中，2获取成功，3获取失败
     */
    private Integer importantBarrageStatus;

    /**
     * 重要弹幕生成开始时间
     */
    private Date importantBarrageTime;

    /**
     * 重要弹幕生成错误原因
     */
    private String importantBarrageError;


}
