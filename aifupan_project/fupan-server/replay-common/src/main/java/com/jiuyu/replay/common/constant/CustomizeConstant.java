package com.jiuyu.replay.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ：lujie
 * &#064;description：自定义常量
 * @date ：2025/6/30 下午6:09
 */
@Getter
@AllArgsConstructor
public enum CustomizeConstant {

    SELECT_ONE_LAST_SQL("limit 1"),
    NO_SPECIFIC_TEXT("无具体内容文本");

    private final String value;

}
