package com.jiuyu.replay.generic.enums.words;

import lombok.Getter;

/**
 * AI 监控类型
 */
@Getter
public enum MonitorTypeEnum {

    /**
     * 话术质检
     */
    QUALITY_INSPECTION(0, "话术质检"),
    /**
     * 话术还原度
     */
    FIDELITY_MONITOR(1, "话术还原度"),
    /**
     * 互动巡检
     */
    INTERACTION_PATROL(2, "互动巡检");

    private final Integer code;
    private final String remarks;

    MonitorTypeEnum(Integer code, String remarks) {
        this.code = code;
        this.remarks = remarks;
    }
}
