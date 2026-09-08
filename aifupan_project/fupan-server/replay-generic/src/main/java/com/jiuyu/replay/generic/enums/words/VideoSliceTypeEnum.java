package com.jiuyu.replay.generic.enums.words;

import lombok.Getter;

@Getter
public enum VideoSliceTypeEnum {

    VIDEO(0, "原视频"),
    VIDEO_SLICE(1, "复盘视频切片"),
    SHORT_VIDEO_SLICE(2, "短视频切片");

    private final Integer code;
    private final String remarks;

    VideoSliceTypeEnum(Integer code, String remarks) {
        this.code = code;
        this.remarks = remarks;
    }
}
