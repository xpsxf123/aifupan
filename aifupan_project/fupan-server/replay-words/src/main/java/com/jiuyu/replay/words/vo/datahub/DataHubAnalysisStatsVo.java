package com.jiuyu.replay.words.vo.datahub;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Data Hub 账号分析聚合数据项（模块内传输对象）
 * 基于 tb_anchor_video 成功分析记录（analysis_status=2）按 租户×业务账号 聚合
 */
@Data
@Schema(description = "Data Hub 账号分析聚合数据项")
public class DataHubAnalysisStatsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID")
    private Long tenantId;

    /**
     * 业务账号 sec_uid
     */
    @Schema(description = "业务账号 sec_uid")
    private String secUid;

    /**
     * 成功分析次数
     */
    @Schema(description = "成功分析次数")
    private Long analysisCount;

    /**
     * 首次成功分析时间
     */
    @Schema(description = "首次成功分析时间")
    private Date firstAnalyzedDate;

    /**
     * 最近一次成功分析时间
     */
    @Schema(description = "最近一次成功分析时间")
    private Date lastAnalyzedDate;
}
