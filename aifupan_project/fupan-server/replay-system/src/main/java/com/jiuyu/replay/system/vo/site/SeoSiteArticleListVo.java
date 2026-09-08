package com.jiuyu.replay.system.vo.site;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 官网文章列表项。
 *
 * <p><b>刻意不含 content</b>：正文可能很大，列表页用不到。
 *
 * <p><b>刻意不含 id / categoryId / articleStatus / viewCount / isDeleted</b>：
 * 官网 URL 里只有 slug，没有任何地方需要内部 ID；而 articleStatus 之类一旦外传，
 * 等于把后台数据结构暴露给公网。这也是本模块不复用后台 VO 的原因——
 * 日后有人给后台 VO 加字段时，不会意识到那个类同时在对匿名公网输出。
 *
 * @author claude
 * @date 2026-08-18
 */
@Data
@Schema(description = "官网文章列表项")
public class SeoSiteArticleListVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "URL 别名，官网 /article/{slug}")
    private String slug;

    @Schema(description = "摘要，可能为空")
    private String summary;

    @Schema(description = "封面图地址")
    private String coverUrl;

    @Schema(description = "所属分类名称")
    private String categoryName;

    /** 列表卡片要能链到分类页，故带 slug；仍然不带 categoryId */
    @Schema(description = "所属分类别名，官网 /category/{slug}")
    private String categorySlug;

    @Schema(description = "关联标签，只含启用中的")
    private List<SeoSiteTagItemVo> tags;

    /**
     * 首次发布时间。
     *
     * <p>序列化格式为 {@code yyyy-MM-dd HH:mm:ss}（JacksonSerializerConfig 全局配置），
     * <b>不是 ISO 8601</b>。官网侧须自行转换后再喂给结构化数据，否则搜索引擎会静默忽略。
     */
    @Schema(description = "发布时间，格式 yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
}
