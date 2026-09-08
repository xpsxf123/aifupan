package com.jiuyu.replay.ai.model;

import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;

import java.util.function.Consumer;

/**
 * ai相关的方法
 */
public interface AiModel {

    /**
     * 获取缓存内容
     * @return 缓存内容
     */
    String getSystemPrompt(String cacheKey);

    /**
     * 文本对话
     *
     * @return
     */
    AiReturnDataVo chatCompletion(AiModelBo aiModel, AiMessageBo params, Long sourceId);

    /**
     * 文本对话-流式返回
     *
     * @return
     */
    AiReturnDataVo chatCompletionStream(AiModelBo aiModel, AiMessageBo params, Consumer<ChatCompletionChoice> callback);

    /**
     * 创建上下文
     *
     * @return
     */
    AiReturnDataVo createContext(AiModelBo aiModel, String systemContent, String content, String redisKey, Long timeout);

    /**
     * 带上下文问答
     *
     * @return
     */
    AiReturnDataVo contextChatCompletion();

    /**
     * 带上下文问答-流式返回
     *
     * @return
     */
    AiReturnDataVo contextChatCompletionStream(AiModelBo aiModel, String contextId, AiMessageBo params, Consumer<ChatCompletionChoice> callback);

}
