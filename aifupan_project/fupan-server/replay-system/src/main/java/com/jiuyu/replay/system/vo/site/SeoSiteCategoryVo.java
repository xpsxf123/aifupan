package com.jiuyu.replay.system.vo.site;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 官网分类项。
 *
 * @author claude
 * @date 2026-08-18
 */
@Data
@Schema(description = "官网分类项")
public class SeoSiteCategoryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类名称，用作栏目页 h1")
    private String categoryName;

    @Schema(description = "分类别名，官网 /category/{slug}")
    private String slug;

    @Schema(description = "分类描述，用作栏目页 meta description")
    private String description;

    /**
     * 关联文章数，<b>只算已发布</b>。
     *
     * <p>与后台 {@code SeoCategoryListVo.articleCount}「含已下架」的口径<b>相反</b>：
     * 官网用这个数字决定空分类页是否 404，含下架会出现「数字是 5、列表却查出 0 篇」的空壳页。
     */
    @Schema(description = "已发布文章数")
    private Integer articleCount;
}
