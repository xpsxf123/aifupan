package com.jiuyu.replay.system.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SeoSlugUtil 单测，对应 openspec AC-1。
 *
 * <p>覆盖 openspec §5.1 列出的三个易错点：查重排除自身、防叠加不能靠「结尾是数字」判断、
 * 以及中英交界连字符与同音异形词冲突。
 *
 * @author claude
 * @date 2026-08-12
 */
@DisplayName("SEO slug 生成与冲突处理")
class SeoSlugUtilTest {

    /** 占用集合为空，永不冲突 */
    private static final Predicate<String> NEVER_TAKEN = slug -> false;

    @Nested
    @DisplayName("拼音生成")
    class PinyinGeneration {

        @Test
        @DisplayName("纯中文逐字全拼且字间不加分隔")
        void toPinyinSlug_pureChinese_noSeparator() {
            assertEquals("zhibofupan", SeoSlugUtil.toPinyinSlug("直播复盘"));
            assertEquals("huashujiqiao", SeoSlugUtil.toPinyinSlug("话术技巧"));
        }

        @Test
        @DisplayName("中英交界处插入连字符——gmvtisheng 可读性差且不符合 SEO 惯例")
        void toPinyinSlug_chineseMixedWithLatin_insertsHyphenAtBoundary() {
            assertEquals("gmv-tisheng", SeoSlugUtil.toPinyinSlug("GMV提升"));
            assertEquals("douyin-seo", SeoSlugUtil.toPinyinSlug("抖音SEO"));
        }

        @Test
        @DisplayName("数字与中文交界同样插连字符")
        void toPinyinSlug_digitsMixedWithChinese_insertsHyphen() {
            assertEquals("2026-nian", SeoSlugUtil.toPinyinSlug("2026年"));
        }

        @Test
        @DisplayName("标点与空白转为连字符并合并")
        void toPinyinSlug_punctuationAndSpaces_convertedToSingleHyphen() {
            assertEquals("zhibo-fupan", SeoSlugUtil.toPinyinSlug("直播 复盘！"));
            assertEquals("zhibo-fupan", SeoSlugUtil.toPinyinSlug("直播——复盘"));
        }

        @Test
        @DisplayName("同音异形词生成相同拼音——这正是 slug 冲突的主要来源")
        void toPinyinSlug_homophones_produceIdenticalSlug() {
            assertEquals(SeoSlugUtil.toPinyinSlug("直播复盘"), SeoSlugUtil.toPinyinSlug("直播复判"));
        }

        @Test
        @DisplayName("空输入返回空串而非 null")
        void toPinyinSlug_blankInput_returnsEmpty() {
            assertEquals("", SeoSlugUtil.toPinyinSlug(null));
            assertEquals("", SeoSlugUtil.toPinyinSlug("   "));
        }

        @Test
        @DisplayName("带变音符的拉丁字母归一化而非丢字")
        void toPinyinSlug_latinWithDiacritics_normalized() {
            assertEquals("cafe-tisheng", SeoSlugUtil.toPinyinSlug("Café提升"));
        }

        @Test
        @DisplayName("无拼音可取的输入返回空串——调用方须用 resolveWithFallback 兜底")
        void toPinyinSlug_nonChineseNonAscii_returnsEmpty() {
            assertEquals("", SeoSlugUtil.toPinyinSlug("こんにちは"));
            assertEquals("", SeoSlugUtil.toPinyinSlug("안녕하세요"));
            assertEquals("", SeoSlugUtil.toPinyinSlug("Привет"));
            assertEquals("", SeoSlugUtil.toPinyinSlug("🔥🔥"));
            assertEquals("", SeoSlugUtil.toPinyinSlug("！！！---"));
        }
    }

    @Nested
    @DisplayName("空 slug 兜底")
    class FallbackForEmptySlug {

