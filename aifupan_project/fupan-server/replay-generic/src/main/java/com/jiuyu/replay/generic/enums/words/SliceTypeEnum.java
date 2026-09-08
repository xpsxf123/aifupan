package com.jiuyu.replay.generic.enums.words;

import lombok.Getter;

@Getter
public enum SliceTypeEnum {

    VIDEO_SLICE(0, "复盘视频切片"),
    SHORT_VIDEO_SLICE(1, "短视频切片");

    private final Integer code;
    private final String remarks;

    SliceTypeEnum(Integer code, String remarks) {
        this.code = code;
        this.remarks = remarks;
    }
}
