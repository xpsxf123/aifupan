package com.jiuyu.governance.business.performance.pojo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 员工业绩趋势请求
 *
 * @author lj
 * @date 2026-03-30
 */
@Getter
@Setter
public class EmployeeTrendRequest {

    /**
     * 员工ID
     */
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDate startDate;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDate endDate;
}