        @Test
        @DisplayName("纯日文标题不会落成空 slug——否则第二篇必撞唯一索引且重试无法自愈")
        void resolveWithFallback_emptyBase_usesPrefixedRandomValue() {
            String resolved = SeoSlugUtil.resolveWithFallback(
                    SeoSlugUtil.toPinyinSlug("こんにちは"), "article", NEVER_TAKEN);

            assertFalse(resolved.isEmpty(), "不得返回空 slug");
            assertTrue(SeoSlugUtil.isValid(resolved), "实际：" + resolved);
            assertTrue(resolved.startsWith("article-"), "实际：" + resolved);
        }

        @Test
        @DisplayName("两次兜底产生不同值，不会互相撞唯一索引")
        void resolveWithFallback_calledTwice_producesDistinctValues() {
            Set<String> results = new HashSet<>();
            for (int i = 0; i < 20; i++) {
                results.add(SeoSlugUtil.resolveWithFallback("", "article", NEVER_TAKEN));
            }

            assertTrue(results.size() > 1, "兜底值应带随机后缀，实际：" + results);
        }

        @Test
        @DisplayName("base 非空时兜底前缀不生效")
        void resolveWithFallback_nonEmptyBase_ignoresPrefix() {
            assertEquals("zhibofupan",
                    SeoSlugUtil.resolveWithFallback("zhibofupan", "article", NEVER_TAKEN));
        }
    }

    @Nested
    @DisplayName("规范化")
    class Normalization {

        @Test
        @DisplayName("大写转小写、首尾空白去除、中间空白转连字符")
        void normalize_mixedCaseAndSpaces_normalized() {
            assertEquals("zhibo-fupan", SeoSlugUtil.normalize("  ZhiBo Fupan  "));
        }

        @Test
        @DisplayName("全角字符转半角——否则会绕过唯一性校验")
        void normalize_fullWidthCharacters_convertedToHalfWidth() {
            assertEquals("gmv-ti", SeoSlugUtil.normalize("ＧＭＶ－ｔｉ"));
        }

        @Test
        @DisplayName("连续连字符合并、首尾连字符去除")
        void normalize_redundantHyphens_collapsed() {
            assertEquals("a-b-c", SeoSlugUtil.normalize("a--b--c"));
            assertEquals("abc", SeoSlugUtil.normalize("-abc-"));
        }

        @Test
        @DisplayName("白名单外字符被折叠——slug 直接进 URL 路径段，放行 / ? # 会破坏路由")
        void normalize_illegalCharacters_foldedToHyphen() {
            // normalize 只做过滤不做拼音转换，中文属白名单外字符，会被折叠掉；
            // 「中文标题 -> 拼音」是 toPinyinSlug 的职责
            assertEquals("etc-passwd", SeoSlugUtil.normalize("文章/../../etc/passwd"));
            // % ? = # 被折叠为连字符；2f 本身是合法字符故保留
            assertEquals("a-2fb-x-1-frag", SeoSlugUtil.normalize("a%2fb?x=1#frag"));
            assertEquals("a-b-c", SeoSlugUtil.normalize("a_b.c"));
        }

        @Test
        @DisplayName("尖括号内容被折叠，不会把标签原样带进 URL")
        void normalize_htmlTags_folded() {
            String result = SeoSlugUtil.normalize("<script>alert(1)</script>");

            assertTrue(SeoSlugUtil.isValid(result), "结果必须是合法 slug，实际：" + result);
            assertFalse(result.contains("<"), "实际：" + result);
        }

        @Test
        @DisplayName("NBSP 当空格转连字符，零宽字符直接删除——两者视觉表现不同")
        void normalize_nbspAndZeroWidth_handledByVisualAppearance() {
            // NBSP 看起来是空格，转连字符
            assertEquals("zhibo-fupan", SeoSlugUtil.normalize("zhibo\u00A0fupan"));
            // 零宽字符看不见，删除；若转连字符会得到与肉眼所见不符的 zhibo-fupan
            assertEquals("zhibofupan", SeoSlugUtil.normalize("zhibo\u200Bfupan"));
            assertEquals("zhibofupan", SeoSlugUtil.normalize("zhibo\uFEFFfupan"));
        }

