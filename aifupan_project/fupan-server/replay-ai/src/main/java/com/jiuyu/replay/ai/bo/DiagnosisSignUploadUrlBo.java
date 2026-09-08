package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/28 下午11:46
 */
@Data
@Schema(description = "获取诊断报告的预上传url")
public class DiagnosisSignUploadUrlBo {


    @Schema(description = "来源id")
    @NotNull(message = "来源id不能为空")
    private  String sourceId;

    @Schema(description = "来源类型 0视频，1文件，2对比分析")
    private  Integer sourceType;

    @Schema(description = "行业id")
    private Long tradeId;

    @Schema(description = "上传类型 0内容诊断，1数据诊断")
    private Integer uploadType;




}
