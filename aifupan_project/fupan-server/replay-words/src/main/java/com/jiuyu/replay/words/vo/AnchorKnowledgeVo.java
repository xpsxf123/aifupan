package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 主播级别知识库详情（聚合三种类型）
 *
 * @author jy
 * @date 2026-06-29
 */
@Data
@Schema(description = "主播级别知识库详情")
public class AnchorKnowledgeVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主播唯一标识")
    private String secUid;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "运营知识库内容")
    private String operationContent;

    @Schema(description = "敏感词知识库内容")
    private String sensitiveContent;

    @Schema(description = "直播间健康值")
    private String healthScore;

    @Schema(description = "创建时间")
    private Date createDate;

    @Schema(description = "更新时间")
    private Date updateDate;
}
