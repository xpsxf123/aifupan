package com.jiuyu.replay.system.util;

import com.github.promeg.pinyinhelper.Pinyin;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * SEO slug 生成与冲突处理。
 *
 * <p>slug 是官网公开 URL 的一部分（/article/{slug}、/category/{slug}、/tag/{slug}），
 * 一经收录就是长期资产，因此这里的规则需要长期稳定。
 *
 * <p>冲突时追加 4 位随机数而非主键 ID，有两个原因：
 * <ol>
 *   <li>slug 公开可见，用自增 ID 会让 article-1523 这类地址泄露站内内容总量；</li>
 *   <li>随机数在插入前即可算出，无需「先插入再回写」，也不必改主键生成策略。</li>
 * </ol>
 *
 * @author claude
 * @date 2026-08-12
 */
public final class SeoSlugUtil {

    /** 冲突时追加的随机数下界（4 位，避开前导零） */
    private static final int RANDOM_SUFFIX_MIN = 1000;

    /** 冲突时追加的随机数上界（含） */
    private static final int RANDOM_SUFFIX_MAX = 9999;

    /** 4 位随机数连续撞车的重试次数，超过则改用 6 位 */
    private static final int MAX_RETRY = 5;

    /** 兜底随机数下界（6 位） */
    private static final int FALLBACK_SUFFIX_MIN = 100000;

    /** 兜底随机数上界（含） */
    private static final int FALLBACK_SUFFIX_MAX = 999999;

    /** 6 位兜底同样需要重试，避免返回已被占用的值 */
    private static final int FALLBACK_MAX_RETRY = 20;

    /** 逻辑删除后释放 slug 用的墓碑分隔符，形如 {原值}__del_{id} */
    private static final String TOMBSTONE_SEPARATOR = "__del_";

    /** 合法 slug：仅小写字母、数字、连字符 */
    private static final Pattern VALID_SLUG = Pattern.compile("^[a-z0-9-]+$");

    /**
     * 零宽字符：视觉上不存在，须<b>直接删除</b>而非转连字符。
     *
     * <p>{@code zhibo<U+200B>fupan} 肉眼看就是 zhibofupan，若转成连字符会得到 zhibo-fupan，
     * 与运营看到的标题对不上，反而制造出「看起来一样、slug 不一样」的困惑。
     */
    private static final Pattern ZERO_WIDTH_CHARS = Pattern.compile("[\\u200B-\\u200D\\uFEFF]+");

    /**
     * 空白字符。除常规 \s 外还要覆盖 NBSP——运营从 Word / 网页粘贴标题时极常见，
     * 漏掉会让「看起来相同」的两个值绕过唯一性校验。NBSP 视觉上是空格，故转连字符。
     */
    private static final Pattern BLANK_CHARS = Pattern.compile("[\\s\\u00A0]+");

    /** slug 白名单之外的字符，一律折叠为连字符 */
    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[^a-z0-9-]+");

    /** 连续连字符 */
    private static final Pattern REPEATED_HYPHEN = Pattern.compile("-+");

    /** 首尾连字符 */
    private static final Pattern EDGE_HYPHEN = Pattern.compile("^-|-$");

    /** 变音符号，用于把 Café 归一化为 cafe 而非丢字 */
    private static final Pattern DIACRITIC_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private SeoSlugUtil() {
    }

