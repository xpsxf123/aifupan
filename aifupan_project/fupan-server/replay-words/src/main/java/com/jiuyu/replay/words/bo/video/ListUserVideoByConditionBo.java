package com.jiuyu.replay.words.bo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "查询用户视频参数")
public class ListUserVideoByConditionBo {

    /**
     * 分析状态 0：未分析 1：分析中 2：分析完成 3分析错误
     */
    @Schema(description = "分析状态 0：未分析 1：分析中 2：分析完成 3分析错误")
    private Integer analysisStatus;
    /**
     * 是否正在录制 0否 1是
     */
    @Schema(description = "是否正在录制 0否 1是")
    private Integer isRecording;
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
    /**
     * 视频切片类型 0：原视频 1：复盘切片视频 2：短视频切片视频
     */
    @Schema(description ="视频切片类型 0：原视频 1：复盘切片视频 2：短视频切片视频")
    private Integer videoSliceType;
}
