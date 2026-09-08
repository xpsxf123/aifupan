package com.jiuyu.replay.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * SEO 分类下拉选项。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@Schema(description = "SEO 分类下拉选项")
public class SeoCategoryOptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "别名")
    private String slug;
}
