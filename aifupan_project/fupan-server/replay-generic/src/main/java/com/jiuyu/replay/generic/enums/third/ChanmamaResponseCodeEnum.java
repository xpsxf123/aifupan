package com.jiuyu.replay.generic.enums.third;

import lombok.Getter;

@Getter
public enum ChanmamaResponseCodeEnum {

    CODE_501(501, "未登录，请重新登录!"),
    CODE_502(502, "系统提示:登录超时!"),
    CODE_504(504, "(token已经被刷新)系统提示:非法登录!"),
    CODE_505(505, "系统提示:非法登录!");

    private final Integer code;
    private final String remarks;

    ChanmamaResponseCodeEnum(Integer code, String remarks) {
        this.code = code;
        this.remarks = remarks;
    }
}
