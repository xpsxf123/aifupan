package com.jiuyu.replay.generic.feign.ai;

import com.jiuyu.replay.generic.bo.ai.ConversationBo;
import com.jiuyu.replay.generic.vo.ai.ConversationVo;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/16 下午3:21
 */
public interface ConversationFeign {

    List<ConversationVo> saveConversationData(List<ConversationBo> dataList);
}
