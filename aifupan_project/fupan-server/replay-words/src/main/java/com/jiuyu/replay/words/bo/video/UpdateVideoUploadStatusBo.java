package com.jiuyu.replay.words.bo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改视频的状态参数")
public class UpdateVideoUploadStatusBo {

    /**
     * 视频id
     */
    @Schema(description = "视频id")
    private String videoId;
    /**
     * 视频上传状态 0：未上传 1：已上传 2：上传中
     */
    @Schema(description = "视频上传状态 0：未上传 1：已上传 2：上传中")
    private Integer uploadStatus;
    /**
     * 在线视频播放地址
     */
    @Schema(description = "在线视频播放地址")
    private String videoOnlinePayUrl;
    /**
     * 分享地址
     */
    @Schema(description = "分享地址")
    private String shareUrl;
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
