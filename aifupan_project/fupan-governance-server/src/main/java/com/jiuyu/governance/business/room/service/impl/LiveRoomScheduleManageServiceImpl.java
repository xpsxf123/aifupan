package com.jiuyu.governance.business.room.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.mybatispuls.util.CustomPage;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.shandard.Range;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.performance.pojo.response.SchedulePerformanceResponse;
import com.jiuyu.governance.business.performance.service.SchedulePerformanceService;
import com.jiuyu.governance.business.org.service.PositionService;
import com.jiuyu.governance.business.rbac.pojo.constants.AccountStatus;
import com.jiuyu.governance.business.rbac.pojo.constants.DefaultPosition;
import com.jiuyu.governance.business.rbac.pojo.entity.Employee;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.business.room.handler.WorkTimeHandler;
import com.jiuyu.governance.business.room.mapper.LiveRoomMapper;
import com.jiuyu.governance.business.room.mapper.LiveRoomScheduleAttributeMapper;
import com.jiuyu.governance.business.room.mapper.ScheduleEmployeeMapper;
import com.jiuyu.governance.business.room.mapper.WorkScheduleMapper;
import com.jiuyu.governance.business.room.pojo.bo.BatchQueryScheduleDto;
import com.jiuyu.governance.business.room.pojo.bo.EmployeeLiveRoomScheduleRawDto;
import com.jiuyu.governance.business.room.pojo.bo.LiveScheduleRawDto;
import com.jiuyu.governance.business.room.pojo.bo.RoomScheduleBo;
import com.jiuyu.governance.business.room.pojo.bo.RoomSchedulesRawBo;
import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoom;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoomScheduleAttribute;
import com.jiuyu.governance.business.room.pojo.entity.ScheduleEmployee;
import com.jiuyu.governance.business.room.pojo.entity.WorkSchedule;
import com.jiuyu.governance.business.room.pojo.request.schedule.*;
import com.jiuyu.governance.business.room.pojo.response.ClientFuturePlanScheduleResponse;
import com.jiuyu.governance.business.room.pojo.response.ClientLiveRoomSchedulesResponse;
import com.jiuyu.governance.business.room.pojo.bo.ScheduleConflictResult;
import com.jiuyu.governance.business.room.pojo.response.schedule.*;
import com.jiuyu.governance.business.room.service.LiveRoomScheduleManageService;
import com.jiuyu.governance.business.room.service.LiveRoomScheduleService;
import com.jiuyu.governance.business.room.handler.LiveRoomScheduleBatchGenerator;
import com.jiuyu.governance.business.room.service.handler.LiveRoomScheduleConflictHandler;
import com.jiuyu.governance.business.room.handler.LiveRoomScheduleRangeMergeHandler;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.plugins.oauth.data.BeforePermission;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 直播间排班管理服务实现类
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveRoomScheduleManageServiceImpl implements LiveRoomScheduleManageService, LiveRoomScheduleService {

    private final LiveRoomScheduleConflictHandler conflictHandler;
    private final WorkScheduleMapper workScheduleMapper;
    private final LiveRoomMapper roomMapper;
    private final ScheduleEmployeeMapper scheduleEmployeeMapper;
    private final EmployeeService employeeService;
    private final PositionService positionService;
    private final LiveRoomScheduleAttributeMapper scheduleAttributeMapper;
    private final SchedulePerformanceService schedulePerformanceService;

    /**
     * 新增直播间排班
     * <p>
     * 核心流程：
     * 1. 根据请求参数批量生成排班数据（支持按周期批量生成）
     * 2. 执行冲突校验（直播间维度 + 人员维度）
     * 3. 批量插入数据库（WorkSchedule + ScheduleEmployee）
     * </p>
     *
     * @param request  新增排班请求参数，包含直播间 ID、日期、时段、人员等信息
     * @param tenantId 租户 ID，用于数据隔离
     * @param userId   操作人 ID，用于记录创建人
     *
     * @return ApiResponse<Void> 操作结果，失败时返回具体错误信息
     *
     * @throws RuntimeException 冲突校验失败时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission(type = OauthConstant.LIVE_ROOM, dataId = "#request.liveRoomId")
    public ApiResponse<Void> addSchedule(LiveRoomScheduleAddRequest request, Long tenantId, Long userId) {
        log.info("[直播间排班 - 新增] 开始处理：liveRoomId={}, tenantId={}, userId={}",
            request.getLiveRoomId(), tenantId, userId);
        if (request.getBatchConfig() != null) {
            if (request.getBatchConfig().getEndDate().isBefore(request.getWorkDay())) {
                return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "结束日期不能早于开始日期");
            }
            if (Duration.between(request.getWorkDay().atStartOfDay(), request.getBatchConfig().getEndDate().atStartOfDay()).toDays() > 30) {
                return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "批量生成排班数据不能超过30天");
            }
            request.getBatchConfig().getCycleDays().stream().filter(cycleDay -> cycleDay < 1 || cycleDay > 7).findAny().ifPresent(cycleDay -> {
                log.warn("[直播间排班 - 新增] 批量生成排班数据周期天数有误：liveRoomId={}, cycleDay={}", request.getLiveRoomId(), cycleDay);
                throw new BusinessException(BizErrorCode.PARAM_INVALID, "批量排班数据周设定有误");
            });
        }

        // ========== 步骤 1: 批量生成排班数据 ==========
        LiveRoomScheduleBatchGenerator.GenerateResult generateResult = LiveRoomScheduleBatchGenerator.generate(request, tenantId, userId);
        List<WorkSchedule> schedules = generateResult.getSchedules();
        List<ScheduleEmployee> scheduleEmployees = generateResult.getScheduleEmployees();

        if (EmptyUtil.isEmpty(schedules)) {
            log.warn("[直播间排班 - 新增] 生成的排班数据为空：liveRoomId={}", request.getLiveRoomId());
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "生成的排班数据为空");
        }

        // ========== 步骤 2: 冲突校验 ==========
        ScheduleConflictResult conflictResult = conflictHandler.validateForAddSchedule(tenantId, schedules, scheduleEmployees);
        if (conflictResult.hasConflict()) {
            log.warn("[直播间排班 - 新增] 冲突校验，存在 {} 个冲突", conflictResult.getConflictSchedules().size());
            // 过滤出成功的数据
            LiveRoomScheduleBatchGenerator.GenerateResult successData = conflictResult.filterSuccessData(generateResult);
            // 如果没有成功的数据
            if (EmptyUtil.isEmpty(successData.getSchedules()) || EmptyUtil.isEmpty(successData.getScheduleEmployees())) {
                List<Long> conflictRomeIds = conflictResult.getConflictSchedules().stream().map(ScheduleConflictResult.ScheduleConflictInfo::getConflictWithRoomId).distinct().toList();
                Map<Long, String> roomNameMap = this.getRoomNameMap(conflictRomeIds);
//                // 构建冲突信息字符串
//                String conflictMsg = conflictResult.getConflictSchedules().stream()
//                    .map(c -> roomNameMap.getOrDefault(c.getConflictWithRoomId(), c.getConflictWithRoomId().toString()) + " -> " + c.getConflictType().getDescription() + ": " + c.getConflictReason())
//                    .distinct()
//                    .limit(5) // 最多显示5条冲突信息
//                    .collect(Collectors.joining("; "));
//                return ApiResponse.failed(BizErrorCode.TIME_CONFLICT.getCode(), "存在排班冲突：" + conflictMsg);
               StringBuilder errorMsg = new StringBuilder();
               roomNameMap.values().forEach(roomName -> errorMsg.append(roomName).append("; "));
               conflictResult.getConflictSchedules().stream()
                    .map(ScheduleConflictResult.ScheduleConflictInfo::getConflictReason).distinct()
                    .forEach(timeRange -> errorMsg.append(timeRange).append("; "));
               return ApiResponse.failed(BizErrorCode.TIME_CONFLICT.getCode(), "存在排班冲突：直播间-" + errorMsg);
            }
            schedules = successData.getSchedules();
            scheduleEmployees = successData.getScheduleEmployees();
        }



        // ========== 步骤 3: 批量插入数据库 ==========
        workScheduleMapper.batchInsert(schedules);
        if (EmptyUtil.isNotEmpty(scheduleEmployees)) {
            scheduleEmployeeMapper.batchInsert(scheduleEmployees);
        }

        log.info("[直播间排班 - 新增] 新增排班成功：liveRoomId={}, tenantId={}, 生成班次数量={}, 涉及人员数量={}",
            request.getLiveRoomId(), tenantId, schedules.size(), scheduleEmployees.size());

        return ApiResponse.success();
    }

    /**
     * 批量导入直播间排班（覆盖式）
     * <p>
     * 核心流程：
     * 1. 校验每个班次的 workDay 不早于今天
     * 2. 收集去重的 workDay
     * 3. 软删该直播间在日期范围内已有排班（整日覆盖）
     * 4. 批量插入新的排班与人员关联
     * 5. 排班时间冲突校验（新数据内部重叠 + 与未覆盖数据的员工/主播冲突），冲突则整体回滚
     * </p>
     *
     * @param request  导入请求，包含直播间 ID 与班次列表
     * @param tenantId 租户 ID，用于数据隔离
     * @param userId   操作人 ID，用于记录创建人
     *
     * @return ApiResponse<Void> 操作结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission(type = OauthConstant.LIVE_ROOM, dataId = "#request.liveRoomId")
    public ApiResponse<Void> importSchedule(LiveRoomScheduleImportRequest request, Long tenantId, Long userId) {
        List<ImportSchedule> importSchedules = request.getSchedules() == null ? List.of() : request.getSchedules();
        List<LocalDate> emptyWorkDays = request.getEmptyWorkDays() == null ? List.of() : request.getEmptyWorkDays();
        // 纯删除场景：schedules 为空但 emptyWorkDays 非空也允许提交；两者都空则拒绝
        if (importSchedules.isEmpty() && emptyWorkDays.isEmpty()) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "请至少导入一个班次");
        }
        log.info("[直播间排班 - 导入] 开始处理：liveRoomId={}, tenantId={}, userId={}, 班次数={}, 空日期数={}",
            request.getLiveRoomId(), tenantId, userId, importSchedules.size(), emptyWorkDays.size());

        // ========== 步骤 1: 校验排班日期不早于今天、且绝对开始时间晚于当前时间 1 小时 ==========
        LocalDate today = LocalDate.now();
        LocalDateTime minStart = LocalDateTime.now().plusHours(1);
        for (ImportSchedule schedule : importSchedules) {
            if (schedule.getWorkDay().isBefore(today)) {
                log.warn("[直播间排班 - 导入] 排班日期早于当天：liveRoomId={}, workDay={}",
                    request.getLiveRoomId(), schedule.getWorkDay());
                return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(),
                    "排班日期[" + schedule.getWorkDay() + "]比当天[" + today + "]还早，请重新选择排班日期");
            }
            LocalDateTime start = schedule.getWorkDay().atTime(schedule.getStartWork());
            if (!start.isAfter(minStart)) {
                log.warn("[直播间排班 - 导入] 排班开始时间需晚于当前 1 小时：liveRoomId={}, workDay={}, startWork={}",
                    request.getLiveRoomId(), schedule.getWorkDay(), schedule.getStartWork());
                return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(),
                    "排班[" + schedule.getWorkDay() + " " + schedule.getStartWork() + "]开始时间需晚于当前时间 1 小时，请调整后再提交");
            }
        }

        // ========== 步骤 2: 收集去重的 workDay（schedules ∪ emptyWorkDays）==========
        List<LocalDate> workDays = Stream.concat(
                importSchedules.stream().map(ImportSchedule::getWorkDay),
                emptyWorkDays.stream())
            .distinct()
            .toList();

        LocalDateTime now = LocalDateTime.now();
        // 已开始 = 绝对开始时间 ≤ 当前时间 + 1 小时（与「排班开始时间需晚于当前 1 小时」同一阈值）
        LocalDateTime startedThreshold = now.plusHours(1);

        // ========== 步骤 3: 校验已开始的排班、软删未开始的排班（已开始的保留不删） ==========
        List<WorkSchedule> existingSchedules = ChainWrappers.lambdaQueryChain(workScheduleMapper)
            .eq(WorkSchedule::getLiveRoomId, request.getLiveRoomId())
            .eq(WorkSchedule::getTenantId, tenantId)
            .in(WorkSchedule::getWorkDay, workDays)
            .eq(WorkSchedule::getIsDeleted, false)
            .list();
        List<WorkSchedule> startedSchedules = existingSchedules.stream()
            .filter(s -> !s.workRange().getStart().isAfter(startedThreshold))
            .toList();

        // 1) 导入的时间段与已开始的排班真正时间重叠 → 报错（不能覆盖已开始的时段）
        List<String> overlapStarted = new ArrayList<>();
        for (ImportSchedule schedule : importSchedules) {
            LocalDateTime s = schedule.getWorkDay().atTime(schedule.getStartWork());
            LocalDateTime e = schedule.getWorkDay().atTime(schedule.getEndWork());
            if (!e.isAfter(s)) {
                e = e.plusDays(1);
            }
            Range<LocalDateTime> newRange = new Range<>(s, e);
            for (WorkSchedule started : startedSchedules) {
                if (WorkTimeHandler.isOverlap(newRange, started.workRange())) {
                    overlapStarted.add(schedule.getWorkDay() + " " + schedule.getStartWork() + "-" + schedule.getEndWork()
                        + "（已存在 " + started.getWorkDay() + " " + started.parseStartWork() + "-" + started.parseEndWork() + "）");
                    break;
                }
            }
        }
        if (EmptyUtil.isNotEmpty(overlapStarted)) {
            String overlapMsg = overlapStarted.stream().limit(5).collect(Collectors.joining("、"));
            log.warn("[直播间排班 - 导入] 导入时间段与已开始的排班重叠：liveRoomId={}, 重叠数={}",
                request.getLiveRoomId(), overlapStarted.size());
            throw new BusinessException(BizErrorCode.PARAM_INVALID,
                "导入的时间段与已开始的排班记录重叠（" + overlapStarted.size() + " 处：" + overlapMsg + "），已开始的排班不能删除，请调整后再提交");
        }

        // 2) 软删未开始的排班（已开始的保留），人员关联按排班 ID 删除
        List<Long> deletableScheduleIds = existingSchedules.stream()
            .filter(s -> s.workRange().getStart().isAfter(startedThreshold))
            .map(WorkSchedule::getId)
            .toList();
        if (EmptyUtil.isNotEmpty(deletableScheduleIds)) {
            ChainWrappers.lambdaUpdateChain(workScheduleMapper)
                .eq(WorkSchedule::getLiveRoomId, request.getLiveRoomId())
                .eq(WorkSchedule::getTenantId, tenantId)
                .in(WorkSchedule::getId, deletableScheduleIds)
                .eq(WorkSchedule::getIsDeleted, false)
                .set(WorkSchedule::getIsDeleted, true)
                .set(WorkSchedule::getUpdateBy, userId)
                .set(WorkSchedule::getUpdateDate, now)
                .update();

            ChainWrappers.lambdaUpdateChain(scheduleEmployeeMapper)
                .in(ScheduleEmployee::getScheduleId, deletableScheduleIds)
                .eq(ScheduleEmployee::getIsDeleted, false)
                .set(ScheduleEmployee::getIsDeleted, true)
                .set(ScheduleEmployee::getUpdateBy, userId)
                .set(ScheduleEmployee::getUpdateDate, now)
                .update();
        }

        // ========== 步骤 4: 批量插入新的排班与人员关联 ==========
        List<WorkSchedule> schedules = new ArrayList<>();
        List<ScheduleEmployee> scheduleEmployees = new ArrayList<>();
        for (ImportSchedule schedule : importSchedules) {
            long scheduleId = IdWorker.getId();
            WorkSchedule workSchedule = new WorkSchedule();
            workSchedule.setId(scheduleId);
            workSchedule.setTenantId(tenantId);
            workSchedule.setLiveRoomId(request.getLiveRoomId());
            workSchedule.setWorkDay(schedule.getWorkDay());
            workSchedule.formatStartWork(schedule.getStartWork());
            workSchedule.formatEndWork(schedule.getEndWork());
            workSchedule.setScheduleDuration(schedule.getScheduleDuration());
            workSchedule.setRestDuration(schedule.getRestDuration());
            workSchedule.setRemark("");
            workSchedule.setCreateBy(userId);
            workSchedule.setUpdateBy(userId);
            workSchedule.setCreateDate(now);
            workSchedule.setUpdateDate(now);
            workSchedule.setIsDeleted(false);
            schedules.add(workSchedule);

            for (ImportEmployee employee : schedule.getEmployees()) {
                ScheduleEmployee emp = new ScheduleEmployee();
                emp.setId(IdWorker.getId());
                emp.setScheduleId(scheduleId);
                emp.setLiveRoomId(request.getLiveRoomId());
                emp.setEmployeeId(employee.getEmployeeId());
                emp.setPositionId(employee.getPositionId());
                emp.setWorkDay(schedule.getWorkDay());
                emp.setCreateBy(userId);
                emp.setUpdateBy(userId);
                emp.setCreateDate(now);
                emp.setUpdateDate(now);
                emp.setIsDeleted(false);
                scheduleEmployees.add(emp);
            }
        }

        // ========== 步骤 5: 排班时间冲突校验（新数据内部重叠 + 与未覆盖数据的员工/主播冲突） ==========
        ScheduleConflictResult conflictResult = conflictHandler.validateForAddSchedule(tenantId, schedules, scheduleEmployees);
        if (conflictResult.hasConflict()) {
            List<Long> conflictRoomIds = conflictResult.getConflictSchedules().stream()
                .map(ScheduleConflictResult.ScheduleConflictInfo::getConflictWithRoomId)
                .filter(id -> id != null)
                .distinct()
                .toList();
            Map<Long, String> roomNameMap = this.getRoomNameMap(conflictRoomIds);
            List<Long> conflictEmployeeIds = conflictResult.getConflictSchedules().stream()
                .map(ScheduleConflictResult.ScheduleConflictInfo::getEmployeeId)
                .filter(id -> id != null)
                .distinct()
                .toList();
            Map<Long, String> employeeNameMap = EmptyUtil.isEmpty(conflictEmployeeIds)
                ? Map.of() : employeeService.getNameMap(conflictEmployeeIds);
            // 按「冲突类型 + 员工 + 直播间」分组，同一员工同一直播间只拼一行，避免逐条重复
            Map<String, List<ScheduleConflictResult.ScheduleConflictInfo>> grouped = conflictResult.getConflictSchedules().stream()
                .collect(Collectors.groupingBy(
                    info -> (ScheduleConflictResult.ConflictType.EMPLOYEE.equals(info.getConflictType()) ? "E:" : "R:")
                        + info.getEmployeeId() + ":" + info.getConflictWithRoomId(),
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
            String conflictMsg = grouped.values().stream()
                .limit(5)
                .map(group -> {
                    ScheduleConflictResult.ScheduleConflictInfo first = group.get(0);
                    String roomName = first.getConflictWithRoomId() == null
                        ? "未知直播间"
                        : roomNameMap.getOrDefault(first.getConflictWithRoomId(), String.valueOf(first.getConflictWithRoomId()));
                    List<String> distinctTimes = group.stream()
                        .map(info -> info.getWorkDay() + " " + info.getTimeRange())
                        .distinct()
                        .toList();
                    String times = distinctTimes.size() > 10
                        ? String.join("、", distinctTimes.subList(0, 10)) + " 等 " + distinctTimes.size() + " 个时段"
                        : String.join("、", distinctTimes);
                    if (ScheduleConflictResult.ConflictType.EMPLOYEE.equals(first.getConflictType())) {
                        String empName = first.getEmployeeId() == null
                            ? "未知员工"
                            : employeeNameMap.getOrDefault(first.getEmployeeId(), String.valueOf(first.getEmployeeId()));
                        return "员工「" + empName + "」与直播间「" + roomName + "」已有排班重叠：" + times;
                    }
                    return "直播间「" + roomName + "」已有排班重叠：" + times;
                })
                .collect(Collectors.joining("；"));
            log.warn("[直播间排班 - 导入] 存在排班时间冲突：liveRoomId={}, 冲突数={}",
                request.getLiveRoomId(), conflictResult.getConflictSchedules().size());
            throw new BusinessException(BizErrorCode.TIME_CONFLICT,
                "导入排班存在时间冲突（" + conflictResult.getConflictSchedules().size() + " 处）：" + conflictMsg);
        }

        if (EmptyUtil.isNotEmpty(schedules)) {
            workScheduleMapper.batchInsert(schedules);
        }
        if (EmptyUtil.isNotEmpty(scheduleEmployees)) {
            scheduleEmployeeMapper.batchInsert(scheduleEmployees);
        }

        log.info("[直播间排班 - 导入] 导入成功：liveRoomId={}, 班次={}, 人员={}",
            request.getLiveRoomId(), schedules.size(), scheduleEmployees.size());
        return ApiResponse.success();
    }


    /**
     * 查询指定时间范围内的排班列表
     * <p>
     * 功能说明：
     * - 支持按时间范围查询（最大 31 天）
     * - 按工作日开始时间和开始时间升序排序
     * - 自动组装关联的人员信息（按岗位分组）
     * </p>
     *
     * @param request  查询请求参数，包含直播间 ID、开始日期、结束日期
     * @param tenantId 租户 ID，用于数据隔离
     *
     * @return List<LiveRoomScheduleResponse> 排班列表，包含完整的人员和岗位信息
     *
     * @throws BusinessException 时间范围超过 31 天时抛出异常
     */
    @BeforePermission(type = OauthConstant.LIVE_ROOM, dataId = "#request.liveRoomId")
    @Override
    public List<LiveRoomScheduleResponse> listRange(LiveRoomScheduleQueryRequest request, Long tenantId) {
        log.debug("[直播间排班 - 范围查询] 开始查询：liveRoomId={}, startDate={}, endDate={}",
            request.getLiveRoomId(), request.getStartDate(), request.getEndDate());

        // 校验时间范围（不超过 31 天）
        if (request.getStartDate() != null && request.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
            if (days < 0 || days > 31) {
                log.warn("[直播间排班 - 范围查询] 查询时间范围超过限制：{} 天", days);
                throw new BusinessException(BizErrorCode.PARAM_INVALID, "查询时间范围不能超过 31 天");
            }
        }
        // 批量查询排班列表
        List<WorkSchedule> schedules = new BatchQuery<>((limit, idx) -> {
            return ChainWrappers.lambdaQueryChain(workScheduleMapper)
                .gt(idx != null, WorkSchedule::getId, idx)
                .eq(request.getScheduleId() != null, WorkSchedule::getId, request.getScheduleId())
                .eq(WorkSchedule::getTenantId, tenantId)
                .eq(WorkSchedule::getLiveRoomId, request.getLiveRoomId())
                .and(request.getStartDate() != null, query -> {
                    query.ge(WorkSchedule::getWorkDay, request.getStartDate()).or(o ->
                        o.and(c -> c.apply("end_work <= start_work and DATE_ADD(work_day, INTERVAL 1 DAY) >= {0}", request.getStartDate())));
                })
                .le(request.getEndDate() != null, WorkSchedule::getWorkDay, request.getEndDate())
                .eq(WorkSchedule::getIsDeleted, false)
                .orderByAsc(WorkSchedule::getId)
                .last("limit " + limit)
                .list();
        }, WorkSchedule::getId).get()
            .stream().sorted(Comparator.comparing(WorkSchedule::getWorkDay).thenComparing(WorkSchedule::getStartWork)).toList();


        log.debug("[直播间排班 - 范围查询] 查询到排班数量：{}", schedules.size());
        if (EmptyUtil.isEmpty(schedules)) {
            return List.of();
        }
        List<LiveRoomScheduleResponse> scheduleResponses = schedules.stream().map(s -> toResponse(s, LiveRoomScheduleResponse::new)).toList();
        // 组装响应数据（包含关联的人员信息）
        populateEmployees(scheduleResponses, request.getPositionId(), request.getEmployeeId(), true);
        return LiveRoomScheduleRangeMergeHandler.mergeForListRange(scheduleResponses);
    }

    /**
     * 获取排班详情
     *
     * @param roomScheduleId 排班ID
     * @param tenantId       租户ID
     *
     * @return 排班详情
     */
    @Override
    public LiveRoomScheduleResponse getAdminDetail(long roomScheduleId, long tenantId) {
        WorkSchedule workSchedule = ChainWrappers.lambdaQueryChain(workScheduleMapper)
            .eq(WorkSchedule::getId, roomScheduleId)
            .eq(WorkSchedule::getTenantId, tenantId)
            .eq(WorkSchedule::getIsDeleted, false)
            .one();
        if (EmptyUtil.isEmpty(workSchedule)) {
            return null;
        }
        LiveRoomScheduleResponse response = this.toResponse(workSchedule, LiveRoomScheduleResponse::new);
        // 组装响应数据（包含关联的人员信息）
        populateEmployees(List.of(response), null, null, false);
        return response;
    }

    /**
     * 查询个人排班
     *
     * @param request    查询请求
     * @param tenantId   租户ID
     * @param employeeId 人员ID
     *
     * @return 分页数据
     */
    @Override
    public List<LiveRoomSchedulePageResponse> employeeScheduleList(EmployeeScheduleQueryRequest request, long tenantId, long employeeId) {
        // 校验时间范围（不超过 31 天）
        if (request.getStartDate() != null && request.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
            if (days < 0 || days > 31) {
                log.warn("[直播间排班 - 范围查询] 查询时间范围超过限制：{} 天", days);
                throw new BusinessException(BizErrorCode.PARAM_INVALID, "查询时间范围不能超过 31 天");
            }
        }
        // 批量查询排班列表
        List<EmployeeLiveRoomScheduleRawDto> scheduleRaws = new BatchQuery<>((limit, idx) -> workScheduleMapper.selectEmployeeSchedule(idx, employeeId, tenantId, request, limit), EmployeeLiveRoomScheduleRawDto::getId).get()
            .stream().sorted(Comparator.comparing(EmployeeLiveRoomScheduleRawDto::getWorkDay).thenComparing(EmployeeLiveRoomScheduleRawDto::getStartWork)).toList();
        if (EmptyUtil.isEmpty(scheduleRaws)) {
            return List.of();
        }
        List<LiveRoomSchedulePageResponse> schedules = scheduleRaws.stream().map(EmployeeLiveRoomScheduleRawDto::toPageInfo).toList();
        // 组装响应数据（包含关联的人员信息）
        populateEmployees(schedules, null, employeeId, false);
        schedules = LiveRoomScheduleRangeMergeHandler.mergeForEmployeeScheduleList(schedules);
        Complete.start(schedules)
            .build(LiveRoomSchedulePageResponse::getLiveRoomId, LiveRoomSchedulePageResponse::setLiveRoomName, this::getRoomNameMap)
            .then().over();
        return schedules;
    }


    /**
     * 查询主播排班
     *
     * @param platformType 直播平台类型
     * @param secUid       直播间唯一ID
     * @param startTime    开始时间
     * @param endTime      结束时间
     * @param tenantId     租户ID
     *
     * @return 排班数据
     */
    @Override
    public List<LiveRoomSchedulePageResponse> queryAnchorRoomSchedule(LivePlatformType platformType, String secUid, LocalDateTime startTime, LocalDateTime endTime, long tenantId) {
        if (startTime == null) {
            return List.of();
        }
        Long anchorPositionId = positionService.getDefaultPositionId(DefaultPosition.ANCHOR, tenantId);
        if (anchorPositionId == null) {
            return List.of();
        }
        Optional<LiveRoom> roomOptional = ChainWrappers.lambdaQueryChain(roomMapper)
            .eq(LiveRoom::getTenantId, tenantId)
            .eq(LiveRoom::getPlatform, platformType.getValue())
            .eq(LiveRoom::getSecUid, secUid)
            .eq(LiveRoom::getIsDeleted, false)
            .oneOpt();
        if (roomOptional.isEmpty()) {
            log.warn("[主播排班] query secUid 直播间不存在：{}, tenantId: {}", secUid, tenantId);
            return List.of();
        }
        LiveRoom liveRoom = roomOptional.get();
        Long roomId = liveRoom.getId();
        LocalDate startDay = startTime.toLocalDate();
        LocalDate endDay = endTime == null ? startDay : endTime.toLocalDate();
        if (ChronoUnit.DAYS.between(startDay, endDay) > 2) {
            throw new BusinessException(BizErrorCode.PARAM_INVALID, "查询时间范围不能超过 3 天");
        }

        List<RoomWorkScheduleDto> roomWorkScheduleDtos = workScheduleMapper.selectAnchorRoomSchedule(tenantId, roomId, startDay, endDay);
        if (EmptyUtil.isEmpty(roomWorkScheduleDtos)) {
            return List.of();
        }
        List<LiveRoomSchedulePageResponse> responses = roomWorkScheduleDtos.stream().map(dto -> {
            LiveRoomSchedulePageResponse response = new LiveRoomSchedulePageResponse();
            response.setLiveRoomName(liveRoom.getAnchorName());
            response.setId(dto.getId());
            response.setLiveRoomId(liveRoom.getId());
            response.setWorkDay(dto.getWorkDay());
            response.setStartWork(dto.parseStartWork());
            response.setEndWork(dto.parseEndWork());
            response.setScheduleDuration(dto.getScheduleDuration());
            response.setRestDuration(dto.getRestDuration());
            response.setRemark(dto.getRemark());
            return response;
        }).toList();
        // 组装响应数据（包含关联的人员信息）
        populateEmployees(responses, null, null, false);
        List<LiveRoomSchedulePageResponse> pageResponses = LiveRoomScheduleRangeMergeHandler.mergeForClientAnchorSchedules(responses, anchorPositionId);
        if (EmptyUtil.isNotEmpty(pageResponses)) {
            pageResponses.forEach(r -> {
                if (EmptyUtil.isNotEmpty(r.getPositions())) {
                    r.getPositions().forEach(p -> {
                        p.setAnchorPosition(Objects.equals(p.getPositionId(), anchorPositionId));
                    });
                }
            });
        }
        return pageResponses;
    }

    /**
     * 批量查询主播排班
     *
     * @param requests 批量查询请求集合
     * @param tenantId 租户ID
     *
     * @return 排班数据列表
     */
    @Override
    public List<LiveRoomScheduleAlignResponse> batchQueryAnchorRoomSchedule(List<AnchorQueryScheduleRequest> requests, long tenantId) {
        if (EmptyUtil.isEmpty(requests)) {
            return List.of();
        }
        Long anchorPositionId = positionService.getDefaultPositionId(DefaultPosition.ANCHOR, tenantId);
        if (anchorPositionId == null) {
            return List.of();
        }

        // 1. 过滤掉没传 startTime 的请求
        List<AnchorQueryScheduleRequest> validRequests = requests.stream()
            .filter(r -> r.getStartTime() != null && r.getEndTime() != null)
            .toList();

        if (validRequests.isEmpty()) {
            return List.of();
        }

        // 2. 收集需要查询的直播间 secUid 和 platform；同时按 platform_secUid 分组保存
        //    每个请求自身的 (voideId, 时间区间) — 后续在生成响应时按 voideId 1:N 展开。
        Map<Integer, Set<String>> platformSecUidMap = new HashMap<>();
        Map<String, List<AnchorReqRange>> reqRangeMap = new HashMap<>();
        for (AnchorQueryScheduleRequest req : validRequests) {
            platformSecUidMap.computeIfAbsent(req.getLivePlatformType(), k -> new HashSet<>()).add(req.getSecUid());
            String key = req.getLivePlatformType() + "_" + req.getSecUid();
            LocalDateTime reqStart = req.getStartTime();
            LocalDateTime reqEnd = req.getEndTime() != null ? req.getEndTime() : reqStart;
            reqRangeMap.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new AnchorReqRange(req.getVideoId(), new Range<>(reqStart, reqEnd)));
        }

        // 3. 批量查询直播间信息
        Map<String, LiveRoom> secUidRoomMap = new HashMap<>();
        platformSecUidMap.forEach((platform, secUids) -> {
            List<LiveRoom> rooms = ChainWrappers.lambdaQueryChain(roomMapper)
                .eq(LiveRoom::getTenantId, tenantId)
                .eq(LiveRoom::getPlatform, platform)
                .in(LiveRoom::getSecUid, secUids)
                .eq(LiveRoom::getIsDeleted, false)
                .list();
            rooms.forEach(room -> secUidRoomMap.put(platform + "_" + room.getSecUid(), room));
        });

        if (secUidRoomMap.isEmpty()) {
            return List.of();
        }

        Map<Long, LiveRoom> roomIdMap = secUidRoomMap.values().stream().collect(Collectors.toMap(LiveRoom::getId, r -> r, (a, b) -> a));

        // 4. 构建批量查询条件（去重以减少 SQL 的 OR 数量）
        Set<BatchQueryScheduleDto> queryDtos = new HashSet<>();
        validRequests.stream().collect(Collectors.groupingBy(req -> req.getLivePlatformType() + "_" + req.getSecUid())).forEach((key, reqs) -> {
            LiveRoom room = secUidRoomMap.get(key);
            if (room != null) {
                // 按开始时间排序
                List<AnchorQueryScheduleRequest> sortedReqs = reqs.stream()
                    .sorted(Comparator.comparing(AnchorQueryScheduleRequest::getStartTime))
                    .toList();

                // 合并时间范围：如果相邻时间段间隔 < 2天，则视为一条数据
                LocalDate mergedStartDay = null;
                LocalDate mergedEndDay = null;

                for (int i = 0; i < sortedReqs.size(); i++) {
                    AnchorQueryScheduleRequest req = sortedReqs.get(i);
                    LocalDate currentStartDay = req.getStartTime().toLocalDate();
                    LocalDate currentEndDay = req.getEndTime() == null ? currentStartDay : req.getEndTime().toLocalDate();

                    if (i == 0) {
                        // 第一条数据，初始化合并范围
                        mergedStartDay = currentStartDay;
                        mergedEndDay = currentEndDay;
                    } else {
                        // 检查与上一条数据的结束时间间隔是否 < 2天
                        long daysBetween = ChronoUnit.DAYS.between(mergedEndDay, currentStartDay);
                        if (daysBetween < 2) {
                            // 间隔 < 2天，合并到当前范围（扩展结束时间）
                            if (currentEndDay.isAfter(mergedEndDay)) {
                                mergedEndDay = currentEndDay;
                            }
                        } else {
                            // 间隔 >= 2天，保存当前的合并范围，开始新的范围
                            addQueryDtoWithValidation(queryDtos, room.getId(), mergedStartDay, mergedEndDay);
                            mergedStartDay = currentStartDay;
                            mergedEndDay = currentEndDay;
                        }
                    }
                }

                // 添加最后一个合并的时间范围
                if (mergedStartDay != null && mergedEndDay != null) {
                    addQueryDtoWithValidation(queryDtos, room.getId(), mergedStartDay, mergedEndDay);
                }
            }
        });

        if (queryDtos.isEmpty()) {
            return List.of();
        }

        // 5. 批量查询排班信息
        List<RoomWorkScheduleDto> scheduleDtos = workScheduleMapper.selectBatchAnchorRoomSchedule(queryDtos);
        if (EmptyUtil.isEmpty(scheduleDtos)) {
            return List.of();
        }

        // 6. 生成响应：每个 (排班, 命中videoId) 输出一行。
        //    一个直播间可能对应多条 videoId（同一直播间多次录制），所以同一 scheduleId 可能产生多条响应；
        //    用 (scheduleId, videoId) 去重，避免同 videoId 因请求时间段重叠而重复输出。
        List<LiveRoomScheduleAlignResponse> responses = new ArrayList<>();
        Map<Long, Set<String>> seenVideoIdsBySchedule = new HashMap<>();
        for (RoomWorkScheduleDto dto : scheduleDtos) {
            LiveRoom room = roomIdMap.get(dto.getLiveRoomId());
            if (room == null) {
                continue;
            }
            String roomKey = room.getPlatform() + "_" + room.getSecUid();
            List<AnchorReqRange> reqRanges = reqRangeMap.get(roomKey);
            if (EmptyUtil.isEmpty(reqRanges)) {
                continue;
            }

            // 排班的完整时间区间（处理跨天）— 每条排班只算一次
            LocalTime startWork = dto.parseStartWork();
            LocalTime endWork = dto.parseEndWork();
            LocalDateTime scheduleStart = LocalDateTime.of(dto.getWorkDay(), startWork);
            LocalDateTime scheduleEnd = LocalDateTime.of(dto.getWorkDay(), endWork);
            if (!endWork.isAfter(startWork)) {
                scheduleEnd = scheduleEnd.plusDays(1);
            }
            Range<LocalDateTime> scheduleRange = new Range<>(scheduleStart, scheduleEnd);

            Set<String> seenVideoIds = seenVideoIdsBySchedule.computeIfAbsent(dto.getId(), k -> new HashSet<>());
            for (AnchorReqRange reqRange : reqRanges) {
                if (!WorkTimeHandler.isOverlap(scheduleRange, reqRange.timeRange())) {
                    continue;
                }
                if (!seenVideoIds.add(reqRange.videoId())) {
                    continue;
                }
                LiveRoomScheduleAlignResponse response = new LiveRoomScheduleAlignResponse();
                response.setId(dto.getId());
                response.setLiveRoomId(dto.getLiveRoomId());
                response.setWorkDay(dto.getWorkDay());
                response.setStartWork(startWork);
                response.setEndWork(endWork);
                response.setScheduleDuration(dto.getScheduleDuration());
                response.setRestDuration(dto.getRestDuration());
                response.setRemark(dto.getRemark());
                response.setLivePlatformType(room.getPlatform());
                response.setSecUid(room.getSecUid());
                response.setVideoId(reqRange.videoId());
                responses.add(response);
            }
        }

        if (responses.isEmpty()) {
            return List.of();
        }

        // 8. 组装响应数据（包含关联的人员信息和直播间名称）
        populateEmployees(responses, null, null, false);
        responses = LiveRoomScheduleRangeMergeHandler.mergeForClientAnchorSchedules(responses, anchorPositionId);
        Complete.start(responses)
            .build(LiveRoomSchedulePageResponse::getLiveRoomId, LiveRoomSchedulePageResponse::setLiveRoomName, this::getRoomNameMap)
            .then().over();
        if (EmptyUtil.isNotEmpty(responses)) {
            responses.forEach(r -> {
                if (EmptyUtil.isNotEmpty(r.getPositions())) {
                    r.getPositions().forEach(p -> {
                        p.setAnchorPosition(Objects.equals(p.getPositionId(), anchorPositionId));
                    });
                }
            });
        }
        return responses;
    }


    /**
     * 获取排班员工
     *
     * @param scheduleId 排班ID
     *
     * @return 排班员工列表
     */
    @Override
    public List<RoomScheduleBo.WorkUser> listScheduleEmployees(long scheduleId) {
        List<ScheduleEmployee> list = ChainWrappers.lambdaQueryChain(scheduleEmployeeMapper)
            .eq(ScheduleEmployee::getScheduleId, scheduleId)
            .eq(ScheduleEmployee::getIsDeleted, false)
            .select(ScheduleEmployee::getEmployeeId, ScheduleEmployee::getPositionId)
            .list();
        if (EmptyUtil.isEmpty(list)) {
            return List.of();
        }
        // 组装该排班下的所有工作人员
        List<RoomScheduleBo.WorkUser> workUserList = list.stream().map(dto -> {
            RoomScheduleBo.WorkUser user = new RoomScheduleBo.WorkUser();
            user.setEmployeeId(dto.getEmployeeId());
            user.setPositionId(dto.getPositionId());
            return user;
        }).toList();

        Complete.start(workUserList)
            .build(RoomScheduleBo.WorkUser::getEmployeeId, RoomScheduleBo.WorkUser::setEmployeeName, employeeService::getNameMap)
            .then()
            .build(RoomScheduleBo.WorkUser::getPositionId, (u, p) -> {
                u.setPositionCode(p.getPositionCode());
                u.setPositionName(p.getName());
            }, positionService::getPositionMap)
            .then().over();
        return workUserList;
    }

    /**
     * 分页查询排班列表
     * <p>
     * 功能说明：
     * - 支持分页查询，默认按工作时间和开始时间降序排序
     * - 自动填充每个排班的人员信息
     * - 补全岗位名称和员工名称
     * </p>
     *
     * @param request  分页查询请求参数，包含直播间 ID、分页参数、时间范围等
     * @param tenantId 租户 ID，用于数据隔离
     *
     * @return PageData<LiveRoomScheduleResponse> 分页数据，包含排班列表和总数
     */
    @Override
    public PageData<LiveRoomSchedulePageResponse> page(LiveRoomSchedulePageRequest request, Long tenantId) {
        log.debug("[直播间排班 - 分页查询] 开始查询：liveRoomId={}, pageNum={}, pageSize={}",
            request.getLiveRoomId(), request.getPage(), request.getLimit());

        // 执行分页查询
        PageData<WorkSchedule> pageData = CustomPage.execute(request, (page, req) -> {
            return ChainWrappers.lambdaQueryChain(workScheduleMapper)
                .eq(WorkSchedule::getTenantId, tenantId)
                .eq(req.getLiveRoomId() != null, WorkSchedule::getLiveRoomId, req.getLiveRoomId())
                .in(EmptyUtil.isNotEmpty(req.getRoomIds()) && req.getLiveRoomId() == null, WorkSchedule::getLiveRoomId, req.getRoomIds())
                .and(request.getStartDate() != null, query -> {
                    query.ge(WorkSchedule::getWorkDay, request.getStartDate()).or(o ->
                        o.and(c -> c.apply("end_work <= start_work and DATE_ADD(work_day, INTERVAL 1 DAY) >= {0}", request.getStartDate())));
                })
                .le(req.getEndDate() != null, WorkSchedule::getWorkDay, req.getEndDate())
                .eq(WorkSchedule::getIsDeleted, false)
                .orderByDesc(WorkSchedule::getWorkDay, WorkSchedule::getStartWork)
                .page(page);
        });

        // 转换为响应对象
        PageData<LiveRoomSchedulePageResponse> responsePageData = pageData.conversion(s -> toResponse(s, LiveRoomSchedulePageResponse::new));

        // 填充人员信息
        if (EmptyUtil.isNotEmpty(responsePageData.getList())) {
            populateEmployees(responsePageData.getList(), request.getPositionId(), request.getEmployeeId(), false);
            Complete.start(responsePageData.getList())
                .build(LiveRoomSchedulePageResponse::getLiveRoomId, LiveRoomSchedulePageResponse::setLiveRoomName, this::getRoomNameMap)
                .then().over();
        }
        log.debug("[直播间排班 - 分页查询] 查询完成：记录数={}", responsePageData.getTotalCount());

        return responsePageData;
    }


    /**
     * 向指定排班添加人员
     * <p>
     * 核心流程：
     * 1. 获取并校验排班（确保未开始）
     * 2. 检查人员是否已存在于该排班
     * 3. 创建人员排班关联关系
     * 4. 执行人员冲突校验
     * 5. 插入数据库
     * </p>
     *
     * @param request  添加人员请求参数，包含排班 ID、员工 ID、岗位 ID
     * @param tenantId 租户 ID，用于数据隔离
     * @param userId   操作人 ID，用于记录创建人
     *
     * @return ApiResponse<Void> 操作结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ApiResponse<Void> addEmployee(ScheduleAddEmployeeRequest request, Long tenantId, Long userId) {
        log.info("[直播间排班 - 添加人员] 开始处理：scheduleId={}, employeeId={}, positionId={}",
            request.getScheduleId(), request.getEmployeeId(), request.getPositionId());

        // 获取并校验排班（确保未开始）
        WorkSchedule schedule = getAndCheckUnstartedSchedule(request.getScheduleId(), tenantId, false);

        // 检查人员是否已经在排班中
        boolean exists = ChainWrappers.lambdaQueryChain(scheduleEmployeeMapper)
            .eq(ScheduleEmployee::getScheduleId, request.getScheduleId())
            .eq(ScheduleEmployee::getEmployeeId, request.getEmployeeId())
            .eq(ScheduleEmployee::getIsDeleted, false)
            .exists();
        if (exists) {
            log.warn("[直播间排班 - 添加人员] 人员已存在：scheduleId={}, employeeId={}",
                request.getScheduleId(), request.getEmployeeId());
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "该人员已在此排班中");
        }

        // 创建人员排班关联
        LocalDateTime now = LocalDateTime.now();
        ScheduleEmployee emp = new ScheduleEmployee();
        emp.setId(IdWorker.getId());
        emp.setScheduleId(schedule.getId());
        emp.setLiveRoomId(schedule.getLiveRoomId());
        emp.setEmployeeId(request.getEmployeeId());
        emp.setPositionId(request.getPositionId());
        emp.setWorkDay(schedule.getWorkDay());
        emp.setCreateBy(userId);
        emp.setUpdateBy(userId);
        emp.setCreateDate(now);
        emp.setUpdateDate(now);
        emp.setIsDeleted(false);

        if (needValidateConflict(schedule, now)) {
            // 校验人员冲突
            ScheduleConflictResult conflictResult = conflictHandler.validateForAddEmployee(tenantId, schedule, emp);
            if (conflictResult.hasConflict()) {
                log.warn("[直播间排班 - 添加人员] 冲突校验失败，存在 {} 个冲突", conflictResult.getConflictSchedules().size());
                List<Long> conflictRoomIds = conflictResult.getConflictSchedules().stream().map(ScheduleConflictResult.ScheduleConflictInfo::getConflictWithRoomId).distinct().toList();
                Map<Long, String> roomNameMap = this.getRoomNameMap(conflictRoomIds);
//                String conflictMsg = conflictResult.getConflictSchedules().stream()
//                    .map(c -> roomNameMap.getOrDefault(c.getConflictWithRoomId(), c.getConflictWithRoomId().toString()) + " -> " + c.getConflictType().getDescription() + ": " + c.getConflictReason())
//                    .findFirst()
//                    .orElse("未知冲突");
//                return ApiResponse.failed(BizErrorCode.TIME_CONFLICT.getCode(), "存在排班冲突：" + conflictMsg);
                StringBuilder errorMsg = new StringBuilder();
                roomNameMap.values().forEach(roomName -> errorMsg.append(roomName).append("; "));
                conflictResult.getConflictSchedules().stream()
                    .map(ScheduleConflictResult.ScheduleConflictInfo::getConflictReason).distinct()
                    .forEach(timeRange -> errorMsg.append(timeRange).append("; "));
                return ApiResponse.failed(BizErrorCode.TIME_CONFLICT.getCode(), "存在排班冲突：直播间" + errorMsg);
            }
        }


        // 插入数据库
        scheduleEmployeeMapper.insert(emp);
        tryUpdateSchedulePerformanceStaff(schedule, tenantId, userId, now);
        log.info("[直播间排班 - 添加人员] 添加成功：scheduleId={}, employeeId={}",
            request.getScheduleId(), request.getEmployeeId());
        return ApiResponse.success();
    }

    /**
     * 添加人员时，是否需要执行人员冲突校验
     */
    static boolean needValidateConflict(WorkSchedule schedule, LocalDateTime now) {
        if (schedule == null || now == null) {
            return false;
        }
        Range<LocalDateTime> range = schedule.workRange();
        if (range == null || range.getStart() == null) {
            return false;
        }
        return range.getStart().isAfter(now);
    }

    /**
     * 移除指定排班中的人员
     * <p>
     * 核心流程：
     * 1. 获取并校验排班（确保未开始）
     * 2. 获取人员排班关联关系
     * 3. 删除人员排班关联关系
     * 4. 执行人员冲突校验
     * 5. 删除人员排班关联关系
     * </p>
     *
     * @param schedule  移除人员请求参数，包含排班 ID、员工 ID
     * @param tenantId 租户 ID，用于数据隔离
     * @param userId   操作人 ID，用于记录创建人
     *
     */
    private void tryUpdateSchedulePerformanceStaff(WorkSchedule schedule, Long tenantId, Long userId, LocalDateTime now) {
        if (schedule == null || tenantId == null || userId == null || now == null) {
            return;
        }
        Range<LocalDateTime> workRange = schedule.workRange();
        if (workRange == null || workRange.getEnd() == null || !workRange.getEnd().isBefore(now)) {
            return;
        }

        List<ScheduleEmployee> employees = ChainWrappers.lambdaQueryChain(scheduleEmployeeMapper)
            .eq(ScheduleEmployee::getScheduleId, schedule.getId())
            .eq(ScheduleEmployee::getIsDeleted, false)
            .list();

        List<Long> positionIds = employees.stream()
            .map(ScheduleEmployee::getPositionId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        List<Long> employeeIds = employees.stream()
            .map(ScheduleEmployee::getEmployeeId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<Long, String> positionNameMap = EmptyUtil.isEmpty(positionIds) ? Map.of() : positionService.getPositionNameMap(positionIds);
        Map<Long, String> employeeNameMap = EmptyUtil.isEmpty(employeeIds) ? Map.of() : employeeService.getNameMap(employeeIds);

        List<SchedulePerformanceResponse.StaffInfo> staffInfoList = employees.stream()
            .map(e -> {
                SchedulePerformanceResponse.StaffInfo staffInfo = SchedulePerformanceResponse.StaffInfo.builder()
                    .positionId(e.getPositionId())
                    .positionName(positionNameMap.get(e.getPositionId()))
                    .employeeId(e.getEmployeeId())
                    .employeeName(employeeNameMap.get(e.getEmployeeId()))
                    .build();
                return staffInfo;
            })
            .toList();
        schedulePerformanceService.updateSchedulePerformanceStaff(staffInfoList, schedule.getId(), tenantId, userId);
    }

    /**
     * 移除排班中的人员
     * <p>
     * 核心流程：
     * 1. 获取并校验排班（确保未开始）
     * 2. 逻辑删除人员排班关联
     * 3. 如果排班下无其他人员，则自动删除排班
     * </p>
     *
     * @param request  移除人员请求参数，包含排班 ID、员工 ID
     * @param tenantId 租户 ID，用于数据隔离
     * @param userId   操作人 ID，用于记录更新人
     *
     * @return ApiResponse<Void> 操作结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ApiResponse<Void> removeEmployee(ScheduleRemoveEmployeeRequest request, Long tenantId, Long userId) {
        log.info("[直播间排班 - 移除人员] 开始处理：scheduleId={}, employeeId={}",
            request.getScheduleId(), request.getEmployeeId());

        // 获取并校验排班（确保未开始）
        WorkSchedule schedule = getAndCheckUnstartedSchedule(request.getScheduleId(), tenantId, false);

        // 逻辑删除人员排班关联
        LocalDateTime now = LocalDateTime.now();
        boolean update = ChainWrappers.lambdaUpdateChain(scheduleEmployeeMapper)
            .eq(ScheduleEmployee::getScheduleId, request.getScheduleId())
            .eq(ScheduleEmployee::getEmployeeId, request.getEmployeeId())
            .eq(ScheduleEmployee::getIsDeleted, false)
            .set(ScheduleEmployee::getIsDeleted, true)
            .set(ScheduleEmployee::getUpdateBy, userId)
            .set(ScheduleEmployee::getUpdateDate, now)
            .update();

        if (!update) {
            log.warn("[直播间排班 - 移除人员] 人员不在该排班中：scheduleId={}, employeeId={}",
                request.getScheduleId(), request.getEmployeeId());
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "人员不在该排班中");
        }

        // 如果该排班下没有人员了，删除排班
        long count = ChainWrappers.lambdaQueryChain(scheduleEmployeeMapper)
            .eq(ScheduleEmployee::getScheduleId, request.getScheduleId())
            .eq(ScheduleEmployee::getIsDeleted, false)
            .count();

        if (count == 0) {
            deleteScheduleInternal(schedule, userId);
            log.info("[直播间排班 - 移除人员] 排班已无人员，自动删除：scheduleId={}", request.getScheduleId());
        }

        tryUpdateSchedulePerformanceStaff(schedule, tenantId, userId, now);
        log.info("[直播间排班 - 移除人员] 移除成功：scheduleId={}, employeeId={}",
            request.getScheduleId(), request.getEmployeeId());
        return ApiResponse.success();
    }

    /**
     * 批量更换排班人员
     * <p>
     * 核心流程：
     * 1. 校验排班存在且为过去的时间（已结束）
     * 2. 校验更换前后人员ID无重复
     * 3. 查询排班现有人员
     * 4. 逐项处理：新增 / 修改岗位 / 替换人员 / 互换人员
     * 5. 批量执行数据库操作
     * </p>
     *
     * @param request  更换请求，包含排班ID和更换项列表
     * @param tenantId 租户ID
     * @param userId   操作人ID
     * @return ApiResponse<Void>
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ApiResponse<Void> replaceEmployees(ScheduleReplaceEmployeeRequest request, long tenantId, long userId) {
        log.info("[直播间排班 - 更换人员] 开始处理：scheduleId={}, items={}", request.getScheduleId(), request.getItems().size());

        // ========== 步骤 1: 校验排班存在且为过去的时间 ==========
        WorkSchedule schedule = getAndCheckPastSchedule(request.getScheduleId(), tenantId);

        List<ScheduleReplaceEmployeeRequest.ReplaceItem> items = request.getItems();

        // ========== 步骤 2: 校验 afterEmployeeId 无重复 ==========
        Set<Long> afterIdSet = new HashSet<>();
        for (ScheduleReplaceEmployeeRequest.ReplaceItem item : items) {
            if (!afterIdSet.add(item.getAfterEmployeeId())) {
                log.warn("[直播间排班 - 更换人员] 更换后的人员ID重复：{}", item.getAfterEmployeeId());
                return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "更换后的人员ID重复: " + item.getAfterEmployeeId());
            }
        }

        // ========== 步骤 3: 校验非空 beforeEmployeeId 无重复 ==========
        Set<Long> beforeIdSet = new HashSet<>();
        for (ScheduleReplaceEmployeeRequest.ReplaceItem item : items) {
            if (item.getBeforeEmployeeId() != null && !beforeIdSet.add(item.getBeforeEmployeeId())) {
                log.warn("[直播间排班 - 更换人员] 更换前的人员ID重复：{}", item.getBeforeEmployeeId());
                return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "更换前的人员ID重复: " + item.getBeforeEmployeeId());
            }
        }

        // ========== 步骤 4: 查询排班现有人员 ==========
        List<ScheduleEmployee> existingEmployees = ChainWrappers.lambdaQueryChain(scheduleEmployeeMapper)
            .eq(ScheduleEmployee::getScheduleId, schedule.getId())
            .eq(ScheduleEmployee::getIsDeleted, false)
            .list();

        Map<Long, ScheduleEmployee> byEmployeeId = existingEmployees.stream()
            .collect(Collectors.toMap(ScheduleEmployee::getEmployeeId, Function.identity()));

        // ========== 步骤 5: 确定互换人员（既出现在 before 又出现在 after 中的） ==========
        Set<Long> swapEmployeeIds = new HashSet<>(beforeIdSet);
        swapEmployeeIds.retainAll(afterIdSet);

        // ========== 步骤 6: 逐项构建变更计划 ==========
        LocalDateTime now = LocalDateTime.now();
        List<ScheduleEmployee> toUpdate = new ArrayList<>();
        List<ScheduleEmployee> toInsert = new ArrayList<>();
        List<Long> toDeleteIds = new ArrayList<>();

        for (ScheduleReplaceEmployeeRequest.ReplaceItem item : items) {
            Long beforeId = item.getBeforeEmployeeId();
            Long afterId = item.getAfterEmployeeId();
            Long afterPos = item.getAfterPositionId();

            if (beforeId == null) {
                // 新增人员：如果人员已存在则改岗位，否则新增记录
                ScheduleEmployee existingAfter = byEmployeeId.get(afterId);
                if (existingAfter != null) {
                    existingAfter.setPositionId(afterPos);
                    existingAfter.setUpdateBy(userId);
                    existingAfter.setUpdateDate(now);
                    toUpdate.add(existingAfter);
                } else {
                    ScheduleEmployee se = new ScheduleEmployee();
                    se.setId(IdWorker.getId());
                    se.setScheduleId(schedule.getId());
                    se.setLiveRoomId(schedule.getLiveRoomId());
                    se.setEmployeeId(afterId);
                    se.setPositionId(afterPos);
                    se.setWorkDay(schedule.getWorkDay());
                    se.setCreateBy(userId);
                    se.setUpdateBy(userId);
                    se.setCreateDate(now);
                    se.setUpdateDate(now);
                    se.setIsDeleted(false);
                    toInsert.add(se);
                }
            } else {
                // 替换人员：beforeId 必须存在
                ScheduleEmployee beforeRecord = byEmployeeId.get(beforeId);
                if (beforeRecord == null) {
                    log.warn("[直播间排班 - 更换人员] 更换前人员不在该排班中：{}", beforeId);
                    return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "人员不在该排班中: " + beforeId);
                }

                ScheduleEmployee existingAfter = byEmployeeId.get(afterId);
                if (existingAfter != null && !existingAfter.getId().equals(beforeRecord.getId())) {
                    if (swapEmployeeIds.contains(afterId)) {
                        // 互换场景：after 员工也是某个 item 的 before，直接改 before 记录
                        beforeRecord.setEmployeeId(afterId);
                        beforeRecord.setPositionId(afterPos);
                        beforeRecord.setUpdateBy(userId);
                        beforeRecord.setUpdateDate(now);
                        toUpdate.add(beforeRecord);
                    } else {
                        // after 已存在且不是互换，更新已有记录的岗位，删除 before 记录
                        existingAfter.setPositionId(afterPos);
                        existingAfter.setUpdateBy(userId);
                        existingAfter.setUpdateDate(now);
                        toUpdate.add(existingAfter);
                        toDeleteIds.add(beforeRecord.getId());
                    }
                } else if (existingAfter != null && existingAfter.getId().equals(beforeRecord.getId())) {
                    // before 和 after 是同一个人，仅修改岗位
                    beforeRecord.setPositionId(afterPos);
                    beforeRecord.setUpdateBy(userId);
                    beforeRecord.setUpdateDate(now);
                    toUpdate.add(beforeRecord);
                } else {
                    // after 不存在于排班中，直接修改 before 记录
                    beforeRecord.setEmployeeId(afterId);
                    beforeRecord.setPositionId(afterPos);
                    beforeRecord.setUpdateBy(userId);
                    beforeRecord.setUpdateDate(now);
                    toUpdate.add(beforeRecord);
                }
            }
        }

        // ========== 步骤 7: 批量执行数据库操作 ==========
        if (EmptyUtil.isNotEmpty(toInsert)) {
            scheduleEmployeeMapper.batchInsert(toInsert);
        }
        if (EmptyUtil.isNotEmpty(toUpdate)) {
            // 去重：同一 ID 只保留最后一条
            Map<Long, ScheduleEmployee> dedupMap = toUpdate.stream()
                .collect(Collectors.toMap(ScheduleEmployee::getId, Function.identity(), (a, b) -> b));
            scheduleEmployeeMapper.updateBatch(new ArrayList<>(dedupMap.values()));
        }
        if (EmptyUtil.isNotEmpty(toDeleteIds)) {
            ChainWrappers.lambdaUpdateChain(scheduleEmployeeMapper)
                .in(ScheduleEmployee::getId, toDeleteIds)
                .eq(ScheduleEmployee::getIsDeleted, false)
                .set(ScheduleEmployee::getIsDeleted, true)
                .set(ScheduleEmployee::getUpdateBy, userId)
                .set(ScheduleEmployee::getUpdateDate, now)
                .update();
        }

        tryUpdateSchedulePerformanceStaff(schedule, tenantId, userId, now);
        log.info("[直播间排班 - 更换人员] 更换成功：scheduleId={}, insert={}, update={}, delete={}",
            request.getScheduleId(), toInsert.size(), toUpdate.size(), toDeleteIds.size());
        return ApiResponse.success();
    }

    /**
     * 修改排班信息
     * <p>
     * 核心流程：
     * 1. 获取并校验排班（确保未开始）
     * 2. 更新排班时间、时长等信息
     * 3. 查询该排班下的所有人员
     * 4. 重新执行人员冲突校验（因为时间可能变化）
     * 5. 更新数据库
     * </p>
     *
     * @param request  修改排班请求参数，包含排班 ID、开始/结束时间、时长等
     * @param tenantId 租户 ID，用于数据隔离
     * @param userId   操作人 ID，用于记录更新人
     *
     * @return ApiResponse<Void> 操作结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ApiResponse<Void> updateSchedule(ScheduleUpdateRequest request, Long tenantId, Long userId) {
        log.info("[直播间排班 - 修改排班] 开始处理：scheduleId={}, tenantId={}", request.getId(), tenantId);

        // 获取并校验排班（确保未开始）
        WorkSchedule schedule = getAndCheckUnstartedSchedule(request.getId(), tenantId, true);

        // 更新排班时间信息
        schedule.formatStartWork(request.getStartWork());
        schedule.formatEndWork(request.getEndWork());
        schedule.setScheduleDuration(request.getScheduleDuration());
        schedule.setRestDuration(request.getRestDuration());
        schedule.setUpdateBy(userId);
        schedule.setUpdateDate(LocalDateTime.now());

        // 查询该排班下的所有人员
        List<ScheduleEmployee> employees = ChainWrappers.lambdaQueryChain(scheduleEmployeeMapper)
            .eq(ScheduleEmployee::getScheduleId, schedule.getId())
            .eq(ScheduleEmployee::getIsDeleted, false)
            .list();
        log.debug("[直播间排班 - 修改排班] 查询到关联人员数量：{}", employees.size());

        // 如果修改了时间，需要重新校验该排班下的所有人员的冲突
        ScheduleConflictResult conflictResult = conflictHandler.validateForUpdateSchedule(tenantId, schedule, employees);
        if (conflictResult.hasConflict()) {
            log.warn("[直播间排班 - 修改排班] 冲突校验失败，存在 {} 个冲突", conflictResult.getConflictSchedules().size());
            List<Long> conflictRoomIds = conflictResult.getConflictSchedules().stream().map(ScheduleConflictResult.ScheduleConflictInfo::getConflictWithRoomId).distinct().toList();
            Map<Long, String> roomNameMap = this.getRoomNameMap(conflictRoomIds);
//            String conflictMsg = conflictResult.getConflictSchedules().stream()
//                .map(c -> roomNameMap.getOrDefault(c.getConflictWithRoomId(), c.getConflictWithRoomId().toString()) + " -> " + c.getConflictType().getDescription() + ": " + c.getConflictReason())
//                .findFirst()
//                .orElse("未知冲突");
//            return ApiResponse.failed(BizErrorCode.TIME_CONFLICT.getCode(), "存在排班冲突：" + conflictMsg);
            StringBuilder errorMsg = new StringBuilder();
            roomNameMap.values().forEach(roomName -> errorMsg.append(roomName).append("; "));
            conflictResult.getConflictSchedules().stream()
                .map(ScheduleConflictResult.ScheduleConflictInfo::getConflictReason).distinct()
                .forEach(timeRange -> errorMsg.append(timeRange).append("; "));
            return ApiResponse.failed(BizErrorCode.TIME_CONFLICT.getCode(), "存在排班冲突：直播间-" + errorMsg);
        }

        // 更新数据库
        workScheduleMapper.updateById(schedule);
        log.info("[直播间排班 - 修改排班] 修改成功：scheduleId={}", request.getId());
        return ApiResponse.success();
    }

    /**
     * 删除排班
     * <p>
     * 核心流程：
     * 1. 获取并校验排班（确保未开始）
     * 2. 逻辑删除排班记录
     * 3. 级联逻辑删除该排班下的所有人员排班关联
     * </p>
     *
     * @param scheduleId 排班 ID
     * @param tenantId   租户 ID，用于数据隔离
     * @param userId     操作人 ID，用于记录更新人
     *
     * @return ApiResponse<Void> 操作结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ApiResponse<Void> deleteSchedule(long scheduleId, Long tenantId, Long userId) {
        log.info("[直播间排班 - 删除排班] 开始处理：scheduleId={}, tenantId={}", scheduleId, tenantId);

        // 获取并校验排班（确保未开始）
        WorkSchedule schedule = getAndCheckUnstartedSchedule(scheduleId, tenantId, true);
        deleteScheduleInternal(schedule, userId);
        log.info("[直播间排班 - 删除排班] 删除成功：scheduleId={}", scheduleId);
        return ApiResponse.success();
    }

    /**
     * 获取并校验已结束的排班（用于更换人员等事后操作）
     *
     * @param scheduleId 排班ID
     * @param tenantId   租户ID
     * @return 排班实体
     * @throws BusinessException 排班不存在或未结束时抛出异常
     */
    private WorkSchedule getAndCheckPastSchedule(Long scheduleId, Long tenantId) {
        WorkSchedule schedule = ChainWrappers.lambdaQueryChain(workScheduleMapper)
            .eq(WorkSchedule::getId, scheduleId)
            .eq(WorkSchedule::getTenantId, tenantId)
            .eq(WorkSchedule::getIsDeleted, false)
            .one();

        if (schedule == null) {
            log.warn("[获取排班] 排班不存在：scheduleId={}, tenantId={}", scheduleId, tenantId);
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "排班不存在");
        }

        if (!schedule.workRange().getEnd().isBefore(LocalDateTime.now())) {
            log.warn("[获取排班] 排班尚未结束，无法操作：scheduleId={}, endTime={}",
                scheduleId, schedule.workRange().getEnd());
            throw new BusinessException(BizErrorCode.INVALID_STATE, "仅支持更换已结束的排班人员");
        }

        return schedule;
    }

    /**
     * 获取并校验未开始的排班
     * <p>
     * 核心逻辑：
     * 1. 根据 ID 和租户查询排班
     * 2. 校验排班是否存在
     * 3. 校验排班开始时间是否至少提前 3 小时（防止修改/删除已开始的排班）
     * </p>
     *
     * @param scheduleId 排班 ID
     * @param tenantId   租户 ID
     * @param checkTime   是否检查时间，默认为 true
     * @return 排班实体
     *
     * @throws BusinessException 排班不存在或已开始时抛出异常
     */
    private WorkSchedule getAndCheckUnstartedSchedule(Long scheduleId, Long tenantId, boolean checkTime) {
        log.debug("[获取排班] 查询排班：scheduleId={}, tenantId={}", scheduleId, tenantId);

        WorkSchedule schedule = ChainWrappers.lambdaQueryChain(workScheduleMapper)
            .eq(WorkSchedule::getId, scheduleId)
            .eq(WorkSchedule::getTenantId, tenantId)
            .eq(WorkSchedule::getIsDeleted, false)
            .one();

        if (schedule == null) {
            log.warn("[获取排班] 排班不存在：scheduleId={}, tenantId={}", scheduleId, tenantId);
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "排班不存在");
        }
        if (checkTime) {
            // 修改删除时，排班开始时间必须至少提前 3 个小时
            if (schedule.workRange().getStart().isBefore(LocalDateTime.now().minusHours(3))) {
                log.warn("[获取排班] 排班已开始，无法操作：scheduleId={}, startTime={}",
                    scheduleId, schedule.workRange().getStart());
                throw new BusinessException(BizErrorCode.INVALID_STATE, "无法操作已开始的排班");
            }
        }
        return schedule;
    }

    /**
     * 删除排班内部方法
     * <p>
     * 核心逻辑：
     * 1. 逻辑删除排班记录
     * 2. 逻辑删除该排班下的所有人员排班关联
     * </p>
     *
     * @param schedule 排班实体
     * @param userId   操作人 ID
     */
    private void deleteScheduleInternal(WorkSchedule schedule, Long userId) {
        log.debug("[删除排班 - 内部] 开始删除：scheduleId={}", schedule.getId());

        LocalDateTime now = LocalDateTime.now();

        // 逻辑删除排班记录
        schedule.setIsDeleted(true);
        schedule.setUpdateBy(userId);
        schedule.setUpdateDate(now);
        workScheduleMapper.updateById(schedule);

        // 逻辑删除关联的人员排班
        ChainWrappers.lambdaUpdateChain(scheduleEmployeeMapper)
            .eq(ScheduleEmployee::getScheduleId, schedule.getId())
            .eq(ScheduleEmployee::getIsDeleted, false)
            .set(ScheduleEmployee::getIsDeleted, true)
            .set(ScheduleEmployee::getUpdateBy, userId)
            .set(ScheduleEmployee::getUpdateDate, now)
            .update();

        log.debug("[删除排班 - 内部] 删除完成：scheduleId={}", schedule.getId());
    }


    /**
     * 转换为响应对象
     *
     * @param schedule 排班实体
     *
     * @return 响应对象
     */
    private <T extends LiveRoomScheduleResponse> T toResponse(WorkSchedule schedule, Supplier<T> initializer) {
        T response = initializer.get();
        response.setId(schedule.getId());
        response.setLiveRoomId(schedule.getLiveRoomId());
        response.setWorkDay(schedule.getWorkDay());
        response.setStartWork(schedule.parseStartWork());
        response.setEndWork(schedule.parseEndWork());
        response.setScheduleDuration(schedule.getScheduleDuration());
        response.setRestDuration(schedule.getRestDuration());
        response.setRemark(schedule.getRemark());
        return response;
    }


    /**
     * 获取直播间的名称
     *
     * @param roomIds 直播间 ID 列表
     *
     * @return 直播间的名称映射
     */
    private Map<Long, String> getRoomNameMap(List<Long> roomIds) {
        if (EmptyUtil.isEmpty(roomIds)) {
            return Map.of();
        }
        return ChainWrappers.lambdaQueryChain(roomMapper)
            .eq(LiveRoom::getIsDeleted, false)
            .in(LiveRoom::getId, roomIds)
            .select(LiveRoom::getId, LiveRoom::getAnchorName)
            .list()
            .stream()
            .collect(Collectors.toMap(LiveRoom::getId, LiveRoom::getAnchorName));
    }

    /**
     * 填充排班的人员信息
     * <p>
     * 核心逻辑：
     * 1. 批量查询所有排班的人员关联数据
     * 2. 查询直播间的岗位配置属性（用于确定岗位展示顺序）
     * 3. 按岗位分组人员，并按照配置的岗位顺序排序
     * 4. 批量补全岗位名称和员工名称
     * </p>
     *
     * @param responses  排班响应列表
     * @param positionId 岗位 ID
     * @param employeeId 员工 ID
     */
    private <T extends LiveRoomScheduleResponse> void populateEmployees(List<T> responses, Long positionId, Long employeeId, boolean loadoPositionOptions) {
        if (EmptyUtil.isEmpty(responses)) {
            return;
        }

        // ========== 步骤 1: 批量查询人员关联数据 ==========
        List<Long> scheduleIds = responses.stream().map(LiveRoomScheduleResponse::getId).toList();
        boolean queryScheduleEmployees = true;
        // 判断是否还需要查询排班人员
        if (responses.get(0) instanceof LiveRoomSchedulePageResponse r) {
            queryScheduleEmployees = r.getEmployeeId() == null;
        }
        List<ScheduleEmployee> scheduleEmployees;
        if (queryScheduleEmployees) {
            scheduleEmployees = new BatchQuery<>((limit, idx) -> {
                return ChainWrappers.lambdaQueryChain(scheduleEmployeeMapper)
                    .gt(idx != null, ScheduleEmployee::getId, idx)
                    .in(ScheduleEmployee::getScheduleId, scheduleIds)
                    .eq(positionId != null, ScheduleEmployee::getPositionId, positionId)
                    .eq(employeeId != null, ScheduleEmployee::getEmployeeId, employeeId)
                    .eq(ScheduleEmployee::getIsDeleted, false)
                    .orderByAsc(ScheduleEmployee::getId)
                    .last("limit " + limit)
                    .list();
            }, ScheduleEmployee::getId).get();
        } else {
            // 直接从响应中获取排班人员
            scheduleEmployees = responses.stream().map(r -> {
                if (r instanceof LiveRoomSchedulePageResponse employee) {
                    ScheduleEmployee scheduleEmployee = new ScheduleEmployee();
                    scheduleEmployee.setId(employee.getScheduleEmployeeId());
                    scheduleEmployee.setLiveRoomId(r.getLiveRoomId());
                    scheduleEmployee.setEmployeeId(employee.getEmployeeId());
                    scheduleEmployee.setPositionId(employee.getPositionId());
                    scheduleEmployee.setScheduleId(r.getId());
                    scheduleEmployee.setWorkDay(r.getWorkDay());
                    return scheduleEmployee;
                } else {
                    return null;
                }
            }).filter(Objects::nonNull).toList();
        }

        Map<Long, List<ScheduleEmployee>> scheduleEmployeeMap = scheduleEmployees.stream()
            .collect(Collectors.groupingBy(ScheduleEmployee::getScheduleId));

        // ========== 步骤 2: 查询直播间岗位配置 ==========
        // 用于确定岗位的展示顺序
        List<Long> liveRoomIds = responses.stream().map(LiveRoomScheduleResponse::getLiveRoomId).distinct().toList();
        Map<Long, List<Long>> roomPositionOptionsMap = new HashMap<>();
        if (loadoPositionOptions) {
            Map<Long, List<Long>> attributeMap = ChainWrappers.lambdaQueryChain(scheduleAttributeMapper)
                .in(LiveRoomScheduleAttribute::getId, liveRoomIds)
                .select(LiveRoomScheduleAttribute::getId, LiveRoomScheduleAttribute::getPositionOptions)
                .list().stream()
                .collect(Collectors.toMap(LiveRoomScheduleAttribute::getId, LiveRoomScheduleAttribute::getPositionOptions));
            if (EmptyUtil.isNotEmpty(attributeMap)) {
                roomPositionOptionsMap.putAll(attributeMap);
            }
        }


        // ========== 步骤 3: 按岗位分组并组装数据 ==========
        for (LiveRoomScheduleResponse response : responses) {
            List<ScheduleEmployee> emps = scheduleEmployeeMap.getOrDefault(response.getId(), new ArrayList<>());

            // 将人员按岗位分组
            Map<Long, List<ScheduleEmployee>> empByPositionMap = emps.stream()
                .collect(Collectors.groupingBy(ScheduleEmployee::getPositionId));

            // 获取该直播间的排班属性中的岗位列表作为基础顺序
            List<Long> positionOptions = new ArrayList<>();
            if (roomPositionOptionsMap.containsKey(response.getLiveRoomId())) {
                positionOptions.addAll(roomPositionOptionsMap.get(response.getLiveRoomId()));
            }
            // 合并实际排班中有但不在配置中的岗位（防御性处理）
            for (Long posId : empByPositionMap.keySet()) {
                if (!positionOptions.contains(posId)) {
                    positionOptions.add(posId);
                }
            }

            // 构建岗位列表
            List<SchedulePositionVO> positionVOs = new ArrayList<>();
            for (Long posId : positionOptions) {
                List<ScheduleEmployee> posEmps = empByPositionMap.get(posId);
                // 只要该岗位下有排班人员，或者属于默认配置，都可以展示
                SchedulePositionVO posVO = new SchedulePositionVO();
                posVO.setPositionId(posId);
                if (EmptyUtil.isNotEmpty(posEmps)) {
                    List<ScheduleEmployeeVO> empVOs = posEmps.stream().map(emp -> {
                        ScheduleEmployeeVO vo = new ScheduleEmployeeVO();
                        vo.setId(emp.getId());
                        vo.setScheduleId(emp.getScheduleId());
                        vo.setEmployeeId(emp.getEmployeeId());
                        vo.setPositionId(emp.getPositionId());
                        return vo;
                    }).toList();
                    posVO.setEmployees(empVOs);
                }
                positionVOs.add(posVO);
            }
            response.setPositions(positionVOs);
        }

        // ========== 步骤 4: 批量补全岗位名称 ==========
        Complete.start(responses)
            // 收集所有需要的岗位 ID
            .build(r -> 0L, (r, d) -> {
            }, ids -> positionService.getPositionOptions(ids).stream().collect(Collectors.toMap(LabelOption::getKey, Function.identity())))
            .filter(r -> EmptyUtil.isNotEmpty(r.getPositions()))
            .addColl(r -> r.getPositions().stream().map(SchedulePositionVO::getPositionId).toList(), (r, positionList) -> {
                Map<Long, String> positionMap = positionList.stream().collect(Collectors.toMap(LabelOption::getKey, LabelOption::getLabel));
                r.getPositions().forEach(pos -> pos.setPositionName(positionMap.getOrDefault(pos.getPositionId(), "未定义")));
            })
            // 补全员工的岗位名称
            .addColl(r -> r.getPositions().stream().filter(p -> EmptyUtil.isNotEmpty(p.getEmployees())).flatMap(p -> p.getEmployees().stream().map(ScheduleEmployeeVO::getPositionId)).distinct().toList(), (r, positionList) -> {
                Map<Long, String> positionMap = positionList.stream().collect(Collectors.toMap(LabelOption::getKey, LabelOption::getLabel));
                r.getPositions().forEach(pos -> {
                    if (EmptyUtil.isNotEmpty(pos.getEmployees())) {
                        pos.getEmployees().forEach(emp -> emp.setPositionName(positionMap.getOrDefault(emp.getPositionId(), "未定义")));
                    }
                });
            })
            .then()
            // ========== 步骤 5: 批量补全员工名称 ==========
            .build(r -> 0L, (r, d) -> {
            }, ids -> employeeService.getNameMap(ids).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Function.identity())))
            .filter(r -> EmptyUtil.isNotEmpty(r.getPositions()))
            .addColl(r -> r.getPositions().stream().filter(p -> EmptyUtil.isNotEmpty(p.getEmployees())).flatMap(p -> p.getEmployees().stream().map(ScheduleEmployeeVO::getEmployeeId)).distinct().toList(), (r, employeeList) -> {
                Map<Long, String> employeeMap = employeeList.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                r.getPositions().forEach(pos -> {
                    if (EmptyUtil.isNotEmpty(pos.getEmployees())) {
                        pos.getEmployees().forEach(emp -> emp.setEmployeeName(employeeMap.getOrDefault(emp.getEmployeeId(), "未定义")));
                    }
                });
            })
            .then()
            .over();

        log.debug("[填充人员信息] 完成：responseCount={}", responses.size());
    }


    /**
     * 获取直播间排班
     * <p>
     * 核心流程：
     * 1. 参数校验（时间范围、平台类型、直播间 ID 列表）
     * 2. 计算查询的日期范围（前后各扩展 1 天，避免边界遗漏）
     * 3. 转换岗位枚举为岗位 ID 列表
     * 4. 批量查询原始排班数据
     * 5. 按排班 ID 分组并过滤时间范围
     * 6. 组装返回结果（包含排班信息和人员列表）
     * </p>
     *
     * @param tenantId     租户 ID，用于数据隔离
     * @param platformType 直播平台类型，指定要查询的平台
     * @param secUidList   直播间唯一 ID 列表，筛选特定直播间
     * @param timeRanges   排班范围区间，支持多个时间段，会过滤出与这些时间段有重叠的排班
     * @param positions    岗位数组，可选参数，不传则查询所有岗位，传入则只查询指定岗位
     *
     * @return {@link List }<{@link RoomScheduleBo }> 排班业务对象列表，每个对象包含：
     *     - 直播间信息（房间 ID、平台类型、secUid）
     *     - 排班信息（排班 ID、工作日、开始/结束时间、时长）
     *     - 人员列表（员工 ID、岗位 ID）
     */
    @Override
    public List<RoomScheduleBo> getLiveSchedule(long tenantId, LivePlatformType platformType, Collection<String> secUidList, List<Range<LocalDateTime>> timeRanges, DefaultPosition... positions) {
        // ========== 步骤 1: 参数校验 ==========
        if (EmptyUtil.isEmpty(timeRanges)) {
            return List.of();
        }
        if (EmptyUtil.isEmpty(platformType) || EmptyUtil.isEmpty(secUidList)) {
            return List.of();
        }

        // ========== 步骤 2: 计算查询日期范围 ==========
        // 从时间范围中提取最小和最大日期，并前后各扩展 1 天，确保边界数据不丢失
        LocalDate minDate = timeRanges.stream().map(r -> r.getStart().toLocalDate()).min(LocalDate::compareTo).orElse(null);
        LocalDate maxDate = timeRanges.stream().map(r -> r.getEnd().toLocalDate()).max(LocalDate::compareTo).orElse(null);
        if (minDate != null) {
            minDate = minDate.minusDays(1);
        }
        if (maxDate != null) {
            maxDate = maxDate.plusDays(1);
        }

        // ========== 步骤 3: 转换岗位枚举为 ID 列表 ==========
        Integer platformVal = platformType.getValue();
        List<Long> positionIds = null;
        Map<Long, String>  positionCodeMap = new HashMap<>();
        if (positions != null && positions.length > 0) {
            // 获取默认岗位映射关系，将岗位枚举转换为实际岗位 ID
            Map<DefaultPosition, Long> defaultPositionMap = positionService.getDefaultPositionMap(List.of(positions), tenantId);
            positionIds = Arrays.stream(positions)
                .map(defaultPositionMap::get)
                .filter(Objects::nonNull)
                .toList();
            if (EmptyUtil.isEmpty(positionIds)) {
                return List.of();
            }
            defaultPositionMap.forEach((key, value) -> positionCodeMap.put(value, key.getValue()));
        }
        LocalDate finalMinDate = minDate;
        LocalDate finalMaxDate = maxDate;
        List<Long> finalPositionIds = positionIds;

        // ========== 步骤 4: 批量查询原始排班数据 最多10w条数据 ==========
        List<LiveScheduleRawDto> rawDtos = new BatchQuery<>((limit, idx) -> {
            return workScheduleMapper.selectRawLiveSchedule(idx, limit, tenantId, platformVal, secUidList, finalMinDate, finalMaxDate, finalPositionIds);
        }, LiveScheduleRawDto::getScheduleEmployeeId).get(null, 100000);
        if (EmptyUtil.isEmpty(rawDtos)) {
            return List.of();
        }
        if (EmptyUtil.isEmpty(positionCodeMap)) {
            List<Long> resultPositionIds = rawDtos.stream().map(LiveScheduleRawDto::getPositionId).distinct().toList();
            positionService.getPositionCodeMap(resultPositionIds).forEach(positionCodeMap::put);
        }


        // ========== 步骤 5: 按排班 ID 分组并过滤时间范围 ==========
        // 将原始数据按 scheduleId 分组，同一个排班的所有人员记录会聚合在一起
        Map<Long, List<LiveScheduleRawDto>> grouped = rawDtos.stream().collect(Collectors.groupingBy(LiveScheduleRawDto::getScheduleId));

        List<RoomScheduleBo> results = new ArrayList<>();
        for (Map.Entry<Long, List<LiveScheduleRawDto>> entry : grouped.entrySet()) {
            List<LiveScheduleRawDto> list = entry.getValue();
            LiveScheduleRawDto first = list.get(0);

            // 构建临时排班对象用于时间范围比较
            WorkSchedule tempSchedule = new WorkSchedule();
            tempSchedule.setWorkDay(first.getWorkDay());
            tempSchedule.setStartWork(first.getStartWork());
            tempSchedule.setEndWork(first.getEndWork());

            Range<LocalDateTime> scheduleRange = tempSchedule.workRange();

            // 检查该排班是否在请求的时间范围内，如果不在则跳过
            boolean overlap = timeRanges.stream().anyMatch(r -> WorkTimeHandler.isOverlap(scheduleRange, r));
            if (!overlap) {
                continue;
            }

            // ========== 步骤 6: 组装返回结果 ==========
            RoomScheduleBo bo = new RoomScheduleBo();
            bo.setRoomId(first.getLiveRoomId());
            bo.setSecUid(first.getSecUid());
            bo.setPlatformType(LivePlatformType.getByValue(first.getPlatformType()));
            bo.setScheduleId(first.getScheduleId());
            bo.setWorkDay(first.getWorkDay());
            bo.setStartWork(scheduleRange.getStart());
            bo.setEndWork(scheduleRange.getEnd());
            bo.setScheduleDuration(first.getScheduleDuration());
            bo.setRestDuration(first.getRestDuration());

            // 组装该排班下的所有工作人员
            List<RoomScheduleBo.WorkUser> workUsers = new ArrayList<>();
            for (LiveScheduleRawDto dto : list) {
                if (dto.getEmployeeId() != null && dto.getPositionId() != null) {
                    RoomScheduleBo.WorkUser user = new RoomScheduleBo.WorkUser();
                    user.setEmployeeId(dto.getEmployeeId());
                    user.setEmployeeName(dto.getEmployeeName());
                    user.setPositionId(dto.getPositionId());
                    user.setPositionCode(positionCodeMap.get(dto.getPositionId()));
                    workUsers.add(user);
                }
            }
            bo.setWorkUserList(workUsers);
            results.add(bo);
        }

        return results;
    }


    /**
     * 获取直播间排班
     *
     * @param day      排班日期
     * @param tenantId 租户ID
     *
     * @return {@link List }<{@link ClientLiveRoomSchedulesResponse }>
     */
    @Override
    public List<ClientLiveRoomSchedulesResponse> getLiveRoomSchedules(LocalDate day, long tenantId) {
        if (day == null) {
            return List.of();
        }
        Long anchorPositionId = positionService.getDefaultPositionId(DefaultPosition.ANCHOR, tenantId);
        if (anchorPositionId == null) {
            return List.of();
        }
        // 批量查询 最多10w条数据
        List<RoomWorkScheduleDto> responses = new BatchQuery<>((limit, idx) -> {
            return workScheduleMapper.selectClientSchedules(idx, limit, tenantId, day, anchorPositionId);
        }, RoomWorkScheduleDto::getId).get(null, 100000);
        if (EmptyUtil.isEmpty(responses)) {
            return List.of();
        }
        return responses.stream().map(dto -> {
            ClientLiveRoomSchedulesResponse response = new ClientLiveRoomSchedulesResponse();
            response.setRoomSchedulesId(dto.getId());
            response.setPlatformType(dto.getPlatformType());
            response.setSecUid(dto.getSecUid());
            response.setGovernanceRoomId(dto.getLiveRoomId());
            response.setWorkDay(dto.getWorkDay());
            response.setStartWork(dto.getWorkDay().atTime(dto.parseStartWork()));
            response.setEndWork(dto.getWorkDay().atTime(dto.parseEndWork()));
            response.setScheduleDuration(dto.getScheduleDuration());
            return response;
        }).toList();
    }

    /**
     * 获取直播间未来排班
     *
     * @param futureDay 未来排班日期
     * @param tenantId  租户ID
     *
     * @return 排班数据列表
     */
    @Override
    public List<ClientFuturePlanScheduleResponse> getLiveRoomFuturePlanSchedules(LocalDate futureDay, long tenantId) {
        if (futureDay == null) {
            return List.of();
        }
        Long anchorPositionId = positionService.getDefaultPositionId(DefaultPosition.ANCHOR, tenantId);
        if (anchorPositionId == null) {
            return List.of();
        }
        // 所有的直播间（用于辅助客户端采集数据，与本项目内部业务无关）
        Map<Long, String> roomMap = new BatchQuery<>((limit, idx) -> {
            return ChainWrappers.lambdaQueryChain(roomMapper)
                .select(LiveRoom::getId, LiveRoom::getSecUid)
                .gt(idx != null, LiveRoom::getId, idx)
                .eq(LiveRoom::getTenantId, tenantId)
                .eq(LiveRoom::getAccountStatus, AccountStatus.NORMAL)
                .eq(LiveRoom::getIsDeleted, false)
                .last("limit " + limit)
                .list();
        }, LiveRoom::getId).get(null, 10000).stream().collect(Collectors.toMap(LiveRoom::getId, LiveRoom::getSecUid));
        if (EmptyUtil.isEmpty(roomMap)) {
            return List.of();
        }
        LocalDate startTime = LocalDate.now();
        // 批量查询 最多10w条数据
        List<RoomWorkScheduleDto> responses = new BatchQuery<>((limit, idx) -> {
            return workScheduleMapper.getLiveRoomFuturePlanSchedules(idx, limit, tenantId, startTime, futureDay, anchorPositionId);
        }, RoomWorkScheduleDto::getId).get(null, 100000);

        Map<Long, List<ClientFuturePlanScheduleResponse>> responseMap;
        if (EmptyUtil.isEmpty(responses)) {
            responseMap = Map.of();
        } else {
            responseMap = responses.stream().collect(Collectors.groupingBy(RoomWorkScheduleDto::getLiveRoomId, Collectors.mapping(dto -> {
                 ClientFuturePlanScheduleResponse response = new ClientFuturePlanScheduleResponse();
                 response.setScheduleId(dto.getId());
                 response.setSecUid(dto.getSecUid());
                 LocalDateTime scheduleStartTime = dto.getWorkDay().atTime(dto.parseStartWork());
                 LocalDateTime scheduleEndTime = dto.getWorkDay().atTime(dto.parseEndWork());
                 if (!scheduleEndTime.isAfter(scheduleStartTime)) {
                     scheduleEndTime = scheduleEndTime.plusDays(1);
                 }
                 response.setStartTime(scheduleStartTime);
                 response.setEndTime(scheduleEndTime);
                 return response;
             }, Collectors.toList())));
        }
        // 当32天内没有排班时 虚拟一个排班出来 原因为让客户端能正常采集上报数据。 这段代码纯属辅助代码 跟本项目内部无关
        LocalDateTime virtualStart = LocalDateTime.now().plusYears(1);
        return roomMap.entrySet().stream().flatMap(entry -> {
            List<ClientFuturePlanScheduleResponse> responseList = responseMap.get(entry.getKey());
            if (EmptyUtil.isNotEmpty(responseList)) {
                return responseList.stream();
            }
            ClientFuturePlanScheduleResponse virtualPlan = new ClientFuturePlanScheduleResponse();
            // 使用直播间ID负数作为虚拟ID
            virtualPlan.setScheduleId(-entry.getKey());
            virtualPlan.setSecUid(entry.getValue());
            virtualPlan.setStartTime(virtualStart);
            virtualPlan.setEndTime(virtualStart.plusMinutes(10));
            return Stream.of(virtualPlan);
        }).toList();
    }

    /**
     * 获取直播间排班
     *
     * @param tenantId 租户ID
     * @param roomIds  直播间ID
     * @param range    时间范围
     *
     * @return {@link List }<{@link RoomSchedulesRawBo }>
     */
    @Override
    public List<RoomSchedulesRawBo> getLiveRoomSchedules(long tenantId, Collection<Long> roomIds, Range<LocalDate> range) {
        if (EmptyUtil.isEmpty(roomIds) || range == null) {
            log.warn("[直播间排班] query rooms schedules plan, roomId is empty or range time is null");
            return List.of();
        }
        List<WorkSchedule> workSchedules = new BatchQuery<>((limit, idx) -> {
            return ChainWrappers.lambdaQueryChain(workScheduleMapper)
                .select(WorkSchedule::getId, WorkSchedule::getLiveRoomId, WorkSchedule::getWorkDay, WorkSchedule::getStartWork, WorkSchedule::getEndWork, WorkSchedule::getScheduleDuration)
                .gt(idx != null, WorkSchedule::getId, idx)
                .eq(WorkSchedule::getIsDeleted, false)
                .eq(WorkSchedule::getTenantId, tenantId)
                .in(WorkSchedule::getLiveRoomId, roomIds)
                .ge(WorkSchedule::getWorkDay, range.getStart())
                .le(WorkSchedule::getWorkDay, range.getEnd())
                .last("limit " + limit)
                .list();
        }, WorkSchedule::getId).get(null, 100000);
        if (EmptyUtil.isEmpty(workSchedules)) {
            return List.of();
        }
        return workSchedules.stream().map(schedule -> {
            RoomSchedulesRawBo response = new RoomSchedulesRawBo();
            response.setRoomId(schedule.getLiveRoomId());
            response.setRoomSchedulesId(schedule.getId());
            response.setWorkDay(schedule.getWorkDay());
            response.setStartWork(schedule.parseStartWork());
            response.setEndWork(schedule.parseEndWork());
            response.setScheduleDuration(schedule.getScheduleDuration());
            return response;
        }).collect(Collectors.toList());
    }


    /**
     * 获取员工排班
     *
     * @param employeeIds 员工ID
     * @param range       时间范围
     *
     * @return {@link Map }<{@link Long }, {@link List }<{@link EmployeeLiveRoomScheduleRawDto }>>
     */
    @Override
    public Map<Long, List<RoomSchedulesRawBo>> getEmployeeSchedules(Collection<Long> employeeIds, Range<LocalDate> range) {
        if (EmptyUtil.isEmpty(employeeIds) || range == null) {
            log.warn("[员工排班] query employee schedules plan, employeeIds is empty or range time is null");
            return Map.of();
        }
        return new BatchQuery<>((limit, idx) -> {
            return workScheduleMapper.loadEmployeeSchedule(idx,  employeeIds, range.getStart(), range.getEnd(), limit);
        }, EmployeeLiveRoomScheduleRawDto::getId).get(null, 30000).stream().collect(Collectors.groupingBy(EmployeeLiveRoomScheduleRawDto::getEmployeeId, Collectors.mapping(dto -> {
            RoomSchedulesRawBo rawBo = new RoomSchedulesRawBo();
            rawBo.setRoomId(dto.getLiveRoomId());
            rawBo.setRoomSchedulesId(dto.getId());
            rawBo.setWorkDay(dto.getWorkDay());
            rawBo.setStartWork(WorkTimeHandler.parseTime(dto.getStartWork()));
            rawBo.setEndWork(WorkTimeHandler.parseTime(dto.getEndWork()));
            rawBo.setScheduleDuration(dto.getScheduleDuration());
            return rawBo;
        }, Collectors.toList())));
    }

    /**
     * 验证并添加查询DTO
     * <p>
     * 校验时间范围是否超过3天，如果超过则抛出异常；否则添加到查询集合中
     * </p>
     *
     * @param queryDtos   查询DTO集合
     * @param roomId      直播间ID
     * @param startDay    开始日期
     * @param endDay      结束日期
     */
    private void addQueryDtoWithValidation(Set<BatchQueryScheduleDto> queryDtos, Long roomId, LocalDate startDay, LocalDate endDay) {
        if (startDay == null || endDay == null) {
            return;
        }
        // 校验单个查询范围不能超过3天
//        if (ChronoUnit.DAYS.between(startDay, endDay) > 2) {
//            throw new BusinessException(BizErrorCode.PARAM_INVALID, "查询时间范围不能超过 3 天");
//        }
        queryDtos.add(new BatchQueryScheduleDto(roomId, startDay, endDay));
    }

    /**
     * batchQueryAnchorRoomSchedule 内部使用：把一个请求的 videoId 与其时间区间打包，
     * 便于按 platform_secUid 分组后在生成响应时一次性完成"区间匹配 + videoId 透传"。
     */
    private record AnchorReqRange(String videoId, Range<LocalDateTime> timeRange) {
    }

    /**
     * 导出直播间排班导入模板（矩阵式 .xlsx）
     * <p>
     * 模板结构：一个 sheet = 一个岗位（来自排班配置 positionOptions），行 = 时间段（由轮班时间 + 班次时长推导），
     * 列 = 日期（14 天 / 两周），姓名格 = 下拉选择 + 可手输（errorStyle=WARNING）。
     * </p>
     *
     * @param liveRoomId 直播间ID
     * @param tenantId   租户ID
     * @return Excel 文件字节
     */
    @Override
    public byte[] exportTemplate(Long liveRoomId, Long tenantId) {
        LiveRoomScheduleAttribute attribute = scheduleAttributeMapper.selectById(liveRoomId);
        if (attribute == null || EmptyUtil.isEmpty(attribute.getShiftOptions()) || EmptyUtil.isEmpty(attribute.getPositionOptions())
            || attribute.parseStartPlan() == null || attribute.parseEndPlan() == null) {
            throw new BusinessException(BizErrorCode.PARAM_INVALID, "请先完成排班配置（轮班时间、班次时长、直播岗位）后再下载模板");
        }

        List<String> timeSlots = buildTimeSlots(attribute);
        if (EmptyUtil.isEmpty(timeSlots)) {
            throw new BusinessException(BizErrorCode.PARAM_INVALID, "轮班时间过短，不足以切分出一个班次，请检查排班配置");
        }

        List<Employee> employees = employeeService.loadTenantAllEmployee(0L, 10000, List.of(tenantId));
        Map<Long, String> positionNameMap = positionService.getPositionNameMap(attribute.getPositionOptions());
        log.info("[排班模板] liveRoomId={}, tenantId={}, 员工总数={}, 岗位={}", liveRoomId, tenantId, employees.size(), attribute.getPositionOptions());

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildInstructionsSheet(workbook);

            for (Long positionId : attribute.getPositionOptions()) {
                String positionName = positionNameMap.getOrDefault(positionId, "已删除");
                List<String> names = employees.stream()
                    .filter(e -> Objects.equals(e.getPositionId(), positionId))
                    .map(Employee::getName)
                    .filter(EmptyUtil::isNotEmpty)
                    .distinct()
                    .toList();
                // 岗位下只列该岗位员工，不跨岗位兜底（主播岗位只出现主播，其他岗位同理）
                if (names.isEmpty()) {
                    log.warn("[排班模板] 岗位「{}」({}) 无匹配员工，该岗位下拉无内容", positionName, positionId);
                }

                // sheet 名不能用「-」等特殊符号：WPS 数据验证公式里含「-」的 sheet 名引用会解析失败，导致下拉列表为空
                String empSheetName = truncateSheetName("员工列表" + positionName);
                // 至少保留一行空数据源，保证岗位无员工时仍显示下拉框（只是内容为空）
                int nameCount = Math.max(names.size(), 1);
                // 下拉数据源：跨 sheet 直接引用隐藏的「员工列表」sheet（对齐 openpyxl 模板，命名区域非必需）
                String formula = "'" + empSheetName + "'!$A$1:$A$" + nameCount;

                // 先建排班 sheet（姓名格挂下拉），再建隐藏的员工列表 sheet 作为下拉数据源
                buildPositionSheet(workbook, positionName + "排班表", timeSlots, formula, 14);

                XSSFSheet empSheet = workbook.createSheet(empSheetName);
                for (int i = 0; i < nameCount; i++) {
                    empSheet.createRow(i).createCell(0).setCellValue(i < names.size() ? names.get(i) : "");
                }
                workbook.setSheetHidden(workbook.getSheetIndex(empSheet), true);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("排班模板生成失败", e);
        }
    }

    /**
     * 模板固定两个时间段行（HH:mm-HH:mm），避免按排班配置切分生成过多行
     */
    private List<String> buildTimeSlots(LiveRoomScheduleAttribute attribute) {
        return List.of("09:00-12:00", "14:00-18:00");
    }

    /**
     * 生成「填写说明」sheet（第一个 sheet，说明模板填写规则）
     */
    private void buildInstructionsSheet(XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet("填写说明");
        String[] lines = {
            "直播间排班导入说明",
            "",
            "1. 一个 Excel 文件 = 一个直播间（导入时在页面选择直播间）。",
            "2. 每个 工作表 = 一个岗位，行 = 时间段，列 = 日期（14 天）。",
            "3. 姓名格可下拉选择本岗位员工，也可直接手输（手输需与系统员工姓名一致）。",
            "4. 跨天时间段如 22:00-00:00 表示当天 22 点播到次日 0 点。",
            "5. 日期格式支持：2026-08-22、2026/8/22、2026年8月22日、8月22日 等。",
            "6. 时间格式支持：09:00-10:00、9:00~10:00、09:00至10:00 等（起止分隔 - ~ 至）。",
            "7. 请勿增删/修改表头、时间段、日期，否则导入会报错。"
        };
        for (int i = 0; i < lines.length; i++) {
            sheet.createRow(i).createCell(0).setCellValue(lines[i]);
        }
        sheet.setColumnWidth(0, 80 * 256);
    }

    /**
     * 生成一个岗位的排班矩阵 sheet
     * <p>
     * 结构：第 1 行 = 日期表头（days 天按 7 天一组拆成多个周块），第 1 列 = 时间段，
     * 其余格为姓名，挂下拉数据验证（跨 sheet 引用员工列表）+ 可手输（errorStyle=WARNING）。
     * </p>
     */
    private void buildPositionSheet(XSSFWorkbook workbook, String sheetName, List<String> timeSlots, String formula, int days) {
        XSSFSheet sheet = workbook.createSheet(truncateSheetName(sheetName));
        CellStyle headerStyle = createStyle(workbook, true, IndexedColors.GREY_25_PERCENT.getIndex(), true);
        CellStyle slotStyle = createStyle(workbook, true, (short) -1, true);
        CellStyle cellStyle = createStyle(workbook, false, (short) -1, true);

        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
        CellRangeAddressList ranges = new CellRangeAddressList();

        LocalDate start = LocalDate.now().plusDays(1);
        int weekBlocks = days / 7;
        int rowIdx = 0;
        for (int block = 0; block < weekBlocks; block++) {
            Row headerRow = sheet.createRow(rowIdx);
            Cell timeCell = headerRow.createCell(0);
            timeCell.setCellValue("时间");
            timeCell.setCellStyle(headerStyle);
            for (int d = 0; d < 7; d++) {
                LocalDate day = start.plusDays((long) block * 7 + d);
                Cell dateCell = headerRow.createCell(1 + d);
                dateCell.setCellValue(day.toString());
                dateCell.setCellStyle(headerStyle);
            }
            rowIdx++;

            for (String slot : timeSlots) {
                Row slotRow = sheet.createRow(rowIdx);
                Cell slotCell = slotRow.createCell(0);
                slotCell.setCellValue(slot);
                slotCell.setCellStyle(slotStyle);
                for (int d = 0; d < 7; d++) {
                    Cell cell = slotRow.createCell(1 + d);
                    cell.setCellStyle(cellStyle);
                    ranges.addCellRangeAddress(new CellRangeAddress(rowIdx, rowIdx, 1 + d, 1 + d));
                }
                rowIdx++;
            }
            rowIdx++;
        }

        DataValidation validation = helper.createValidation(constraint, ranges);
        // WPS 对 showDropDown=true 不兼容（下拉列表内容读不到），须关闭下拉箭头，对齐 openpyxl 模板的 showDropDown="0"
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.WARNING);
        validation.createErrorBox("提示", "该姓名不在员工下拉列表中，可继续输入，但导入时需与系统员工姓名一致");
        validation.createPromptBox("员工姓名", "点击下拉选择，或直接手输姓名");
        validation.setShowPromptBox(true);
        sheet.addValidationData(validation);

        sheet.setColumnWidth(0, 12 * 256);
        for (int d = 0; d < 7; d++) {
            sheet.setColumnWidth(1 + d, 12 * 256);
        }
    }

    /**
     * 创建单元格样式（居中 + 可选加粗/底色/边框）
     *
     * @param bgColor 背景色索引，-1 表示无底色
     */
    private CellStyle createStyle(XSSFWorkbook workbook, boolean bold, short bgColor, boolean border) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(bold);
        font.setFontName("微软雅黑");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        if (bgColor >= 0) {
            style.setFillForegroundColor(bgColor);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (border) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
        return style;
    }

    /**
     * 截断 sheet 名到 31 字符（Excel sheet 名最大长度限制）
     */
    private String truncateSheetName(String name) {
        return name.length() > 31 ? name.substring(0, 31) : name;
    }

}
