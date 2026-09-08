package com.jiuyu.replay.generic.enums.words;

import lombok.Getter;

/**
 * 星标来源类型枚举
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-07
 */
@Getter
public enum SourceStarTypeEnum {

    VIDEO(0, "视频"),
    FILE(1, "文件"),
    CONTRAST(2, "对比");

    private final Integer code;
    private final String remarks;

    SourceStarTypeEnum(Integer code, String remarks) {
        this.code = code;
        this.remarks = remarks;
    }
}
