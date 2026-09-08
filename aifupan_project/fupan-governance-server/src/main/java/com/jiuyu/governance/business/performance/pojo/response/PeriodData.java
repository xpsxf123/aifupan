package com.jiuyu.governance.business.performance.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 时段数据封装
 * <p>
 * 封装今天、昨天、本周、上周、本月、上月六个时段的数据
 * </p>
 *
 * @param <T> 数据类型
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodData<T> {

    /**
     * 今天
     * 时间范围：当天 00:00:00 至当前时间
     */
    private T today;

    /**
     * 昨天
     * 时间范围：昨天 00:00:00 至 23:59:59
     */
    private T yesterday;

    /**
     * 本周
     * 时间范围：本周一 00:00:00 至当前时间（周一作为一周开始）
     */
    private T thisWeek;

    /**
     * 上周
     * 时间范围：上周一 00:00:00 至上周日 23:59:59
     */
    private T lastWeek;

    /**
     * 本月
     * 时间范围：本月1日 00:00:00 至当前时间
     */
    private T thisMonth;

    /**
     * 上月
     * 时间范围：上月1日 00:00:00 至上月最后一天 23:59:59
     */
    private T lastMonth;
}
