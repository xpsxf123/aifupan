package com.jiuyu.replay.video.project.vo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/10/25 14:33
 */
@Schema(description = "提取文案的提示词出参")
@Data
public class ExtractPromptVo {

    @Schema(description = "提示词")
    private String cueWord;

    @Schema(description = "ai的身份")
    private String aiIdentity;

    @Schema(description = "使用的ai模型")
    private Integer aiModel;
}
