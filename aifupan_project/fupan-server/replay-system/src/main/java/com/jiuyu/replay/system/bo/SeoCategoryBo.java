package com.jiuyu.replay.system.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * SEO 分类新增 / 修改入参。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@Schema(description = "SEO 分类新增/修改入参")
public class SeoCategoryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，新增时不传 */
    @Schema(description = "主键，新增时不传")
    private Long id;

    /** 分类名称，必填，1-20 字 */
    @Schema(description = "分类名称，必填")
    private String categoryName;

    /** 别名，留空时由名称生成拼音 */
    @Schema(description = "别名，留空时由名称生成拼音")
    private String slug;

    /** 分类描述 */
    @Schema(description = "分类描述")
    private String description;

    /** 排序，越小越靠前 */
    @Schema(description = "排序，越小越靠前")
    private Integer sort;
}
