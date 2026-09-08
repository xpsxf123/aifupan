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
 * 员工业绩按天维度分页请求
 * <p>
 * 使用场景：员工业绩详情页-按天维度分页列表
 * 以天为维度统计员工业绩，每天一条记录，包含当天参与的直播间列表
 * </p>
 *
 * @author lj
 * @date 2026-03-30
 */
@Getter
@Setter
public class EmployeeDailyPerformanceRequest extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 员工ID
     */
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    /**
     * 直播间名称（模糊搜索，筛选当天有该直播间的记录）
     */
    private String liveRoomName;

    /**
     * 开始时间（可选，格式：yyyy-MM-dd）
     * 查询 stats_date >= startDate 的数据
     * 不传时查询所有数据
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 结束时间（可选，格式：yyyy-MM-dd）
     * 查询 stats_date <= endDate 的数据
     * 不传时查询所有数据
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 排序字段
     * 可选：statsDate, scheduleCount, liveDurationMinutes, viewCount, salesRevenue, refund, netSales, investment, roi
     */
    private String sortField = "statsDate";

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
