package com.jiuyu.replay.common.constant.packageunit;

import lombok.Data;

/**
 * 版本有效期对象
 *
 * @author RayChou
 * @date 2025/6/6 10:11
 */
@Data
public class ValidityPeriod {
    // 有效期数值
    private int validityNum;
    // 有效期单位
    private int validityUnit;
    // 有效单位名称
    private String validityName;

    public ValidityPeriod(int validityNum, int validityUnit) {
        this.validityNum = validityNum;
        this.validityUnit = validityUnit;
    }

    public ValidityPeriod(int validityNum, int validityUnit, String validityName) {
        this.validityNum = validityNum;
        this.validityUnit = validityUnit;
        this.validityName = validityName;
    }

    /**
     * 转换为小时
     */
    public int toHours() {
        ValidityUnitEnum unit = ValidityUnitEnum.getByCode(validityUnit);
        return validityNum * unit.getHoursMultiplier();
    }

    @Override
    public String toString() {
        ValidityUnitEnum unit = ValidityUnitEnum.getByCode(validityUnit);
        return validityNum + unit.getName();
    }
}