package com.jiuyu.governance.business.performance.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 组织业绩聚合BO（分公司/部门/小组通用）
 * 用于 LEFT JOIN session_performance + GROUP BY 的查询结果
 *
 * @author lj
 * @date 2026-06-26
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OrgPerformanceBO extends PerformanceAggregationBO {

    /** 组织名称 */
    private String name;
}
