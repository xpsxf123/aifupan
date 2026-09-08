package com.jiuyu.governance.business.room.service.handler;

import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.shandard.Range;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.org.service.PositionService;
import com.jiuyu.governance.business.rbac.pojo.constants.DefaultPosition;
import com.jiuyu.governance.business.room.handler.WorkTimeHandler;
import com.jiuyu.governance.business.room.mapper.ScheduleEmployeeMapper;
import com.jiuyu.governance.business.room.mapper.WorkScheduleMapper;
import com.jiuyu.governance.business.room.pojo.bo.ScheduleConflictResult;
import com.jiuyu.governance.business.room.pojo.entity.ScheduleEmployee;
import com.jiuyu.governance.business.room.pojo.entity.WorkSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 排班冲突判断处理器
 * <p>
 * 负责校验直播间和人员的排班时间冲突，确保：
 * 1. 总体按“直播间 + 人员”维度阻断时间重叠（同一直播间允许多人同时间排班，不同员工不互斥）
 * 2. 同一员工在同一时间段内只能有一个排班
 * 3. 主播岗位（{@link DefaultPosition#ANCHOR}）：同一直播间内不同排班（不同 scheduleId）的主播排班不允许时间重叠（允许同排班多主播）
 * </p>
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveRoomScheduleConflictHandler {

    private final WorkScheduleMapper workScheduleMapper;

    private final ScheduleEmployeeMapper scheduleEmployeeMapper;

    private final PositionService positionService;

    private enum ValidateScene {
        ADD_SCHEDULE,
        UPDATE_SCHEDULE,
        ADD_EMPLOYEE
    }

    /**
     * 新增排班场景的冲突校验入口。
     * <p>
     * 特点：
     * - 通常传入批量生成的 {@link WorkSchedule} + {@link ScheduleEmployee} 列表
     * - 该入口仅负责参数规整与场景语义表达；底层冲突规则统一委托给 {@link #validateConflict(Long, List, List)}
     * </p>
     *
     * @param tenantId     租户 ID
     * @param newSchedules 新提交排班列表（通常为批量生成）
     * @param newEmployees 新提交人员列表（与排班关联）
     *
     * @return 冲突校验结果
     */
    public ScheduleConflictResult validateForAddSchedule(
        Long tenantId,
        List<WorkSchedule> newSchedules,
        List<ScheduleEmployee> newEmployees
    ) {
        return validateByScene(ValidateScene.ADD_SCHEDULE, tenantId, newSchedules, newEmployees);
    }

    /**
     * 修改排班场景的冲突校验入口。
     * <p>
     * 特点：
     * - 仅修改单条排班的时间信息，但需要携带该排班下所有人员重新校验
     * - 该入口负责将单条排班包装为列表，统一委托给 {@link #validateConflict(Long, List, List)}
     * </p>
     *
     * @param tenantId          租户 ID
     * @param schedule          待修改的排班
     * @param scheduleEmployees 该排班下的人员列表
     *
     * @return 冲突校验结果
     */
    public ScheduleConflictResult validateForUpdateSchedule(
        Long tenantId,
        WorkSchedule schedule,
        List<ScheduleEmployee> scheduleEmployees
    ) {
        if (schedule == null) {
            return emptyResult();
        }
        return validateByScene(ValidateScene.UPDATE_SCHEDULE, tenantId, List.of(schedule), scheduleEmployees);
    }

    /**
     * 添加人员场景的冲突校验入口。
     * <p>
     * 特点：
     * - 只新增单条 {@link ScheduleEmployee} 关联，但冲突判断需要依赖其所属排班时间区间
     * - 该入口会对人员关联的关键字段做防御性规整（scheduleId/liveRoomId/workDay），避免调用方遗漏导致误判
     * - 底层仍统一委托给 {@link #validateConflict(Long, List, List)}
     * </p>
     *
     * @param tenantId        租户 ID
     * @param schedule        目标排班
     * @param scheduleEmployee 待新增人员关联
     *
     * @return 冲突校验结果
     */
    public ScheduleConflictResult validateForAddEmployee(
        Long tenantId,
        WorkSchedule schedule,
        ScheduleEmployee scheduleEmployee
    ) {
        if (schedule == null) {
            return emptyResult();
        }
        if (scheduleEmployee == null) {
            return validateByScene(ValidateScene.ADD_EMPLOYEE, tenantId, List.of(schedule), List.of());
        }
        ScheduleEmployee normalizedEmployee = normalizeEmployeeForSchedule(schedule, scheduleEmployee);
        return validateByScene(ValidateScene.ADD_EMPLOYEE, tenantId, List.of(schedule), List.of(normalizedEmployee));
    }

    private ScheduleEmployee normalizeEmployeeForSchedule(WorkSchedule schedule, ScheduleEmployee scheduleEmployee) {
        if (scheduleEmployee == null) {
            return null;
        }
        if (Objects.equals(scheduleEmployee.getScheduleId(), schedule.getId())
            && Objects.equals(scheduleEmployee.getLiveRoomId(), schedule.getLiveRoomId())
            && Objects.equals(scheduleEmployee.getWorkDay(), schedule.getWorkDay())) {
            return scheduleEmployee;
        }
        ScheduleEmployee copy = new ScheduleEmployee();
        copy.setId(scheduleEmployee.getId());
        copy.setIsDeleted(scheduleEmployee.getIsDeleted());
        copy.setCreateBy(scheduleEmployee.getCreateBy());
        copy.setUpdateBy(scheduleEmployee.getUpdateBy());
        copy.setCreateDate(scheduleEmployee.getCreateDate());
        copy.setUpdateDate(scheduleEmployee.getUpdateDate());
        copy.setEmployeeId(scheduleEmployee.getEmployeeId());
        copy.setPositionId(scheduleEmployee.getPositionId());

        copy.setScheduleId(schedule.getId());
        copy.setLiveRoomId(schedule.getLiveRoomId());
        copy.setWorkDay(schedule.getWorkDay());
        return copy;
    }

    private ScheduleConflictResult validateByScene(
        ValidateScene scene,
        Long tenantId,
        List<WorkSchedule> newSchedules,
        List<ScheduleEmployee> newEmployees
    ) {
        return validateConflictInternal(scene, tenantId, newSchedules, newEmployees);
    }

    private ScheduleConflictResult emptyResult() {
        return ScheduleConflictResult.builder().build();
    }

    /**
     * 校验排班是否冲突
     * <p>
     * 兼容入口：历史调用方沿用该方法时，默认按“新增排班”场景执行冲突校验。
     * 如需更清晰的语义入口，请使用 {@link #validateForAddSchedule(Long, List, List)}、
     * {@link #validateForUpdateSchedule(Long, WorkSchedule, List)}、
     * {@link #validateForAddEmployee(Long, WorkSchedule, ScheduleEmployee)}。
     * <p>
     * 核心逻辑：
     * 1. 提取待校验的直播间 ID、员工 ID 和时间范围
     * 2. 批量查询数据库中已存在的冲突记录（直播间维度 + 人员维度）
     * 3. 执行双重校验：
     * - 直播间冲突：同一直播间在同一时间段内不能有多个排班
     * - 人员冲突：同一员工在同一时间段内不能有多个排班
     * 4. 同时校验新提交的排班之间的内部冲突
     * 5. 收集所有冲突信息并返回，不抛出异常
     * </p>
     *
     * @param tenantId     租户 ID，用于数据隔离
     * @param newSchedules 待新增/修改的班次列表
     * @param newEmployees 待新增/修改的人员排班列表
     *
     * @return ScheduleConflictResult 包含成功列表和冲突列表的校验结果
     */
    public ScheduleConflictResult validateConflict(Long tenantId, List<WorkSchedule> newSchedules, List<ScheduleEmployee> newEmployees) {
        return validateConflictInternal(ValidateScene.ADD_SCHEDULE, tenantId, newSchedules, newEmployees);
    }

    private ScheduleConflictResult validateConflictInternal(
        ValidateScene scene,
        Long tenantId,
        List<WorkSchedule> newSchedules,
        List<ScheduleEmployee> newEmployees
    ) {
        log.debug("[排班冲突校验] 场景={}, tenantId={}, schedulesCount={}, employeesCount={}",
            scene, tenantId, newSchedules != null ? newSchedules.size() : 0, newEmployees != null ? newEmployees.size() : 0);

        ScheduleConflictResult result = ScheduleConflictResult.builder().build();

        // 1. 入参规整：排班为空直接返回；人员为空视为无人员排班变更
        if (EmptyUtil.isEmpty(newSchedules)) {
            log.debug("[排班冲突校验] 排班列表为空，跳过校验");
            return result;
        }
        List<ScheduleEmployee> finalNewEmployees = newEmployees == null ? List.of() : newEmployees;

        // 2. 区分新增方/修改方：用于日志、以及修改场景排除自身冲突
        long newScheduleCount = newSchedules.stream().filter(s -> s.getId() == null).count();
        long modifyScheduleCount = newSchedules.size() - newScheduleCount;

        // liveRoomIds：新提交排班涉及的直播间ID集合（新增方/修改方），employeeIds：新提交人员排班涉及的员工ID集合（新增方/修改方）
        // ignoreScheduleIds：修改场景需要排除的排班ID集合（修改方自身，避免自我冲突）
        List<Long> liveRoomIds = newSchedules.stream().map(WorkSchedule::getLiveRoomId).distinct().toList();
        List<Long> employeeIds = finalNewEmployees.stream().map(ScheduleEmployee::getEmployeeId).distinct().toList();
        List<Long> ignoreScheduleIds = newSchedules.stream().map(WorkSchedule::getId).filter(Objects::nonNull).toList();

        log.debug("[排班冲突校验] 本次变更维度：liveRooms={}, employees={}, 新增排班数={}, 修改排班数={}, ignoreScheduleIds={}",
            liveRoomIds.size(), employeeIds.size(), newScheduleCount, modifyScheduleCount, ignoreScheduleIds.size());

        // 3. 提取整体时间范围：用于批量拉取已存在的人员排班、排班数据
        ValidationRange validationRange = extractValidationRange(newSchedules);
        if (validationRange == null) {
            log.warn("[排班冲突校验] 无法提取有效时间范围，跳过校验");
            return result;
        }
        log.debug("[排班冲突校验] 时间范围：{} ~ {}", validationRange.minTime(), validationRange.maxTime());

        // existEmployees：已存在人员排班集合（已存在人员方），empScheduleIds：已存在人员排班关联到的排班ID集合（用于反查已存在排班方）
        // 4. 查询已存在人员排班（已存在人员方）
        List<ScheduleEmployee> existEmployees = queryExistingEmployees(employeeIds, validationRange.startDate(), validationRange.endDate());
        List<Long> empScheduleIds = existEmployees.stream().map(ScheduleEmployee::getScheduleId).distinct().toList();
        if (EmptyUtil.isNotEmpty(employeeIds)) {
            log.debug("[排班冲突校验] 查询到已有员工排班记录数：{}, 涉及排班数：{}", existEmployees.size(), empScheduleIds.size());
        }

        // existingSchedules：已存在排班集合（已存在排班方：直播间维度相关排班 + 已存在人员关联的排班）
        // 5. 查询已存在排班（已存在排班方）：包含直播间维度相关排班、以及已存在人员关联的排班
        List<WorkSchedule> existingSchedules = queryExistingSchedules(
            tenantId, liveRoomIds, ignoreScheduleIds, empScheduleIds, validationRange.startDate(), validationRange.endDate()
        );
        log.debug("[排班冲突校验] 查询到已有排班记录数：{}", existingSchedules.size());

        // existScheduleMap：已存在排班索引（已存在排班方），newScheduleMap：新提交排班索引（新增方/修改方）
        Map<Long, WorkSchedule> existScheduleMap = existingSchedules.stream().collect(Collectors.toMap(WorkSchedule::getId, s -> s));
        Map<Long, WorkSchedule> newScheduleMap = newSchedules.stream().collect(Collectors.toMap(WorkSchedule::getId, s -> s));

        // conflictScheduleIds：冲突排班ID集合（用于去重、以及反推成功排班/人员），conflictEmployeeIds：冲突员工ID集合（用于统计/日志）
        java.util.Set<Long> conflictScheduleIds = new java.util.HashSet<>();
        java.util.Set<Long> conflictEmployeeIds = new java.util.HashSet<>();

        // 6. 冲突校验：先校验与已存在冲突，再校验新提交内部冲突（顺序保持与历史逻辑一致）
        // 总体规则：冲突维度为“直播间 + 人员”。同一直播间允许多人在同一时间段排班，因此不执行“所有岗位按直播间维度禁止重叠”的校验。
        // 但主播岗位为例外：同一直播间内若两个不同 scheduleId 都包含主播岗位，则两排班时间段不得重叠（允许同排班多主播）。
        validateAnchorRoomConflictIfNeeded(
            tenantId, liveRoomIds, validationRange.startDate(), validationRange.endDate(),
            finalNewEmployees, newScheduleMap, existingSchedules, conflictScheduleIds, result
        );
        log.debug("[排班冲突校验] 主播岗位-直播间维度校验完成，冲突数：{}", conflictScheduleIds.size());

        validateEmployeeConflict(
            finalNewEmployees, newScheduleMap, ignoreScheduleIds, existEmployees, existScheduleMap,
            conflictScheduleIds, conflictEmployeeIds, result
        );
        validateNewEmployeesInternalConflict(finalNewEmployees, newScheduleMap, conflictScheduleIds, conflictEmployeeIds, result);
        log.debug("[排班冲突校验] 人员维度校验完成，冲突排班数：{}，冲突员工数：{}", conflictScheduleIds.size(), conflictEmployeeIds.size());

        fillSuccessIds(newSchedules, finalNewEmployees, conflictScheduleIds, result);

        log.info("[排班冲突校验] 校验完成：tenantId={}, 总排班数={}, 成功数={}, 冲突数={}, 总员工数={}, 成功员工数={}, 冲突员工数={}",
            tenantId, newSchedules.size(), result.getSuccessScheduleIds().size(),
            conflictScheduleIds.size(), finalNewEmployees.size(), result.getSuccessEmployeeIds().size(),
            conflictEmployeeIds.size());

        return result;
    }

    /**
     * 提取本次排班变更的整体时间范围（同时扩展日期范围用于查询已存在数据）。
     *
     * @param newSchedules 待新增/修改的排班列表
     *
     * @return {@link ValidationRange}；若无法提取有效时间范围则返回 null
     */
    private ValidationRange extractValidationRange(List<WorkSchedule> newSchedules) {
        LocalDateTime minTime = newSchedules.stream().map(s -> s.workRange().getStart()).min(LocalDateTime::compareTo).orElse(null);
        LocalDateTime maxTime = newSchedules.stream().map(s -> s.workRange().getEnd()).max(LocalDateTime::compareTo).orElse(null);
        if (minTime == null || maxTime == null) {
            return null;
        }
        return new ValidationRange(minTime, maxTime, minTime.toLocalDate().minusDays(1), maxTime.toLocalDate().plusDays(1));
    }

    /**
     * 查询时间范围内（按 workDay）员工相关的已存在人员排班记录（已存在人员方）。
     *
     * @param employeeIds 员工 ID 列表
     * @param startDate  开始日期（含）
     * @param endDate    结束日期（含）
     *
     * @return {@link ScheduleEmployee} 列表
     */
    private List<ScheduleEmployee> queryExistingEmployees(List<Long> employeeIds, LocalDate startDate, LocalDate endDate) {
        if (EmptyUtil.isEmpty(employeeIds)) {
            log.debug("[排班冲突校验] 未传入 employeeIds，跳过查询已存在人员排班");
            return List.of();
        }
        log.debug("[排班冲突校验] 查询已存在人员排班：employeeCount={}, dateRange={}~{}", employeeIds.size(), startDate, endDate);
        return new BatchQuery<>((limit, idx) -> ChainWrappers.lambdaQueryChain(scheduleEmployeeMapper)
            .in(ScheduleEmployee::getEmployeeId, employeeIds)
            .ge(ScheduleEmployee::getWorkDay, startDate)
            .le(ScheduleEmployee::getWorkDay, endDate)
            .eq(ScheduleEmployee::getIsDeleted, false)
            .gt(idx != null, ScheduleEmployee::getId, idx)
            .last("limit " + limit)
            .list(), ScheduleEmployee::getId).get();
    }

    private List<ScheduleEmployee> queryExistingAnchorEmployees(
        List<Long> liveRoomIds,
        Long anchorPositionId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        if (EmptyUtil.isEmpty(liveRoomIds) || anchorPositionId == null) {
            return List.of();
        }
        return new BatchQuery<>((limit, idx) -> ChainWrappers.lambdaQueryChain(scheduleEmployeeMapper)
            .in(ScheduleEmployee::getLiveRoomId, liveRoomIds)
            .eq(ScheduleEmployee::getPositionId, anchorPositionId)
            .ge(ScheduleEmployee::getWorkDay, startDate)
            .le(ScheduleEmployee::getWorkDay, endDate)
            .eq(ScheduleEmployee::getIsDeleted, false)
            .gt(idx != null, ScheduleEmployee::getId, idx)
            .last("limit " + limit)
            .list(), ScheduleEmployee::getId).get();
    }

    private void validateAnchorRoomConflictIfNeeded(
        Long tenantId,
        List<Long> liveRoomIds,
        LocalDate startDate,
        LocalDate endDate,
        List<ScheduleEmployee> newEmployees,
        Map<Long, WorkSchedule> newScheduleMap,
        List<WorkSchedule> existingSchedules,
        java.util.Set<Long> conflictScheduleIds,
        ScheduleConflictResult result
    ) {
        if (tenantId == null) {
            return;
        }
        Long anchorPositionId = positionService.getDefaultPositionId(DefaultPosition.ANCHOR, tenantId);
        if (anchorPositionId == null) {
            return;
        }
        List<ScheduleEmployee> newAnchorEmployees = newEmployees.stream()
            .filter(e -> Objects.equals(e.getPositionId(), anchorPositionId))
            .toList();
        if (EmptyUtil.isEmpty(newAnchorEmployees)) {
            return;
        }

        List<ScheduleEmployee> existAnchorEmployees = queryExistingAnchorEmployees(liveRoomIds, anchorPositionId, startDate, endDate);
        java.util.Set<Long> existAnchorScheduleIdSet = existAnchorEmployees.stream()
            .map(ScheduleEmployee::getScheduleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, List<WorkSchedule>> existAnchorSchedulesByRoom = existingSchedules.stream()
            .filter(s -> existAnchorScheduleIdSet.contains(s.getId()))
            .collect(Collectors.groupingBy(WorkSchedule::getLiveRoomId));

        List<Long> newAnchorScheduleIds = newAnchorEmployees.stream()
            .map(ScheduleEmployee::getScheduleId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        for (Long scheduleId : newAnchorScheduleIds) {
            WorkSchedule newSchedule = newScheduleMap.get(scheduleId);
            if (newSchedule == null) {
                continue;
            }
            List<WorkSchedule> existSchedulesInRoom = existAnchorSchedulesByRoom.get(newSchedule.getLiveRoomId());
            if (EmptyUtil.isEmpty(existSchedulesInRoom)) {
                continue;
            }
            Range<LocalDateTime> newRange = newSchedule.workRange();
            for (WorkSchedule existSchedule : existSchedulesInRoom) {
                if (Objects.equals(existSchedule.getId(), newSchedule.getId())) {
                    continue;
                }
                if (WorkTimeHandler.isOverlap(newRange, existSchedule.workRange())) {
                    log.warn("[排班冲突校验] 发现主播直播间冲突：liveRoomId={}, newSide={}, newScheduleId={}, existScheduleId={}",
                        newSchedule.getLiveRoomId(), scheduleSide(newSchedule), newSchedule.getId(), existSchedule.getId());

                    conflictScheduleIds.add(newSchedule.getId());
                    result.getConflictSchedules().add(ScheduleConflictResult.ScheduleConflictInfo.builder()
                        .conflictType(ScheduleConflictResult.ConflictType.ROOM)
                        .scheduleId(newSchedule.getId())
                        .conflictWithScheduleId(existSchedule.getId())
                        .conflictWithRoomId(existSchedule.getLiveRoomId())
                        .liveRoomId(newSchedule.getLiveRoomId())
                        .workDay(newSchedule.getWorkDay().toString())
                        .timeRange(newSchedule.parseStartWork() + "-" + newSchedule.parseEndWork())
                        .conflictReason(existSchedule.getWorkDay() + " " + existSchedule.parseStartWork()
                            + " - " + existSchedule.parseEndWork())
                        .build());
                    break;
                }
            }
        }

        Map<Long, List<Long>> newAnchorScheduleIdsByRoom = newAnchorEmployees.stream()
            .filter(e -> e.getLiveRoomId() != null && e.getScheduleId() != null)
            .collect(Collectors.groupingBy(
                ScheduleEmployee::getLiveRoomId,
                Collectors.mapping(ScheduleEmployee::getScheduleId, Collectors.collectingAndThen(Collectors.toSet(), List::copyOf))
            ));

        for (Map.Entry<Long, List<Long>> entry : newAnchorScheduleIdsByRoom.entrySet()) {
            List<Long> scheduleIds = entry.getValue();
            if (scheduleIds.size() <= 1) {
                continue;
            }
            for (int i = 0; i < scheduleIds.size(); i++) {
                for (int j = i + 1; j < scheduleIds.size(); j++) {
                    Long s1Id = scheduleIds.get(i);
                    Long s2Id = scheduleIds.get(j);
                    if (Objects.equals(s1Id, s2Id)) {
                        continue;
                    }
                    WorkSchedule s1 = newScheduleMap.get(s1Id);
                    WorkSchedule s2 = newScheduleMap.get(s2Id);
                    if (s1 == null || s2 == null) {
                        continue;
                    }
                    if (WorkTimeHandler.isOverlap(s1.workRange(), s2.workRange())) {
                        if (!conflictScheduleIds.contains(s1.getId())) {
                            conflictScheduleIds.add(s1.getId());
                            result.getConflictSchedules().add(ScheduleConflictResult.ScheduleConflictInfo.builder()
                                .conflictType(ScheduleConflictResult.ConflictType.ROOM)
                                .scheduleId(s1.getId())
                                .conflictWithScheduleId(s2.getId())
                                .conflictWithRoomId(s2.getLiveRoomId())
                                .liveRoomId(s1.getLiveRoomId())
                                .workDay(s1.getWorkDay().toString())
                                .timeRange(s1.parseStartWork() + "-" + s1.parseEndWork())
                                .conflictReason(s2.getWorkDay() + " " + s2.parseStartWork() + " - " + s2.parseEndWork())
                                .build());
                        }
                        if (!conflictScheduleIds.contains(s2.getId())) {
                            conflictScheduleIds.add(s2.getId());
                            result.getConflictSchedules().add(ScheduleConflictResult.ScheduleConflictInfo.builder()
                                .conflictType(ScheduleConflictResult.ConflictType.ROOM)
                                .scheduleId(s2.getId())
                                .conflictWithScheduleId(s1.getId())
                                .conflictWithRoomId(s1.getLiveRoomId())
                                .liveRoomId(s2.getLiveRoomId())
                                .workDay(s2.getWorkDay().toString())
                                .timeRange(s2.parseStartWork() + "-" + s2.parseEndWork())
                                .conflictReason(s1.getWorkDay() + " " + s1.parseStartWork() + " - " + s1.parseEndWork())
                                .build());
                        }
                    }
                }
            }
        }
    }

    /**
     * 查询时间范围内（按 workDay）与直播间/人员关联的已存在排班（已存在排班方）。
     *
     * @param tenantId         租户 ID
     * @param liveRoomIds      直播间 ID 列表
     * @param ignoreScheduleIds 修改场景需要排除的排班 ID 列表（避免自我冲突）
     * @param empScheduleIds   已存在人员排班记录关联的排班 ID 列表
     * @param startDate        开始日期（含）
     * @param endDate          结束日期（含）
     *
     * @return {@link WorkSchedule} 列表
     */
    private List<WorkSchedule> queryExistingSchedules(
        Long tenantId,
        List<Long> liveRoomIds,
        List<Long> ignoreScheduleIds,
        List<Long> empScheduleIds,
        LocalDate startDate,
        LocalDate endDate
    ) {
        log.debug("[排班冲突校验] 查询已存在排班：tenantId={}, liveRoomCount={}, ignoreScheduleIds={}, empScheduleIds={}, dateRange={}~{}",
            tenantId, liveRoomIds != null ? liveRoomIds.size() : 0, ignoreScheduleIds != null ? ignoreScheduleIds.size() : 0,
            empScheduleIds != null ? empScheduleIds.size() : 0, startDate, endDate);
        BatchQuery<Long, WorkSchedule> scheduleBatchQuery = new BatchQuery<>((limit, idx) -> ChainWrappers.lambdaQueryChain(workScheduleMapper)
            .eq(WorkSchedule::getTenantId, tenantId)
            .eq(WorkSchedule::getIsDeleted, false)
            .ge(WorkSchedule::getWorkDay, startDate)
            .le(WorkSchedule::getWorkDay, endDate)
            .notIn(EmptyUtil.isNotEmpty(ignoreScheduleIds), WorkSchedule::getId, ignoreScheduleIds)
            .and(w -> w.in(WorkSchedule::getLiveRoomId, liveRoomIds)
                .or(EmptyUtil.isNotEmpty(empScheduleIds), o -> o.in(WorkSchedule::getId, empScheduleIds)))
            .gt(idx != null, WorkSchedule::getId, idx)
            .last("limit " + limit)
            .list(), WorkSchedule::getId);
        return scheduleBatchQuery.get(null, 100000);
    }

    /**
     * 校验直播间维度：新提交排班（新增方/修改方）与已存在排班（已存在排班方）是否存在时间重叠。
     *
     * @param newSchedules       新提交排班
     * @param existingSchedules  已存在排班
     * @param conflictScheduleIds 冲突排班集合（用于去重）
     * @param result             冲突结果承载
     */
    private void validateRoomConflict(
        List<WorkSchedule> newSchedules,
        List<WorkSchedule> existingSchedules,
        java.util.Set<Long> conflictScheduleIds,
        ScheduleConflictResult result
    ) {
        log.debug("[排班冲突校验] 直播间维度-对已存在排班校验开始：newSchedules={}, existingSchedules={}",
            newSchedules.size(), existingSchedules.size());

        // existSchedulesByRoom：已存在排班按直播间分组（已存在排班方），用于减少循环次数
        Map<Long, List<WorkSchedule>> existSchedulesByRoom = existingSchedules.stream()
            .collect(Collectors.groupingBy(WorkSchedule::getLiveRoomId));

        // 1. 遍历新提交排班（新增方/修改方），按直播间维度与已存在排班方逐个做时间重叠判断
        for (WorkSchedule newSchedule : newSchedules) {
            Range<LocalDateTime> newRange = newSchedule.workRange();
            List<WorkSchedule> existSchedulesInRoom = existSchedulesByRoom.get(newSchedule.getLiveRoomId());
            if (EmptyUtil.isNotEmpty(existSchedulesInRoom)) {
                for (WorkSchedule existSchedule : existSchedulesInRoom) {
                    Range<LocalDateTime> existRange = existSchedule.workRange();

                    // 2. 关键判断：时间区间重叠则视为冲突（直播间冲突）
                    if (WorkTimeHandler.isOverlap(newRange, existRange)) {
                        log.warn("[排班冲突校验] 发现直播间冲突：liveRoomId={}, newSide={}, newScheduleId={}, existSide=已存在排班, existScheduleId={}, date={}, time={}-{}",
                            newSchedule.getLiveRoomId(), scheduleSide(newSchedule), newSchedule.getId(), existSchedule.getId(),
                            newSchedule.getWorkDay(), newSchedule.parseStartWork(), newSchedule.parseEndWork());

                        // 3. 记录冲突：只记录“新提交排班”这一侧的冲突条目
                        // - scheduleId：发生冲突的新提交排班ID（新增方/修改方）
                        // - conflictWithScheduleId/conflictWithRoomId：与之冲突的已存在排班方信息
                        conflictScheduleIds.add(newSchedule.getId());
                        result.getConflictSchedules().add(ScheduleConflictResult.ScheduleConflictInfo.builder()
                            .conflictType(ScheduleConflictResult.ConflictType.ROOM)
                            .scheduleId(newSchedule.getId())
                            .conflictWithScheduleId(existSchedule.getId())
                            .conflictWithRoomId(existSchedule.getLiveRoomId())
                            .liveRoomId(newSchedule.getLiveRoomId())
                            .workDay(newSchedule.getWorkDay().toString())
                            .timeRange(newSchedule.parseStartWork() + "-" + newSchedule.parseEndWork())
                            .conflictReason(existSchedule.getWorkDay() + " " + existSchedule.parseStartWork() + " - " + existSchedule.parseEndWork())
                            .build());

                        // 4. 关键分支：一个新提交排班只需要记录一次“对已存在排班”的冲突，命中后直接结束内层循环
                        break;
                    }
                }
            }
        }
        log.debug("[排班冲突校验] 直播间维度-对已存在排班校验结束：conflictSchedules={}", conflictScheduleIds.size());
    }

    /**
     * 校验直播间维度：新提交排班内部（新增方/修改方之间）是否存在时间重叠。
     *
     * @param newSchedules       新提交排班
     * @param conflictScheduleIds 冲突排班集合（用于去重）
     * @param result             冲突结果承载
     */
    private void validateNewSchedulesRoomInternalConflict(
        List<WorkSchedule> newSchedules,
        java.util.Set<Long> conflictScheduleIds,
        ScheduleConflictResult result
    ) {
        log.debug("[排班冲突校验] 直播间维度-新提交内部校验开始：newSchedules={}", newSchedules.size());

        // newSchedulesByRoom：新提交排班按直播间分组（新增方/修改方），用于组内两两比较
        Map<Long, List<WorkSchedule>> newSchedulesByRoom = newSchedules.stream()
            .collect(Collectors.groupingBy(WorkSchedule::getLiveRoomId));

        // 1. 同一直播间内两两比较排班时间是否重叠
        for (List<WorkSchedule> roomSchedules : newSchedulesByRoom.values()) {
            if (roomSchedules.size() > 1) {
                for (int i = 0; i < roomSchedules.size(); i++) {
                    for (int j = i + 1; j < roomSchedules.size(); j++) {
                        WorkSchedule s1 = roomSchedules.get(i);
                        WorkSchedule s2 = roomSchedules.get(j);

                        // 2. 关键判断：新提交排班之间时间区间重叠则视为冲突（直播间冲突-内部）
                        if (WorkTimeHandler.isOverlap(s1.workRange(), s2.workRange())) {
                            log.warn("[排班冲突校验] 新提交的排班之间存在冲突：liveRoomId={}, schedule1Side={}, schedule1Id={}, schedule2Side={}, schedule2Id={}",
                                s1.getLiveRoomId(), scheduleSide(s1), s1.getId(), scheduleSide(s2), s2.getId());

                            // 3. 关键分支：对冲突的两侧分别生成冲突记录，但同一个排班只记录一次（用 conflictScheduleIds 去重）
                            if (!conflictScheduleIds.contains(s1.getId())) {
                                conflictScheduleIds.add(s1.getId());
                                result.getConflictSchedules().add(ScheduleConflictResult.ScheduleConflictInfo.builder()
                                    .conflictType(ScheduleConflictResult.ConflictType.ROOM)
                                    .scheduleId(s1.getId())
                                    .conflictWithScheduleId(s2.getId())
                                    .conflictWithRoomId(s2.getLiveRoomId())
                                    .liveRoomId(s1.getLiveRoomId())
                                    .workDay(s1.getWorkDay().toString())
                                    .timeRange(s1.parseStartWork() + "-" + s1.parseEndWork())
                                    .conflictReason(s2.getWorkDay() + " " + s2.parseStartWork() + " - " + s2.parseEndWork())
                                    .build());
                            }
                            if (!conflictScheduleIds.contains(s2.getId())) {
                                conflictScheduleIds.add(s2.getId());
                                result.getConflictSchedules().add(ScheduleConflictResult.ScheduleConflictInfo.builder()
                                    .conflictType(ScheduleConflictResult.ConflictType.ROOM)
                                    .scheduleId(s2.getId())
                                    .conflictWithScheduleId(s1.getId())
                                    .conflictWithRoomId(s1.getLiveRoomId())
                                    .liveRoomId(s2.getLiveRoomId())
                                    .workDay(s2.getWorkDay().toString())
                                    .timeRange(s2.parseStartWork() + "-" + s2.parseEndWork())
                                    .conflictReason(s2.getWorkDay() + " " + s2.parseStartWork() + " - " + s2.parseEndWork())
                                    .build());
                            }
                        }
                    }
                }
            }
        }
        log.debug("[排班冲突校验] 直播间维度-新提交内部校验结束：conflictSchedules={}", conflictScheduleIds.size());
    }

    /**
     * 校验人员维度：新提交人员排班（新增方/修改方）与已存在人员排班（已存在人员方）是否存在时间重叠。
     *
     * @param newEmployees        新提交人员排班
     * @param newScheduleMap      新提交排班映射（scheduleId -> schedule）
     * @param ignoreScheduleIds   修改场景需要排除的排班 ID 列表（避免自我冲突）
     * @param existEmployees      已存在人员排班
     * @param existScheduleMap    已存在排班映射（scheduleId -> schedule）
     * @param conflictScheduleIds 冲突排班集合（用于去重）
     * @param conflictEmployeeIds 冲突员工集合（用于统计）
     * @param result              冲突结果承载
     */
    private void validateEmployeeConflict(
        List<ScheduleEmployee> newEmployees,
        Map<Long, WorkSchedule> newScheduleMap,
        List<Long> ignoreScheduleIds,
        List<ScheduleEmployee> existEmployees,
        Map<Long, WorkSchedule> existScheduleMap,
        java.util.Set<Long> conflictScheduleIds,
        java.util.Set<Long> conflictEmployeeIds,
        ScheduleConflictResult result
    ) {
        log.debug("[排班冲突校验] 人员维度-对已存在人员排班校验开始：newEmployees={}, existEmployees={}",
            newEmployees.size(), existEmployees.size());
        Map<Long, List<ScheduleEmployee>> existEmployeesByEmpId = existEmployees.stream()
            .collect(Collectors.groupingBy(ScheduleEmployee::getEmployeeId));

        for (ScheduleEmployee newEmp : newEmployees) {
            WorkSchedule newSchedule = newScheduleMap.get(newEmp.getScheduleId());
            if (newSchedule == null) {
                log.warn("[排班冲突校验] 未找到对应的排班：scheduleId={}, employeeId={}，跳过该校验",
                    newEmp.getScheduleId(), newEmp.getEmployeeId());
                continue;
            }
            Range<LocalDateTime> newRange = newSchedule.workRange();

            List<ScheduleEmployee> existEmpsForThisEmployee = existEmployeesByEmpId.get(newEmp.getEmployeeId());
            if (EmptyUtil.isNotEmpty(existEmpsForThisEmployee)) {
                for (ScheduleEmployee existEmp : existEmpsForThisEmployee) {
                    if (ignoreScheduleIds.contains(existEmp.getScheduleId())) {
                        log.debug("[排班冲突校验] 人员维度-跳过自我冲突：employeeId={}, newSide={}, newScheduleId={}, ignoreScheduleId={}",
                            newEmp.getEmployeeId(), scheduleSide(newSchedule), newSchedule.getId(), existEmp.getScheduleId());
                        continue;
                    }
                    WorkSchedule existSchedule = existScheduleMap.get(existEmp.getScheduleId());
                    if (existSchedule != null && WorkTimeHandler.isOverlap(newRange, existSchedule.workRange())) {
                        log.warn("[排班冲突校验] 发现人员冲突：employeeId={}, newSide={}, newScheduleId={}, existSide=已存在人员排班, existScheduleId={}",
                            newEmp.getEmployeeId(), scheduleSide(newSchedule), newSchedule.getId(), existEmp.getScheduleId());

                        conflictScheduleIds.add(newSchedule.getId());
                        conflictEmployeeIds.add(newEmp.getEmployeeId());
                        result.getConflictSchedules().add(ScheduleConflictResult.ScheduleConflictInfo.builder()
                            .conflictType(ScheduleConflictResult.ConflictType.EMPLOYEE)
                            .scheduleId(newSchedule.getId())
                            .conflictWithScheduleId(existEmp.getScheduleId())
                            .conflictWithRoomId(existEmp.getLiveRoomId())
                            .employeeId(newEmp.getEmployeeId())
                            .liveRoomId(newSchedule.getLiveRoomId())
                            .workDay(newSchedule.getWorkDay().toString())
                            .timeRange(newSchedule.parseStartWork() + "-" + newSchedule.parseEndWork())
                            .conflictReason(existSchedule.getWorkDay() + " " + existSchedule.parseStartWork() + " - " + existSchedule.parseEndWork())
                            .build());
                        break;
                    }
                }
            }
        }
        log.debug("[排班冲突校验] 人员维度-对已存在人员排班校验结束：conflictSchedules={}, conflictEmployees={}",
            conflictScheduleIds.size(), conflictEmployeeIds.size());
    }

    /**
     * 校验人员维度：新提交人员排班内部（同一员工）是否存在时间重叠。
     *
     * @param newEmployees        新提交人员排班
     * @param newScheduleMap      新提交排班映射（scheduleId -> schedule）
     * @param conflictScheduleIds 冲突排班集合（用于去重）
     * @param conflictEmployeeIds 冲突员工集合（用于统计）
     * @param result              冲突结果承载
     */
    private void validateNewEmployeesInternalConflict(
        List<ScheduleEmployee> newEmployees,
        Map<Long, WorkSchedule> newScheduleMap,
        java.util.Set<Long> conflictScheduleIds,
        java.util.Set<Long> conflictEmployeeIds,
        ScheduleConflictResult result
    ) {
        log.debug("[排班冲突校验] 人员维度-新提交内部校验开始：newEmployees={}", newEmployees.size());

        // newEmployeesByEmpId：新提交人员排班按员工分组（新增方/修改方），用于同一员工组内两两比较
        Map<Long, List<ScheduleEmployee>> newEmployeesByEmpId = newEmployees.stream()
            .collect(Collectors.groupingBy(ScheduleEmployee::getEmployeeId));

        // 1. 同一员工内两两比较其关联排班的时间是否重叠
        for (List<ScheduleEmployee> empSchedules : newEmployeesByEmpId.values()) {
            if (empSchedules.size() > 1) {
                for (int i = 0; i < empSchedules.size(); i++) {
                    for (int j = i + 1; j < empSchedules.size(); j++) {
                        ScheduleEmployee emp1 = empSchedules.get(i);
                        ScheduleEmployee emp2 = empSchedules.get(j);

                        // schedule1/schedule2：新提交排班方（通过 newScheduleMap 反查），用于取时间区间与直播间ID
                        WorkSchedule schedule1 = newScheduleMap.get(emp1.getScheduleId());
                        WorkSchedule schedule2 = newScheduleMap.get(emp2.getScheduleId());

                        // 2. 关键判断：两侧排班均存在且时间区间重叠，则视为冲突（人员冲突-内部）
                        if (schedule1 != null && schedule2 != null && WorkTimeHandler.isOverlap(schedule1.workRange(), schedule2.workRange())) {
                            log.warn("[排班冲突校验] 新提交的人员排班之间存在冲突：employeeId={}, schedule1Side={}, schedule1Id={}, schedule2Side={}, schedule2Id={}",
                                emp1.getEmployeeId(), scheduleSide(schedule1), schedule1.getId(), scheduleSide(schedule2), schedule2.getId());

                            // 3. 关键分支：对冲突的两侧分别生成冲突记录，但同一个排班只记录一次（用 conflictScheduleIds 去重）
                            if (!conflictScheduleIds.contains(schedule1.getId())) {
                                conflictScheduleIds.add(schedule1.getId());
                                conflictEmployeeIds.add(emp1.getEmployeeId());
                                result.getConflictSchedules().add(ScheduleConflictResult.ScheduleConflictInfo.builder()
                                    .conflictType(ScheduleConflictResult.ConflictType.EMPLOYEE)
                                    .scheduleId(schedule1.getId())
                                    .conflictWithScheduleId(schedule2.getId())
                                    .conflictWithRoomId(schedule2.getLiveRoomId())
                                    .employeeId(emp1.getEmployeeId())
                                    .liveRoomId(schedule1.getLiveRoomId())
                                    .workDay(schedule1.getWorkDay().toString())
                                    .timeRange(schedule1.parseStartWork() + "-" + schedule1.parseEndWork())
                                    .conflictReason(schedule2.getWorkDay() + " " + schedule2.parseStartWork() + " - " + schedule2.parseEndWork())
                                    .build());
                            }
                            if (!conflictScheduleIds.contains(schedule2.getId())) {
                                conflictScheduleIds.add(schedule2.getId());
                                conflictEmployeeIds.add(emp2.getEmployeeId());
                                result.getConflictSchedules().add(ScheduleConflictResult.ScheduleConflictInfo.builder()
                                    .conflictType(ScheduleConflictResult.ConflictType.EMPLOYEE)
                                    .scheduleId(schedule2.getId())
                                    .conflictWithScheduleId(schedule1.getId())
                                    .conflictWithRoomId(schedule1.getLiveRoomId())
                                    .employeeId(emp2.getEmployeeId())
                                    .liveRoomId(schedule2.getLiveRoomId())
                                    .workDay(schedule2.getWorkDay().toString())
                                    .timeRange(schedule2.parseStartWork() + "-" + schedule2.parseEndWork())
                                    .conflictReason(schedule1.getWorkDay() + " " + schedule1.parseStartWork() + " - " + schedule1.parseEndWork())
                                    .build());
                            }
                        }
                    }
                }
            }
        }
        log.debug("[排班冲突校验] 人员维度-新提交内部校验结束：conflictSchedules={}, conflictEmployees={}",
            conflictScheduleIds.size(), conflictEmployeeIds.size());
    }

    /**
     * 计算本次提交中成功的排班 ID、成功的人员排班 ID。
     *
     * @param newSchedules       新提交排班
     * @param newEmployees       新提交人员排班
     * @param conflictScheduleIds 冲突排班集合
     * @param result             冲突结果承载
     */
    private void fillSuccessIds(
        List<WorkSchedule> newSchedules,
        List<ScheduleEmployee> newEmployees,
        java.util.Set<Long> conflictScheduleIds,
        ScheduleConflictResult result
    ) {
        result.setSuccessScheduleIds(
            newSchedules.stream()
                .map(WorkSchedule::getId)
                .filter(id -> !conflictScheduleIds.contains(id))
                .toList()
        );
        result.setSuccessEmployeeIds(
            newEmployees.stream()
                .filter(emp -> !conflictScheduleIds.contains(emp.getScheduleId()))
                .map(ScheduleEmployee::getId)
                .toList()
        );
        log.debug("[排班冲突校验] 成功数据汇总：successSchedules={}, successEmployees={}",
            result.getSuccessScheduleIds().size(), result.getSuccessEmployeeIds().size());
    }

    private String scheduleSide(WorkSchedule schedule) {
        if (schedule == null || schedule.getId() == null) {
            return "新增方";
        }
        return "修改方";
    }

    /**
     * 排班时间区间
     */
    private record ValidationRange(LocalDateTime minTime, LocalDateTime maxTime, LocalDate startDate,
                                   LocalDate endDate) {
    }

}
