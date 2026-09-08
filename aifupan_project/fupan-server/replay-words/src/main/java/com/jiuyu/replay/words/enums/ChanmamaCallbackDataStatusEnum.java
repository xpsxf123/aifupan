package com.jiuyu.replay.words.enums;

import lombok.Getter;

@Getter
public enum ChanmamaCallbackDataStatusEnum {

    NORMAL(0, "正常数据"),
    ABNORMAL_REPAIR(1, "异常数据但已校验完成"),
    ABNORMAL(2, "异常数据，未校验"),
    NOT_CHECK(3, "未校验");

    private final Integer status;
    private final String remarks;

    ChanmamaCallbackDataStatusEnum(Integer status, String remarks) {
        this.status = status;
        this.remarks = remarks;
    }
}
