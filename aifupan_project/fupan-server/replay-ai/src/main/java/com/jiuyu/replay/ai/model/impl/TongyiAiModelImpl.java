package com.jiuyu.replay.ai.model.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ：lujie
 * @description：通义ai
 * @date ：2025/3/20 上午11:13
 */
@Slf4j
public class TongyiAiModelImpl extends DefaultAiModelImpl {

    private List<MultiModalMessage> getMessage(AiMessageBo messages){
        ArrayList<MultiModalMessage> result = new ArrayList<>();

        if (ObjectUtil.isNotEmpty(messages.getSystem())){
            MultiModalMessage message = new MultiModalMessage();
            message.setRole(Role.SYSTEM.getValue());
            message.setContent(messages.getSystem());
            result.add(message);
        }
        if (ObjectUtil.isNotEmpty(messages.getUser())){
            MultiModalMessage message = new MultiModalMessage();
            message.setRole(Role.USER.getValue());
            message.setContent(messages.getUser());
            result.add(message);
        }
        if (ObjectUtil.isNotEmpty(messages.getAssistant())){
            MultiModalMessage message = new MultiModalMessage();
            message.setRole(Role.ASSISTANT.getValue());
            message.setContent(messages.getAssistant());
            result.add(message);
        }
        if (ObjectUtil.isNotEmpty(messages.getTool())){
            MultiModalMessage message = new MultiModalMessage();
            message.setRole(Role.TOOL.getValue());
            message.setContent(messages.getTool());
            result.add(message);
        }
        log.debug("===================通义的提示词：{}", JSONUtil.toJsonStr(result));
        RRException.isNotEmpty(result, "消息列表为空");
        return result;
    }

    @Override
    public AiReturnDataVo chatCompletion(AiModelBo aiModel, AiMessageBo params, Long sourceId) {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(aiModel.getApiKey())
                .model(aiModel.getModelName())
                .messages(getMessage(params))
                .build();
        AiReturnDataVo result = new AiReturnDataVo();
        MultiModalConversationResult res = null;
        try{
            res = call(conv, param);
        }catch (Exception e){
            log.error("调用通义千问报错(Exception):msg={}", e.getMessage());

        }
        try{
            result.setSourceId(sourceId);
            if (res == null || res.getOutput() == null){
                result.setStatus(1);
                return result;
            }
            result.setStatus(0);
            MultiModalConversationOutput.Choice choice = res.getOutput().getChoices().get(0);

            result.setRequestId(res.getRequestId());
            result.setContent(choice.getMessage().getContent().get(0).get("text").toString());
            result.setFinishReason(choice.getFinishReason());
            result.setPromptTokens(res.getUsage().getInputTokens());
            result.setCompletionTokens(res.getUsage().getOutputTokens());
            result.setVideoTokens(res.getUsage().getVideoTokens());
            result.setAudioTokens(res.getUsage().getAudioTokens());
            result.setImageTokens(res.getUsage().getImageTokens());
            result.setTotalTokens(res.getUsage().getTotalTokens());
            return result;
        }catch (Exception e){
            log.error("调用通义千问成功-后续报错(Exception):msg={}", e.getMessage());
            result.setSourceId(sourceId);
            result.setStatus(1);
            return result;
        }

    }


    private MultiModalConversationResult call(MultiModalConversation conv, MultiModalConversationParam param) {
        try {
            return conv.call(param);
        } catch (NoApiKeyException | UploadFileException e) {
            log.error("调用通义千问失败(call):msg={}", e.getMessage());
        }
        return null;
    }
}
