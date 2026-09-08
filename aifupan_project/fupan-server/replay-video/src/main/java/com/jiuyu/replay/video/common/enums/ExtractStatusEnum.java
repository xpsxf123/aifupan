package com.jiuyu.replay.video.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 提取状态枚举
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 文案提取状态枚举
 */
@Getter
@AllArgsConstructor
public enum ExtractStatusEnum {

    /**
     * 未提取
     */
    NON((byte) 0, "未提取"),

    /**
     * 待处理
     */
    PENDING((byte) 1, "待处理"),

    /**
     * 处理中
     */
    PROCESSING((byte) 2, "处理中"),

    /**
     * 已完成
     */
    COMPLETED((byte) 3, "已完成"),

    /**
     * 失败
     */
    FAILED((byte) 4, "失败"),

    /**
     * 视频提取完成
     */
    VIDEO_COMPLETED((byte) 1, "视频提取完成");

    private final Byte code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static ExtractStatusEnum getByCode(Byte code) {
        if (code == null) {
            return null;
        }
        for (ExtractStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }

    /**
     * 验证code是否有效
     */
    public static boolean isValid(Byte code) {
        return getByCode(code) != null;
    }
}
