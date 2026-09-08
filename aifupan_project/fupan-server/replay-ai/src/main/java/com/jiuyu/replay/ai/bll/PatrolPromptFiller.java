package com.jiuyu.replay.ai.bll;

import org.springframework.stereotype.Component;

/**
 * 互动巡检提示词占位符填充工具。
 *
 * <p>负责将切片分析提示词模板中的 {@code #{platform}} / {@code #{trade}} 占位符
 * 替换为实际平台名/行业名；缺失数据时使用默认值。</p>
 *
 * <p>使用 {@code contains} 判断避免字符串不含占位符时的误替换（原样透传）。</p>
 *
 * @author beta
 * @date 2026-06-04
 */
@Component
public class PatrolPromptFiller {

    /**
     * 平台占位符
     */
    private static final String PLATFORM_PLACEHOLDER = "#{platform}";

    /**
     * 行业占位符
     */
    private static final String TRADE_PLACEHOLDER = "#{trade}";

    /**
     * 平台默认值（platformName 为 null 时使用）
     */
    private static final String DEFAULT_PLATFORM = "全平台";

    /**
     * 行业默认值（tradeName 为 null 时使用）
     */
    private static final String DEFAULT_TRADE = "通用";

    /**
     * 填充提示词模板中的占位符。
     *
     * <p>填充规则：</p>
     * <ul>
     *   <li>模板含 {@code #{platform}} → 替换为 platformName（null 时用"全平台"）</li>
     *   <li>模板含 {@code #{trade}} → 替换为 tradeName（null 时用"通用"）</li>
     *   <li>模板不含占位符 → 原样透传</li>
     * </ul>
     *
     * @param promptTemplate 提示词模板（可含 0-2 个占位符）
     * @param platformName   平台名称（VideoPlatformEnum.remarks，null 时填默认值）
     * @param tradeName      行业名称（TradeVo.name，null 时填默认值）
     * @return 填充后的提示词
     */
    public String fill(String promptTemplate, String platformName, String tradeName) {
        if (promptTemplate == null) {
            return "";
        }
        String result = promptTemplate;
        if (result.contains(PLATFORM_PLACEHOLDER)) {
            String actual = (platformName != null) ? platformName : DEFAULT_PLATFORM;
            result = result.replace(PLATFORM_PLACEHOLDER, actual);
        }
        if (result.contains(TRADE_PLACEHOLDER)) {
            String actual = (tradeName != null) ? tradeName : DEFAULT_TRADE;
            result = result.replace(TRADE_PLACEHOLDER, actual);
        }
        return result;
    }
}
