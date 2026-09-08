package com.jiuyu.replay.generic.enums.words;

import lombok.Getter;

@Getter
public enum VideoPlatformEnum {

    ALL("0", "全平台"),
    DOUYIN("1", "抖音"),
    KUAISHOU("2", "快手"),
    SHIPINHAO("3", "视频号");

    private final String code;
    private final String remarks;

    VideoPlatformEnum(String code, String remarks) {
        this.code = code;
        this.remarks = remarks;
    }
}
