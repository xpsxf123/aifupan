package com.jiuyu.replay.system.vo.site;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 官网标签项，供标签云使用。
 *
 * @author claude
 * @date 2026-08-18
 */
@Data
@Schema(description = "官网标签项")
public class SeoSiteTagVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "标签别名，官网 /tag/{slug}")
    private String slug;

    /**
     * 关联文章数，<b>只算已发布</b>，口径同 {@link SeoSiteCategoryVo#getArticleCount()}。
     *
     * <p>本接口<b>不做 {@code < 3} 的过滤</b>：标签云要展示全部有内容的标签，
     * 薄内容页的 noindex 由官网在标签页用列表的 totalCount 自行判断。
     * 只有 sitemap 接口才按阈值过滤——那里是主动提交给搜索引擎，口径不同。
     */
    @Schema(description = "已发布文章数")
    private Integer articleCount;
}
