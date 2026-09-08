package com.jiuyu.replay.third.api;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.common.bll.SystemKvBll;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.feign.ai.AiChatFeign;
import com.jiuyu.replay.generic.feign.order.AiTokenUseRecordFeign;
import com.jiuyu.replay.generic.feign.third.AiFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.third.AiTempTokenVo;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import com.jiuyu.replay.third.bll.AiModelBll;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * @author lyw
 */
@Component
@AllArgsConstructor
@Slf4j
public class AiApi implements AiFeign {

    @Resource
    private AiModelBll aiModelBll;
    @Resource
    private SystemKvBll systemKvBll;
    @Resource
    private AiChatFeign aiChatFeign;

    @Override
    public List<String> getVideoContentSection(List<String> contentList, String cueWord, Map<String, String> positionMap) {
        int questionNum = 10000;
        SystemKvInfoVo questionNumVo = ResultUtil.getResult(systemKvBll.getByKey("ai_question_length"));
        if (questionNumVo != null) {
            questionNum = NumberUtil.parseInt(questionNumVo.getKvValue(), 10000);
        }
        return getParms(contentList, cueWord, questionNum, null, positionMap.getOrDefault("trade", ""));
    }

    @Override
    public AiTempTokenVo getAnalysisTempToken(String code) {
        return aiModelBll.getAnalysisTempToken(code);
    }

    private List<String> getParms(List<String> contents, String cueWord, Integer length, Integer type, String tradeName) {
        SystemKvInfoVo outWordsNum = ResultUtil.getResult(systemKvBll.getByKey("ai_question_outWordsNum"));
        //开头提示：如、请帮我整理一下直播文稿
        // 检查并替换
        if (cueWord.contains("#{trade}")) {
            cueWord = cueWord.replace("#{trade}", tradeName);
        }

        StringBuilder sb = new StringBuilder(cueWord);
        SystemKvInfoVo aiQuestionRequire = ResultUtil.getResult(systemKvBll.getByKey("ai_question_require"));
        if (ObjectUtil.isNotNull(aiQuestionRequire)){
            //要求前缀拼接
            sb.append("\n").append(aiQuestionRequire.getKvValue());
        }
        sb.append("\n")
                .append("以下的是一场直播稿的某一个片段，原文直播稿片段如下：").append("\n");
        String prefix = sb.toString();

        List<String> result = new ArrayList<>(contents);

        List<String> newData = new ArrayList<>();
        StringBuilder currentGroup = new StringBuilder();
        int contentLen = 0;
        int total = result.size();
        for (int i = 0; i < total; i++) {
            String content = result.get(i);
            int remaining = total - i;
            int sepLen = !currentGroup.isEmpty() ? 2 : 0;
            int newLen = contentLen + sepLen + content.length();

            if (newLen <= length) {
                if (!currentGroup.isEmpty()) {
                    currentGroup.append("\n\n");
                }
                currentGroup.append(content);
                contentLen = newLen;
            } else if (remaining <= 2) {
                // 剩余 ≤ 2 段时强制归并到当前组，避免尾部产生孤段
                if (!currentGroup.isEmpty()) {
                    currentGroup.append("\n\n");
                }
                currentGroup.append(content);
                contentLen = newLen;
            } else {
                // 封包并起新组
                splitAndAdd(prefix + currentGroup, currentGroup.toString(), newData, outWordsNum);
                currentGroup.setLength(0);
                currentGroup.append(content);
                contentLen = content.length();
            }
        }
        if (!currentGroup.isEmpty()) {
            splitAndAdd(prefix + currentGroup, currentGroup.toString(), newData, outWordsNum);
        }
        return newData;
    }


    //替换内容的占位符
    private void splitAndAdd(String newStr,String text, List<String> result,SystemKvInfoVo outWordsNum) {
        if (ObjectUtil.isNotNull(outWordsNum)) {
            BigDecimal bigDecimal = new BigDecimal(outWordsNum.getKvValue());
            BigDecimal multiply = bigDecimal.multiply(BigDecimal.valueOf(text.length()));
            String replacement = multiply.toBigInteger().toString();
            if (newStr.contains("#{outWordsNum}")) {
                String replace = newStr.replace("#{outWordsNum}", replacement);
                result.add(replace);
            }else {
                result.add(newStr);
            }
        }else {
            result.add(newStr);
        }
    }




    //如果他本身一段就超过了一万字wordsNum  那么就进行拆解
    private void splitAndAdd(String text, List<String> result,Integer wordsNum) {
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + wordsNum, text.length());

            // 查找最近的分割点（避免在中间切断单词/句子）
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                int lastNewline = text.lastIndexOf('\n', end);

                // 优先选择换行符位置，其次选择空格位置
                int optimalSplit = Math.max(lastNewline, lastSpace);
                if (optimalSplit > start) {
                    end = optimalSplit;
                }
            }

            result.add(text.substring(start, end).trim());
            start = end;

            // 跳过分割后的空白字符
            while (start < text.length() &&
                    (text.charAt(start) == ' ' || text.charAt(start) == '\n')) {
                start++;
            }
        }
    }


}
