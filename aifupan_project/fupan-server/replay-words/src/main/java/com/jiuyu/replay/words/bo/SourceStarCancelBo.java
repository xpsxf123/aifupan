package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 取消星标参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-07
 */
@Data
@Schema(description = "取消星标参数")
public class SourceStarCancelBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 来源id（视频的video_id、文件的file_id、对比的contrast_id）
     */
    @Schema(description = "来源id（视频的video_id、文件的file_id、对比的contrast_id）")
    private String sourceId;
    /**
     * 来源类型（0：视频 1：文件 2：对比）
     */
    @Schema(description = "来源类型（0：视频 1：文件 2：对比）")
    private Integer sourceType;
}
