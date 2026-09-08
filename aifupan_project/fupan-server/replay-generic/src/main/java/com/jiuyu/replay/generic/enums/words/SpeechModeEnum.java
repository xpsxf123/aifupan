package com.jiuyu.replay.generic.enums.words;

import lombok.Getter;

/**
 * 标准稿话术模式
 */
@Getter
public enum SpeechModeEnum {

    /**
     * 非循环模式，按完整直播流程顺序生成
     */
    NON_CYCLE(0, "非循环模式"),
    /**
     * 循环模式，按预估时长生成单轮时间轴，实际循环铺满
     */
    CYCLE(1, "循环模式");

    private final Integer code;
    private final String remarks;

    SpeechModeEnum(Integer code, String remarks) {
        this.code = code;
        this.remarks = remarks;
    }
}
