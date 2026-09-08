package com.jiuyu.replay.words.vo;

import lombok.Data;

@Data
public class OnlineContrastAnalysisInfoVo {

    /**
     * 对比数据1
     */
    private OnlineAnalysisInfoVo sentenceMark1;
    /**
     * 对比数据2
     */
    private OnlineAnalysisInfoVo sentenceMark2;
}
