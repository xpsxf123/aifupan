package com.jiuyu.governance.business.performance.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 员工业绩汇总响应
 *
 * @author lj
 * @date 2026-03-30
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSummaryResponse {

    /**
     * 总场观
     */
    private Integer totalViewCount;

    /**
     * 总销售额
     */
    private BigDecimal totalSalesRevenue;

    /**
     * 整体转化率
     */
    private Double wholeConversionRate;
}
