package com.jiuyu.replay.system.util;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import java.util.List;
import java.util.Locale;

/**
 * SEO 正文 HTML 的 XSS 白名单过滤。
 *
 * <p>正文有两个来源——后台富文本编辑器产出的 HTML、批量导入的 Markdown 转换结果
 * （Markdown 允许内嵌原始 HTML），两者都是不可信输入，统一在入库前过滤。
 *
 * <p>两处与「照搬默认策略」不同的刻意选择，见 {@link #POLICY} 上的说明。
 *
 * @author claude
 * @date 2026-08-12
 */
public final class SeoHtmlSanitizer {

    /**
     * 官网站内 apex 域名。带 www 的、其他子域、带端口的形式均视为站内。
     *
     * <p>若测试/预发环境用了别的域名，那里的站内链接会被当外链打上 nofollow 并固化入库，
     * 需要时提为配置项。
     */
    private static final String SITE_APEX_HOST = "ifupan.com";

    /** 外链附加的 rel 值：nofollow 防权重外流，noopener 防反向标签劫持 */
    private static final String EXTERNAL_LINK_REL = "nofollow noopener";

    /**
     * 正文过滤策略。
     *
     * <p><b>h1 重命名为 h2 而非剥离</b>：官网用文章标题渲染页面唯一的 H1，正文再出现 H1
     * 会削弱结构信号。若把 h1 排除在白名单外，OWASP 会连标签一起剥掉——文字还在，
     * 但「这是一个小节标题」的层级信息丢了。重命名能同时满足两点。
     *
     * <p><b>只给外链加 nofollow，不用 requireRelNofollowOnLinks()</b>：那个 API 会给
     * 所有链接都加 nofollow，包括指向本站分类页/标签页/其他文章的内链——而内链权重传递
     * 正是「标签产出聚合页」这一设计的核心。给内链加 nofollow 等于自断内链。
     */
    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements(
                    "h2", "h3", "p", "strong", "em", "ul", "ol", "li", "blockquote",
                    "table", "thead", "tbody", "tr", "th", "td",
                    "code", "pre", "hr", "br")
            // h1 -> h2，保留层级而非剥成纯文本
            .allowElements((elementName, attrs) -> "h2", "h1")
            .allowElements(SeoHtmlSanitizer::applyLinkPolicy, "a")
            .allowAttributes("href").onElements("a")
            // 刻意不放行作者自带的 rel：否则运营粘贴来的 rel="nofollow" 会留在内链上，
            // 与「绝不给内链加 nofollow」的设计相悖。外链的 rel 由 applyLinkPolicy 直接写入
            .allowAttributes("src", "alt", "title").onElements("img")
            .allowElements("img")
            .allowUrlProtocols("http", "https")
            .toFactory();

    private SeoHtmlSanitizer() {
    }

    /**
     * 过滤正文 HTML。
     *
     * @param rawHtml 原始 HTML，可为 null
     * @return 过滤后的安全 HTML；入参为空时返回空串
     */
    public static String sanitize(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }
        return POLICY.sanitize(rawHtml);
    }

    /**
     * 链接元素策略：外链补 rel，内链保持原样。
     *
     * @param elementName 元素名，恒为 a
     * @param attrs       属性列表，形如 [name1, value1, name2, value2, ...]，可就地修改
     * @return 元素名；返回 null 表示丢弃该元素
     */
    private static String applyLinkPolicy(String elementName, List<String> attrs) {
        String href = findAttribute(attrs, "href");
        if (href != null && isExternalLink(href)) {
            removeAttribute(attrs, "rel");
            attrs.add("rel");
            attrs.add(EXTERNAL_LINK_REL);
        }
        return "a";
    }

    /**
     * 判定是否为站外链接。
     *
     * <p>站内：相对路径、锚点、以及 host 为 {@value #SITE_APEX_HOST} 或其子域的绝对路径。
     *
     * <p>三处容易判错的地方：
     * <ul>
     *   <li><b>协议相对 URL</b> {@code //evil.com} 会被浏览器补成 https，是不折不扣的外链，
     *       不能因为「不以 http 开头」就归为内链；</li>
     *   <li>host 里可能带<b>端口</b>（www.ifupan.com:443）或<b>末尾点</b>（www.ifupan.com.），
     *       直接 equals 会把内链误判为外链，反而自断内链；</li>
     *   <li>用 endsWith 匹配域名会被 <b>www.ifupan.com.evil.com</b> 绕过，
     *       故按「完全相等 或 以 .apex 结尾」判定。</li>
     * </ul>
     */
    private static boolean isExternalLink(String href) {
        String normalized = href.trim().toLowerCase(Locale.ROOT);
        // 协议相对 URL：浏览器会补全当前协议，实际是外链
        if (normalized.startsWith("//")) {
            return true;
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            // 相对路径、锚点、mailto 之外的协议已被 allowUrlProtocols 拦掉，此处视为站内
            return false;
        }
        String withoutProtocol = normalized.replaceFirst("^https?://", "");
        String authority = withoutProtocol.split("[/?#]", 2)[0];
        // 去掉 userinfo（user@host）后再取 host，避免 https://www.ifupan.com@evil.com 蒙混过关
        int atIndex = authority.lastIndexOf('@');
        String hostPort = atIndex >= 0 ? authority.substring(atIndex + 1) : authority;
        String host = hostPort.split(":", 2)[0].replaceAll("\\.$", "");
        boolean internal = host.equals(SITE_APEX_HOST) || host.endsWith("." + SITE_APEX_HOST);
        return !internal;
    }

    private static String findAttribute(List<String> attrs, String name) {
        for (int i = 0; i + 1 < attrs.size(); i += 2) {
            if (name.equalsIgnoreCase(attrs.get(i))) {
                return attrs.get(i + 1);
            }
        }
        return null;
    }

    private static void removeAttribute(List<String> attrs, String name) {
        for (int i = 0; i + 1 < attrs.size(); i += 2) {
            if (name.equalsIgnoreCase(attrs.get(i))) {
                attrs.remove(i + 1);
                attrs.remove(i);
                return;
            }
        }
    }
}
