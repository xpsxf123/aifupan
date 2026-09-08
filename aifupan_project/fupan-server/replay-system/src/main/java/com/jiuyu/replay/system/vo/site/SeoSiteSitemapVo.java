package com.jiuyu.replay.system.vo.site;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * sitemap 全量数据，供官网 /sitemap.xml 生成。
 *
 * <p>不分页：sitemap 协议单文件上限 5 万 URL，当前内容量远不到。
 * 日后文章过万再考虑拆 sitemap index。
 *
 * @author claude
 * @date 2026-08-18
 */
@Data
@Schema(description = "官网 sitemap 数据")
public class SeoSiteSitemapVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "全部已发布文章")
    private List<SeoSiteSitemapItemVo> articles;

    /** updateDate 取该分类下最新文章的时间——分类本身的 update_date 改个描述就会变，不代表内容更新 */
    @Schema(description = "有已发布文章的分类")
    private List<SeoSiteSitemapItemVo> categories;

    /**
     * 标签，<b>只含已发布文章 >= {@code SITE_TAG_INDEX_MIN_COUNT} 篇的</b>。
     *
     * <p>薄内容聚合页不主动提交给搜索引擎（决策 7）。阈值放后端统一把控，
     * 官网只负责渲染，避免同一个阈值散落两处、日后改一处忘另一处。
     */
    @Schema(description = "文章数达标的标签")
    private List<SeoSiteSitemapItemVo> tags;
}
