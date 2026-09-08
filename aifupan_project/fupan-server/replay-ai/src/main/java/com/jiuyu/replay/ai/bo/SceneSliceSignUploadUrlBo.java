package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 获取场景切片上传预签名URL请求
 *
 * @author lj
 * @date 2026-07-06
 */
@Data
@Schema(description = "获取场景切片上传预签名URL请求")
public class SceneSliceSignUploadUrlBo {

    @Schema(description = "视频ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "视频ID不能为空")
    private String videoId;
}
