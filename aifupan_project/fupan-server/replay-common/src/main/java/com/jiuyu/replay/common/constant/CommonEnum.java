package com.jiuyu.replay.common.constant;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/26 上午10:09
 */
public enum CommonEnum {

    INIT_TRADE_ID(1L, "通用行业的id");

    private final Object code;
    private final String desc;

    CommonEnum(Object code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCodeInt() {
        return (int) code;
    }

    public long getCodeLong() {
        return (long) code;
    }

    public String getCodeString() {
        return (String) code;
    }


}
