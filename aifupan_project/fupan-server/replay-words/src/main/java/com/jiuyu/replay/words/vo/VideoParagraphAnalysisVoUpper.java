package com.jiuyu.replay.words.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class VideoParagraphAnalysisVoUpper {

    /**
     * ID
     */
    private Long id;
    /**
     * 视频唯一标识uuid
     */
    private String videoId;
    /**
     * 当前段落 从1开始
     */
    private Integer paragraph;
    /**
     * 识别状态状态 0：成功 1：失败
     */
    private Integer status;
    /**
     * 词语json字符串内容
     */
    private String dataJson;
    /**
     * 行业id
     */
    private Long tradeId;
}
