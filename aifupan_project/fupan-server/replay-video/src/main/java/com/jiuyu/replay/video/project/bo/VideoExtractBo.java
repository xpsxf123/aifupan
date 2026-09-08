package com.jiuyu.replay.video.project.bo;

import com.jiuyu.replay.common.validated.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频URL提取文案业务对象
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 通过视频URL提取文案的请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "短视频提取文案业务对象")
public class VideoExtractBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键id", example = "1")
    private Long id;

    @Schema(description = "视频来源：1: 短视频URL 2:本地上传 3：搜达人  4：搜爆款", example = "1")
    @NotNull(message = "视频来源：1: 短视频URL 2:本地上传 3：搜达人  4：搜爆款")
    @EnumValue(byteValues = {1, 2, 3, 4}, message = "视频来源不合法")
    private Byte sourceType;

    @Schema(description = "短视频名称", example = "精彩视频分享")
    @Length(max = 500, message = "短视频名称不合法")
    private String videoTitle;

    @Schema(description = "短视频文件hash值", example = "7123456789012345678")
    @Length(max = 50, message = "短视频文件hash值不合法")
    private String videoHash;

    @Schema(description = "视频URL", example = "https://v.douyin.com/iFyMvPsj/")
    private String videoUrl;

    @Schema(description = "文案提取状态: 0-未提取 1-待处理, 2-处理中, 3-已完成, 4-失败", example = "1")
    @NotNull(message = "文案提取状态: 0-未提取 1-待处理, 2-处理中, 3-已完成, 4-失败")
    @EnumValue(byteValues = {1, 2, 3, 4}, message = "文案提取状态不合法")
    private Byte extractStatus;

    @Schema(description = "AI优化文案内容", example = "这是AI优化文案内容")
    private String extractContent;

    @Schema(description = "原文文案内容", example = "这是原文文案内容")
    private String originalExtractContent;

    @Schema(description = "视频时长", example = "60")
    private Integer duration;

    @Schema(description = "封面图片URL", example = "https://example.com/cover.jpg")
    private String coverUrl;

    @Schema(description = "提取失败原因", example = "时长超过10分钟")
    private String extractErrorReason;
}
