package com.jiuyu.replay.common.utils;

import cn.hutool.core.util.ReUtil;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/24 16:36
 */
public class AiUtils {

    /**
     * 默认消耗倍率
     */
    private static final double DEFAULT_CONSUME_MULTIPLE = 1.2;

    /**
     * 获取默认消耗倍率（供计费系数配置对外暴露，避免调用方硬编码）
     *
     * @return 默认消耗倍率
     */
    public static double getDefaultConsumeMultiple() {
        return DEFAULT_CONSUME_MULTIPLE;
    }

    /**
     * 删除内容中的深度思考部分
     *
     * @param content 内容
     * @return 删除深度思考部分后的内容
     */
    public static String deleteDeepThinking(String content) {
        // 使用 Hutool 的正则工具
        String regex = "<div\\s+class=['\"]deepThinking['\"][^>]*>.*?</div>";
        return ReUtil.replaceAll(content, regex, "").trim();
    }

    /**
     * 根据临时令牌和消耗倍率计算实际消耗的令牌数
     *
     * @param tokenNum        令牌数
     * @param consumeMultiple 消耗倍率
     * @return 实际消耗的令牌数
     */
    public static long aiTokenConsumeMultiple(Long tokenNum, Double consumeMultiple) {
        double token = 0D;

        if (tokenNum != null) {
            token = tokenNum * DEFAULT_CONSUME_MULTIPLE;
        }

        if (consumeMultiple != null) {
            token = token * consumeMultiple;
        }
        return (long) token;
    }

    /**
     * 含缓存折扣的 token 消耗计算
     *
     * @param tokenNum        总 token 数
     * @param cachedTokens    缓存命中的 token 数
     * @param consumeMultiple 模型消耗倍率
     * @param cacheRate       缓存命中计费倍率（0.01=1%），null 或 <=0 或 >=1 视为不打折
     * @return 实际计费的 token 数
     */
    public static long aiTokenConsumeMultiple(Long tokenNum, Long cachedTokens, Double consumeMultiple, Double cacheRate) {
        long billing = tokenNum == null ? 0 : tokenNum;
        if (cachedTokens != null && cachedTokens > 0 && cacheRate != null && cacheRate > 0 && cacheRate < 1) {
            billing = (billing - cachedTokens) + (long) (cachedTokens * cacheRate);
        }
        return aiTokenConsumeMultiple(billing, consumeMultiple);
    }
}
