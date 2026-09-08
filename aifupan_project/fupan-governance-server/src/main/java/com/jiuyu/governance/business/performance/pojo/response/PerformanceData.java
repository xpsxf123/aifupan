package com.jiuyu.governance.business.performance.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 业绩数据（含图片URL和修改状态）
 *
 * @author lj
 * @date 2026-03-28
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceData {

    /**
     * 场观
     */
    private FieldDetail<Integer> viewCount;

    /**
     * 销售额
     */
    private FieldDetail<BigDecimal> salesRevenue;

    /**
     * 退款
     */
    private FieldDetail<BigDecimal> refund;

    /**
     * 投放
     */
    private FieldDetail<BigDecimal> investment;

    /**
     * 退款单量
     */
    private FieldDetail<Integer> refundQuantity;

    /**
     * 成交单量
     */
    private FieldDetail<Integer> payComboCnt;

    /**
     * 净销售额
     */
    private FieldDetail<BigDecimal> netSales;

    /**
     * ROI
     */
    private FieldDetail<BigDecimal> roi;

    /**
     * 曝光次数
     */
    private FieldDetail<Integer> exposureCount;

    /**
     * 涨粉人数
     */
    private FieldDetail<Integer> followCount;

    /**
     * 点击-成交率
     */
    private FieldDetail<BigDecimal> clickPaymentRate;

    /**
     * 互动率
     */
    private FieldDetail<BigDecimal> interactionRate;

    /**
     * 最高在线
     */
    private FieldDetail<Integer> maxOnline;

    /**
     * 退款率
     */
    private FieldDetail<BigDecimal> refundRate;

    /**
     * 千次成交
     */
    private FieldDetail<BigDecimal> thousandSales;

    /**
     * 带货转化率
     */
    private FieldDetail<BigDecimal> conversionRate;

    /**
     * UV价值
     */
    private FieldDetail<BigDecimal> uvValue;

    /**
     * 涨粉率
     */
    private FieldDetail<BigDecimal> followRate;
}
