package com.jiuyu.replay.system.vo.site;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * sitemap 条目：一个 slug + 最后更新时间。
 *
 * @author claude
 * @date 2026-08-18
 */
@Data
@Schema(description = "sitemap 条目")
public class SeoSiteSitemapItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "别名")
    private String slug;

    @Schema(description = "最后更新时间，官网取日期部分作为 lastmod")
    private LocalDateTime updateDate;
}
