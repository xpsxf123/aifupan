package com.jiuyu.replay.common.enums;

/**
 * 缓存降级数据类型枚举
 *
 * @author RayChou
 * @date 2025/7/11 10:21
 */
public enum CacheFallbackDataTypeEnum {

    STRING((byte) 0, "STRING"), LIST((byte) 1, "LIST"), HASH((byte) 2, "HASH"), SET((byte) 3, "SET"), ZSET((byte) 4, "ZSET");

    private byte code;
    private String type;

    CacheFallbackDataTypeEnum(byte code, String type) {
        this.code = code;
        this.type = type;
    }

    public byte getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    /**
     * 通过code获取type
     *
     * @param code 代码
     * @return 名称
     */
    public static String getTypeByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CacheFallbackDataTypeEnum item : CacheFallbackDataTypeEnum.values()) {
            if (item.getCode() == code) {
                return item.getType();
            }
        }

        return null;
    }

    /**
     * 通过code获取枚举
     *
     * @param code 编码
     * @return 枚举对象
     */
    public static CacheFallbackDataTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CacheFallbackDataTypeEnum item : CacheFallbackDataTypeEnum.values()) {
            if (item.getCode() == code) {
                return item;
            }
        }
        return null;
    }
}
