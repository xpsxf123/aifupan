package com.jiuyu.replay.system.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.system.bo.SeoSiteArticleListBo;
import com.jiuyu.replay.system.entity.SeoArticleEntity;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleDetailVo;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleListVo;
import com.jiuyu.replay.system.vo.site.SeoSiteCategoryVo;
import com.jiuyu.replay.system.vo.site.SeoSiteSitemapItemVo;
import com.jiuyu.replay.system.vo.site.SeoSiteTagVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 官网公开接口的查询，SQL 见 {@code resources/mapper/SeoSiteDao.xml}。
 *
 * <p>不继承 {@code BaseMapper}：本 Dao 只做官网侧的只读查询，没有单表 CRUD 的需要；
 * 继承进来反而会把 insert/update/delete 一并暴露给一个「公开只读」语义的类。
 *
 * <p><b>所有查询都固定 article_status = 已发布</b>——与后台那套「状态是可选筛选条件」
 * 的语义完全不同，漏掉就会把下架文章送上公网。
 *
 * @author claude
 * @date 2026-08-18
 */
@Mapper
public interface SeoSiteDao {

    /**
     * 官网文章列表分页，列表页 / 分类页 / 标签页共用。
     *
     * @param page       分页参数
     * @param bo         查询条件，categorySlug 与 tagSlug 互斥（调用方已校验）
     * @param notDeleted 未删除标记，传 {@code SeoConstant.NOT_DELETED}
     * @param published  已发布标记，传 {@code SeoConstant.STATUS_ENABLED}
     * @param enabled    标签启用标记，传 {@code SeoConstant.STATUS_ENABLED}
     * @return 分页结果，<b>不含 content、不含标签</b>（标签由 Service 批量填充，避免 N+1）
     */
    IPage<SeoSiteArticleListVo> selectSitePage(@Param("page") IPage<SeoArticleEntity> page,
                                               @Param("bo") SeoSiteArticleListBo bo,
                                               @Param("notDeleted") Integer notDeleted,
                                               @Param("published") Integer published,
                                               @Param("enabled") Integer enabled);

    /**
     * 按 slug 查已发布文章详情，含正文。
     *
     * @param slug       URL 别名
     * @param notDeleted 未删除标记
     * @param published  已发布标记
     * @return 详情；slug 不存在或文章已下架/已删除时返回 null
     */
    SeoSiteArticleDetailVo selectSiteDetail(@Param("slug") String slug,
                                            @Param("notDeleted") Integer notDeleted,
                                            @Param("published") Integer published);

    /**
     * 批量查文章的启用中标签，供列表与详情填充，避免 N+1。
     *
     * <p>按 slug 而不是 id：官网侧的 VO 刻意不含任何内部 ID，
     * 若这里要 id，调用方就得为此额外查一次主键，反而把「不外传 ID」的设计撬开一道缝。
     *
     * <p>只返回 {@code tag_status = 1} 的——禁用标签的聚合页在官网不可访问，
     * 展示出来就是一个必然 404 的链接。这与后台刻意相反。
     *
     * @param articleSlugs 文章 slug 集合，调用方需保证非空且已分批
     * @param notDeleted   未删除标记
     * @param enabled      标签启用标记
     * @return 每行含 articleSlug / tagName / tagSlug，按关联表主键排序
     */
    List<Map<String, Object>> selectSiteTagsByArticleSlugs(@Param("articleSlugs") List<String> articleSlugs,
                                                           @Param("notDeleted") Integer notDeleted,
                                                           @Param("enabled") Integer enabled);

    /**
     * 全部有已发布文章的分类，按 sort 升序。
     *
     * <p>articleCount <b>只算已发布</b>，且 0 篇的分类不会出现在结果里（决策 9）。
     *
     * @param notDeleted 未删除标记
     * @param published  已发布标记
     * @return 分类列表
     */
    List<SeoSiteCategoryVo> selectSiteCategories(@Param("notDeleted") Integer notDeleted,
                                                 @Param("published") Integer published);

    /**
     * 按 slug 查单个分类，含已发布文章数。
     *
     * @param slug       分类别名
     * @param notDeleted 未删除标记
     * @param published  已发布标记
     * @return 分类；slug 不存在时返回 null（articleCount 可能为 0）
     */
    SeoSiteCategoryVo selectSiteCategoryBySlug(@Param("slug") String slug,
                                               @Param("notDeleted") Integer notDeleted,
                                               @Param("published") Integer published);

