package com.jiuyu.replay.ai.api;

import com.jiuyu.replay.ai.rse.ConversationRse;
import com.jiuyu.replay.generic.bo.ai.ConversationBo;
import com.jiuyu.replay.generic.feign.ai.ConversationFeign;
import com.jiuyu.replay.generic.vo.ai.ConversationVo;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/16 下午3:21
 */
@Service
@AllArgsConstructor
public class ConversationApi implements ConversationFeign {

    private final ConversationRse conversationRse;

    @Resource(name = "generateHtmlExecutor")
    private Executor generateHtmlExecutor;

    @Override
    public List<ConversationVo> saveConversationData(List<ConversationBo> dataList) {
        return conversationRse.saveAll(dataList);
    }
}
