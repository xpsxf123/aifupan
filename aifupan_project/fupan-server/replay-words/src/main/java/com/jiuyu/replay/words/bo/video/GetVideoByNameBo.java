package com.jiuyu.replay.words.bo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 根据视频名称获取视频信息入参
 */
@Data
@Schema(description = "根据视频名称获取视频信息入参")
public class GetVideoByNameBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 视频名称
     */
    @Schema(description = "视频名称")
    private String videoName;
}
