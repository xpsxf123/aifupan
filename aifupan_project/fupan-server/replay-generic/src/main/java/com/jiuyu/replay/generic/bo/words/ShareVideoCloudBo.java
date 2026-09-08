package com.jiuyu.replay.generic.bo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "分享视频到云空间数据对象")
public class ShareVideoCloudBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 视频id
     */
    @Schema(description = "视频id")
    private String videoId;

    /**
     * 视频在线播放地址
     */
    @Schema(description = "视频在线播放地址")
    private String onlineFileUrl;
    /**
     * 云空间备注
     */
    @Schema(description ="云空间备注")
    private String cloudRemarks;
}
