package com.jiuyu.replay.ai.model.impl;

import com.jiuyu.replay.ai.model.AiModel;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/3/24 上午11:49
 */
@Slf4j
public class DefaultAiModelImpl implements AiModel {

    @Override
    public String getSystemPrompt(String cacheKey) {
        return null;
    }

    @Override
    public AiReturnDataVo chatCompletion(AiModelBo aiModel, AiMessageBo params, Long sourceId) {
        RRException.create("请重新写对应方法");
        return null;
    }

    @Override
    public AiReturnDataVo chatCompletionStream(AiModelBo aiModel, AiMessageBo params, Consumer<ChatCompletionChoice> callback) {
        RRException.create("请重新写对应方法");
        return null;
    }

    @Override
    public AiReturnDataVo createContext(AiModelBo aiModel, String systemContent, String content, String redisKey, Long timeout) {
        RRException.create("请重新写对应方法");
        return null;
    }

    @Override
    public AiReturnDataVo contextChatCompletion() {
        RRException.create("请重新写对应方法");
        return null;
    }

    @Override
    public AiReturnDataVo contextChatCompletionStream(AiModelBo aiModel, String contextId, AiMessageBo params, Consumer<ChatCompletionChoice> callback) {
        RRException.create("请重新写对应方法");
        return null;
    }
}