        @Test
        @DisplayName("输出恒为合法 slug 或空串")
        void normalize_anyInput_alwaysValidOrEmpty() {
            for (String input : new String[]{
                    "文章/../../etc/passwd", "a%2fb?x=1#frag", "直播复盘", "<script>x</script>",
                    "！！！---", "🔥🔥", "Café提升", "  ZhiBo  "}) {
                String result = SeoSlugUtil.normalize(input);
                assertTrue(result.isEmpty() || SeoSlugUtil.isValid(result),
                        "输入 [" + input + "] 产出非法 slug：" + result);
            }
        }
    }

    @Nested
    @DisplayName("冲突解析")
    class ConflictResolution {

        @Test
        @DisplayName("不冲突时原样返回")
        void resolve_noConflict_returnsBaseUnchanged() {
            assertEquals("zhibofupan", SeoSlugUtil.resolve("zhibofupan", NEVER_TAKEN));
        }

        @Test
        @DisplayName("冲突时追加 4 位随机数，且结果不再冲突")
        void resolve_conflict_appendsFourDigitRandomSuffix() {
            Set<String> taken = new HashSet<>(Set.of("zhibofupan"));

            String resolved = SeoSlugUtil.resolve("zhibofupan", taken::contains);

            assertNotEquals("zhibofupan", resolved);
            assertFalse(taken.contains(resolved), "解析结果不得是已被占用的值");
            assertTrue(resolved.matches("^zhibofupan-\\d{4}$"), "应为 4 位随机后缀，实际：" + resolved);
        }

        @Test
        @DisplayName("追加的是随机数而非自增序号——避免公开 URL 泄露内容总量")
        void resolve_conflict_suffixIsRandomNotSequential() {
            Set<String> taken = new HashSet<>(Set.of("zhibofupan"));

            Set<String> results = new HashSet<>();
            for (int i = 0; i < 20; i++) {
                results.add(SeoSlugUtil.resolve("zhibofupan", taken::contains));
            }

            assertTrue(results.size() > 1, "20 次解析应产生多个不同后缀，实际只有：" + results);
        }

        @Test
        @DisplayName("结尾带数字的合法 slug 冲突时仍正确追加，不被误判为「已有后缀」")
        void resolve_baseEndingWithDigits_stillAppendsSuffix() {
            // 若防叠加用「结尾是数字就跳过」实现，这里会直接返回已被占用的值
            Set<String> taken = new HashSet<>(Set.of("top-10-huashu"));

            String resolved = SeoSlugUtil.resolve("top-10-huashu", taken::contains);

            assertFalse(taken.contains(resolved), "返回了已被占用的 slug —— 防叠加实现有误");
            assertTrue(resolved.matches("^top-10-huashu-\\d{4}$"), "实际：" + resolved);
        }

        @Test
        @DisplayName("已带随机后缀的 slug 未冲突时不再叠加")
        void resolve_alreadySuffixedButFree_doesNotStack() {
            assertEquals("zhibofupan-4827", SeoSlugUtil.resolve("zhibofupan-4827", NEVER_TAKEN));
        }

        // 注：「编辑时查重排除自身」的逻辑不在本工具类内，而在 Service 层的
        // exists(slug, excludeId) 查询里。此处不放桩测试——之前那版谓词写成
        // selfId != 1001L 恒为 false，等价于空测试，看着覆盖了其实什么都没验。
        // 该场景由 Service 层测试用真实 excludeId 覆盖。

