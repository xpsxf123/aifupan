package com.jiuyu.replay.words.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "对比分析结果-云")
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class AnalysisContractResultCloudVo {
    /**
     * 视频1/文件1分析信息
     */
    @Schema(description = "视频1/文件1分析信息")
    private AnalysisResultCloudVo sentenceMark1;
    /**
     * 视频2/文件2分析信息
     */
    @Schema(description = "视频2/文件2分析信息")
    private AnalysisResultCloudVo sentenceMark2;
    /**
     * 对比信息
     */
    @Schema(description = "对比信息")
    private SyncContrastInfoVoUpper videoContrast;
}
