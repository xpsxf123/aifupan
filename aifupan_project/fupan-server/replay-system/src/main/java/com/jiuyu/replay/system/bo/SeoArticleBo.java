package com.jiuyu.replay.system.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * SEO 文章新增 / 修改入参。
 *
 * <p>status 由本对象携带，但 publishTime 不接受外部指定——首次发布时由后端写入，
 * 重新发布不覆盖，避免搜索引擎重新判定内容时效。
 *
 * @author claude
 * @date 2026-08-13
 */
@Data
@Schema(description = "SEO 文章新增/修改入参")
public class SeoArticleBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键，新增时不传")
    private Long id;

    @Schema(description = "文章标题，必填，1-100 字")
    private String title;

    @Schema(description = "URL 别名，留空时由标题生成拼音")
    private String slug;

    @Schema(description = "所属分类 ID，必填")
    private Long categoryId;

    @Schema(description = "正文 HTML，必填。入库前做 XSS 过滤并把 h1 降级为 h2")
    private String content;

    @Schema(description = "摘要，选填，≤200 字")
    private String summary;

    @Schema(description = "封面图 COS 地址，必填")
    private String coverUrl;

    @Schema(description = "标签 ID 集合，选填，上限 10 个")
    private List<Long> tagIds;

    @Schema(description = "状态：1 已发布 0 已下架，默认 1")
    private Integer articleStatus;

    @Schema(description = "SEO 标题，留空时取标题")
    private String seoTitle;

    @Schema(description = "SEO 描述，必填")
    private String seoDescription;

    @Schema(description = "SEO 关键词，英文逗号分隔")
    private String seoKeywords;
}
