package com.jiuyu.replay.common.utils;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;

import java.util.Date;
import java.util.function.BiFunction;

/**
 * 计算有效期的utils
 */
public class ExpirationUtils {

    /**
     * 获取有效期结束日期
     * @param startDate         开始时间
     * @param expiration        有效期
     * @param expirationUnit    单位
     * @return                  结束时间
     */
    public static Date getExpirationEndDate(Date startDate, int expiration, int expirationUnit) {
        return getDate()[expirationUnit].apply(startDate, expiration);
    }

    /**
     * 获取不同单位时间
     * @return            BiFunction[]
     */
    public static BiFunction<Date, Integer, Date>[] getDate(){

        BiFunction<Date, Integer, Date> hours = (start, expiration) -> {
            DateTime temp = new DateTime(start);
            return temp.offset(DateField.HOUR_OF_DAY, expiration);
        };

        BiFunction<Date, Integer, Date> days = (start, expiration) -> {
            DateTime temp = new DateTime(start);
            return temp.offset(DateField.DAY_OF_MONTH, expiration);
        };

        BiFunction<Date, Integer, Date> months = (start, expiration) -> {
            DateTime temp = new DateTime(start);
            return temp.offset(DateField.MONTH, expiration);
        };

        BiFunction<Date, Integer, Date> quarters = (start, expiration) -> {
            DateTime temp = new DateTime(start);
            return temp.offset(DateField.MONTH, expiration * 3);
        };

        BiFunction<Date, Integer, Date> halfYears = (start, expiration) -> {
            DateTime temp = new DateTime(start);
            return temp.offset(DateField.MONTH, expiration * 6);
        };

        BiFunction<Date, Integer, Date> years = (start, expiration) -> {
            DateTime temp = new DateTime(start);
            return temp.offset(DateField.YEAR, expiration);
        };

        return new BiFunction[]{hours, days, months, quarters, halfYears, years};
    }

    public static String getExpirationUnitName(int num, int unit) {
        return switch (unit) {
            case 0 -> StrUtil.format("{}小时", num);
            case 1 -> StrUtil.format("{}天", num);
            case 2 -> StrUtil.format("{}个月", num);
            case 3 -> StrUtil.format("{}个月", num * 3);
            case 4 -> StrUtil.format("{}个月", num * 6);
            case 5 -> StrUtil.format("{}年", num);
            default -> "未知";
        };
    }


}
