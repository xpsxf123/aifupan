package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 算力计费系数配置。
 *
 * <p>供 AI Agent 客户端本地复刻服务端计费公式：
 * {@code billing = (tokenNum - cachedTokens) + cachedTokens * cacheRate}，再
 * {@code billing * defaultConsumeMultiple * 模型 consumeMultiple}，取整即实际计费 token。
 * 公式真相源见 {@code com.jiuyu.replay.common.utils.AiUtils#aiTokenConsumeMultiple}。</p>
 *
 * @author fupan-server
 */
@Data
public class AiCoefficientConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 默认消耗倍率（全局，AiUtils 内置基准）
     */
    private Double defaultConsumeMultiple;

    /**
     * 缓存命中计费倍率（全局，systemKv: ai_cache_token_rate；0.01=1%，1=不打折）
     */
    private Double cacheRate;

    /**
     * 各模型消耗倍率
     */
    private List<ModelMultiple> models;

    /**
     * 单个模型的消耗倍率。
     */
    @Data
    public static class ModelMultiple implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 模型编码
         */
        private String modelCode;

        /**
         * 模型名称
         */
        private String modelName;

        /**
         * 模型消耗倍率
         */
        private Double consumeMultiple;
    }
}
