package com.jiuyu.replay.generic.vo.words;

import lombok.Data;

@Data
public class WordListItemVo {

    /**
     * 词语
     */
    private String word;
    /**
     * 开始时间，毫秒
     */
    private Long startTime;
    /**
     * 结束时间，毫秒
     */
    private Long endTime;
}
