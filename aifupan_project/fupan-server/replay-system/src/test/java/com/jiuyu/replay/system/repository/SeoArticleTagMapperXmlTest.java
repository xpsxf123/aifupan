package com.jiuyu.replay.system.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 对 {@code mapper/SeoArticleTagDao.xml} 的静态校验。
 *
 * <p>这条统计原先是「把关联行全查回内存 → 回查文章表 → 在 Java 里累加」，
 * 一个热门标签关联上万篇文章就会把上万行拉回来，直接违反 openspec §6
 * 「articleCount 由 SQL join 聚合返回，禁止 N+1」。改成 JOIN + GROUP BY 之后，
 * 「有没有真的在 SQL 层聚合」「有没有漏掉 is_deleted 过滤」这两件事只剩 SQL 文本可查。
 *
 * <p>能力边界同 {@link SeoArticleMapperXmlTest}：防文本回退，不验 SQL 语义与执行结果。
 *
 * @author claude
 * @date 2026-08-13
 */
@DisplayName("SeoArticleTagDao.xml 静态校验")
class SeoArticleTagMapperXmlTest {

    private static final String XML_PATH = "mapper/SeoArticleTagDao.xml";

    private static String xml() {
        try (InputStream in = SeoArticleTagMapperXmlTest.class.getClassLoader().getResourceAsStream(XML_PATH)) {
            assertNotNull(in, XML_PATH + " 不在 classpath 上，接口会在运行时才报 BindingException");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取 " + XML_PATH + " 失败", e);
        }
    }

    @Test
    @DisplayName("namespace 与 statement id 对得上 Dao")
    void xml_bindingMatchesDao() {
        String content = xml();

        assertTrue(content.contains("namespace=\"com.jiuyu.replay.system.repository.dao.SeoArticleTagDao\""),
                "namespace 与 Dao 全限定名对不上，MyBatis 启动会报 BindingException");
        assertTrue(content.contains("id=\"countArticlesByTagIds\""));
        assertTrue(content.contains("resultType=\"com.jiuyu.replay.system.vo.SeoIdCountVo\""));
    }

    @Test
    @DisplayName("必须 JOIN 文章表——关联表不带 is_deleted，只数关联行会把已删文章算进去")
    void countArticlesByTagIds_joinsArticleTableToExcludeDeleted() {
        String content = xml();
        String upper = content.toUpperCase(Locale.ROOT);

        assertTrue(upper.contains("JOIN TB_SEO_ARTICLE"),
                "没有 JOIN 文章表。tb_seo_article_tag 不带 is_deleted，"
                        + "文章被逻辑删除后关联行仍在，只数关联行会虚高");
        assertTrue(content.contains("a.is_deleted = #{notDeleted}"),
                "JOIN 了但没过滤已删除文章，等于没 JOIN");
        assertTrue(content.contains("a.id = t.article_id"),
                "JOIN 条件缺失或写错，会产生笛卡尔积");
    }

    @Test
    @DisplayName("在 SQL 层 GROUP BY 聚合，不是拉明细回内存累加")
    void countArticlesByTagIds_aggregatesInSql() {
        String upper = xml().toUpperCase(Locale.ROOT);

        assertTrue(upper.contains("GROUP BY T.TAG_ID"),
                "必须在 SQL 层聚合——原先的 Java 内存累加正是要修掉的问题");
        assertTrue(upper.contains("COUNT(*)"), "没有聚合函数");
    }

    @Test
    @DisplayName("统计口径含已下架文章，不得按状态过滤")
    void countArticlesByTagIds_includesUnpublishedArticles() {
        String content = xml().toLowerCase(Locale.ROOT);

        // 与分类口径一致（openspec §6.4）：下架只是不对外，关联关系仍然存在。
        // 按状态过滤会让运营以为标签没被用过，进而误删
        assertFalse(content.contains("article_status"),
                "统计口径含已下架文章。实际 SQL：" + content);
    }

    @Test
    @DisplayName("IN 列表走 foreach 占位，且全程无 ${} 拼接")
    void countArticlesByTagIds_parametersAreBound() {
        String content = xml();

        assertTrue(content.contains("<foreach collection=\"tagIds\""));
        assertTrue(content.contains("#{tagId}"), "IN 的每个值都要走预编译占位");
        assertFalse(Pattern.compile("\\$\\{").matcher(content).find(),
                "出现了 ${} 拼接，存在 SQL 注入风险");
    }
}
