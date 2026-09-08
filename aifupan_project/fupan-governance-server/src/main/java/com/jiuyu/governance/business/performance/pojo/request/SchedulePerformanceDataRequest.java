package com.jiuyu.governance.business.performance.pojo.request;

import com.jiuyu.governance.business.performance.pojo.response.FieldDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 排班业绩保存请求 - 业绩数据
 * <p>
 * 用于客户端提交业绩数据，只包含基础指标，衍生指标由后端自动计算。
 * </p>
 *
 * @author lj
 * @date 2026-04-14
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePerformanceDataRequest {

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
     * 退款数量
     */
    private FieldDetail<Integer> refundQuantity;

    /**
     * 销售单量
     */
    private FieldDetail<Integer> payComboCnt;

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
}
