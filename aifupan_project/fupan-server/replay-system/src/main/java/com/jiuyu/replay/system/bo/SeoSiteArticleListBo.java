package com.jiuyu.replay.system.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 官网文章列表查询参数。文章列表页 / 分类页 / 标签页共用。
 *
 * <p>用 GET + 对象绑定（不加 {@code @RequestBody}）而非后台那套 POST + JSON：
 * 官网侧是只读查询，GET 幂等、可被 ISR/CDN 缓存，出问题能直接用浏览器复现。
 *
 * @author claude
 * @date 2026-08-18
 */
@Data
@Schema(description = "官网文章列表查询参数")
public class SeoSiteArticleListBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码，从 1 开始，默认 1")
    private Integer page;

    /**
     * 每页条数，默认 10，<b>上限 {@code SeoConstant.SITE_MAX_PAGE_SIZE}</b>。
     *
     * <p>必须封顶：这是匿名公开接口，不限制的话一个 {@code limit=999999} 就能把
     * 全站文章一次性拉走，既是内容被批量抓取的口子，也能拖垮数据库。
     */
    @Schema(description = "每页条数，默认 10，最大 50")
    private Integer limit;

    /** 传了则按分类过滤，与 tagSlug 互斥 */
    @Schema(description = "分类别名，分类页传")
    private String categorySlug;

    /** 传了则按标签过滤，与 categorySlug 互斥 */
    @Schema(description = "标签别名，标签页传")
    private String tagSlug;
}
