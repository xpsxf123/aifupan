package com.jiuyu.replay.words.vo;

import lombok.Data;

@Data
public class ContrastAnalysisResultCloudVo {

    /**
     * 对比数据1
     */
    private AnalysisResultClientCloudVo sentenceMark1;
    /**
     * 对比数据2
     */
    private AnalysisResultClientCloudVo sentenceMark2;
}
