package com.jiuyu.replay.api.bo.datahub;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Data Hub 账号分析状态查询请求
 */
@Data
@Schema(description = "Data Hub 账号分析状态查询请求")
public class DataHubAnalysisStatsQueryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID列表
     */
    @NotEmpty(message = "tenantIds不能为空")
    @Size(max = 200, message = "tenantIds数量不能超过200")
    @Schema(description = "租户ID列表（1~200 个）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> tenantIds;

    /**
     * 统计窗口起（含），格式 yyyy-MM-dd HH:mm:ss，可空
     */
    @Schema(description = "统计窗口起（含），yyyy-MM-dd HH:mm:ss，可空")
    private String startTime;

    /**
     * 统计窗口止（不含），格式 yyyy-MM-dd HH:mm:ss，可空
     */
    @Schema(description = "统计窗口止（不含），yyyy-MM-dd HH:mm:ss，可空")
    private String endTime;
}
