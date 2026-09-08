package com.jiuyu.replay.common.diff;

import lombok.Getter;

import java.util.List;
import java.util.function.Function;

/**
 * 业务类型
 *
 * @author HeHui
 * @date 2025-06-23 11:48
 */
@Getter
public enum Business {
    USER_DETAILS(1, "USER_DETAILS", List.of("channelId","saleId","tradeId","phones"));


    /**
     * 业务类型
     */
    private final int type;

    /**
     * 业务描述
     */
    private final String desc;

    /**
     * 字段名称
     */
    private final List<String> fieldNames;





    Business(int type, String desc, List<String> fieldNames) {
        this.type = type;
        this.desc = desc;
        this.fieldNames = fieldNames;
    }
}
