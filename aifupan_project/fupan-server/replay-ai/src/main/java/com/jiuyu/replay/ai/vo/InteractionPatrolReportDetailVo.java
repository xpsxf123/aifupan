package com.jiuyu.replay.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 互动巡检报告详情响应 VO。
 *
 * <p>由 {@link com.jiuyu.replay.ai.bll.ScriptMonitorBll#patrolReportDetail(Long)} 返回，
 * 通过 {@code GET /replay/script-monitor/patrolReportDetail} 暴露给前端。</p>
 *
 * @author beta
 * @date 2026-06-04
 */
@Data
public class InteractionPatrolReportDetailVo {

    /**
     * 报告 ID
     */
    @Schema(description = "报告 ID")
    private Long reportId;

    /**
     * 资源类型 0录制视频 1上传文件
     */
    @Schema(description = "资源类型 0录制视频 1上传文件")
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
     * 摘要 Markdown 文本片段（来自 AI 合并报告 {@code <aifupan-data-block>} 标签内文本 trim 后）。
     *
     * <p>2026-06-07 改造：与话术质检统一为 Markdown + 标签提取范式。前端**直接渲染**，
     * 不再需要 {@code JSON.parse}。未生成 / 无标签 / 内容 null 时为 null；空标签返回 {@code ""}。</p>
     *
     * <p>字段名沿用 {@code summaryJson} 是历史命名（最初存 JSON 子对象 toJSONString），
     * 现存 Markdown 文本片段；为零迁移成本保留字段名。</p>
     */
    @Schema(description = "摘要 Markdown 文本片段（直接渲染，不再需要 JSON.parse；来自 AI 合并报告 <aifupan-data-block> 标签内文本 trim 后；未生成 / 无标签时为 null；空标签返回 ''；字段名沿用历史命名）")
    private String summaryJson;

    /**
     * 合并报告 Markdown 全文（来自 MongoDB）。
     *
     * <p>2026-06-07 起从 JSON 改 Markdown 格式（与话术质检统一）。前端直接渲染，
     * 不再需要 {@code JSON.parse}。</p>
     */
    @Schema(description = "合并报告 Markdown 全文（来自 MongoDB，含弹幕明细表格 + <aifupan-data-block> 标签包裹的摘要段；前端直接渲染）")
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
     * 是否已读 0未读 1已读（录制人首次查看 detail 接口后自动置1；来源：tb_script_monitor_report.is_read）
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
