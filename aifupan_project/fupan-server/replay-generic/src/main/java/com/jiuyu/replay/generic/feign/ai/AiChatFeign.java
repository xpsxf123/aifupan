package com.jiuyu.replay.generic.feign.ai;

import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;

/**
 * AI 聊天调用 Feign 接口。
 * 封装 {@code ModelFactoryUtils.getAiModel() + AiModel.chatCompletion()}，
 * 使 replay-words 模块无需直接依赖 replay-third。
 *
 * @author lujie
 * @date 2025/6/25
 */
public interface AiChatFeign {

    /**
     * 非流式 AI 文本对话。
     *
     * @param aiModel      模型配置（含 apiKey、modelCode 等）
     * @param params       消息参数（system/user/assistant 消息列表）
     * @param resourceType 厂商类型：0=豆包，1=通义，2=DeepSeek
     * @return AI 返回结果（含 content、status、token 用量）
     */
    AiReturnDataVo chatCompletion(AiModelBo aiModel, AiMessageBo params, Integer resourceType);

    /**
     * 文本对话
     *
     * @param aiModel  模型
     * @param params   参数
     * @param sourceId 源id
     * @return 响应
     */
    AiReturnDataVo chatCompletion(AiModelBo aiModel, AiMessageBo params, Long sourceId);
}
