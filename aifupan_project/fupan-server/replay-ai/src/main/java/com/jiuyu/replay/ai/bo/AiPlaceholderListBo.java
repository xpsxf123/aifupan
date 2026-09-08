package com.jiuyu.replay.ai.bo;

import com.jiuyu.replay.generic.bo.common.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * AI占位符配置 列表查询 BO
 *
 * @author jy
 * @date 2026-06-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "AI占位符配置列表查询参数")
public class AiPlaceholderListBo extends PageBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索关键词")
    private String keyword;

    @Schema(description = "分类：1-默认展示，2-更多参数")
    private Integer category;

    @Schema(description = "前端展示：1-是，0-否")
    private Integer isFrontendShow;

    @Schema(description = "状态：0-启用，1-禁用")
    private Integer status;
}
