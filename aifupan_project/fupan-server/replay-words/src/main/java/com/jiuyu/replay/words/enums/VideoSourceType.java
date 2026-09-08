package com.jiuyu.replay.words.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 视频来源类型
 *
 * @author HeHui
 * @date 2025-06-07 17:41
 */
@Getter
public enum VideoSourceType {
    LOCAL(0, "本地录制"),
    UPLOAD(1, "上传文件"),
    CONTRAST(2, "对比分析")
    ;


    private final int code;

    private final String desc;

    VideoSourceType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }


    public static Optional<VideoSourceType> codeOf(Integer code) {
        return Arrays.stream(values()).filter(c -> Objects.equals(code, c.code)).findFirst();
    }
}
