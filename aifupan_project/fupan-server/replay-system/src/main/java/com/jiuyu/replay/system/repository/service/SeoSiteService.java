package com.jiuyu.replay.system.repository.service;

import com.jiuyu.replay.system.bo.SeoSiteArticleListBo;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleDetailVo;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleListResultVo;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleListVo;
import com.jiuyu.replay.system.vo.site.SeoSiteCategoryVo;
import com.jiuyu.replay.system.vo.site.SeoSiteSitemapVo;
import com.jiuyu.replay.system.vo.site.SeoSiteTagVo;

import java.util.List;

/**
 * 官网公开只读查询（{@code /replay/site/**}）。
 *
 * <p>不继承 {@code IService}：本 Service 不对应单一实体，也不提供任何写操作。
 *
 * <p><b>整组接口对匿名公网开放</b>（LoginInterceptor 白名单放行 {@code /replay/site/**}），
 * 因此每个方法都必须满足三条硬约束：
 * <ol>
 *   <li>只返回 {@code article_status = 1} 且未删除的内容；</li>
 *   <li>出参不含 id / categoryId / articleStatus / viewCount / isDeleted；</li>
 *   <li>标签只返回启用中的。</li>
 * </ol>
 * 在这里新增方法前，请先确认这三条都守住了。
 *
 * @author claude
 * @date 2026-08-18
 */
public interface SeoSiteService {

    /**
     * 官网文章列表，文章列表页 / 分类页 / 标签页共用。
     *
     * @param bo 查询参数；categorySlug 与 tagSlug 互斥，limit 上限 50
     * @return 分页数据 + 当前分类/标签信息（供页面渲染 h1 与 meta）
     * @throws com.jiuyu.replay.generic.vo.common.exception.BusinessException
     *         两个 slug 同传，或 categorySlug/tagSlug 对应的分类/标签不存在。
     *         <b>slug 不存在必须抛错而不是返回空列表</b>——官网据此返回 404，
     *         返回 200 空页面会被搜索引擎当作有效页收录
     */
    SeoSiteArticleListResultVo queryArticleList(SeoSiteArticleListBo bo);

    /**
     * 官网文章详情，按 slug 查。
     *
     * <p>三种结果：
     * <ul>
     *   <li>命中已发布文章 → 完整详情，{@code redirectSlug} 为 null；</li>
     *   <li>slug 命中历史表且目标文章仍在 → 只含 {@code redirectSlug}，官网据此发 301；</li>
     *   <li>都没命中 / 已下架 / 已删除 → 抛 BusinessException，官网据此 404。</li>
     * </ul>
     *
     * @param slug URL 别名
     * @return 详情或迁移指示
     * @throws com.jiuyu.replay.generic.vo.common.exception.BusinessException 文章不存在
     */
    SeoSiteArticleDetailVo getArticleDetail(String slug);

    /**
     * 文末相关文章：共享标签优先 + 同分类兜底（决策 5）。
     *
     * @param slug  当前文章 slug
     * @param limit 取几篇，默认 4
     * @return 相关文章；当前文章不存在时返回空列表（而不是抛错——
     *         推荐区缺失不该让整个详情页失败）
     */
    List<SeoSiteArticleListVo> listRelated(String slug, Integer limit);

    /**
     * 全部有已发布文章的分类，按 sort 升序，供官网导航与栏目索引。
     *
     * <p>articleCount 只算已发布，0 篇的分类不返回（决策 9）。
     *
     * @return 分类列表
     */
    List<SeoSiteCategoryVo> listCategories();

    /**
     * 启用中且有已发布文章的标签，按热度降序，供标签云。
     *
     * @return 标签列表
     */
    List<SeoSiteTagVo> listTags();

    /**
     * sitemap 全量数据。标签只含已发布文章数达标的（决策 7）。
     *
     * @return 文章 / 分类 / 标签的 slug + updateDate
     */
    SeoSiteSitemapVo getSitemap();

    /**
     * 让本组接口的全部缓存立即失效。
     *
     * <p><b>必须在任何会改变官网可见内容的写操作之后调用</b>——
     * 文章的新增/修改/删除/上下架、分类与标签的增删改。
     *
     * <p>为什么这件事重要：官网详情页<b>不做页面级缓存</b>（Nitro 只缓存 2xx，
     * 一个 URL 从 200 变 404 后会永久返回旧内容，实测见官网
     * {@code docs/SEO文章模块/技术设计.md} §4.7），改由这里的接口缓存承担压力。
     * 因此这一层是「下架后多久从官网消失」的唯一闸门：不清缓存，
     * 下架的文章会继续对外可见到 TTL 自然到期为止。
     *
     * <p>实现上是版本号自增而非逐个删 key：官网侧的缓存 key 组合多
     * （分页 × 分类 × 标签），SCAN + DEL 在热点时段既慢又容易漏，
     * 递增版本号能让全部旧 key 一次性失效，旧值随各自 TTL 自然淘汰。
     */
    void evictAll();
}
