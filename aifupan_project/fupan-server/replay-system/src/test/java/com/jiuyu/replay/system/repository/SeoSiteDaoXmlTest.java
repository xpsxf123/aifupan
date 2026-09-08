package com.jiuyu.replay.system.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 对 {@code mapper/SeoSiteDao.xml} 的静态校验。
 *
 * <p>与 {@link SeoArticleMapperXmlTest} 同样的思路：直接读 XML 文本做断言，
 * 防住「有人改 SQL 时把关键限定去掉」这类文本层面的回退。
 *
 * <p><b>本类锁的是两件在官网侧后果最严重、却都不报错的事</b>：
 * <ol>
 *   <li><b>状态过滤丢失</b>——官网接口一旦漏掉 {@code article_status}，
 *       下架的文章就会直接出现在公网上。后台那边状态是可选筛选条件，
 *       复制粘贴过来很容易把它写成 {@code <if>} 包裹的可选条件；</li>
 *   <li><b>子查询关联列漏写表别名</b>——{@code tb_seo_article_tag} 自己也有 id 主键，
 *       写裸 {@code id} 会被 MySQL 解析成子查询自己的列，筛选恒为空且不报任何错。
 *       这是上一迭代出过的真实事故。</li>
 * </ol>
 *
 * <p>能做什么、不能做什么：能防文本回退；<b>不能</b>验证 SQL 语义是否正确、
 * 能否在 MySQL 上跑通。后者需要真实数据库的集成测试，本项目暂无该基础设施。
 *
 * @author claude
 * @date 2026-08-18
 */
@DisplayName("SeoSiteDao.xml 静态校验")
class SeoSiteDaoXmlTest {

    private static final String XML_PATH = "mapper/SeoSiteDao.xml";

    /** 所有必须固定过滤已发布状态的 statement */
    private static final List<String> PUBLISHED_FILTERED = List.of(
            "selectSitePage",
            "selectSiteDetail",
            "selectSiteCategories",
            "selectSiteCategoryBySlug",
            "selectSiteTags",
            "selectSiteTagBySlug",
            "selectRelatedByTags",
            "selectRelatedByCategory",
            "selectSitemapArticles",
            "selectSitemapCategories",
            "selectSitemapTags");

    private static String xml() {
        try (InputStream in = SeoSiteDaoXmlTest.class.getClassLoader().getResourceAsStream(XML_PATH)) {
            assertNotNull(in, XML_PATH + " 不在 classpath 上——mapper-locations 配的是 "
                    + "classpath*:/mapper/**/*.xml，文件放错目录会让接口在运行时才报 "
                    + "BindingException，单测阶段完全无感");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取 " + XML_PATH + " 失败", e);
        }
    }

    private static String statement(String xml, String id) {
        int start = xml.indexOf("id=\"" + id + "\"");
        assertTrue(start > 0, "找不到 statement: " + id);
        int end = xml.indexOf("</select>", start);
        assertTrue(end > start, id + " 缺少结束标签");
        return xml.substring(start, end);
    }

    @Nested
    @DisplayName("已发布状态过滤")
    class PublishedFilter {

        @Test
        @DisplayName("每个查询都必须固定过滤 article_status，漏掉即把下架文章送上公网")
        void everyStatementFiltersPublished() {
            String xml = xml();
            for (String id : PUBLISHED_FILTERED) {
                String sql = statement(xml, id);
                assertTrue(sql.contains("article_status = #{published}"),
                        id + " 缺少 article_status = #{published}。"
                                + "官网接口的状态过滤是固定条件，不是可选筛选——"
                                + "漏掉会让已下架的文章直接出现在公网上，且不报任何错");
            }
        }

        @Test
        @DisplayName("状态过滤不得写成 <if> 可选条件")
        void statusMustNotBeOptional() {
            String xml = xml();
            for (String id : PUBLISHED_FILTERED) {
                String sql = statement(xml, id);
                int statusAt = sql.indexOf("article_status");
                assertTrue(statusAt > 0, id + " 应含 article_status");
                // 取 article_status 之前最近的一段，确认它不在 <if> 块里
                String before = sql.substring(0, statusAt);
                int lastIf = before.lastIndexOf("<if");
                int lastEndIf = before.lastIndexOf("</if>");
                assertFalse(lastIf > lastEndIf,
                        id + " 的 article_status 被包在 <if> 里，成了可选条件。"
                                + "后台的 articleStatus 是可选筛选，官网的必须固定——"
                                + "照搬后台写法会让下架文章在不传该参数时全部泄漏");
            }
        }
    }

    @Nested
    @DisplayName("子查询表别名限定")
    class SubqueryAlias {

