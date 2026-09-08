package com.jiuyu.replay.ai.api;

import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.feign.ai.AiChatFeign;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.ai.factory.ModelFactoryUtils;
import com.jiuyu.replay.ai.model.AiModel;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * @author ：lujie
 * @description：AI 聊天 Feign 实现
 * @date ：2025/6/25
 */
@Component
public class AiChatApi implements AiChatFeign {

    @Override
    public AiReturnDataVo chatCompletion(AiModelBo aiModel, AiMessageBo params, Integer resourceType) {
        AiModel aiModelImpl = ModelFactoryUtils.getAiModel(resourceType);
        return aiModelImpl.chatCompletion(aiModel, params, null);
    }


    @Override
    public AiReturnDataVo chatCompletion(AiModelBo aiModelConfig, AiMessageBo params, Long sourceId) {
        AiModel aiModel = ModelFactoryUtils.getAiModel(aiModelConfig.getResourceType());
        if (aiModel == null) {
            throw new BusinessException("未知类型");
        }
        return aiModel.chatCompletion(aiModelConfig, params, sourceId);
    }
}
