package com.jiuyu.replay.ai.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 手动触发报告生成请求
 *
 * @author beta
 * @date 2026-05-29
 */
@Data
public class TriggerReportBo {

    /**
     * 资源类型 0录制视频 1上传文件
     */
    @NotNull(message = "sourceType 不能为空")
    private Integer sourceType;

    /**
     * 业务场景 0复盘 1视频分析 2文案预审
     */
    @NotNull(message = "sceneType 不能为空")
    private Integer sceneType;

    /**
     * 资源ID
     */
    @NotBlank(message = "sourceId 不能为空")
    private String sourceId;

    /**
     * 监控类型 0质检 1还原度 2巡检
     */
    @NotNull(message = "monitorType 不能为空")
    private Integer monitorType;
}
