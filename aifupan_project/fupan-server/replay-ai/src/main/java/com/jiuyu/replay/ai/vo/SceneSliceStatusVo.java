package com.jiuyu.replay.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 场景切片状态响应
 *
 * @author lj
 * @date 2026-07-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "场景切片状态响应")
public class SceneSliceStatusVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "状态: 0-待开始 2-处理完成")
    private Integer status;

    @Schema(description = "预签名上传URL (仅status=0时返回)")
    private String signedUrl;

    @Schema(description = "OSS文件key (仅status=0时返回)")
    private String ossKey;

    @Schema(description = "截取秒数-距视频结束 (仅status=0时返回)")
    private Integer sliceSeconds;
}
