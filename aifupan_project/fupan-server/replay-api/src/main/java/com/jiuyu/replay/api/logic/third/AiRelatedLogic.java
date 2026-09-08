package com.jiuyu.replay.api.logic.third;

import com.jiuyu.replay.common.utils.CustomizeSseEmitter;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.third.vo.DanMuVo;
import com.jiuyu.replay.words.bo.AskRequestBo;
import com.jiuyu.replay.words.bo.ChatStreamProxyBo;
import com.jiuyu.replay.words.vo.AiOptionConfigVo;
import com.jiuyu.replay.words.vo.PromptAssemblyResultVo;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/3/22 下午4:38
 */
public interface AiRelatedLogic {

    /**
     * 问答接口
     * @param askRequestBo 参数
     * @param emitter 回调
     */
    void ask(AskRequestBo askRequestBo, CustomizeSseEmitter emitter, String webVersion);

    /**
     * 组装提示词（供C#客户端调用，服务器完成全部提示词拼装后返回结果）
     */
    PromptAssemblyResultVo assemblePrompt(AskRequestBo askRequestBo, String webVersion);

    /**
     * 组装提示词（供C#客户端调用，服务器完成全部提示词拼装后返回结果）
     */
    PromptAssemblyResultVo assemblePromptV2(AskRequestBo askRequestBo, String webVersion);

    /**
     * 构建额外的上一次对话的输出字符串
     *
     * @param askRequestBo 参数
     * @return 额外的上一次对话的输出字符串
     */
    String extraBuildLastOutString(AskRequestBo askRequestBo);

    /**
     * 获取上下文缓存id
     * @param askRequestBo
     * @return
     */
    R<String> getContextId(AskRequestBo askRequestBo);

    /**
     * 更新上下文缓存id
     * @param askRequestBo
     * @return
     */
    R<String> updateContextId(AskRequestBo askRequestBo);

    /**
     * 获取视频对应的ai配置选项
     * @param sourceType
     * @param sourceId
     * @return
     */
    R<AiOptionConfigVo> aiOptionConfig(Integer sourceType, String sourceId);

    /**
     * 获取弹幕列表
     *
     * @param queryDanMuBo
     * @return
     */
    R<List<DanMuVo>> danMuCacheList(QueryDanMuBo queryDanMuBo);

    /**
     * 服务端AI流式对话代理（供C#客户端调用，服务端持有API Key转发到各厂商API）
     *
     * @param bo      请求参数
     * @param emitter SSE回调
     */
    void chatStreamProxy(ChatStreamProxyBo bo, CustomizeSseEmitter emitter);
}
