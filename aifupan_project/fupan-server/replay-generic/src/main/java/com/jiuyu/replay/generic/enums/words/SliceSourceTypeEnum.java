package com.jiuyu.replay.generic.enums.words;

import lombok.Getter;

@Getter
public enum SliceSourceTypeEnum {

    VIDEO_SLICE(0, "视频切片"),
    FILE_SLICE(1, "文件切片");

    private final Integer code;
    private final String remarks;

    SliceSourceTypeEnum(Integer code, String remarks) {
        this.code = code;
        this.remarks = remarks;
    }
}
