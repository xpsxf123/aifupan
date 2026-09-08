package com.jiuyu.replay.system.controller;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.system.bo.SeoSiteArticleListBo;
import com.jiuyu.replay.system.repository.service.SeoSiteService;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleDetailVo;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleListResultVo;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleListVo;
import com.jiuyu.replay.system.vo.site.SeoSiteCategoryVo;
import com.jiuyu.replay.system.vo.site.SeoSiteSitemapVo;
import com.jiuyu.replay.system.vo.site.SeoSiteTagVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 官网公开接口（www.ifupan.com 文章模块）。
 *
 * <p><b>⚠️ 这是本仓库唯一一组对匿名公网开放的业务读接口。</b>
 * {@code LoginInterceptor.excludePathList} 放行了 {@code /replay/site/**} 前缀，
 * 这里新增的任何 endpoint 都会立即暴露在公网上，没有任何登录校验。
 *
 * <p>因此本类有三条不可放松的约束：
 * <ol>
 *   <li><b>只读。</b>不得新增任何写操作——没有鉴权的写接口等于把数据库交出去；</li>
 *   <li><b>只返回已发布内容。</b>下架/删除的文章必须让官网 404，不能靠前端过滤；</li>
 *   <li><b>出参只用 {@code vo.site} 包下的类。</b>不得复用后台 VO：
 *       那些类日后被人加字段时，加的人不会意识到它同时在对匿名公网输出。</li>
 * </ol>
 *
 * <p>与 {@link SeoArticleController} 等后台接口刻意分开，正是为了让上面这三条
 * 有一个明确的边界——混在一个 Controller 里，迟早有人给后台接口加字段时把内部数据漏到公网。
 *
 * @author claude
 * @date 2026-08-18
 */
@RestController
@CrossOrigin
@RequestMapping("replay/site")
@Tag(name = "官网公开接口-SEO 内容")
public class SeoSiteController {

    private final SeoSiteService seoSiteService;

    public SeoSiteController(SeoSiteService seoSiteService) {
        this.seoSiteService = seoSiteService;
    }

    /**
     * 文章列表。文章列表页 / 分类页 / 标签页共用。
     *
     * <p>用 GET 而非后台那套 POST + JSON：只读查询，GET 幂等、可被官网的 ISR/CDN 缓存，
     * 出问题能直接用浏览器复现。
     *
     * @param bo 查询参数，categorySlug 与 tagSlug 互斥
     * @return 分页数据 + 当前分类/标签信息
     */
    @GetMapping("/article/list")
    @Operation(summary = "文章列表（列表页/分类页/标签页共用）")
    public R<SeoSiteArticleListResultVo> articleList(SeoSiteArticleListBo bo) {
        return R.ok("获取成功", seoSiteService.queryArticleList(bo));
    }

    /**
     * 文章详情，含正文。
     *
     * <p>出参的 {@code redirectSlug} 非空时表示该 slug 已迁移，官网应发 301。
     *
     * @param slug URL 别名
     * @return 详情或迁移指示
     */
    @GetMapping("/article/detail")
    @Operation(summary = "文章详情")
    public R<SeoSiteArticleDetailVo> articleDetail(
            @Parameter(description = "文章别名", required = true) @RequestParam("slug") String slug) {
        return R.ok("获取成功", seoSiteService.getArticleDetail(slug));
    }

    /**
     * 文末相关文章：共享标签优先 + 同分类兜底。
     *
     * @param slug  当前文章别名
     * @param limit 取几篇，默认 4
     * @return 相关文章
     */
    @GetMapping("/article/related")
    @Operation(summary = "相关文章")
    public R<List<SeoSiteArticleListVo>> articleRelated(
            @Parameter(description = "文章别名", required = true) @RequestParam("slug") String slug,
            @Parameter(description = "条数，默认 4") @RequestParam(value = "limit", required = false) Integer limit) {
        return R.ok("获取成功", seoSiteService.listRelated(slug, limit));
    }

    /**
     * 全部有已发布文章的分类，按 sort 升序。
     *
     * @return 分类列表
     */
    @GetMapping("/category/list")
    @Operation(summary = "分类列表")
    public R<List<SeoSiteCategoryVo>> categoryList() {
        return R.ok("获取成功", seoSiteService.listCategories());
    }

    /**
     * 启用中且有已发布文章的标签，按热度降序。
     *
     * @return 标签列表
     */
    @GetMapping("/tag/list")
    @Operation(summary = "标签列表（标签云）")
    public R<List<SeoSiteTagVo>> tagList() {
        return R.ok("获取成功", seoSiteService.listTags());
    }

    /**
     * sitemap 全量数据，供官网 /sitemap.xml 生成。
     *
     * @return 文章 / 分类 / 标签的 slug + updateDate
     */
    @GetMapping("/sitemap")
    @Operation(summary = "sitemap 数据")
    public R<SeoSiteSitemapVo> sitemap() {
        return R.ok("获取成功", seoSiteService.getSitemap());
    }
}
