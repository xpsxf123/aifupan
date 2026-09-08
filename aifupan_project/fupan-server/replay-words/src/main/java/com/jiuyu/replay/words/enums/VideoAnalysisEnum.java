package com.jiuyu.replay.words.enums;

public enum VideoAnalysisEnum {

    ANALYSIS_NO(0, "未分析"),
    ANALYSIS_ING(1, "分析中"),
    ANALYSIS_ED(2, "已分析"),
    ANALYSIS_EER(3, "分析失败");

    private final Integer status;
    private final String msg;

    VideoAnalysisEnum(Integer status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    public Integer getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }

}
