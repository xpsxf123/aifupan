package com.jiuyu.replay.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SEO 文章详情，含正文。
 *
 * @author claude
 * @date 2026-08-13
 */
@Data
@Schema(description = "SEO 文章详情")
public class SeoArticleInfoVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "URL 别名")
    private String slug;

    @Schema(description = "所属分类 ID")
    private Long categoryId;

    @Schema(description = "所属分类名称")
    private String categoryName;

    @Schema(description = "正文 HTML")
    private String content;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "封面图地址")
    private String coverUrl;

    /** 编辑时已挂的禁用标签也要返回，否则保存会被静默丢弃 */
    @Schema(description = "关联标签，含状态；已禁用的标签也返回")
    private List<SeoArticleTagVo> tags;

    @Schema(description = "状态：1 已发布 0 已下架")
    private Integer articleStatus;

    @Schema(description = "SEO 标题")
    private String seoTitle;

    @Schema(description = "SEO 描述")
    private String seoDescription;

    @Schema(description = "SEO 关键词")
    private String seoKeywords;

    @Schema(description = "首次发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "创建时间")
    private LocalDateTime createDate;

    @Schema(description = "最后修改时间")
    private LocalDateTime updateDate;
}
