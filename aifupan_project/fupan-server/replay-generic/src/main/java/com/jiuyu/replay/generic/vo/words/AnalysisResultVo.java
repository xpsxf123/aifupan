package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "分析结果")
public class AnalysisResultVo {

    /**
     * 段落分析信息
     */
    @Schema(description = "段落分析信息")
    private List<SentenceMarkVo> sentenceMarkVos;
    /**
     * 分析结果的关键词类型信息
     */
    @Schema(description = "分析结果的关键词类型信息")
    private List<AnalysisResultCruxTypeVo> cruxTypeList;
    /**
     * 词语汇总列表
     */
    @Schema(description = "词语汇总列表")
    private List<WordsMarkVo> wordsCollect;
    /**
     * 词语tab列表
     */
    @Schema(description = "词语tab列表")
    private List<WordsTabVo> wordsTabList;
}
