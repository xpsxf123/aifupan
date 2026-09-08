package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * AI优化目的保存Bo
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-07
 */
@Data
@Schema(description = "AI优化目的保存参数")
public class AiOptimizePurposeSaveBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 来源id
     */
    @Schema(description = "来源id")
    private String sourceId;
    /**
     * 来源类型（0：视频 1：文件 2：对比）
     */
    @Schema(description = "来源类型（0：视频 1：文件 2：对比）")
    private Integer sourceType;
    /**
     * 用户id
     */
    @Schema(description = "用户id", hidden = true)
    private Long userId;
    /**
     * 租户id
     */
    @Schema(description = "租户id", hidden = true)
    private Long tenantId;
    /**
     * 优化动作
     */
    @Schema(description = "优化动作")
    private String optimizeAction;
    /**
     * 优化目的
     */
    @Schema(description = "优化目的")
    private String optimizePurpose;
}
