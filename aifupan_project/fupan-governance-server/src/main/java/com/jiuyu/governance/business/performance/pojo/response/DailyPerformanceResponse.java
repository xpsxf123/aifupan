package com.jiuyu.governance.business.performance.pojo.response;

import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceMetrics;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * 每日业绩数据响应
 * <p>
 * 使用场景：数据趋势柱形图、数据详情分页列表
 * 以时间(yyyy-MM-dd)为维度的汇总数据
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
@SuperBuilder
public class DailyPerformanceResponse extends BasePerformanceMetrics {

    /**
     * 统计日期（格式：yyyy-MM-dd）
     */
    private LocalDate date;
}
