package com.jiuyu.governance.business.performance.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.governance.business.performance.pojo.bo.DailyPerformanceStatsBO;
import com.jiuyu.governance.business.performance.pojo.bo.DailyPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.EmployeePerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.PeriodPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.MultiPeriodSessionStatsBO;
import com.jiuyu.governance.business.performance.pojo.bo.SchedulePerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.StaffDailyStatsBO;
import com.jiuyu.governance.business.performance.pojo.bo.EmployeeDailyPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.EmployeeSchedulePerformanceBO;
import com.jiuyu.governance.business.performance.pojo.entity.SchedulePerformance;
import com.jiuyu.governance.business.performance.pojo.request.EmployeePerformancePageRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 排班业绩表Mapper
 *
 * @author lj
 * @date 2026-03-24
 */
@Mapper
public interface SchedulePerformanceMapper extends BaseMapper<SchedulePerformance> {

    /**
     * 排班业绩分页查询
     *
     * @param page      分页对象
     * @param tenantId  租户ID
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param anchorName 主播名称（可选）
     * @param liveRoomId 直播间ID（可选）
     * @return 排班业绩分页结果
     */
    IPage<SchedulePerformanceBO> pageQuerySchedulePerformance(
            Page<SchedulePerformanceBO> page,
            @Param("tenantId") Long tenantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("anchorName") String anchorName,
            @Param("liveRoomId") Long liveRoomId
    );

