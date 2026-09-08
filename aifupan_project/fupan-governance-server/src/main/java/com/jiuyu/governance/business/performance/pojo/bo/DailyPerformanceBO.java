package com.jiuyu.governance.business.performance.pojo.bo;

import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceMetrics;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * 每日业绩聚合统计BO
 * 用于接收SQL按天（stats_date）聚合查询结果
 *
 * @author lj
 * @date 2026-03-27
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DailyPerformanceBO extends BasePerformanceMetrics {

    /**
     * 统计日期
     */
    private LocalDate statsDate;
}
