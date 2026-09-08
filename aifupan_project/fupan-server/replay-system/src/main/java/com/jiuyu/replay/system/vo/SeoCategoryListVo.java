package com.jiuyu.replay.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SEO 分类列表项。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@Schema(description = "SEO 分类列表项")
public class SeoCategoryListVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "别名")
    private String slug;

    @Schema(description = "分类描述")
    private String description;

    @Schema(description = "排序")
    private Integer sort;

    /** 关联文章数，含已下架文章——该数字用于判断内容存量，下架内容依然存在 */
    @Schema(description = "关联文章数，含已下架")
    private Integer articleCount;

    @Schema(description = "创建时间")
    private LocalDateTime createDate;
}
