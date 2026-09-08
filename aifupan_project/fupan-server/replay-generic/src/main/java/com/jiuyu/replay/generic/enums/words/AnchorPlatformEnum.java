package com.jiuyu.replay.generic.enums.words;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/11/8 14:12
 */
@Getter
@AllArgsConstructor
public enum AnchorPlatformEnum {

    DOU_YIN(0, "抖音"),
    KUAI_SHOU(1, "快手"),
    SHI_PING_HAO(2, "视频号");

    private final Integer code;
    private final String msg;
}
