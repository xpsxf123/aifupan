package com.jiuyu.replay.system.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SeoHtmlSanitizer 单测，对应 openspec AC-2。
 *
 * @author claude
 * @date 2026-08-12
 */
@DisplayName("SEO 正文 XSS 过滤")
class SeoHtmlSanitizerTest {

    @Nested
    @DisplayName("危险内容剥离")
    class DangerousContentRemoval {

        @Test
        @DisplayName("script 标签及其内容被剥离")
        void sanitize_scriptTag_removed() {
            String result = SeoHtmlSanitizer.sanitize("<p>正文</p><script>alert(1)</script>");

            assertFalse(result.contains("script"), "实际：" + result);
            assertFalse(result.contains("alert"), "script 内容也应剥离，实际：" + result);
            assertTrue(result.contains("正文"));
        }

        @Test
        @DisplayName("事件处理属性被剥离")
        void sanitize_eventHandlerAttribute_removed() {
            String result = SeoHtmlSanitizer.sanitize("<img src=\"https://a.com/x.jpg\" onerror=\"alert(1)\">");

            assertFalse(result.contains("onerror"), "实际：" + result);
            assertFalse(result.contains("alert"), "实际：" + result);
        }

        @Test
        @DisplayName("iframe 被剥离")
        void sanitize_iframe_removed() {
            String result = SeoHtmlSanitizer.sanitize("<p>a</p><iframe src=\"https://evil.com\"></iframe>");

            assertFalse(result.contains("iframe"), "实际：" + result);
        }

        @Test
        @DisplayName("javascript: 协议的链接被剥离")
        void sanitize_javascriptProtocolLink_removed() {
            String result = SeoHtmlSanitizer.sanitize("<a href=\"javascript:alert(1)\">点我</a>");

            assertFalse(result.contains("javascript:"), "实际：" + result);
            assertTrue(result.contains("点我"), "文字应保留，实际：" + result);
        }

        @Test
        @DisplayName("style 属性被剥离——行内样式会污染官网排版")
        void sanitize_styleAttribute_removed() {
            String result = SeoHtmlSanitizer.sanitize("<p style=\"color:red;font-size:40px\">正文</p>");

            assertFalse(result.contains("style"), "实际：" + result);
            assertTrue(result.contains("正文"));
        }
    }

    @Nested
    @DisplayName("白名单标签保留")
    class WhitelistPreservation {

        @Test
        @DisplayName("标题、强调、列表、引用保留")
        void sanitize_structuralTags_preserved() {
            String raw = "<h2>小节</h2><h3>子节</h3><p><strong>粗</strong><em>斜</em></p>"
                    + "<ul><li>一</li></ul><ol><li>二</li></ol><blockquote>引用</blockquote>";

            String result = SeoHtmlSanitizer.sanitize(raw);

            assertTrue(result.contains("<h2>小节</h2>"), "实际：" + result);
            assertTrue(result.contains("<h3>子节</h3>"), "实际：" + result);
            assertTrue(result.contains("<strong>粗</strong>"));
            assertTrue(result.contains("<em>斜</em>"));
            assertTrue(result.contains("<li>一</li>"));
            assertTrue(result.contains("<blockquote>引用</blockquote>"));
        }

        @Test
        @DisplayName("表格与代码块保留")
        void sanitize_tableAndCode_preserved() {
            String raw = "<table><thead><tr><th>头</th></tr></thead>"
                    + "<tbody><tr><td>体</td></tr></tbody></table><pre><code>int a = 1;</code></pre>";

            String result = SeoHtmlSanitizer.sanitize(raw);

            assertTrue(result.contains("<table>"), "实际：" + result);
            assertTrue(result.contains("<th>头</th>"));
            assertTrue(result.contains("<td>体</td>"));
            assertTrue(result.contains("<pre><code>"), "代码块结构应保留，实际：" + result);
            // OWASP 会把文本中的 = 编码为 &#61;（HTML 实体，官网渲染时仍显示为 =），
            // 故此处不断言原始等号，只断言代码文本本身没丢
            assertTrue(result.contains("int a"), "实际：" + result);
        }

        @Test
        @DisplayName("图片保留 src/alt/title")
        void sanitize_imageAllowedAttributes_preserved() {
            String result = SeoHtmlSanitizer.sanitize(
                    "<img src=\"https://cos.example.com/a.jpg\" alt=\"封面\" title=\"标题\">");

            assertTrue(result.contains("src=\"https://cos.example.com/a.jpg\""), "实际：" + result);
            assertTrue(result.contains("alt=\"封面\""), "实际：" + result);
        }
    }

    @Nested
    @DisplayName("h1 降级")
    class HeadingDowngrade {

        @Test
        @DisplayName("h1 降级为 h2 且文字不丢——官网用文章标题渲染唯一 H1")
        void sanitize_h1_downgradedToH2WithTextPreserved() {
            String result = SeoHtmlSanitizer.sanitize("<h1>这是一级标题</h1>");

            assertTrue(result.contains("这是一级标题"), "标题文字不得丢失，实际：" + result);
            assertTrue(result.contains("<h2>"), "应降级为 h2，实际：" + result);
            assertFalse(result.contains("<h1>"), "不应残留 h1，实际：" + result);
        }

