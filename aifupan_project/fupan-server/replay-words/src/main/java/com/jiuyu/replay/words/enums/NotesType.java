package com.jiuyu.replay.words.enums;

import lombok.Getter;

import java.util.Optional;

/**
 * 笔记类型
 *
 * @author HeHui
 * @date 2025-07-03 10:42
 */
@Getter
public enum NotesType {
    // 笔记的类型  1: 原文笔记,2：复盘小结，3分段笔记
    ORIGINAL_NOTES(1, "原文笔记"),
    REVIEW_NOTES(2, "复盘小结"),
    SECTION_NOTES(3, "分段笔记");
    private final int code;
    private final String desc;

    NotesType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }


    public static Optional<NotesType> codeOf(Integer code) {
        for (NotesType value : NotesType.values()) {
            if (value.code == code) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

}
