package com.jiuyu.replay.system.vo.site;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 官网文章详情。
 *
 * <p>同样不含 id / categoryId / articleStatus / viewCount，理由见 {@link SeoSiteArticleListVo}。
 *
 * @author claude
 * @date 2026-08-18
 */
@Data
@Schema(description = "官网文章详情")
public class SeoSiteArticleDetailVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "URL 别名")
    private String slug;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "封面图地址，用作 og:image")
    private String coverUrl;

    @Schema(description = "所属分类名称")
    private String categoryName;

    @Schema(description = "所属分类别名")
    private String categorySlug;

    @Schema(description = "所属分类描述")
    private String categoryDescription;

    @Schema(description = "关联标签，只含启用中的")
    private List<SeoSiteTagItemVo> tags;

    @Schema(description = "正文 HTML，已过 XSS 白名单")
    private String content;

    @Schema(description = "SEO 标题，后台已兜底为 title")
    private String seoTitle;

    @Schema(description = "SEO 描述")
    private String seoDescription;

    @Schema(description = "SEO 关键词，逗号分隔，可能为空")
    private String seoKeywords;

    @Schema(description = "发布时间，格式 yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @Schema(description = "最后修改时间，格式 yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateDate;

    /**
     * 非空表示该 slug 已迁移，官网应对 /article/{redirectSlug} 发 301。
     *
     * <p>此时其余字段全为 null——命中历史表时不查正文，省一次大字段读取。
     * 官网的判断顺序是「先看 redirectSlug、再看 notFound」，不可颠倒。
     */
    @Schema(description = "已迁移时的新 slug；为空表示本条即最终内容")
    private String redirectSlug;
}
