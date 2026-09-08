package com.jiuyu.governance.business.performance.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 业绩时段统计响应
 * <p>
 * 使用场景：业绩汇总详情页-业绩天、周、月的汇总统计
 * </p>
 * <p>
 * 时段维度：今天、昨天、本周、上周、本月、上月
 * 数据维度：直播场次、场观、销售额、退款、净销售额、投放
 * </p>
 * <p>
 * 示例格式：
 * {
 *   "sessionStats": {
 *     "today": { "count": 3, "duration": 180 },
 *     "yesterday": { "count": 6, "duration": 360 },
 *     "thisWeek": { "count": 15, "duration": 900 },
 *     "lastWeek": { "count": 20, "duration": 1200 },
 *     "thisMonth": { "count": 45, "duration": 2700 },
 *     "lastMonth": { "count": 50, "duration": 3000 }
 *   },
 *   "viewCount": {
 *     "today": 95231,
 *     "yesterday": 6398,
 *     ...
 *   },
 *   ...
 * }
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformancePeriodStatsResponse {

    /**
     * 直播场次统计
     * 包含今天、昨天、本周、上周、本月、上月的场次数量和直播时长
     */
    private PeriodData<SessionStats> sessionStats;

    /**
     * 场观统计
     */
    private PeriodData<Integer> viewCount;

    /**
     * 销售额统计（单位：元）
     */
    private PeriodData<BigDecimal> salesRevenue;

    /**
     * 退款统计（单位：元）
     */
    private PeriodData<BigDecimal> refund;

    /**
     * 净销售额统计（单位：元）
     */
    private PeriodData<BigDecimal> netSales;

    /**
     * 投放统计（单位：元）
     */
    private PeriodData<BigDecimal> investment;


}
