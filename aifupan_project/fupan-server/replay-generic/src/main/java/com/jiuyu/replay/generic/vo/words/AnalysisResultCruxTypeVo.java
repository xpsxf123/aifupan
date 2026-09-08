package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分析结果的关键词类型信息
 */
@Data
@Schema(description = "分析结果的关键词类型信息")
public class AnalysisResultCruxTypeVo {

    /**
     * 关键词分类信息
     */
    @Schema(description = "关键词分类信息")
    private CruxTypeInfoVo cruxTypeInfoVo;
    /**
     * 分类关键词占比比例值，0.21表示21%
     */
    @Schema(description = "占比比例值，0.21表示21%")
    private Double scale;
    /**
     * 分类关键词数量
     */
    @Schema(description = "分类关键词数量")
    private Integer num;
}
