package com.jiuyu.governance.business.performance.pojo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 人员业绩时段统计请求
 *
 * @author lj
 * @date 2026-03-30
 */
@Getter
@Setter
public class EmployeePeriodStatsRequest {

    /**
     * 员工ID
     */
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;
}
