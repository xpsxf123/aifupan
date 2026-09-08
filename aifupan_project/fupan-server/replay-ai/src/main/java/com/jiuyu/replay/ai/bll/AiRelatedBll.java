package com.jiuyu.replay.ai.bll;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.ai.rse.ConversationRse;
import com.jiuyu.replay.ai.rse.CustPromptRse;
import com.jiuyu.replay.ai.vo.AiPromptWordVo;
import com.jiuyu.replay.common.utils.AESUtil;
import com.jiuyu.replay.generic.feign.words.CueWordsFeign;
import com.jiuyu.replay.generic.vo.ai.ConversationVo;
import com.jiuyu.replay.generic.vo.ai.CustPromptVo;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/11/26 10:46
 */
@Component
@AllArgsConstructor
public class AiRelatedBll {

    private final CustPromptRse custPromptRse;
    private final CueWordsFeign cueWordsFeign;
    private final ConversationRse conversationRse;

    /**
     * 获取ai提示词
     *
     * @param cueWordsId   提示词id
     * @param cueWordsType 提示词类型 0：系统提示词，1：自定义提示词
     * @return 提示词
     */
    public String getAiPromptWord(Long cueWordsId, Integer cueWordsType, boolean encrypt) {
        String promptWord = "";
        if (cueWordsType == null) {
            CueWordsInfoVo wordsInfoVo = cueWordsFeign.getById(cueWordsId);
            if (wordsInfoVo != null) {
                promptWord = wordsInfoVo.getProblem();
            } else {
                CustPromptVo info = custPromptRse.info(cueWordsId);
                if (info != null) {
                    promptWord = info.getPromptContent();
                }
            }
        } else if (cueWordsType == 0) {
            CueWordsInfoVo wordsInfoVo = cueWordsFeign.getById(cueWordsId);
            if (wordsInfoVo != null) {
                promptWord = wordsInfoVo.getProblem();
            }
        } else if (cueWordsType == 1) {
            CustPromptVo info = custPromptRse.info(cueWordsId);
            if (info != null) {
                promptWord = info.getPromptContent();
            }
        } else {
            throw new BusinessException("提示词类型未知");
        }
        if (encrypt) {
            return ObjectUtil.isNotEmpty(promptWord) ? AESUtil.encrypt(promptWord) : null;
        } else {
            return promptWord;
        }
    }

    /**
     * 获取ai提示词
     *
     * @param cueWordsId         提示词id
     * @param cueWordsType       提示词类型 0：系统提示词，1：自定义提示词
     * @param lastConversationId 上次对话id
     * @return 提示词
     */
    public AiPromptWordVo getAiPromptWord2(Long cueWordsId, Integer cueWordsType, String lastConversationId, boolean encrypt) {
        // 获取当前提示词（未加密）
        String promptWord = getAiPromptWord(cueWordsId, cueWordsType, false);

        AiPromptWordVo vo = new AiPromptWordVo();
        vo.setPrompt(promptWord);
        if (encrypt && ObjectUtil.isNotEmpty(promptWord)) {
            vo.setPrompt(AESUtil.encrypt(promptWord));
        }

        if (ObjectUtil.isNotEmpty(lastConversationId)) {
            // 如果提示词中包含占位符 #{reason}，从上一次对话中提取 reason 内容
            ConversationVo lastConversation = conversationRse.getById(lastConversationId);
            if (lastConversation == null) {
                return vo;
            }
            if (ObjectUtil.isNotEmpty(promptWord) && promptWord.contains("#{reason}")) {
                if (ObjectUtil.isNotEmpty(lastConversation.getRealContent())) {
                    // 取 #{reason} 前后4个字符作为锚点，构建正则表达式匹配上一次的 reason 内容
                    int idx = promptWord.indexOf("#{reason}");
                    String before = promptWord.substring(Math.max(0, idx - 4), idx);
                    String after = promptWord.substring(idx + 9, Math.min(promptWord.length(), idx + 9 + 4));
                    String regex = Pattern.quote(before) + "([\\s\\S]*)" + Pattern.quote(after);
                    Matcher matcher = Pattern.compile(regex).matcher(lastConversation.getRealContent());
                    if (matcher.find()) {
                        vo.setReason(matcher.group(1));
                    }
                }
            }
            // 判断上次对话中是否包含 [的优化目的：]或者 [的优化动作：] 占位符
            if (ObjectUtil.isNotEmpty(lastConversation.getRealContent()) &&
                    (lastConversation.getRealContent().contains("的优化目的：") || lastConversation.getRealContent().contains("的优化动作："))) {
                vo.setOptimizeActions(1);
            }
        }

        return vo;
    }
}
