package com.jiuyu.replay.common.constant.packageunit;

/**
 * 版本有效期枚举
 *
 * @author RayChou
 * @date 2025/6/6 10:11
 */
public enum ValidityUnitEnum {

    HOUR(0, 1, "小时"), DAY(1, 24, "天"), MONTH(2, 30 * 24, "月"), QUARTER(3, 90 * 24, "季度"), HALF_YEAR(4, 180 * 24, "半年"), YEAR(5, 365 * 24, "年");
    // 单位代码
    private int code;
    // 转换为小时的系数
    private int hoursMultiplier;
    private String name;

    ValidityUnitEnum(int code, int hoursMultiplier, String name) {
        this.code = code;
        this.hoursMultiplier = hoursMultiplier;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public int getHoursMultiplier() {
        return hoursMultiplier;
    }

    public String getName() {
        return name;
    }


    /**
     * 通过code获取name
     *
     * @param code 编码
     * @return 名称
     */
    public static String getNameByCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (ValidityUnitEnum item : ValidityUnitEnum.values()) {
            if (item.getCode() == code) {
                return item.getName();
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
    public static ValidityUnitEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (ValidityUnitEnum item : ValidityUnitEnum.values()) {
            if (item.getCode() == code) {
                return item;
            }
        }

        return null;
    }
}