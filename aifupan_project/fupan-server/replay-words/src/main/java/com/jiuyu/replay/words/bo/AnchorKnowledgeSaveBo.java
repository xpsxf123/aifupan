package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 主播级别知识库新增/修改请求
 *
 * @author jy
 * @date 2026-06-29
 */
@Data
@Schema(description = "主播级别知识库新增/修改请求")
public class AnchorKnowledgeSaveBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主播唯一标识")
    private String secUid;

    @Schema(description = "用户ID（后端从JWT注入，前端不传）", hidden = true)
    private Long userId;

    @Schema(description = "运营知识库内容")
    private String operationContent;

    @Schema(description = "敏感词知识库内容")
    private String sensitiveContent;

    @Schema(description = "直播间健康值")
    private String healthScore;
}
