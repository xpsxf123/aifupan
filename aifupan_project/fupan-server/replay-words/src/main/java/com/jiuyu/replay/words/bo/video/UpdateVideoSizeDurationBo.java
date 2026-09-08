package com.jiuyu.replay.words.bo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改视频的大小时长参数")
public class UpdateVideoSizeDurationBo {

    /**
     * 视频id
     */
    @Schema(description = "视频id")
    public String videoId;
    /**
     * 时长，秒
     */
    @Schema(description = "时长，秒")
    public Long duration;
    /**
     * 文件大小，B
     */
    @Schema(description = "文件大小，B")
    public Long fileSize;
    /**
     * 录制结束时间
     */
    @Schema(description = "录制结束时间")
    public String endTime;
    /**
     * 是否在录制 0：否 1：是
     */
    @Schema(description = "是否在录制 0：否 1：是")
    public Integer isRecording;
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
