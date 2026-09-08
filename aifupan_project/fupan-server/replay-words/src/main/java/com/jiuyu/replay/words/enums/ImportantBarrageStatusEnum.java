package com.jiuyu.replay.words.enums;

import lombok.Getter;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/9/23 17:06
 */
@Getter
public enum ImportantBarrageStatusEnum {

    // 重要弹幕生成状态：0未获取，1获取中，2获取成功，3获取失败
    NOT_OBTAINED(0, "未获取"),
    IN_PROGRESS(1, "获取中"),
    SUCCESS(2, "获取成功"),
    ACQUISITION_FAILED(3, "获取失败");

    private final int code;
    private final String msg;

    ImportantBarrageStatusEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}
