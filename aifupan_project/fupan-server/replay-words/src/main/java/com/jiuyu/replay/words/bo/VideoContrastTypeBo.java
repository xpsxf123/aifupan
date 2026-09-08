package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 视频对比类型判断入参
 *
 * @author jxy
 * @email 1776764427@qq.com
 */
@Data
@Schema(description = "视频对比类型判断入参")
public class VideoContrastTypeBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 视频1的视频ID
     */
    @Schema(description = "视频1的视频ID")
    private String videoOneId;

    /**
     * 视频2的视频ID
     */
    @Schema(description = "视频2的视频ID")
    private String videoTwoId;
}
