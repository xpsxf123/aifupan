package com.jiuyu.replay.video.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订阅分组类型枚举
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 订阅分组类型枚举，区分达人订阅和爆款订阅分组
 */
@Getter
@AllArgsConstructor
public enum SubscriptionGroupTypeEnum {

    /**
     * 达人订阅分组
     */
    INFLUENCER(1, "达人订阅"),

    /**
     * 爆款订阅分组
     */
    HOT(2, "爆款订阅");

    /**
     * 类型值
     */
    private final Integer value;

    /**
     * 类型描述
     */
    private final String description;

    /**
     * 根据值获取枚举
     *
     * @param value 值
     * @return 枚举
     */
    public static SubscriptionGroupTypeEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (SubscriptionGroupTypeEnum type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 是否为达人订阅分组
     *
     * @param value 值
     * @return 是否为达人订阅分组
     */
    public static boolean isInfluencer(Integer value) {
        return INFLUENCER.getValue().equals(value);
    }

    /**
     * 是否为爆款订阅分组
     *
     * @param value 值
     * @return 是否为爆款订阅分组
     */
    public static boolean isHot(Integer value) {
        return HOT.getValue().equals(value);
    }
}
