package com.jiuyu.replay.system.util;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 导入用的 Markdown 解析：拆 YAML front matter + 正文转 HTML。
 *
 * <p>只做「文本 → 结构」这一层，不碰数据库、不做业务校验、不抓图片，
 * 因此可以纯单测覆盖。业务规则（分类必须存在、封面必填等）在导入执行器里判。
 *
 * @author claude
 * @date 2026-08-13
 */
public final class SeoMarkdownParser {

    /**
     * front matter 定界符。要求文件以 --- 开头（允许 BOM 与前导空行），
     * 到下一个单独成行的 --- 或 ... 为止。
     */
    private static final Pattern FRONT_MATTER = Pattern.compile(
            "\\A\\uFEFF?\\s*^-{3}\\s*$\\R(.*?)\\R^(?:-{3}|\\.{3})\\s*$\\R?",
            Pattern.DOTALL | Pattern.MULTILINE);

    /**
     * YAML 别名/锚点会被 SafeConstructor 正常展开，但十亿笑（billion laughs）
     * 靠的正是嵌套别名的指数级展开——上限设小即可，front matter 本就只有十来个标量键。
     */
    private static final int YAML_MAX_ALIASES = 16;

    /** 单个 front matter 的字符上限，防止畸形文件把整份正文当 YAML 解析 */
    private static final int MAX_FRONT_MATTER_CHARS = 20_000;

    private static final List<org.commonmark.Extension> EXTENSIONS =
            List.of(TablesExtension.create());

    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();

    /**
     * 渲染器不开 escapeHtml：md 里内联的 HTML 需要保留（作者会写 &lt;br&gt;）。
     * 安全性不靠这里保证——渲染结果一律再过 {@link SeoHtmlSanitizer}，
     * 那才是唯一的过滤点。两处都过滤反而会让白名单出现两份、迟早不一致。
     */
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    private SeoMarkdownParser() {
    }

    /**
     * 解析结果。
     *
     * @param frontMatter front matter 键值对，缺失时为空 Map（不是 null）
     * @param bodyHtml    正文渲染出的 HTML，尚未过滤
     */
    public record ParsedMarkdown(Map<String, Object> frontMatter, String bodyHtml) {
    }

    /** front matter 缺失或格式非法时抛出，消息直接进导入结果的 notes */
    public static class MarkdownParseException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public MarkdownParseException(String message) {
            super(message);
        }
    }

    /**
     * 解析一份 Markdown 文本。
     *
     * @param markdown 文件全文
     * @return front matter + 正文 HTML
     * @throws MarkdownParseException front matter 缺失或 YAML 非法
     */
    public static ParsedMarkdown parse(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            throw new MarkdownParseException("文件内容为空");
        }
        Matcher matcher = FRONT_MATTER.matcher(markdown);
        if (!matcher.find()) {
            throw new MarkdownParseException(
                    "未找到 YAML front matter：文件需以一行 --- 开头，写入 title/category/cover 等字段后再以一行 --- 结束");
        }
        String yamlText = matcher.group(1);
        if (yamlText.length() > MAX_FRONT_MATTER_CHARS) {
            throw new MarkdownParseException("front matter 过长（" + yamlText.length() + " 字符），请检查是否漏写了结束的 ---");
        }
        Map<String, Object> frontMatter = parseYaml(yamlText);

        String body = markdown.substring(matcher.end());
        return new ParsedMarkdown(frontMatter, toHtml(body));
    }

    /** Markdown 正文转 HTML。结果仍需过 {@link SeoHtmlSanitizer} 才能入库 */
    public static String toHtml(String markdownBody) {
        if (markdownBody == null || markdownBody.isBlank()) {
            return "";
        }
        Node document = PARSER.parse(markdownBody);
        return RENDERER.render(document);
    }

    /**
     * 取字符串字段。YAML 会把 {@code 2026-08-13} 解析成 Date、{@code 123} 解析成 Integer，
     * 所以统一 toString 而不是强转 String——强转会在这些情况下抛 ClassCastException，
     * 运营看到的将是「导入失败」而不是具体原因。
     *
     * @param frontMatter front matter
     * @param key         键名
     * @return 去空白后的值；键不存在、值为 null 或去空白后为空串时返回空 Optional
     */
    public static Optional<String> getString(Map<String, Object> frontMatter, String key) {
        Object value = frontMatter.get(key);
        if (value == null) {
            return Optional.empty();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? Optional.empty() : Optional.of(text);
    }

    /**
     * 取字符串列表字段。同时接受两种写法，AI 产出的 md 两种都会出现：
     * <pre>
     * tags: [直播复盘, 数据分析]     → List
     * tags: 直播复盘, 数据分析       → 逗号分隔的标量（中英文逗号都认）
     * </pre>
     *
     * @param frontMatter front matter
     * @param key         键名
     * @return 去重后的非空字符串列表，顺序保持原样
     */
    public static List<String> getStringList(Map<String, Object> frontMatter, String key) {
        Object value = frontMatter.get(key);
        if (value == null) {
            return Collections.emptyList();
        }
        List<String> raw = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    raw.add(String.valueOf(item));
                }
            }
        } else {
            for (String part : String.valueOf(value).split("[,，]")) {
                raw.add(part);
            }
        }
        List<String> result = new ArrayList<>(raw.size());
        for (String item : raw) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * SafeConstructor 而非默认 Constructor：默认构造器支持 {@code !!javax.script.ScriptEngineManager}
     * 这类标签，能在解析阶段实例化任意类——front matter 来自导入的文件，是不可信输入。
     */
    private static Map<String, Object> parseYaml(String yamlText) {
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(YAML_MAX_ALIASES);
        options.setAllowDuplicateKeys(false);
        Object loaded;
        try {
            loaded = new Yaml(new SafeConstructor(options)).load(yamlText);
        } catch (MarkedYAMLException e) {
            throw new MarkdownParseException("front matter YAML 解析失败" + describeLine(e.getProblemMark())
                    + "：" + e.getProblem());
        } catch (YAMLException e) {
            throw new MarkdownParseException("front matter YAML 解析失败：" + e.getMessage());
        }
        if (loaded == null) {
            return Collections.emptyMap();
        }
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new MarkdownParseException("front matter 必须是 key: value 形式的键值对");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()).trim(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 行号从 0 起，展示时 +1。标的是 <b>front matter 内部</b>的行号而非文件行号——
     * 定界符 --- 之前允许有 BOM 和空行，换算成文件行号会不准，宁可说清楚基准。
     * 拿不到位置就不写行号，别给个假的。
     */
    private static String describeLine(Mark mark) {
        return mark == null ? "" : "（front matter 第 " + (mark.getLine() + 1) + " 行）";
    }
}
