package com.jiuyu.replay.video.project.bo;

import com.jiuyu.replay.common.validated.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量提取文案业务对象
 * 状态机接口，完全参考extractFromUrlAndLocal的逻辑
 * 用于通过抖音或快手搜索展示的短视频进行批量提取文案
 *
 * @author RayChou
 * @date 2025/8/27 10:01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量提取文案业务对象")
public class VideoExtractRemoteVideoBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 来源类型
     * 3-达人视频列表，4-爆款视频
     */
    @Schema(description = "来源类型：3-达人短视频，4-爆款视频", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "来源类型不能为空")
    @EnumValue(byteValues = {3, 4}, message = "来源类型不合法")
    private Byte sourceType;

    /**
     * 来源id
     */
    @Schema(description = "来源id 短视频：用户id 搜达人：达人id 搜爆款：爆款搜索id")
    @NotNull(message = "来源ID不能为空")
    private Long sourceId;

    /**
     * 短视频ID列表
     * 来源于抖音或快手搜索展示的短视频
     * 仅在PENDING状态时需要
     */
    @Schema(description = "短视频信息列表")
    @NotNull(message = "短视频信息列表不能为空")
    @Valid
    private List<VideoExtractRemoteVideoInfoVo> videoInfoVos;


    /**
     * 视频提取项视图对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "视频提取项视图对象")
    public static class VideoExtractRemoteVideoInfoVo implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long videoId;

        private String title;

        private Byte extractStatus;

        private Integer duration;

        private String videoUrl;

        private String authorId;

        private String authorName;

        /**
         * 平台类型: 1-抖音, 2-快手, 3-视频号, 4-本地上传
         */
        @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号, 4-本地上传", example = "1")
        @NotNull(message = "平台类型不能为空")
        @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法")
        private Byte platformType;

        /**
         * 平台视频ID
         */
        @Schema(description = "平台视频ID", example = "7123456789")
        @NotBlank(message = "平台视频ID不能为空")
        @Length(max = 100, message = "平台视频ID不合法")
        private String platformVideoId;
    }
}
