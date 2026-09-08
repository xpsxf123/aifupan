package com.jiuyu.governance.business.performance.pojo.bo;

import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceMetrics;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 业绩聚合统计BO
 * 用于接收SQL层面的聚合查询结果
 *
 * @author lj
 * @date 2026-03-27
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceAggregationBO extends BasePerformanceMetrics {

    /**
     * 组织ID（公司ID/部门ID/团队ID）
     */
    private Long id;
}
