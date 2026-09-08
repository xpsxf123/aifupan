package com.jiuyu.replay.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 报告状态查询响应（单个资源，含三类监控状态）
 */
@Data
public class ScriptMonitorReportStatusVo {

    @Schema(description = "资源类型 0录制视频 1上传文件")
    private Integer sourceType;

    @Schema(description = "业务场景 0复盘 1视频分析 2文案预审")
    private Integer sceneType;

    @Schema(description = "资源ID")
    private String sourceId;

    @Schema(description = "三类监控状态(质检/还原度/巡检)")
    private List<MonitorTypeStatusVo> monitors;
}