        @Test
        @DisplayName("4 位连续撞满重试次数后降级为 6 位随机数，且结果仍不冲突")
        void resolve_fourDigitExhausted_fallsBackToSixDigits() {
            // 占用 base 与全部 4 位后缀，强制走兜底分支
            Predicate<String> takenAllFourDigit = slug ->
                    "zhibofupan".equals(slug) || slug.matches("^zhibofupan-\\d{4}$");

            String resolved = SeoSlugUtil.resolve("zhibofupan", takenAllFourDigit);

            assertTrue(resolved.matches("^zhibofupan-\\d{6}$"), "应降级为 6 位，实际：" + resolved);
            assertFalse(takenAllFourDigit.test(resolved), "兜底分支也不得返回已被占用的值");
        }

        @Test
        @DisplayName("所有候选都被占用时抛异常，而不是吐出一个已占用的值")
        void resolve_allCandidatesTaken_throwsInsteadOfReturningTakenValue() {
            assertThrows(IllegalStateException.class,
                    () -> SeoSlugUtil.resolve("zhibofupan", slug -> true));
        }

        @Test
        @DisplayName("existsCheck 为 null 时快速失败")
        void resolve_nullExistsCheck_throws() {
            assertThrows(IllegalArgumentException.class, () -> SeoSlugUtil.resolve("zhibofupan", null));
        }

        @Test
        @DisplayName("入参未规范化时先规范化再解析")
        void resolve_unnormalizedInput_normalizedFirst() {
            assertEquals("zhibo-fupan", SeoSlugUtil.resolve("  ZhiBo Fupan ", NEVER_TAKEN));
        }
    }

    @Nested
    @DisplayName("合法性校验与墓碑值")
    class ValidationAndTombstone {

        @Test
        @DisplayName("仅小写字母数字连字符为合法")
        void isValid_variousInputs_judgedCorrectly() {
            assertTrue(SeoSlugUtil.isValid("zhibo-fupan-2026"));
            assertFalse(SeoSlugUtil.isValid("Zhibo"), "大写非法");
            assertFalse(SeoSlugUtil.isValid("zhibo_fupan"), "下划线非法");
            assertFalse(SeoSlugUtil.isValid("直播复盘"), "中文非法");
            assertFalse(SeoSlugUtil.isValid(""));
            assertFalse(SeoSlugUtil.isValid(null));
        }

        @Test
        @DisplayName("墓碑值带主键，保证多次删除同名记录不互撞唯一索引")
        void toTombstone_withId_producesUniqueValue() {
            String first = SeoSlugUtil.toTombstone("zhibofupan", 1001L);
            String second = SeoSlugUtil.toTombstone("zhibofupan", 1002L);

            assertEquals("zhibofupan__del_1001", first);
            assertNotEquals(first, second);
        }

        @Test
        @DisplayName("原值自带 __del_ 时仍无条件追加——否则该名称会永久占用唯一索引")
        void toTombstone_valueContainingSeparator_stillAppends() {
            // 运营完全可以把标签命名为「年终促销__del_1」。若因值里含分隔符就跳过追加，
            // 删除后该 name 保持原样，在 uk_seo_tag_name 上永久占位，再也建不了同名标签
            String result = SeoSlugUtil.toTombstone("年终促销__del_1", 2002L);

            assertEquals("年终促销__del_1__del_2002", result);
        }

        @Test
        @DisplayName("按长度截断且不留尾部连字符——100 字标题转全拼会超出 slug 字段长度")
        void truncate_overlongSlug_cutAndTrimmed() {
            assertEquals("abc", SeoSlugUtil.truncate("abcdefg", 3));
            // 截断点恰好落在连字符上时不能留下尾部连字符
            assertEquals("ab", SeoSlugUtil.truncate("ab-cdefg", 3));
            assertEquals("abc", SeoSlugUtil.truncate("abc", 10));
            assertEquals("", SeoSlugUtil.truncate(null, 10));
        }

        @Test
        @DisplayName("墓碑后缀长度为 25 字符，与 DDL 预留一致")
        void tombstoneSuffixLength_matchesDdlReservation() {
            assertEquals(25, SeoSlugUtil.tombstoneSuffixLength());
            assertEquals(25, SeoSlugUtil.toTombstone("x", 1234567890123456789L).length() - 1);
        }
    }
}
