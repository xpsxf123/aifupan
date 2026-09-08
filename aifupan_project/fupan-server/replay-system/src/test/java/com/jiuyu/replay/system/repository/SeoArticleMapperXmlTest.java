package com.jiuyu.replay.system.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * 对 {@code mapper/SeoArticleDao.xml} 的静态校验。
 *
 * <p><b>这组测试存在的原因</b>：文章列表的查询条件原先用 {@code LambdaQueryWrapper} 拼，
 * 单测可以断言它生成的 SQL 片段——其中一条断言正是唯一能抓住
 * 「子查询关联列漏写表限定」的防线（那个 bug 曾让标签筛选恒为空且不报错）。
 * SQL 挪进 XML 之后，那批断言全部失效，防线出现缺口。
 *
 * <p>本类把这个能力接回来：直接读 XML 文本做断言。
 *
 * <p><b>它能做什么、不能做什么</b>——必须说清楚，否则会给人虚假的安全感：
 * <ul>
 *   <li>能：防住「有人改 XML 时把表别名限定去掉」「误加 SELECT content」这类文本层面的回退；</li>
 *   <li>不能：验证 SQL 语义是否正确、能否在 MySQL 上跑通、执行结果是否符合预期。
 *       这些需要真实数据库的集成测试，本项目暂无该基础设施。</li>
 * </ul>
 *
 * @author claude
 * @date 2026-08-13
 */
@DisplayName("SeoArticleDao.xml 静态校验")
class SeoArticleMapperXmlTest {

    private static final String XML_PATH = "mapper/SeoArticleDao.xml";

    private static String xml() {
        try (InputStream in = SeoArticleMapperXmlTest.class.getClassLoader().getResourceAsStream(XML_PATH)) {
            assertNotNull(in, XML_PATH + " 不在 classpath 上——mapper-locations 配的是 "
                    + "classpath*:/mapper/**/*.xml，文件放错目录会让接口在运行时才报 "
                    + "BindingException，单测阶段完全无感");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取 " + XML_PATH + " 失败", e);
        }
    }

    /** 截取指定 statement 的 SQL 片段，避免跨 statement 的断言互相干扰 */
    private static String statement(String xml, String id) {
        int start = xml.indexOf("id=\"" + id + "\"");
        assertTrue(start > 0, "找不到 statement: " + id);
        int end = xml.indexOf("</select>", start);
        assertTrue(end > start, id + " 缺少结束标签");
        return xml.substring(start, end);
    }

    @Nested
    @DisplayName("与 Dao 接口的绑定")
    class Binding {

        @Test
        @DisplayName("namespace 指向 SeoArticleDao，且声明了 selectArticlePage")
        void xml_namespaceAndStatementId_matchDaoInterface() {
            String content = xml();

            assertTrue(content.contains("namespace=\"com.jiuyu.replay.system.repository.dao.SeoArticleDao\""),
                    "namespace 与 Dao 全限定名对不上，MyBatis 启动时会报 BindingException");
            assertTrue(content.contains("id=\"selectArticlePage\""),
                    "statement id 必须与 Dao 方法名一致");
            assertTrue(content.contains("resultType=\"com.jiuyu.replay.system.entity.SeoArticleEntity\""),
                    "resultType 与 Dao 返回的泛型对不上");
        }
    }

    @Nested
    @DisplayName("标签筛选的关联条件")
    class TagJoinCondition {

        @Test
        @DisplayName("关联列必须带表别名限定——写裸 id 会让筛选恒为空")
        void tagSubquery_joinColumn_isQualifiedWithOuterAlias() {
            String content = xml();

            assertTrue(content.contains("t.article_id = a.id"),
                    "关联列没有用外层表别名限定。tb_seo_article_tag 自己也有 id 主键列，"
                            + "子查询里的未限定列名 MySQL 优先在子查询自己的表里解析——"
                            + "写成 t.article_id = id 会退化为 t.article_id = t.id，"
                            + "几乎永不成立，标签筛选恒为空且不报任何错。测试环境实际踩过这个坑");

            // 反向：不允许出现未限定的裸 id 作为关联列
            assertFalse(Pattern.compile("t\\.article_id\\s*=\\s*id\\b").matcher(content).find(),
                    "出现了未限定的裸 id 作为关联列");
        }

        @Test
        @DisplayName("用 EXISTS 半连接，不是先查 ID 集合再 IN")
        void tagSubquery_usesExistsSemiJoin() {
            String content = xml().toUpperCase(Locale.ROOT);

            assertTrue(content.contains("EXISTS"),
                    "标签筛选应走 EXISTS 半连接：先查出 article_id 集合再 IN 是两次查询，"
                            + "且一个标签下可能有上万篇文章，IN 列表既走不了索引也没法分页");
            assertTrue(content.contains("TB_SEO_ARTICLE_TAG"), "没有查关联表");
        }

        @Test
        @DisplayName("标签条件包在 <if> 里，不传 tagId 时不参与拼接")
        void tagCondition_wrappedInIfTag() {
            String content = xml();

            assertTrue(content.contains("<if test=\"bo.tagId != null\">"),
                    "标签条件必须是可选的，否则不传 tagId 时会变成必选筛选");
        }
    }

