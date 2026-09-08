package com.jiuyu.replay.words.bo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改文件的分析状态参数")
public class UpdateFileAnalysisStatusBo {

    /**
     * 文件id
     */
    @Schema(description = "文件id")
    private String fileId;
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

}
