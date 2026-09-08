package com.jiuyu.replay.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SEO 文章列表项。
 *
 * <p><b>刻意不含 content</b>：正文可能很大，列表页用不到，返回它会显著拖慢列表接口。
 * <p>本期不含 viewCount——浏览量已确认不做。
 *
 * @author claude
 * @date 2026-08-13
 */
@Data
@Schema(description = "SEO 文章列表项")
public class SeoArticleListVo implements Serializable {

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

    @Schema(description = "封面图地址")
    private String coverUrl;

    @Schema(description = "关联标签，含状态")
    private List<SeoArticleTagVo> tags;

    @Schema(description = "状态：1 已发布 0 已下架")
    private Integer articleStatus;

    @Schema(description = "首次发布时间，未发布过为 null")
    private LocalDateTime publishTime;

    @Schema(description = "最后修改时间")
    private LocalDateTime updateDate;
}