        @Test
        @DisplayName("关联列必须写 a.id，裸 id 会被解析成关联表自己的主键")
        void tagSubqueryQualifiesArticleId() {
            String xml = xml();
            String page = statement(xml, "selectSitePage");
            assertTrue(page.contains("t.article_id = a.id"),
                    "selectSitePage 的标签子查询必须写 t.article_id = a.id。"
                            + "tb_seo_article_tag 自己也有 id 主键列，写裸 id 会让 MySQL "
                            + "在子查询自己的表里解析，条件退化成 t.article_id = t.id，"
                            + "标签筛选恒为空且不报任何错——这是已经发生过的事故");

            String related = statement(xml, "selectRelatedByTags");
            assertTrue(related.contains("t.article_id = a.id"),
                    "selectRelatedByTags 的 JOIN 必须写 t.article_id = a.id，理由同上");
            assertTrue(related.contains("a2.id = t2.article_id"),
                    "selectRelatedByTags 取当前文章标签的子查询必须限定 a2.id = t2.article_id");
        }

        @Test
        @DisplayName("不得出现未限定的裸 article_id = id")
        void noUnqualifiedIdComparison() {
            String xml = xml().replaceAll("\\s+", " ");
            assertFalse(xml.contains("article_id = id"),
                    "出现了裸的 article_id = id。必须带外层表别名（a.id / a2.id），"
                            + "否则 MySQL 会解析成关联表自己的主键，条件几乎永不成立");
        }
    }

    @Nested
    @DisplayName("计数口径")
    class CountSemantics {

        @Test
        @DisplayName("分类/标签计数必须只算已发布，与后台'含已下架'的口径相反")
        void countOnlyPublished() {
            String xml = xml();
            for (String id : List.of("selectSiteCategories", "selectSiteCategoryBySlug",
                    "selectSiteTags", "selectSiteTagBySlug")) {
                String sql = statement(xml, id);
                assertTrue(sql.contains("article_status = #{published}"),
                        id + " 的计数必须过滤 article_status。"
                                + "后台 countByCategoryIds / countArticlesByTagIds 的口径是"
                                + "「含已下架」，直接复用会让官网出现"
                                + "「articleCount 是 5、列表却查出 0 篇」的 200 空壳页，"
                                + "也会让薄内容标签页的 noindex 该加的没加");
            }
        }

        @Test
        @DisplayName("sitemap 的标签必须按阈值过滤（决策 7）")
        void sitemapTagsFilteredByMinCount() {
            String sql = statement(xml(), "selectSitemapTags");
            assertTrue(sql.contains("HAVING") && sql.contains("#{minCount}"),
                    "selectSitemapTags 必须用 HAVING COUNT(...) >= #{minCount} 过滤薄内容标签。"
                            + "阈值放后端统一把控，官网不再重复判断");
        }
    }

    @Nested
    @DisplayName("其他约束")
    class OtherConstraints {

        @Test
        @DisplayName("列表查询不得 SELECT content")
        void listMustNotSelectContent() {
            String sql = statement(xml(), "selectSitePage");
            assertFalse(sql.contains("a.content"),
                    "selectSitePage 不应查 content：正文可能很大，列表页用不到，"
                            + "带上会显著拖慢接口");
        }

        @Test
        @DisplayName("列表按 publish_time 排序，且必须有 id 兜底")
        void listOrderedByPublishTimeWithIdTiebreaker() {
            String sql = statement(xml(), "selectSitePage");
            assertTrue(sql.contains("ORDER BY a.publish_time DESC, a.id DESC"),
                    "selectSitePage 必须按 publish_time 排序并以 a.id 兜底。"
                            + "用 update_date 排会让运营改个错别字就把老文章顶到首位；"
                            + "缺 id 兜底时，批量导入产生的同时间戳文章在翻页时会重复或漏行");
        }

        @Test
        @DisplayName("标签相关查询必须过滤禁用标签")
        void tagQueriesFilterDisabled() {
            String xml = xml();
            for (String id : List.of("selectSiteTagsByArticleSlugs", "selectSiteTags",
                    "selectSiteTagBySlug", "selectSitemapTags")) {
                String sql = statement(xml, id);
                assertTrue(sql.contains("tag_status = #{enabled}"),
                        id + " 必须过滤 tag_status。禁用标签的聚合页在官网不可访问，"
                                + "展示出来就是一个必然 404 的链接");
            }
        }

        @Test
        @DisplayName("相关文章的兜底查询用 <if> 包住 NOT IN，避免空集合拼出语法错误")
        void relatedFallbackGuardsEmptyExclude() {
            String sql = statement(xml(), "selectRelatedByCategory");
            int notInAt = sql.indexOf("NOT IN");
            assertTrue(notInAt > 0, "selectRelatedByCategory 应含 NOT IN 排除已选中的文章");
            String before = sql.substring(0, notInAt);
            assertTrue(before.lastIndexOf("<if") > before.lastIndexOf("</if>"),
                    "NOT IN 必须包在 <if test=\"excludeSlugs != null and excludeSlugs.size() > 0\"> 里。"
                            + "第一段无结果时 excludeSlugs 为空，直接拼会产出 NOT IN () 语法错误");
        }
    }
}
