package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * AI占位符配置 新增/修改 BO
 *
 * @author jy
 * @date 2026-06-16
 */
@Data
@Schema(description = "AI占位符配置新增/修改参数")
public class AiPlaceholderBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID（修改时传）")
    private Long id;

    @Schema(description = "占位符名称")
    private String name;

    @Schema(description = "缩写（前端展示）")
    private String abbreviation;

    @Schema(description = "占位符key")
    private String placeholderKey;

    @Schema(description = "参数说明")
    private String paramDesc;

    @Schema(description = "功能说明")
    private String description;

    @Schema(description = "细节说明")
    private String detail;

    @Schema(description = "分类：1-默认展示，2-更多参数")
    private Integer category;

    @Schema(description = "是否前端展示 0否 1是")
    private Integer isFrontendShow;

    @Schema(description = "是否默认勾选 0否 1是")
    private Integer isDefaultChecked;

    @Schema(description = "状态 0启用 1禁用")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "租户ID")
    private Long tenantId;
}