    @Nested
    @DisplayName("列表字段与排序")
    class ColumnsAndOrdering {

        @Test
        @DisplayName("不查 content——正文可能很大，列表页用不到")
        void select_doesNotIncludeContentColumn() {
            String content = xml();
            String selectClause = content.substring(
                    content.indexOf("SELECT"), content.indexOf("FROM tb_seo_article"));
            // 先剥掉 XML 注释再判断：注释里出现「正文/content」是正常的说明文字，
            // 直接对整段做 contains 会把注释误当成列名
            String columnsOnly = selectClause.replaceAll("(?s)<!--.*?-->", "");

            assertFalse(Pattern.compile("\\ba\\.content\\b").matcher(columnsOnly).find(),
                    "列表查询带上了正文列，会显著拖慢接口。实际列清单：" + columnsOnly);
            // 正向对照：确认断言作用在真实的列清单上，而不是一段恰好为空的文本
            assertTrue(columnsOnly.contains("a.title"), "实际列清单：" + columnsOnly);
            assertTrue(columnsOnly.contains("a.publish_time"),
                    "列表需展示发布时间。实际列清单：" + columnsOnly);
        }

        @Test
        @DisplayName("排序带 id 兜底——否则批量导入产生的同时间戳行翻页会重复或漏行")
        void orderBy_hasIdTiebreaker() {
            String content = xml();

            assertTrue(content.contains("ORDER BY a.update_date DESC, a.id DESC"),
                    "排序缺少 id 兜底。update_date 相同的行在 MySQL 里顺序不确定，"
                            + "而批量导入正好会让一批文章的 update_date 完全一致");
        }

        @Test
        @DisplayName("逻辑删除标记走参数占位，不在 XML 里硬编码 0")
        void isDeleted_usesParameterPlaceholder() {
            String content = xml();

            assertTrue(content.contains("a.is_deleted = #{notDeleted}"),
                    "未删除标记应由 SeoConstant.NOT_DELETED 传入——口径只该有一处定义");
        }

        @Test
        @DisplayName("其余三个筛选条件都是可选的")
        void optionalFilters_wrappedInIfTags() {
            String content = xml();

            assertTrue(content.contains("<if test=\"bo.categoryId != null\">"));
            assertTrue(content.contains("<if test=\"bo.articleStatus != null\">"));
            assertTrue(content.contains("bo.title != null and bo.title.trim() != ''"),
                    "标题为空白时不应拼 LIKE");
        }
    }

    @Nested
    @DisplayName("分类文章数聚合")
    class CategoryCountAggregation {

        @Test
        @DisplayName("用 GROUP BY 聚合，不是把明细行拉回内存计数")
        void countByCategoryIds_usesGroupBy() {
            String content = xml();

            assertTrue(content.contains("id=\"countByCategoryIds\""), "statement 不存在");
            assertTrue(content.toUpperCase(Locale.ROOT).contains("GROUP BY A.CATEGORY_ID"),
                    "一个分类下的文章可能上万，必须在 SQL 层聚合");
            assertTrue(content.contains("COUNT(*)"), "没有聚合函数");
        }

        @Test
        @DisplayName("统计口径：排除已删除，但【包含已下架】")
        void countByCategoryIds_countingScope_excludesDeletedButIncludesUnpublished() {
            String stmt = statement(xml(), "countByCategoryIds");

            assertTrue(stmt.contains("a.is_deleted = #{notDeleted}"),
                    "已删除文章仍被计入会让分类永远删不掉");
            // 下架只是暂时不对外，内容仍占用这个分类。按 status 过滤会让运营
            // 把非空分类误判为空分类，进而重复选题
            assertFalse(stmt.toLowerCase(Locale.ROOT).contains("article_status"),
                    "统计口径含已下架文章，不得按状态过滤。实际 SQL：" + stmt);
        }

        @Test
        @DisplayName("IN 列表用 foreach 展开为占位符，不是字符串拼接")
        void countByCategoryIds_inClause_usesForeach() {
            String stmt = statement(xml(), "countByCategoryIds");

            assertTrue(stmt.contains("<foreach collection=\"categoryIds\""), "实际 SQL：" + stmt);
            assertTrue(stmt.contains("#{categoryId}"), "IN 的每个值都要走预编译占位");
        }
    }

    @Nested
    @DisplayName("参数绑定安全")
    class ParameterBinding {

        @Test
        @DisplayName("全部用 #{} 预编译占位，没有 ${} 字符串拼接")
        void parameters_useHashPlaceholderNotDollar() {
            String content = xml();

            assertFalse(Pattern.compile("\\$\\{").matcher(content).find(),
                    "出现了 ${} 拼接，存在 SQL 注入风险。动态列名/排序另有 <choose> 等安全写法");
            assertTrue(content.contains("#{bo.tagId}"), "标签 ID 必须走预编译占位");
        }
    }
}
