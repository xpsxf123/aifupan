package com.jiuyu.replay.video.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author RayChou
 * @date 2025/8/15 17:49
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文案数据结构视图对象")
public class VideoContentExtractVo {

    @Schema(description = "视频文件hash值")
    private String videoHash;

    @Schema(description = "文案内容")
    private String audioContent;
}
