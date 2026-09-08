package com.jiuyu.replay.video.project.vo;

import com.jiuyu.replay.video.project.entity.VideoUserVideoEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 视频提取结果视图对象
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 视频提取操作的返回结果
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频提取结果视图对象")
public class VideoExtractVo extends VideoUserVideoEntity {

    @Schema(description = "视频URL")
    private String videoUrl;

    @Schema(description = "视频提取文案", example = "这是一段文案")
    private String extractContent;

    @Schema(description = "作者ID", example = "MS4wLjABAAAAZRA0x4QdqhMJBbK-bjWMb0StjdftXlNjBhP0uh0sPmxkMbDKjxMJAPCR-5bVO9u_")
    private String authorId;

    @Schema(description = "作者名称", example = "一条小团团")
    private String authorName;
}
