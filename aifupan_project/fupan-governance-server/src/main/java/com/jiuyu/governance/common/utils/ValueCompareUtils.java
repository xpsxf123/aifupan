package com.jiuyu.governance.common.utils;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;

import java.math.BigDecimal;

/**
 * 值比较工具类
 *
 * @author lj
 * @date 2026-03-28
 */
public class ValueCompareUtils {

    private ValueCompareUtils() {
        // 工具类禁止实例化
    }

    /**
     * 判断两个值是否相等
     * <p>
     * 支持BigDecimal的特殊比较（使用compareTo而非equals）
     * </p>
     *
     * @param value1 第一个值
     * @param value2 第二个值
     * @return true-相等，false-不相等
     */
    public static boolean isValueEqual(Object value1, Object value2) {
        if (value1 == null && value2 == null) {
            return true;
        }
        if (value1 == null || value2 == null) {
            return false;
        }
        if (value1 instanceof Number && value2 instanceof Number) {
            return NumberUtil.equals((Number) value1, (Number) value2);
        }
        return ObjUtil.equal(value1, value2);
    }
}
