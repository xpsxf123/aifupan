package com.jiuyu.governance.business.room.handler;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.jiuyu.governance.business.room.pojo.entity.ScheduleEmployee;
import com.jiuyu.governance.business.room.pojo.entity.WorkSchedule;
import com.jiuyu.governance.business.room.pojo.request.schedule.LiveRoomScheduleAddRequest;
import com.jiuyu.governance.business.room.pojo.request.schedule.ScheduleBatchRequest;
import com.jiuyu.governance.business.room.pojo.request.schedule.ScheduleEmployeeRequest;
import com.jiuyu.governance.business.room.pojo.request.schedule.ScheduleSessionRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量排班数据生成器
 * <p>
 * 根据用户提交的排班请求，批量生成指定日期范围内的班次和人员排班数据。
 * 支持按周期（工作日、周末等）批量生成多天的排班数据。
 * </p>
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Slf4j
public class LiveRoomScheduleBatchGenerator {

    /**
     * 批量生成的实体列表
     */
    @Getter
    public static class GenerateResult {

        /**
         * 班次列表
         */
        private final List<WorkSchedule> schedules = new ArrayList<>();

        /**
         * 排班人员列表
         */
        private final List<ScheduleEmployee> scheduleEmployees = new ArrayList<>();
    }

    /**
     * 生成批量排班数据
     * <p>
     * 核心逻辑：
     * 1. 解析批量配置，计算目标日期列表（支持按星期几筛选）
     * 2. 遍历每个日期和每个时段，生成对应的班次实体
     * 3. 为每个班次关联对应的人员排班信息
     * </p>
     *
     * @param request  新增请求，包含直播间 ID、日期、时段、人员等信息
     * @param tenantId 租户 ID，用于标识数据归属
     * @param userId   操作人 ID，用于记录创建/更新人
     *
     * @return 批量生成的实体列表，包含班次列表和人员排班列表
     */
    public static GenerateResult generate(LiveRoomScheduleAddRequest request, Long tenantId, Long userId) {
        log.debug("[排班批量生成] 开始生成排班数据：liveRoomId={}, workDay={}, tenantId={}",
            request.getLiveRoomId(), request.getWorkDay(), tenantId);

        GenerateResult result = new GenerateResult();
        LocalDate startDate = request.getWorkDay();
        ScheduleBatchRequest batchConfig = request.getBatchConfig();

        // ========== 步骤 1: 计算目标日期列表 ==========
        List<LocalDate> targetDates = new ArrayList<>();
        targetDates.add(startDate);

        // 如果配置了批量排班，则生成后续的日期
        if (batchConfig != null && batchConfig.getEndDate() != null && batchConfig.getCycleDays() != null && !batchConfig.getCycleDays().isEmpty()) {
            log.debug("[排班批量生成] 检测到批量配置：endDate={}, cycleDays={}",
                batchConfig.getEndDate(), batchConfig.getCycleDays());

            LocalDate current = startDate.plusDays(1);
            LocalDate end = batchConfig.getEndDate();
            while (!current.isAfter(end)) {
                // cycleDays 存储的是星期几（1-7，1 表示周一）
                if (batchConfig.getCycleDays().contains(current.getDayOfWeek().getValue())) {
                    targetDates.add(current);
                }
                current = current.plusDays(1);
            }
            log.debug("[排班批量生成] 计算得到目标日期数量：{}", targetDates.size());
        }

        LocalDateTime now = LocalDateTime.now();

        // ========== 步骤 2: 遍历日期和时段，生成实体 ==========
        for (LocalDate date : targetDates) {
            for (ScheduleSessionRequest session : request.getSessions()) {
                // 创建班次实体
                long scheduleId = IdWorker.getId();
                WorkSchedule schedule = new WorkSchedule();
                schedule.setId(scheduleId);
                schedule.setTenantId(tenantId);
                schedule.setLiveRoomId(request.getLiveRoomId());
                schedule.setWorkDay(date);
                schedule.formatStartWork(session.getStartWork());
                schedule.formatEndWork(session.getEndWork());
                schedule.setScheduleDuration(session.getScheduleDuration());
                schedule.setRestDuration(session.getRestDuration());
                schedule.setRemark("");
                schedule.setCreateBy(userId);
                schedule.setUpdateBy(userId);
                schedule.setCreateDate(now);
                schedule.setUpdateDate(now);
                schedule.setIsDeleted(false);

                result.getSchedules().add(schedule);
                log.trace("[排班批量生成] 生成班次：scheduleId={}, date={}, time={}-{}",
                    scheduleId, date, session.getStartWork(), session.getEndWork());

                // 为该班次创建人员排班关联
                if (session.getEmployees() != null) {
                    for (ScheduleEmployeeRequest empReq : session.getEmployees()) {
                        ScheduleEmployee emp = new ScheduleEmployee();
                        emp.setId(IdWorker.getId());
                        emp.setScheduleId(scheduleId);
                        emp.setLiveRoomId(request.getLiveRoomId());
                        emp.setEmployeeId(empReq.getEmployeeId());
                        emp.setPositionId(empReq.getPositionId());
                        emp.setWorkDay(date);
                        emp.setCreateBy(userId);
                        emp.setUpdateBy(userId);
                        emp.setCreateDate(now);
                        emp.setUpdateDate(now);
                        emp.setIsDeleted(false);
                        result.getScheduleEmployees().add(emp);
                        log.trace("[排班批量生成] 生成人员排班：employeeId={}, positionId={}, scheduleId={}",
                            empReq.getEmployeeId(), empReq.getPositionId(), scheduleId);
                    }
                }
            }
        }

        log.info("[排班批量生成] 生成完成：日期数={}, 班次总数={}, 人员排班总数={}",
            targetDates.size(), result.getSchedules().size(), result.getScheduleEmployees().size());

        return result;
    }


}
