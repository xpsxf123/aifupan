package com.jiuyu.governance.business.room.pojo.constants;

import com.jiuyu.framework.shandard.BaseEnum;
import lombok.Getter;

/**
 * 直播平台类型
 *
 * @author HeHui
 * @date 2026-03-24 14:54
 */
@Getter
public enum LivePlatformType implements BaseEnum<Integer> {
    DOU_YIN(0, "抖音"),
    KUAI_SHOU(1, "快手"),
    SHI_PING_HAO(2, "视频号"),
    ;


    private final Integer value;

    private final String desc;

    LivePlatformType(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }


    /**
     * 根据value获取枚举
     *
     * @param value code值
     *
     * @return {@link LivePlatformType }
     */
    public static LivePlatformType getByValue(Integer value) {
        for (LivePlatformType item : values()) {
            if (item.getValue().equals(value)) {
                return item;
            }
        }
        return null;
    }
}
