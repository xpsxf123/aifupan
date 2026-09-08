package com.jiuyu.replay.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SEO 标签列表项。
 *
 * <p>本期不含关联文章浏览量——浏览量已确认不做。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@Schema(description = "SEO 标签列表项")
public class SeoTagListVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "拼音别名")
    private String slug;

    @Schema(description = "状态：1 启用 0 禁用")
    private Integer tagStatus;

    /** 关联文章数，含已下架文章 */
    @Schema(description = "关联文章数，含已下架")
    private Integer articleCount;

    @Schema(description = "创建时间")
    private LocalDateTime createDate;
}
