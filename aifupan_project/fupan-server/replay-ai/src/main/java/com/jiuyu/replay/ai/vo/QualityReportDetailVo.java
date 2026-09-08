package com.jiuyu.replay.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 质检报告详情响应
 *
 * @author beta
 * @date 2026-05-30
 */
@Data
public class QualityReportDetailVo {

    /**
     * 报告ID
     */
    @Schema(description = "报告ID")
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
     * 资源ID
     */
    @Schema(description = "资源ID")
    private String sourceId;

    /**
     * 报告状态 0未生成 1生成中 2已生成 3失败 4不可生成
     */
    @Schema(description = "报告状态 0未生成 1生成中 2已生成 3失败 4不可生成")
    private Integer status;

    /**
     * 摘要 JSON 字符串（前端 JSON.parse 后取 crashCount/slackCount/brandDamageCount/afterSalesCount 字段）。
     *
     * <p>2026-06-02 改造：原顶层 4 个 count 字段合并为 1 个 summaryJson；未生成或 AI 返回非法 JSON 时为 null。</p>
     */
    @Schema(description = "摘要 JSON 字符串（前端 JSON.parse；未生成或非法时为 null）")
    private String summaryJson;

    /**
     * 报告正文 HTML（来自 MongoDB）
     */
    @Schema(description = "报告正文 HTML")
    private String reportContent;

    /**
     * 主播名称（来自 anchorInfo.anchorName，无则取 userNickName）
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
     * 是否已读 0未读 1已读
     */
    @Schema(description = "是否已读 0未读 1已读")
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
