package com.jiuyu.replay.system.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * SEO 文章列表查询参数。
 *
 * @author claude
 * @date 2026-08-13
 */
@Data
@Schema(description = "SEO 文章列表查询参数")
public class SeoArticleListBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章标题，模糊匹配")
    private String title;

    @Schema(description = "所属分类 ID")
    private Long categoryId;

    @Schema(description = "状态筛选：1 已发布 0 已下架，不传为全部")
    private Integer articleStatus;

    /**
     * 标签筛选。供标签管理页「关联文章数」点击跳转时带入。
     *
     * <p>文章与标签是多对多，条件落在关联表 tb_seo_article_tag 上，
     * 实现见 {@code SeoArticleServiceImpl.queryPage} 的 EXISTS 子查询。
     */
    @Schema(description = "标签 ID，按该标签筛选文章")
    private Long tagId;
}
