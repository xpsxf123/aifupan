package com.jiuyu.replay.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单个监控类型的报告状态
 */
@Data
public class MonitorTypeStatusVo {

    @Schema(description = "监控类型 0质检 1还原度 2巡检")
    private Integer monitorType;

    @Schema(description = "监控类型文案")
    private String monitorTypeText;

    @Schema(description = "状态 0未生成 1生成中 2已生成 3失败 4不可生成")
    private Integer status;

    @Schema(description = "状态文案")
    private String statusText;

    @Schema(description = "报告ID，未生成时为 null")
    private Long reportId;

    /**
     * 报告摘要（原始 JSON 字符串）。
     *
     * <p>2026-06-02 改造：9 列合并为 1 列 summary_json TEXT；类型从嵌套 Object 改为 String。
     * 前端按 monitorType 自行 JSON.parse 取字段（质检 crashCount 等 / 还原度 score 等 / 巡检 effectivenessPercentage 等）。
     * 未生成 / 生成中 / 失败 / 不可生成 / AI 返回非法 JSON → 返 null。</p>
     */
    @Schema(description = "报告摘要原始 JSON 字符串（前端 JSON.parse；未生成或失败时为 null）")
    private String summary;

    /**
     * 是否已读 0未读 1已读（来源主表 tb_script_monitor_report.is_read；仅录制人本人首次打开 detail 接口后置1）
     */
    @Schema(description = "是否已读 0未读 1已读（来源主表 tb_script_monitor_report.is_read）")
    private Integer isRead;

    @Schema(description = "不可生成原因")
    private String unavailableReason;

    /**
     * 该 monitorType 在该直播间的开关状态。
     *
     * <p>来自 tb_anchor_url_user 三开关字段，按 (userId, tenantId, secUid) 查询：
     * 0=已关闭 / 1=已开启 / null=不适用（请求未带 secUid、上传文件场景或 anchor_url_user 无记录）。</p>
     */
    @Schema(description = "监控开关 0关 1开 null不适用")
    private Integer monitorEnabled;
}