    /**
     * 规范化 slug，保证输出要么为空串、要么是合法 slug。
     *
     * <p>必须在唯一性校验之前执行，否则 "Zhibo" 与 "zhibo" 会被当成两个不同的值绕过校验。
     *
     * <p>处理链：变音符归一 → 全角转半角 → 小写 → 空白转连字符 →
     * <b>白名单外字符折叠为连字符</b> → 合并连续连字符 → 去首尾连字符。
     *
     * <p>白名单过滤这一步不可省：slug 会直接进 URL 路径段，若放行 {@code / ? # %}
     * 会破坏路由，放行中文会产生百分号编码的脏 URL，放行尖括号则可能被官网模板回显。
     *
     * @param input 原始输入，可为 null
     * @return 规范化后的合法 slug；无有效字符时返回空串
     */
    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(input.length());
        for (char ch : input.toCharArray()) {
            builder.append(toHalfWidth(ch));
        }
        String text = stripDiacritics(builder.toString()).toLowerCase(Locale.ROOT);
        text = ZERO_WIDTH_CHARS.matcher(text).replaceAll("");
        text = BLANK_CHARS.matcher(text).replaceAll("-");
        text = ILLEGAL_CHARS.matcher(text).replaceAll("-");
        text = REPEATED_HYPHEN.matcher(text).replaceAll("-");
        return EDGE_HYPHEN.matcher(text).replaceAll("");
    }

    /**
     * 由中文名称生成拼音 slug。
     *
     * <p>规则：中文逐字转不带声调全拼且字间不分隔；英文数字原样保留转小写；
     * 标点空白转连字符；<b>中文与英文/数字的交界处插入连字符</b>。
     *
     * <p>最后一条是刻意的：不加分隔的 gmvtisheng 可读性差，也不符合 SEO URL 惯例，
     * 加了之后是 gmv-tisheng。
     *
     * <p>纯日文 / 韩文 / 西里尔 / emoji 等无拼音可取的输入会返回空串，
     * 调用方须用 {@link #resolveWithFallback} 兜底，不能让空 slug 入库。
     *
     * @param name 名称，如「GMV提升」
     * @return 拼音 slug，如 gmv-tisheng；无可转换字符时返回空串
     */
    public static String toPinyinSlug(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        CharKind lastKind = CharKind.SEPARATOR;
        // 先去变音符：否则 Café 的 é 会在字符分类时被当作分隔符丢掉，只剩 caf
        String source = stripDiacritics(name);

        for (char rawChar : source.toCharArray()) {
            char ch = toHalfWidth(rawChar);
            CharKind kind = classify(ch);

            if (kind == CharKind.SEPARATOR) {
                flush(segments, current);
                lastKind = CharKind.SEPARATOR;
                continue;
            }
            // 中英交界处切段，使 GMV提升 -> gmv-tisheng
            if (lastKind != CharKind.SEPARATOR && lastKind != kind) {
                flush(segments, current);
            }
            current.append(kind == CharKind.CHINESE
                    ? Pinyin.toPinyin(ch).toLowerCase(Locale.ROOT)
                    : Character.toLowerCase(ch));
            lastKind = kind;
        }
        flush(segments, current);

        return normalize(String.join("-", segments));
    }

    /**
     * 解析出可用的 slug：不冲突则原样返回，冲突则追加随机数。
     *
     * <p><b>防叠加机制</b>：靠「未冲突就原样返回」。编辑场景下查重排除自身，
     * slug 未变更即不会进入追加分支，因此反复保存不会变成 xxx-4827-3391。
     *
     * <p><b>切勿改成「slug 结尾是数字就跳过追加」</b>——那样 top-10-huashu 这类
     * 合法 slug 会被误判，且真冲突时会返回一个已被占用的值，反而制造唯一键冲突。
     *
     * @param baseSlug    候选 slug，内部会先规范化
     * @param existsCheck 占用判定；入参为候选 slug，返回 true 表示已被他人占用（须排除自身），不可为 null
     * @return 可用的 slug；baseSlug 规范化后为空时返回空串（调用方应改用 {@link #resolveWithFallback}）
     */
    public static String resolve(String baseSlug, Predicate<String> existsCheck) {
        if (existsCheck == null) {
            throw new IllegalArgumentException("existsCheck must not be null");
        }
        String base = normalize(baseSlug);
        if (base.isEmpty()) {
            return base;
        }
        if (!existsCheck.test(base)) {
            return base;
        }
        for (int i = 0; i < MAX_RETRY; i++) {
            String candidate = base + "-" + randomBetween(RANDOM_SUFFIX_MIN, RANDOM_SUFFIX_MAX);
            if (!existsCheck.test(candidate)) {
                return candidate;
            }
        }
        // 4 位连续撞满，扩到 6 位。同样逐个校验占用——数据库唯一索引虽能兜底，
        // 但没必要把一个已知被占用的值送到 DB 去换一次脏写异常
        for (int i = 0; i < FALLBACK_MAX_RETRY; i++) {
            String candidate = base + "-" + randomBetween(FALLBACK_SUFFIX_MIN, FALLBACK_SUFFIX_MAX);
            if (!existsCheck.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("无法为 slug 生成可用值，疑似占用判定异常：" + base);
    }

    /**
     * 带兜底前缀的解析：名称无法转出拼音时（纯日文 / emoji / 纯标点等）用前缀兜底。
     *
     * <p>不做兜底的后果：两篇这样的文章都会拿到空 slug，第二篇撞唯一索引，
     * 而「捕获冲突后重新生成」在这里必然二次失败——因为重新生成的还是空串。
     *
     * @param baseSlug       候选 slug
     * @param fallbackPrefix 兜底前缀，如 article / category / tag
     * @param existsCheck    占用判定
     * @return 可用的 slug，保证非空且合法
     */
    public static String resolveWithFallback(String baseSlug, String fallbackPrefix, Predicate<String> existsCheck) {
        String resolved = resolve(baseSlug, existsCheck);
        if (!resolved.isEmpty()) {
            return resolved;
        }
        String prefix = normalize(fallbackPrefix);
        if (prefix.isEmpty()) {
            prefix = "item";
        }
        return resolve(prefix + "-" + randomBetween(FALLBACK_SUFFIX_MIN, FALLBACK_SUFFIX_MAX), existsCheck);
    }

    /**
     * 校验 slug 是否只含小写字母、数字与连字符。
     *
     * @param slug 待校验值
     * @return 合法返回 true
     */
    public static boolean isValid(String slug) {
        return slug != null && !slug.isEmpty() && VALID_SLUG.matcher(slug).matches();
    }

    /**
     * 按最大长度截断 slug，并保证不以连字符结尾。
     *
     * <p>必须截断的原因：文章标题上限 100 字，转成全拼可达 300+ 字符，
     * 而 tb_seo_article.slug 是 varchar(180)——不截断会在**新增时**就撞
     * MySQL strict 模式的 1406 Data too long，而不是等到删除才出问题。
     *
     * @param slug   已规范化的 slug
     * @param maxLen 最大字符数
     * @return 截断后的 slug
     */
    public static String truncate(String slug, int maxLen) {
        if (slug == null || slug.length() <= maxLen) {
            return slug == null ? "" : slug;
        }
        String cut = slug.substring(0, maxLen);
        return EDGE_HYPHEN.matcher(cut).replaceAll("");
    }

    /**
     * 生成逻辑删除用的墓碑值。
     *
     * <p>删除时把 slug / name 改写为墓碑值，唯一索引即可保持单列，
     * 同时「删掉后能立刻重建同名同 slug 的记录」这一行为得以成立。
     *
     * <p><b>无条件追加，不因原值含分隔符就跳过。</b>「是否已删除」应由 isDeleted 状态判定，
     * 不能靠值里有没有 {@code __del_}——运营完全可以把标签取名为「年终促销__del_1」，
     * 若那时跳过追加，该名称会永久占用唯一索引，之后再也建不了同名标签。
     *
     * @param originalValue 原值，不可为 null
     * @param id            记录主键，保证多次删除同名记录不会互相撞索引
     * @return 墓碑值，形如 zhibofupan__del_1955000000000000001
     */
    public static String toTombstone(String originalValue, Long id) {
        if (originalValue == null || id == null) {
            throw new IllegalArgumentException("originalValue / id must not be null");
        }
        return originalValue + TOMBSTONE_SEPARATOR + id;
    }

    /**
     * 生成把某列改写为墓碑值的 SQL 片段，供批量删除一次性更新使用。
     *
     * <p>与 {@link #toTombstone} 必须保持同一格式——分隔符只在本类里出现一次，
     * 两处各自拼字符串迟早会不一致，那时「删除后能重建同名记录」这条就会静默失效。
     *
     * @param column 列名，仅接受调用方硬编码的常量（不接受用户输入）
     * @return 形如 {@code slug = CONCAT(slug, '__del_', id)}
     */
    public static String tombstoneSetSql(String column) {
        return column + " = CONCAT(" + column + ", '" + TOMBSTONE_SEPARATOR + "', id)";
    }

    /** 墓碑后缀的字符数上限：{@code __del_} 6 位 + 雪花 ID 19 位 */
    public static int tombstoneSuffixLength() {
        return TOMBSTONE_SEPARATOR.length() + 19;
    }

    /** 字符类别，用于中英交界处切段 */
    private enum CharKind {
        /** 有拼音可取的汉字 */
        CHINESE,
        /** ASCII 字母或数字 */
        ALNUM,
        /** 其余一律视为分隔符 */
        SEPARATOR
    }

    private static CharKind classify(char ch) {
        if (Pinyin.isChinese(ch)) {
            return CharKind.CHINESE;
        }
        if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
            return CharKind.ALNUM;
        }
        return CharKind.SEPARATOR;
    }

    /**
     * 全角字符转半角。
     *
     * <p>不做这一步的话，运营从别处粘贴来的全角字符会绕过唯一性校验。
     * 区间取 U+FF01–U+FF5E，精确映射到 ASCII U+0021–U+007E；两端的 U+FF00 未分配、
     * U+FF5F/U+FF60 无 ASCII 对应物，纳入会映射成控制字符，故排除。
     */
    private static char toHalfWidth(char ch) {
        if (ch == '　') {
            return ' ';
        }
        if (ch > '＀' && ch < '｟') {
            return (char) (ch - 65248);
        }
        return ch;
    }

    /** 去掉拉丁字母的变音符号，使 Café 归一为 cafe 而不是被当作分隔符丢掉 */
    private static String stripDiacritics(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return DIACRITIC_MARKS.matcher(decomposed).replaceAll("");
    }

    private static int randomBetween(int minInclusive, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    private static void flush(List<String> segments, StringBuilder current) {
        if (!current.isEmpty()) {
            segments.add(current.toString());
            current.setLength(0);
        }
    }
}
