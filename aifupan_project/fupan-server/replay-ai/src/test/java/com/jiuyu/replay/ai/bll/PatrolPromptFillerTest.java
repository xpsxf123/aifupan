package com.jiuyu.replay.ai.bll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PatrolPromptFiller 单测。
 *
 * <p>覆盖 AC-5 占位符填充的各分支。</p>
 *
 * @author beta
 * @date 2026-06-04
 */
class PatrolPromptFillerTest {

    private PatrolPromptFiller filler;

    @BeforeEach
    void setUp() {
        filler = new PatrolPromptFiller();
    }

    /**
     * AC-5: 两个占位符均存在 → 均被替换。
     */
    @Test
    void fill_bothPlaceholders_replaced() {
        String template = "平台：#{platform}，行业：#{trade}，分析要求如下。";
        String result = filler.fill(template, "抖音", "美妆");
        assertEquals("平台：抖音，行业：美妆，分析要求如下。", result);
    }

    /**
     * AC-5: 不含任何占位符 → 原样透传。
     */
    @Test
    void fill_neitherPlaceholder_unchanged() {
        String template = "这是一段没有占位符的提示词内容，直接分析以下数据。";
        String result = filler.fill(template, "抖音", "美妆");
        assertEquals(template, result, "无占位符时应原样返回");
    }

    /**
     * AC-5: platformName 为 null → 使用默认值"全平台"。
     */
    @Test
    void fill_platformNameNull_usesDefault() {
        String template = "平台：#{platform}，数据分析如下。";
        String result = filler.fill(template, null, "美妆");
        assertEquals("平台：全平台，数据分析如下。", result);
    }

    /**
     * AC-5: tradeName 为 null → 使用默认值"通用"。
     */
    @Test
    void fill_tradeNameNull_usesDefault() {
        String template = "行业：#{trade}，分析如下。";
        String result = filler.fill(template, "抖音", null);
        assertEquals("行业：通用，分析如下。", result);
    }

    /**
     * AC-5: 模板只含 platform 占位符，无 trade → trade 占位符不被改动。
     */
    @Test
    void fill_onlyPlatformPlaceholder_tradeUnchanged() {
        String template = "平台：#{platform}，分析如下。";
        String result = filler.fill(template, "快手", null);
        assertEquals("平台：快手，分析如下。", result);
    }

    /**
     * AC-5: 模板为 null → 返回空字符串（防御性）。
     */
    @Test
    void fill_nullTemplate_returnsEmpty() {
        String result = filler.fill(null, "抖音", "美妆");
        assertNotNull(result);
        assertEquals("", result);
    }

    /**
     * AC-5: 两个占位符均存在，两个值均为 null → 使用两个默认值。
     */
    @Test
    void fill_bothNull_usesBothDefaults() {
        String template = "平台：#{platform}，行业：#{trade}。";
        String result = filler.fill(template, null, null);
        assertEquals("平台：全平台，行业：通用。", result);
    }
}