        @Test
        @DisplayName("多个 h1 全部降级")
        void sanitize_multipleH1_allDowngraded() {
            String result = SeoHtmlSanitizer.sanitize("<h1>甲</h1><p>x</p><h1>乙</h1>");

            assertFalse(result.contains("<h1>"), "实际：" + result);
            assertTrue(result.contains("甲") && result.contains("乙"));
        }
    }

    @Nested
    @DisplayName("链接 rel 策略")
    class LinkRelPolicy {

        @Test
        @DisplayName("外链加 nofollow noopener——防权重外流与反向标签劫持")
        void sanitize_externalLink_getsNofollow() {
            String result = SeoHtmlSanitizer.sanitize("<a href=\"https://other.com/x\">外站</a>");

            assertTrue(result.contains("nofollow"), "实际：" + result);
            assertTrue(result.contains("noopener"), "实际：" + result);
        }

        @Test
        @DisplayName("站内绝对链接不加 nofollow——内链权重传递是标签聚合页的设计核心")
        void sanitize_internalAbsoluteLink_noNofollow() {
            String result = SeoHtmlSanitizer.sanitize(
                    "<a href=\"https://www.ifupan.com/tag/zhibofupan\">标签页</a>");

            assertFalse(result.contains("nofollow"), "站内链接被加了 nofollow，会自断内链：" + result);
            assertTrue(result.contains("标签页"));
        }

        @Test
        @DisplayName("相对路径视为内链，不加 nofollow")
        void sanitize_relativeLink_noNofollow() {
            String result = SeoHtmlSanitizer.sanitize("<a href=\"/article/zhibo-fupan\">另一篇</a>");

            assertFalse(result.contains("nofollow"), "实际：" + result);
        }

        @Test
        @DisplayName("协议相对 URL 是外链——浏览器会补全协议，不能因不以 http 开头就当内链")
        void sanitize_protocolRelativeUrl_treatedAsExternal() {
            String result = SeoHtmlSanitizer.sanitize("<a href=\"//evil.com/x\">x</a>");

            assertTrue(result.contains("nofollow"), "//evil.com 被误判为内链：" + result);
        }

        @Test
        @DisplayName("伪装子域不得被当作内链")
        void sanitize_lookalikeSubdomain_treatedAsExternal() {
            String result = SeoHtmlSanitizer.sanitize("<a href=\"https://www.ifupan.com.evil.com/x\">x</a>");

            assertTrue(result.contains("nofollow"), "伪装域名被当成内链：" + result);
        }

        @Test
        @DisplayName("userinfo 形式的伪装链接不得被当作内链")
        void sanitize_userinfoDisguisedUrl_treatedAsExternal() {
            String result = SeoHtmlSanitizer.sanitize("<a href=\"https://www.ifupan.com@evil.com/x\">x</a>");

            assertTrue(result.contains("nofollow"), "userinfo 伪装被当成内链：" + result);
        }

        @Test
        @DisplayName("apex 域名与带端口的站内链接不加 nofollow——误判会自断内链")
        void sanitize_apexAndPortedInternalLink_noNofollow() {
            String apex = SeoHtmlSanitizer.sanitize("<a href=\"https://ifupan.com/x\">x</a>");
            String ported = SeoHtmlSanitizer.sanitize("<a href=\"https://www.ifupan.com:443/x\">x</a>");

            assertFalse(apex.contains("nofollow"), "apex 域名被误判为外链：" + apex);
            assertFalse(ported.contains("nofollow"), "带端口的站内链接被误判为外链：" + ported);
        }

        @Test
        @DisplayName("大小写域名视为内链")
        void sanitize_uppercaseInternalHost_noNofollow() {
            String result = SeoHtmlSanitizer.sanitize("<a href=\"https://WWW.IFUPAN.COM/x\">x</a>");

            assertFalse(result.contains("nofollow"), "实际：" + result);
        }

        @Test
        @DisplayName("作者自带的 rel 不被放行——避免运营粘贴的 nofollow 留在内链上")
        void sanitize_authorSuppliedRelOnInternalLink_dropped() {
            String result = SeoHtmlSanitizer.sanitize(
                    "<a href=\"/tag/zhibofupan\" rel=\"nofollow\">标签</a>");

            assertFalse(result.contains("nofollow"), "内链保留了作者写的 nofollow：" + result);
        }
    }

    @Nested
    @DisplayName("边界输入")
    class EdgeCases {

        @Test
        @DisplayName("空输入返回空串而非 null")
        void sanitize_blankInput_returnsEmpty() {
            assertEquals("", SeoHtmlSanitizer.sanitize(null));
            assertEquals("", SeoHtmlSanitizer.sanitize("   "));
        }

        @Test
        @DisplayName("纯文本原样保留")
        void sanitize_plainText_preserved() {
            assertTrue(SeoHtmlSanitizer.sanitize("直播复盘怎么做").contains("直播复盘怎么做"));
        }
    }
}
