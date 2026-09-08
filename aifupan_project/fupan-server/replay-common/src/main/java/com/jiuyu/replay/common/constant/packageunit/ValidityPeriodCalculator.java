package com.jiuyu.replay.common.constant.packageunit;

import java.util.List;

/**
 * 版本有效期计算工具
 *
 * @author RayChou
 * @date 2025/6/6 10:11
 */
public class ValidityPeriodCalculator {

    /**
     * 计算多个有效期的总和，并自动选择合适的单位
     *
     * @param periods 有效期列表
     * @return 总有效期对象
     */
    public static ValidityPeriod addWithOptimalUnit(List<ValidityPeriod> periods) {
        if (periods == null || periods.isEmpty()) {
            return new ValidityPeriod(0, ValidityUnitEnum.HOUR.getCode(), ValidityUnitEnum.HOUR.getName());
        }

        if (periods.size() == 1) {
            ValidityPeriod validityPeriod = periods.get(0);
            return new ValidityPeriod(validityPeriod.getValidityNum(), validityPeriod.getValidityUnit(), ValidityUnitEnum.getNameByCode(validityPeriod.getValidityUnit()));
        }

        // 将所有有效期转换为小时
        int totalHours = 0;
        for (ValidityPeriod period : periods) {
            totalHours += period.toHours();
        }

        // 选择最合适的单位
        ValidityUnitEnum optimalUnit;
        int resultValue;

        if (totalHours >= ValidityUnitEnum.YEAR.getHoursMultiplier() && totalHours % ValidityUnitEnum.YEAR.getHoursMultiplier() == 0) {
            optimalUnit = ValidityUnitEnum.YEAR;
        } else if (totalHours >= ValidityUnitEnum.HALF_YEAR.getHoursMultiplier() && totalHours % ValidityUnitEnum.HALF_YEAR.getHoursMultiplier() == 0) {
            optimalUnit = ValidityUnitEnum.HALF_YEAR;
        } else if (totalHours >= ValidityUnitEnum.QUARTER.getHoursMultiplier() && totalHours % ValidityUnitEnum.QUARTER.getHoursMultiplier() == 0) {
            optimalUnit = ValidityUnitEnum.QUARTER;
        } else if (totalHours >= ValidityUnitEnum.MONTH.getHoursMultiplier() && totalHours % ValidityUnitEnum.MONTH.getHoursMultiplier() == 0) {
            optimalUnit = ValidityUnitEnum.MONTH;
        } else if (totalHours >= ValidityUnitEnum.DAY.getHoursMultiplier() && totalHours % ValidityUnitEnum.DAY.getHoursMultiplier() == 0) {
            optimalUnit = ValidityUnitEnum.DAY;
        } else {
            optimalUnit = ValidityUnitEnum.HOUR;
        }

        resultValue = totalHours / optimalUnit.getHoursMultiplier();

        // 创建新的有效期对象
        return new ValidityPeriod(resultValue, optimalUnit.getCode(), optimalUnit.getName());
    }
}