    /**
     * 按天维度统计排班业绩
     *
     * @param page       分页对象
     * @param tenantId   租户ID
     * @param startTime  开始时间（可选）
     * @param endTime    结束时间（可选）
     * @param anchorName 主播名称（可选）
     * @param liveRoomId 直播间ID
     * @param sortField  排序字段
     * @param sortOrder  排序方式
     * @return 按天统计的分页结果
     */
    IPage<DailyPerformanceStatsBO> selectDailyStats(
            Page<DailyPerformanceStatsBO> page,
            @Param("tenantId") Long tenantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("anchorName") String anchorName,
            @Param("liveRoomId") Long liveRoomId,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 查询人员每日统计信息
     *
     * @param tenantId   租户ID
     * @param liveRoomId 直播间ID
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 人员每日统计列表
     */
    List<StaffDailyStatsBO> selectStaffDailyStats(
            @Param("tenantId") Long tenantId,
            @Param("liveRoomId") Long liveRoomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 按直播间和日期批量查询人员（一天内人员+岗位去重）
     *
     * @param tenantId   租户ID
     * @param liveRoomIds 直播间ID列表
     * @param dates      日期列表
     * @return 按直播间+日期+人员+岗位去重的人员列表
     */
    List<StaffDailyStatsBO> selectStaffByRoomAndDates(
            @Param("tenantId") Long tenantId,
            @Param("liveRoomIds") List<Long> liveRoomIds,
            @Param("dates") List<LocalDate> dates
    );

    /**
     * 员工业绩分页查询（按员工分组，聚合直播间ID）
     *
     * @param page          分页对象
     * @param req     参数
     * @param tenantId      租户ID
     * @return 员工业绩分页结果
     */
    IPage<EmployeePerformanceBO> pageQueryEmployeePerformance(Page<EmployeePerformanceBO> page, @Param("req") EmployeePerformancePageRequest req, @Param("tenantId") Long tenantId);

    /**
     * 按员工ID查询业绩统计（聚合所有直播间）
     *
     * @param tenantId     租户ID
     * @param employeeIds  员工ID列表
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @return 聚合统计结果列表
     */
    List<PeriodPerformanceBO> queryPeriodStatsByEmployee(
            @Param("tenantId") Long tenantId,
            @Param("employeeIds") List<Long> employeeIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 按员工ID和直播间ID批量查询业绩统计
     *
     * @param tenantId     租户ID
     * @param employeeIds  员工ID列表
     * @param liveRoomIds  直播间ID列表
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @return 聚合统计结果列表
     */
    List<PeriodPerformanceBO> queryPeriodStatsByEmployeeAndRoom(
            @Param("tenantId") Long tenantId,
            @Param("employeeIds") List<Long> employeeIds,
            @Param("liveRoomIds") List<Long> liveRoomIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 查询员工业绩汇总（总场观、总销售额）
     *
     * @param tenantId   租户ID
     * @param employeeId 员工ID
     * @param liveRoomId 直播间ID
     * @return 汇总统计结果
     */
    PeriodPerformanceBO queryEmployeeSummary(
            @Param("tenantId") Long tenantId,
            @Param("employeeId") Long employeeId,
            @Param("liveRoomId") Long liveRoomId
    );

    /**
     * 查询员工业绩汇总（按员工聚合所有直播间）
     *
     * @param tenantId   租户ID
     * @param employeeId 员工ID
     * @return 汇总统计结果
     */
    PeriodPerformanceBO queryEmployeeSummaryByEmployee(
            @Param("tenantId") Long tenantId,
            @Param("employeeId") Long employeeId
    );

    /**
     * 查询员工业绩趋势（按天聚合）
     *
     * @param tenantId   租户ID
     * @param employeeId 员工ID
     * @param liveRoomId 直播间ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 日维度业绩趋势列表
     */
    List<DailyPerformanceBO> queryEmployeeTrend(
            @Param("tenantId") Long tenantId,
            @Param("employeeId") Long employeeId,
            @Param("liveRoomId") Long liveRoomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 查询员工业绩趋势（按员工聚合所有直播间）
     *
     * @param tenantId   租户ID
     * @param employeeId 员工ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 日维度业绩趋势列表
     */
    List<DailyPerformanceBO> queryEmployeeTrendByEmployee(
            @Param("tenantId") Long tenantId,
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 员工业绩按天维度分页查询
     *
     * @param page         分页对象
     * @param tenantId     租户ID
     * @param employeeId   员工ID
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @param liveRoomName 直播间名称（可选，模糊搜索）
     * @param sortField    排序字段
     * @param sortOrder    排序方式
     * @return 按天维度统计的分页结果
     */
    IPage<EmployeeDailyPerformanceBO> pageEmployeeDailyPerformance(
            Page<EmployeeDailyPerformanceBO> page,
            @Param("tenantId") Long tenantId,
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("liveRoomName") String liveRoomName,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 员工业绩按班次维度分页查询
     *
     * @param page         分页对象
     * @param tenantId     租户ID
     * @param employeeId   员工ID
     * @param startDate    开始日期（可选）
     * @param endDate      结束日期（可选）
     * @param liveRoomName 直播间名称（可选，模糊搜索）
     * @param sortField    排序字段
     * @param sortOrder    排序方式
     * @return 按班次维度统计的分页结果
     */
    IPage<EmployeeSchedulePerformanceBO> pageEmployeeSchedulePerformance(
            Page<EmployeeSchedulePerformanceBO> page,
            @Param("tenantId") Long tenantId,
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("liveRoomName") String liveRoomName,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 按直播间ID列表批量查询多时段场次统计
     */
    List<MultiPeriodSessionStatsBO> queryPeriodSessionStatsByLiveRoomIds(
            @Param("tenantId") Long tenantId,
            @Param("liveRoomIds") List<Long> liveRoomIds,
            @Param("today") LocalDate today,
            @Param("yesterday") LocalDate yesterday,
            @Param("thisWeekStart") LocalDate thisWeekStart,
            @Param("lastWeekStart") LocalDate lastWeekStart,
            @Param("lastWeekEnd") LocalDate lastWeekEnd,
            @Param("thisMonthStart") LocalDate thisMonthStart,
            @Param("lastMonthStart") LocalDate lastMonthStart,
            @Param("lastMonthEnd") LocalDate lastMonthEnd
    );

    /**
     * 查询多时段场次统计（一次查询返回6个时段的场次数量和直播时长）
     */
    MultiPeriodSessionStatsBO queryPeriodSessionStats(
            @Param("tenantId") Long tenantId,
            @Param("idField") String idField,
            @Param("sourceId") Long sourceId,
            @Param("today") LocalDate today,
            @Param("yesterday") LocalDate yesterday,
            @Param("thisWeekStart") LocalDate thisWeekStart,
            @Param("lastWeekStart") LocalDate lastWeekStart,
            @Param("lastWeekEnd") LocalDate lastWeekEnd,
            @Param("thisMonthStart") LocalDate thisMonthStart,
            @Param("lastMonthStart") LocalDate lastMonthStart,
            @Param("lastMonthEnd") LocalDate lastMonthEnd
    );

}
