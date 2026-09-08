package com.jiuyu.replay.generic.feign.third;


import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;

import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.third.AiTempTokenVo;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;

import java.util.List;
import java.util.Map;

/**
 * @author lyw
 */
public interface AiFeign {

    /**
     * 把分钟段落转成对应问答内容
     *
     * @param contentList
     * @param cueWord
     * @param positionMap
     * @return
     */
    List<String> getVideoContentSection(List<String> contentList, String cueWord, Map<String, String> positionMap);

    /**
     * 获取分析模型临时token
     *
     * @param code 模型编码
     * @return 临时token
     */
    AiTempTokenVo getAnalysisTempToken(String code);
}
