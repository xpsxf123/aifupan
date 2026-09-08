package com.jiuyu.governance.business.room.pojo.bo;

import com.jiuyu.governance.business.room.pojo.entity.ScheduleEmployee;
import com.jiuyu.governance.business.room.pojo.entity.WorkSchedule;
import com.jiuyu.governance.business.room.handler.LiveRoomScheduleBatchGenerator;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 排班冲突校验结果
 * <p>
 * 用于返回排班冲突校验的结果，包含：
 * 1. 成功的排班列表（无冲突）
 * 2. 冲突的排班列表（存在时间冲突）
 * </p>
 *
 * @author HeHui
 * @date 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleConflictResult {

    /**
     * 成功的排班 ID 列表（无冲突）
     */
    @Builder.Default
    private List<Long> successScheduleIds = new ArrayList<>();

    /**
     * 成功的人员排班 ID 列表（无冲突）
     */
    @Builder.Default
    private List<Long> successEmployeeIds = new ArrayList<>();

    /**
     * 冲突的排班信息列表
     */
    @Builder.Default
    private List<ScheduleConflictInfo> conflictSchedules = new ArrayList<>();

    /**
     * 是否存在冲突
     *
     * @return true-存在冲突，false-无冲突
     */
    public boolean hasConflict() {
        return !conflictSchedules.isEmpty();
    }

    /**
     * 从批量生成结果中过滤出成功的数据
     * <p>
     * 过滤规则：
     * 1. 只保留 successScheduleIds 中的排班
     * 2. 只保留 successEmployeeIds 中的人员
     * 3. 如果某个排班下没有任何成功的人员，则该排班也被视为不成功（从结果中移除）
     * </p>
     *
     * @param generateResult 批量生成的原始数据
     *
     * @return 过滤后的 GenerateResult，只包含成功的排班和人员
     */
    public LiveRoomScheduleBatchGenerator.GenerateResult filterSuccessData(LiveRoomScheduleBatchGenerator.GenerateResult generateResult) {
        LiveRoomScheduleBatchGenerator.GenerateResult filteredResult = new LiveRoomScheduleBatchGenerator.GenerateResult();

        if (generateResult == null) {
            return filteredResult;
        }

        List<WorkSchedule> allSchedules = generateResult.getSchedules();
        List<ScheduleEmployee> allEmployees = generateResult.getScheduleEmployees();

        // 将成功 ID 转为 Set，提高查找效率
        Set<Long> successScheduleIdSet = Set.copyOf(successScheduleIds);
        Set<Long> successEmployeeIdSet = Set.copyOf(successEmployeeIds);

        // ========== 步骤 1: 过滤出成功的排班 ==========
        List<WorkSchedule> validSchedules = allSchedules.stream()
            .filter(schedule -> successScheduleIdSet.contains(schedule.getId()))
            .toList();

        // ========== 步骤 2: 过滤出成功的人员 ==========
        List<ScheduleEmployee> validEmployees = allEmployees.stream()
            .filter(emp -> successEmployeeIdSet.contains(emp.getId()))
            .toList();

        // ========== 步骤 3: 构建排班ID到人员的映射 ==========
        Map<Long, List<ScheduleEmployee>> scheduleToEmployeesMap = validEmployees.stream()
            .collect(Collectors.groupingBy(ScheduleEmployee::getScheduleId));

        // ========== 步骤 4: 二次过滤排班：排除那些没有成功人员的排班 ==========
        List<WorkSchedule> finalSchedules = validSchedules.stream()
            .filter(schedule -> {
                List<ScheduleEmployee> employees = scheduleToEmployeesMap.get(schedule.getId());
                // 如果该排班下有至少一个成功的人员，则保留
                return employees != null && !employees.isEmpty();
            })
            .toList();

        // 获取最终保留的排班 ID 集合
        Set<Long> finalScheduleIdSet = finalSchedules.stream()
            .map(WorkSchedule::getId)
            .collect(Collectors.toSet());

        // ========== 步骤 5: 再次过滤人员：只保留属于最终排班的人员 ==========
        List<ScheduleEmployee> finalEmployees = validEmployees.stream()
            .filter(emp -> finalScheduleIdSet.contains(emp.getScheduleId()))
            .toList();

        filteredResult.getSchedules().addAll(finalSchedules);
        filteredResult.getScheduleEmployees().addAll(finalEmployees);

        return filteredResult;
    }

    /**
     * 排班冲突信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleConflictInfo {

        /**
         * 冲突类型：ROOM-直播间冲突, EMPLOYEE-人员冲突
         */
        private ConflictType conflictType;

        /**
         * 冲突的排班 ID
         */
        private Long scheduleId;

        /**
         * 与哪一个排班冲突
         */
        private Long conflictWithScheduleId;

        /**
         * 与哪一个直播间冲突
         */
        private Long conflictWithRoomId;

        /**
         * 冲突的员工 ID（仅人员冲突时有值）
         */
        private Long employeeId;

        /**
         * 冲突日期
         */
        private String workDay;

        /**
         * 冲突时间段
         */
        private String timeRange;

        /**
         * 冲突原因描述
         */
        private String conflictReason;

        /**
         * 冲突的直播间 ID
         */
        private Long liveRoomId;
    }

    /**
     * 冲突类型枚举
     */
    @Getter
    public enum ConflictType {
        /**
         * 直播间冲突：同一直播间在同一时间段内有多个排班
         */
        ROOM("直播间冲突"),

        /**
         * 人员冲突：同一员工在同一时间段内有多个排班
         */
        EMPLOYEE("人员冲突");

        private final String description;

        ConflictType(String description) {
            this.description = description;
        }

    }
}
