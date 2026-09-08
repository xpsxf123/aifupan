package com.jiuyu.replay.words.bo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改视频的状态参数")
public class UpdateVideoAnalysisStatusBo {

    /**
     * 视频id
     */
    @Schema(description = "视频id")
    private String videoId;
    /**
     * 分析状态 0：未分析 1：分析中 2：分析完成 3：分析失败
     */
    @Schema(description = "分析状态 0：未分析 1：分析中 2：分析完成 3：分析失败")
    private Integer analysisStatus;
    /**
     * 失败原因
     */
    @Schema(description = "失败原因")
    private String errorReason;
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

    @Schema(description = "不生成原文，2.5.0及之前的版本默认生成，以后的版本不生成, 1：不生成，其他值为生成")
    private Integer noVideoContent;
}
