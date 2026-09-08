package com.jiuyu.replay.video.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author RayChou
 * @date 2025/8/14 18:31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "短视频提取文案历史记录业务对象")
public class VideoExtractQueryVo {

    @Schema(description = "短视频提取文案ID", example = "1")
    private Long id;

    @Schema(description = "短视频ID", example = "1")
    private Long videoId;

    @Schema(description = "文案提取ID", example = "1")
    private Long extractId;

    @Schema(description = "短视频名称", example = "精彩视频分享")
    private String videoTitle;

    @Schema(description = "类型：1: 短视频URL 2:本地上传", example = "1")
    private Byte sourceType;

    @Schema(description = "短视频URL 本地文件-本地文件路径")
    private String videoUrl;

    @Schema(description = "视频文件hash值")
    private String videoHash;

    @Schema(description = "封面图片URL", example = "https://example.com/cover.jpg")
    private String coverUrl;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "操作人昵称")
    private String nickName;

    @Schema(description = "提取时间")
    private LocalDateTime extractTime;

    @Schema(description = "文案提取状态: 0-未提取 1-待处理, 2-处理中, 3-已完成, 4-失败", example = "1")
    private Byte extractStatus;

    @Schema(description = "文案内容")
    private String extractContent;

    @Schema(description = "视频时长(秒)", example = "300")
    private Integer duration;

    @Schema(description = "文案提取错误原因")
    private String extractErrorReason;
}
