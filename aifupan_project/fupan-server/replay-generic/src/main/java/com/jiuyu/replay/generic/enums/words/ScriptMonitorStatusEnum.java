package com.jiuyu.replay.generic.enums.words;

import lombok.Getter;

/**
 * AI 监控报告状态
 */
@Getter
public enum ScriptMonitorStatusEnum {

    /**
     * 未生成，可手动触发
     */
    NOT_GENERATED(0, "未生成"),
    /**
     * 生成中，禁止重复触发
     */
    GENERATING(1, "生成中"),
    /**
     * 已生成，可查看、确认已读
     */
    GENERATED(2, "已生成"),
    /**
     * 生成失败，可重试
     */
    GENERATE_FAILED(3, "生成失败"),
    /**
     * 不可生成，展示原因
     */
    NOT_APPLICABLE(4, "不可生成");

    private final Integer code;
    private final String remarks;

    ScriptMonitorStatusEnum(Integer code, String remarks) {
        this.code = code;
        this.remarks = remarks;
    }
}
