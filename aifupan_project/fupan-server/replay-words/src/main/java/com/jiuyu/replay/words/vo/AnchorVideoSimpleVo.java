package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 主播视频VO
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-14
 */
@Data
@Schema(description = "主播视频信息")
public class AnchorVideoSimpleVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主播secUid
     */
    @Schema(description = "主播secUid")
    private String secUid;
    /**
     * 视频ID
     */
    @Schema(description = "视频ID")
    private String videoId;

}
