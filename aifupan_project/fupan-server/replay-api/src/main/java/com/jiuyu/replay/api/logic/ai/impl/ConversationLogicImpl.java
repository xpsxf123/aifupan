package com.jiuyu.replay.api.logic.ai.impl;

import com.jiuyu.replay.ai.bll.ConversationBll;
import com.jiuyu.replay.generic.bo.ai.ConversationBo;
import com.jiuyu.replay.generic.bo.ai.ConversationListBo;
import com.jiuyu.replay.generic.vo.ai.ConversationPage;
import com.jiuyu.replay.generic.vo.ai.ConversationVo;
import com.jiuyu.replay.api.logic.ai.ConversationLogic;
import com.jiuyu.replay.generic.vo.common.R;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/22 下午3:40
 */
@Component
@AllArgsConstructor
public class ConversationLogicImpl implements ConversationLogic {

    private final ConversationBll conversationBll;

}
