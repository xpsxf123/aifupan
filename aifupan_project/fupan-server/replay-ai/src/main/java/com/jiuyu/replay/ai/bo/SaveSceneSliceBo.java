package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 保存场景切片记录请求
 *
 * @author lj
 * @date 2026-07-06
 */
@Data
@Schema(description = "保存场景切片记录请求")
public class SaveSceneSliceBo {

    @Schema(description = "视频ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "视频ID不能为空")
    private String videoId;

    @Schema(description = "OSS文件key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "OSS文件key不能为空")
    private String ossKey;

    @Schema(description = "截取秒数（距视频结束的秒数）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "截取秒数不能为空")
    private Integer sliceSeconds;
}
