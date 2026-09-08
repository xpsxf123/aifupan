package com.jiuyu.governance.plugins.useragent;

import com.jiuyu.framework.shandard.BaseEnum;
import lombok.Getter;

/**
 * 客户端设备类型
 *
 * @author HeHui
 * @date 2026-03-01 20:22
 */
@Getter
public enum ClientDeviceType implements BaseEnum<Integer> {
    OTHER(0, "其他", 1),
    PC(1, "电脑", 1),
    ANDROID(2, "安卓手机", 2),
    IOS(3, "苹果手机", 2),
    IPAD_IOS(4, "Apple平板", 2),
    IPAD_ANDROID(5, "安卓平板", 2),
    ;

    private final Integer value;

    private final String desc;

    private final Integer maxOnline;

    ClientDeviceType(Integer value, String desc, Integer maxOnline) {
        this.value = value;
        this.desc = desc;
        this.maxOnline = maxOnline;
    }
}
