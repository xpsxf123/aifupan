package com.jiuyu.replay.api.logic.ai;

import com.jiuyu.replay.generic.vo.ai.AiContentCorrectionConfigVo;

/**
 * AI配置
 *
 * @author lujie
 * @date 2026-06-01
 */
public interface AiConfigLogic {

    /**
     * 获取不同场景的模型和提示词配置
     *
     * @param sceneType 场景类型 0-AI问答助手的纠正检查 1-AI问答助手的纠正 2-自然、优化原文的纠正检查 3-自然、优化原文的纠正
     * @return 配置信息
     */
    AiContentCorrectionConfigVo getContentCorrectionConfig(Integer sceneType);
}
