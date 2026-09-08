package com.jiuyu.governance.business.performance.pojo.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 视频处理状态枚举
 *
 * @author lj
 * @date 2026-03-19
 */
@Getter
@AllArgsConstructor
public enum ProcessStatus {

    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    SUCCESS(2, "处理成功"),
    FAILED(3, "处理失败");

    private final int code;
    private final String desc;

    public static ProcessStatus fromCode(int code) {
        for (ProcessStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
