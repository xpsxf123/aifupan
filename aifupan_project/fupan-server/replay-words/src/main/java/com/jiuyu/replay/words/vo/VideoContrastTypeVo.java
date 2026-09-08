package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 视频对比类型判断结果
 *
 * @author jxy
 * @email 1776764427@qq.com
 */
@Data
@Schema(description = "视频对比类型判断结果")
public class VideoContrastTypeVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 优化场次视频ID
     */
    @Schema(description = "优化场次视频ID")
    private String optimizeVideoId;

    /**
     * 对标场次视频ID
     */
    @Schema(description = "对标场次视频ID")
    private String benchmarkVideoId;
}