    /**
     * 启用中且有已发布文章的标签，按文章数降序，供标签云。
     *
     * <p><b>不做 {@code >= SITE_TAG_INDEX_MIN_COUNT} 的过滤</b>——
     * 标签云要展示全部有内容的标签，薄内容的 noindex 由官网自行判断。
     *
     * @param notDeleted 未删除标记
     * @param published  已发布标记
     * @param enabled    标签启用标记
     * @return 标签列表
     */
    List<SeoSiteTagVo> selectSiteTags(@Param("notDeleted") Integer notDeleted,
                                      @Param("published") Integer published,
                                      @Param("enabled") Integer enabled);

    /**
     * 按 slug 查单个启用中标签。
     *
     * @param slug       标签别名
     * @param notDeleted 未删除标记
     * @param published  已发布标记
     * @param enabled    标签启用标记
     * @return 标签；slug 不存在或标签已禁用时返回 null
     */
    SeoSiteTagVo selectSiteTagBySlug(@Param("slug") String slug,
                                     @Param("notDeleted") Integer notDeleted,
                                     @Param("published") Integer published,
                                     @Param("enabled") Integer enabled);

    /**
     * 相关文章第一段：与当前文章共享标签最多的若干篇（决策 5）。
     *
     * <p>当前文章的标签由 SQL 内的子查询取，<b>不需要调用方先查出 tagIds</b>。
     * 这样无标签的文章不会拼出 {@code IN ()} 语法错误，而是自然返回 0 行、
     * 平滑落到 {@link #selectRelatedByCategory} 兜底。
     *
     * @param currentSlug 当前文章 slug，从结果中排除
     * @param limit       取几篇
     * @param notDeleted  未删除标记
     * @param published   已发布标记
     * @param enabled     标签启用标记
     * @return 按共享标签数降序的文章；当前文章无标签时返回空列表
     */
    List<SeoSiteArticleListVo> selectRelatedByTags(@Param("currentSlug") String currentSlug,
                                                   @Param("limit") Integer limit,
                                                   @Param("notDeleted") Integer notDeleted,
                                                   @Param("published") Integer published,
                                                   @Param("enabled") Integer enabled);

    /**
     * 相关文章第二段：同分类最新，用于补齐第一段不足的名额。
     *
     * <p>分类同样由 SQL 内的子查询按当前 slug 取。
     *
     * @param currentSlug  当前文章 slug，始终排除
     * @param excludeSlugs 第一段已选中的 slug，<b>可以为空</b>——为空时 NOT IN 子句不出现
     * @param limit        还缺几篇
     * @param notDeleted   未删除标记
     * @param published    已发布标记
     * @return 同分类最新文章
     */
    List<SeoSiteArticleListVo> selectRelatedByCategory(@Param("currentSlug") String currentSlug,
                                                       @Param("excludeSlugs") List<String> excludeSlugs,
                                                       @Param("limit") Integer limit,
                                                       @Param("notDeleted") Integer notDeleted,
                                                       @Param("published") Integer published);

    /**
     * sitemap：全部已发布文章。
     *
     * @param notDeleted 未删除标记
     * @param published  已发布标记
     * @return slug + updateDate
     */
    List<SeoSiteSitemapItemVo> selectSitemapArticles(@Param("notDeleted") Integer notDeleted,
                                                     @Param("published") Integer published);

    /**
     * sitemap：有已发布文章的分类，updateDate 取该分类下最新文章的时间。
     *
     * @param notDeleted 未删除标记
     * @param published  已发布标记
     * @return slug + updateDate
     */
    List<SeoSiteSitemapItemVo> selectSitemapCategories(@Param("notDeleted") Integer notDeleted,
                                                       @Param("published") Integer published);

    /**
     * sitemap：已发布文章数达标的标签（决策 7）。
     *
     * @param minCount   最低文章数，传 {@code SeoConstant.SITE_TAG_INDEX_MIN_COUNT}
     * @param notDeleted 未删除标记
     * @param published  已发布标记
     * @param enabled    标签启用标记
     * @return slug + updateDate
     */
    List<SeoSiteSitemapItemVo> selectSitemapTags(@Param("minCount") Integer minCount,
                                                 @Param("notDeleted") Integer notDeleted,
                                                 @Param("published") Integer published,
                                                 @Param("enabled") Integer enabled);
}
