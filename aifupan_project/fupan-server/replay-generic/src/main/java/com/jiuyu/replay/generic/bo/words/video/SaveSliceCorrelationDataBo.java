package com.jiuyu.replay.generic.bo.words.video;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存切片相关数据")
public class SaveSliceCorrelationDataBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 切片视频所属原视频id
     */
    @Schema(description = "切片视频所属原视频id")
    @NotBlank
    private String sourceVideoId;
    /**
     * 切片视频id
     */
    @Schema(description = "切片视频id")
    @NotBlank
    private String sliceVideoId;
    /**
     * 切片视频自然时间戳-开始
     */
    @Schema(description = "切片视频自然时间戳-开始")
    private Long sliceStartNaturalTime;
    /**
     * 切片视频自然时间戳-结束
     */
    @Schema(description = "切片视频自然时间戳-结束")
    private Long sliceEndNaturalTime;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;
}
