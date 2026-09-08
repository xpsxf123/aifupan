package com.jiuyu.replay.video.common.enums;

import com.jiuyu.framework.util.EmptyUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 平台类型枚举
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 支持的视频平台类型
 */
@Getter
@AllArgsConstructor
public enum PlatformTypeEnum {

    /**
     * 抖音
     */
    DOUYIN((byte) 1, "抖音"),

    /**
     * 快手
     */
    KUAISHOU((byte) 2, "快手"),

    /**
     * 视频号
     */
    WEIXIN_VIDEO((byte) 3, "视频号"),

    /**
     * 本地上传
     */
    LOCAL_UPLOAD((byte) 4, "本地上传");

    private final Byte code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static PlatformTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PlatformTypeEnum typeEnum : values()) {
            if (typeEnum.getCode().equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }

    /**
     * 验证code是否有效
     */
    public static boolean isValid(Integer code) {
        return getByCode(code) != null;
    }

    public static Optional<PlatformTypeEnum> getByCodeOpt(Byte code) {
       return Arrays.stream(values()).filter(v -> Objects.equals(code, v.getCode())).findFirst();
    }

    public static String parseCodeToString(Byte code) {
        if (EmptyUtil.isEmpty(code)) {
            return null;
        }
        Optional<PlatformTypeEnum> optional = getByCodeOpt(code);
        return optional.map(platformTypeEnum -> switch (platformTypeEnum) {
            case DOUYIN:
                yield "1";
            case KUAISHOU:
                yield "2";
            case WEIXIN_VIDEO:
                yield "3";
            case LOCAL_UPLOAD:
                yield "4";
            default:
                yield null;
        }).orElse(null);
    }
}
