package com.jiuyu.governance.business.performance.utils;

import cn.hutool.core.util.NumberUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * 指标计算工具类
 * 提供常见业务指标的计算方法，直接返回 BigDecimal，参数为 null 或除零时返回 null。
 */
public class MetricsUtil {

    private static final int DEFAULT_SCALE = 4;              // 默认小数保留位数
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    /**
     * 计算最大场观
      * @param viewCounts 浏览量列表
     * @return 最大场观
     */
    public static Integer calcMaxViewCount(List<Integer> viewCounts){
        if (viewCounts == null || viewCounts.isEmpty()){
            return null;
        }
        return viewCounts.stream().filter(Objects::nonNull).max(Integer::compare).orElse(null);
    }

    /**
     * 计算最大金额
     * @param amounts 金额列表
     * @return 最大金额
     */
    public static BigDecimal calcMaxAmount(List<BigDecimal> amounts){
        if (amounts == null || amounts.isEmpty()){
            return null;
        }
        return amounts.stream().filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(null);
    }

    /**
     * 投资回报率 (ROI)，返回小数形式（例如 2.0 表示 200%）
     * @param revenue 收益金额
     * @param cost    成本金额
     * @return ROI 值，若任一参数为 null 或 cost 为 0 返回 null
     */
    public static BigDecimal roi(BigDecimal revenue, BigDecimal cost) {
        if (revenue == null || cost == null || cost.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return revenue.subtract(cost).divide(cost, DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * 计算投放产出
     * @param investment 投资金额
     * @param roi 投资回报率
     * @return 投放产出，若任一参数为 null 返回 null
     */
    public static BigDecimal calcAdOutput(BigDecimal investment, BigDecimal roi) {
        if (investment == null || roi == null) {
            return null;
        }
        return investment.multiply(roi).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 将分转换为元
     * @param cents 分
     * @return 元
     */
    public static BigDecimal centsToYuan(BigDecimal cents) {
        if (cents == null) {
            return null;
        }
        return divide(cents, BigDecimal.valueOf(100), 2);
    }

    /**
     * 将分转换为元
     * @param yuan 元
     * @return 分
     */
    public static BigDecimal centsToYuan(Integer yuan) {
        if (yuan == null) {
            return null;
        }
        return centsToYuan(BigDecimal.valueOf(yuan));
    }

    /**
     * 将元转换为分
     * @param yuan 元
     * @return 分
     */
    public static BigDecimal yuanToCents(BigDecimal yuan) {
        if (yuan == null) {
            return null;
        }
        return yuan.multiply(BigDecimal.valueOf(100));
    }

    /**
     * 计算两个整数的差
     * @param maxVal 最大值
     * @param minVal 最小值
     * @return 两个整数的差，若任一参数为 null 返回 null
     */
    public static Integer diffInt(Integer maxVal, Integer minVal) {
        if (maxVal == null || minVal == null) {
            return null;
        }
        return maxVal - minVal;
    }

    /**
     * 计算两个整数的差
     * @param maxVal 最大值
     * @param minVal 最小值
     * @return 两个整数的差，若任一参数为 null 返回 null
     */
    public static BigDecimal diffInt(BigDecimal maxVal, BigDecimal minVal) {
        if (maxVal == null || minVal == null) {
            return null;
        }
        return maxVal.subtract(minVal);
    }

    /**
     * 退款率（按订单数），返回小数形式
     * @param refundOrders 退款订单数
     * @param totalOrders  总订单数
     * @return 退款率，若任一参数为 null 或 totalOrders 为 0 返回 null
     */
    public static BigDecimal refundRateByOrder(BigDecimal refundOrders, BigDecimal totalOrders) {
        return divide(refundOrders, totalOrders);
    }

    /**
     * 退款率（按金额），返回小数形式
     * @param refundAmount 退款金额
     * @param paidAmount   支付总金额
     * @return 退款率，若任一参数为 null 或 paidAmount 为 0 返回 null
     */
    public static BigDecimal refundRateByAmount(BigDecimal refundAmount, BigDecimal paidAmount) {
        return divide(refundAmount, paidAmount);
    }

    /**
     * 支付转化率（从访问到支付），返回小数形式
     * @param payingUsers 支付用户数
     * @param visitors    访问用户数
     * @return 支付转化率，若任一参数为 null 或 visitors 为 0 返回 null
     */
    public static BigDecimal conversionRate(BigDecimal payingUsers, BigDecimal visitors) {
        return divide(payingUsers, visitors);
    }

    /**
     * 加购率，返回小数形式
     * @param cartUsers 添加购物车用户数
     * @param visitors  访问用户数
     * @return 加购率，若任一参数为 null 或 visitors 为 0 返回 null
     */
    public static BigDecimal addToCartRate(BigDecimal cartUsers, BigDecimal visitors) {
        return divide(cartUsers, visitors);
    }

    /**
     * 点击率 (CTR)，返回小数形式
     * @param clicks     点击次数
     * @param impressions 曝光次数
     * @return 点击率，若任一参数为 null 或 impressions 为 0 返回 null
     */
    public static BigDecimal clickThroughRate(BigDecimal clicks, BigDecimal impressions) {
        return divide(clicks, impressions);
    }

    /**
     * 客单价
     * @param totalRevenue 总成交金额
     * @param payingUsers  支付用户数
     * @return 客单价，若任一参数为 null 或 payingUsers 为 0 返回 null
     */
    public static BigDecimal averageOrderValue(BigDecimal totalRevenue, BigDecimal payingUsers) {
        return divide(totalRevenue, payingUsers);
    }

    /**
     * 除法运算，返回小数形式，返回结果保留默认小数位数
     * @param numerator   分子
     * @param denominator 分母
     * @return 除法结果，若任一参数为 null 或 denominator 为 0 返回 null
     */
    public static BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        return divide(numerator, denominator, DEFAULT_SCALE);
    }

    /**
     * 除法运算，返回小数形式，返回结果保留默认小数位数
     * @param numerator   分子
     * @param denominator 分母
     * @return 除法结果，若任一参数为 null 或 denominator 为 0 返回 null
     */
    public static BigDecimal divide(Number numerator, Number denominator) {
        if (numerator == null || denominator == null || denominator.doubleValue() == 0) {
            return null;
        }
        return divide(BigDecimal.valueOf(numerator.doubleValue()), BigDecimal.valueOf(denominator.doubleValue()), DEFAULT_SCALE);
    }

    /**
     * 除法运算，返回小数形式
     * @param numerator   分子
     * @param denominator 分母
     * @param scale       保留小数位数
     * @return 除法结果，若任一参数为 null 或 denominator 为 0 返回 null
     */
    public static BigDecimal divide(BigDecimal numerator, BigDecimal denominator, int scale) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, scale, DEFAULT_ROUNDING);
    }

    /**
     * 乘法运算，返回小数形式，返回结果保留默认小数位数
     * @param multiplicand 被乘数
     * @param multiplier   乘数
     * @return 乘法结果，若任一参数为 null 返回 null
     */
    public static BigDecimal multiply(BigDecimal multiplicand, BigDecimal multiplier) {
        return multiply(multiplicand, multiplier, DEFAULT_SCALE);
    }

    /**
     * 乘法运算，返回小数形式
     * @param multiplicand 被乘数
     * @param multiplier   乘数
     * @param scale        保留小数位数
     * @return 乘法结果，若任一参数为 null 返回 null
     */
    public static BigDecimal multiply(BigDecimal multiplicand, BigDecimal multiplier, int scale) {
        if (multiplicand == null || multiplier == null) {
            return null;
        }
        return multiplicand.multiply(multiplier).setScale(scale, DEFAULT_ROUNDING);
    }

    /**
     * 减法运算，返回小数形式
     * @param minuend 被减数
     * @param subtrahend 减数
     * @return 减法结果，若任一参数为 null 返回 null
     */
    public static BigDecimal subtract(BigDecimal minuend, BigDecimal subtrahend) {
        return diffInt(minuend, subtrahend);
    }

    /**
     * 将小数转换为百分比字符串（带 % 号）
     * @param decimal 小数（如 0.5）
     * @param scale   保留小数位数（四舍五入）
     * @return 百分比字符串，如 "50.00%"，若 decimal 为 null 返回 null
     */
    public static String toPercentString(BigDecimal decimal, int scale) {
        if (decimal == null) {
            return null;
        }
        BigDecimal percent = decimal.multiply(BigDecimal.valueOf(100))
                .setScale(scale, DEFAULT_ROUNDING);
        return percent + "%";
    }

    /**
     * 将小数转换为百分比数值（乘以 100 后的 BigDecimal）
     * @param decimal 小数（如 0.5）
     * @param scale   保留小数位数（四舍五入）
     * @return 百分比数值（如 50.00），若 decimal 为 null 返回 null
     */
    public static BigDecimal toPercentValue(BigDecimal decimal, int scale) {
        if (decimal == null) {
            return null;
        }
        return decimal.multiply(BigDecimal.valueOf(100))
                .setScale(scale, DEFAULT_ROUNDING);
    }
}