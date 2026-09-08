package com.jiuyu.replay.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 话术还原度报告详情响应 VO。
 *
 * <p>由 {@link com.jiuyu.replay.ai.bll.ScriptMonitorBll#fidelityReportDetail(Long)} 返回，
 * 通过 {@code GET /replay/script-monitor/fidelityReportDetail} 暴露给前端。</p>
 *
 * <p>字段定义 1:1 mirror {@link QualityReportDetailVo}（13 字段），summaryJson Schema 描述
 * 与互动巡检 {@link InteractionPatrolReportDetailVo} 对齐（实际存 Markdown 文本）。</p>
 *
 * @author beta
 * @date 2026-06-13
 */
@Data
public class FidelityReportDetailVo {

    /**
     * 报告 ID
     */
    @Schema(description = "报告 ID")
    private Long reportId;

    /**
     * 资源类型 0录制视频 1上传文件（还原度固定 0）
     */
    @Schema(description = "资源类型 0录制视频 1上传文件（还原度固定 0）")
    private Integer sourceType;

    /**
     * 业务场景 0复盘 1视频分析 2文案预审
     */
    @Schema(description = "业务场景 0复盘 1视频分析 2文案预审")
    private Integer sceneType;

    /**
     * 资源 ID（videoId）
     */
    @Schema(description = "资源 ID（videoId）")
    private String sourceId;

    /**
     * 报告状态 0未生成 1生成中 2已生成 3失败 4不可生成
     */
    @Schema(description = "报告状态 0未生成 1生成中 2已生成 3失败 4不可生成")
    private Integer status;

    /**
     * 摘要 Markdown 文本（字段名为历史命名，实际存 Markdown；mirror 巡检 patrol-summary-md-cast 范式）。
     *
     * <p>未生成时为 null；前端直接渲染，不需要 {@code JSON.parse}。</p>
     */
    @Schema(description = "摘要 Markdown 文本（字段名为历史命名，实际存 Markdown；与巡检同范式；未生成时为 null）")
    private String summaryJson;

    /**
     * 报告正文 Markdown 全文（来自 MongoDB）
     */
    @Schema(description = "报告正文 Markdown 全文（来自 MongoDB；前端直接渲染）")
    private String reportContent;

    /**
     * 主播名称
     */
    @Schema(description = "主播名称")
    private String anchorName;

    /**
     * 直播标题
     */
    @Schema(description = "直播标题")
    private String liveTitle;

    /**
     * 直播时间（录制开始时间）
     */
    @Schema(description = "直播时间（录制开始时间）")
    private Date liveTime;

    /**
     * 是否已读 0未读 1已读（录制人首次查看 detail 接口后自动置 1；来源：tb_script_monitor_report.is_read）
     */
    @Schema(description = "是否已读 0未读 1已读（首次查看自动置 1）")
    private Integer isRead;

    /**
     * 确认时间（录制人首次查看时间；来源：tb_script_monitor_report.confirmed_at；未读时为 null）
     */
    @Schema(description = "确认时间（录制人首次查看时间；来源 tb_script_monitor_report.confirmed_at；未读时 null）")
    private Date confirmedAt;

    /**
     * 报告创建时间
     */
    @Schema(description = "报告创建时间")
    private Date createDate;
}
