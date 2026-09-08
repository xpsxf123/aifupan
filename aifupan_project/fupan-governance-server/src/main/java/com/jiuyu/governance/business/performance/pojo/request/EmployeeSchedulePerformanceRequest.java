package com.jiuyu.governance.business.performance.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.framework.shandard.PageRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 员工业绩按班次维度分页请求
 * <p>
 * 使用场景：员工业绩详情页-按班次维度分页列表
 * 以班次为维度查询员工业绩，每条记录是一个直播班次
 * </p>
 *
 * @author lj
 * @date 2026-03-30
 */
@Getter
@Setter
public class EmployeeSchedulePerformanceRequest extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 员工ID
     */
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    /**
     * 开始日期（可选，格式：yyyy-MM-dd）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 结束日期（可选，格式：yyyy-MM-dd）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 直播间名称（模糊搜索）
     */
    private String liveRoomName;

    /**
     * 排序字段
     * 可选：startTime, endTime, viewCount, salesRevenue, refund, netSales, investment, roi
     */
    private String sortField = "startTime";

    /**
     * 排序方式：ASC-升序，DESC-降序
     */
    private String sortOrder = "DESC";

    /**
     * 租户ID（由框架自动注入）
     */
    @JsonIgnore
    private Long tenantId;
}
