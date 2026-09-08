package com.jiuyu.governance.business.room.handler;

import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomSchedulePageResponse;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomScheduleResponse;
import com.jiuyu.governance.business.room.pojo.response.schedule.ScheduleEmployeeVO;
import com.jiuyu.governance.business.room.pojo.response.schedule.SchedulePositionVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * listRange / employeeScheduleList 返回数据的“同天同岗重叠消融”处理器（静态工具类）。
 * <p>
 * 设计目标：
 * 1) 仅在“同一天 + 同岗位(positionId)”维度处理重叠；
 * 2) 不拆分基准排班（基准排班保持原始 start/end，不会被切成多段返回）；
 * 3) 对满足条件的排班做“消融”：将该排班在该岗位下的人员合并到覆盖它的基准排班上，
 *    并从当前排班移除该岗位（若排班无任何岗位则从返回中移除）。
 * <p>
 * 说明：
 * - 该类不依赖外部资源，因此以静态方法提供能力，避免 Spring Bean 注入扩散。
 * - 该处理仅影响返回结构（展示层），不影响数据库中的排班/人员真实关系。
 * </p>
 *
 * @author HeHui
 * @date 2026-04-29
 */
public final class LiveRoomScheduleRangeMergeHandler {

    /**
     * 工具类构造器（禁止实例化）。
     */
    private LiveRoomScheduleRangeMergeHandler() {
    }

    @FunctionalInterface
    private interface GetFun<T, R> {
        R get(T data);
    }

    /**
     * 个人排班列表的同天同岗重叠消融（复用同一套核心算法）。
     *
     * @param schedules 原始排班列表（已 populateEmployees）
     *
     * @return 处理后的排班列表
     */
    public static List<LiveRoomSchedulePageResponse> mergeForEmployeeScheduleList(List<LiveRoomSchedulePageResponse> schedules) {
        return mergeForListRange(schedules);
    }

    /**
     * 范围查询列表的同天同岗重叠消融（核心入口，泛型化以复用 listRange 与 employeeScheduleList）。
     * <p>
     * 关键规则（同一天 + 同岗位）：
     * - 候选排班需满足：起点落入他人区间内；且不被任何单一排班完整包含；且可被“其它排班时间并集”完整覆盖；
     * - 满足条件则认为该排班在该岗位下为“可消融排班”，将其岗位人员合并到与之时间段有交集的保留排班同岗位下；
     * - 最后移除可消融排班的该岗位，若排班岗位为空则从结果中移除该排班。
     *
     * @param schedules 原始排班列表（已 populateEmployees）
     * @param <S>       具体返回类型（listRange: LiveRoomScheduleResponse；employeeScheduleList: LiveRoomSchedulePageResponse）
     *
     * @return 处理后的排班列表
     */
    public static <S extends LiveRoomScheduleResponse> List<S> mergeForListRange(List<S> schedules) {
        if (EmptyUtil.isEmpty(schedules)) {
            return List.of();
        }

        Map<Long, S> scheduleMap = schedules.stream()
            .filter(s -> s.getId() != null)
            .collect(Collectors.toMap(LiveRoomScheduleResponse::getId, Function.identity(), (a, b) -> a));

        Map<LocalDate, List<S>> byDay = schedules.stream()
            .filter(s -> s.getWorkDay() != null)
            .collect(Collectors.groupingBy(LiveRoomScheduleResponse::getWorkDay));

        for (Map.Entry<LocalDate, List<S>> dayEntry : byDay.entrySet()) {
            Map<Long, List<Long>> positionToScheduleIds = new HashMap<>();
            for (S schedule : dayEntry.getValue()) {
                if (EmptyUtil.isEmpty(schedule.getPositions()) || schedule.getId() == null) {
                    continue;
                }
                for (SchedulePositionVO position : schedule.getPositions()) {
                    if (position == null || position.getPositionId() == null || EmptyUtil.isEmpty(position.getEmployees())) {
                        continue;
                    }
                    positionToScheduleIds.computeIfAbsent(position.getPositionId(), k -> new ArrayList<>()).add(schedule.getId());
                }
            }

            for (Map.Entry<Long, List<Long>> posEntry : positionToScheduleIds.entrySet()) {
                Long positionId = posEntry.getKey();
                List<Long> scheduleIds = posEntry.getValue().stream().distinct().toList();
                if (scheduleIds.size() <= 1) {
                    continue;
                }

                Map<Long, TimeRange> rangeMap = scheduleIds.stream()
                    .map(scheduleMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(LiveRoomScheduleResponse::getId, LiveRoomScheduleRangeMergeHandler::toRange, (a, b) -> a));

                // 计算“可消融排班”：只消融候选排班，不拆分保留排班
                Set<Long> dissolveScheduleIds = new HashSet<>();
                for (Long candidateId : scheduleIds) {
                    TimeRange candidate = rangeMap.get(candidateId);
                    if (candidate == null) {
                        continue;
                    }

                    // others = 同天同岗下的其它排班区间（用于判断候选排班是否满足消融条件）
                    List<TimeRangeWithId> others = scheduleIds.stream()
                        .filter(id -> !Objects.equals(id, candidateId))
                        .map(id -> new TimeRangeWithId(id, rangeMap.get(id)))
                        .filter(r -> r.range() != null)
                        .toList();

                    if (EmptyUtil.isEmpty(others)) {
                        continue;
                    }

                    // 条件 1：候选排班的开始时间落在“至少一个其它排班”的时间范围内
                    if (!isStartInsideAny(candidate, others)) {
                        continue;
                    }

                    // 条件 2：候选排班不能被任意一条其它排班“完整包含”（否则不消融，避免影响完整排班语义）
                    if (isContainedByAny(candidate, others)) {
                        continue;
                    }

                    // 条件 3：候选排班必须能被“其它排班的时间并集”完整覆盖（否则无法正确分摊）
                    if (!isCoveredByUnion(candidate, others)) {
                        continue;
                    }

                    dissolveScheduleIds.add(candidateId);
                }

                if (dissolveScheduleIds.isEmpty()) {
                    continue;
                }

                Set<Long> keepScheduleIds = scheduleIds.stream()
                    .filter(id -> !dissolveScheduleIds.contains(id))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
                if (keepScheduleIds.isEmpty()) {
                    continue;
                }

                for (Long dissolveId : dissolveScheduleIds) {
                    S dissolveSchedule = scheduleMap.get(dissolveId);
                    if (dissolveSchedule == null) {
                        continue;
                    }
                    SchedulePositionVO dissolvePosition = findPosition(dissolveSchedule, positionId);
                    if (dissolvePosition == null || EmptyUtil.isEmpty(dissolvePosition.getEmployees())) {
                        continue;
                    }

                    TimeRange dissolveRange = rangeMap.get(dissolveId);
                    if (dissolveRange == null) {
                        continue;
                    }

                    boolean merged = false;
                    for (Long keepId : keepScheduleIds) {
                        S keepSchedule = scheduleMap.get(keepId);
                        if (keepSchedule == null) {
                            continue;
                        }
                        TimeRange keepRange = rangeMap.get(keepId);
                        if (keepRange == null) {
                            continue;
                        }
                        // 只对与可消融排班时间段有交集的“保留排班”做人员合并
                        if (!isOverlap(dissolveRange, keepRange)) {
                            continue;
                        }
                        SchedulePositionVO keepPosition = findPosition(keepSchedule, positionId);
                        if (keepPosition == null) {
                            continue;
                        }
                        mergeEmployees(keepPosition, dissolvePosition.getEmployees(), ScheduleEmployeeVO::getEmployeeId);
                        merged = true;
                    }

                    if (merged) {
                        // 合并成功后移除可消融排班的该岗位；如果该排班不再包含任何岗位，最终会被过滤掉
                        removePosition(dissolveSchedule, positionId);
                    }
                }
            }
        }

        // 移除岗位为空的排班（例如可消融排班只剩该岗位，岗位被移除后应从返回消失）
        return schedules.stream()
            // 移除岗位为空的排班（例如可消融排班只剩该岗位，岗位被移除后应从返回消失）
            .filter(s -> EmptyUtil.isNotEmpty(s.getPositions()))
            .sorted(Comparator.comparing(LiveRoomScheduleResponse::getWorkDay).thenComparing(LiveRoomScheduleResponse::getStartWork))
            .toList();
    }

    /**
     * 客户端“主播排班为主体”的数据合并（直播间维度）。
     * <p>
     * 处理目标（单直播间/多直播间均适用）：
     * - 主体：仅返回包含主播岗位的排班；
     * - 合并：将与主播排班时间段重叠的“非主播排班（不包含主播岗位）”中的岗位人员，
     *   合并到对应主播排班的岗位节点中（按 employeeId 去重）；
     * - 多主播同排班：本身属于同一排班记录，populateEmployees 后天然在同一个“主播岗位节点”下聚合，
     *   此处不做额外去重或拆分；
     * - 多直播间：按 liveRoomId 分组后分别处理，避免跨直播间误合并。
     * </p>
     *
     * @param schedules         原始排班列表（已 populateEmployees，positions/employees 已完整组装）
     * @param anchorPositionId  主播岗位ID（DefaultPosition.ANCHOR 对应租户岗位ID）
     * @param <S>               具体返回类型（queryAnchorRoomSchedule: LiveRoomSchedulePageResponse；batchQueryAnchorRoomSchedule: LiveRoomScheduleAlignResponse）
     *
     * @return 仅包含主播岗位的排班列表（已完成跨岗位人员合并）
     */
    public static <S extends LiveRoomSchedulePageResponse> List<S> mergeForClientAnchorSchedules(List<S> schedules, Long anchorPositionId) {
        if (EmptyUtil.isEmpty(schedules) || anchorPositionId == null) {
            return EmptyUtil.isEmpty(schedules) ? List.of() : schedules;
        }

        Map<Long, List<S>> byRoom = schedules.stream()
            .filter(s -> s != null && s.getLiveRoomId() != null)
            .collect(Collectors.groupingBy(LiveRoomSchedulePageResponse::getLiveRoomId));

        if (byRoom.isEmpty()) {
            return List.of();
        }

        List<S> results = new ArrayList<>();
        for (Map.Entry<Long, List<S>> roomEntry : byRoom.entrySet()) {
            List<S> roomSchedules = roomEntry.getValue();
            if (EmptyUtil.isEmpty(roomSchedules)) {
                continue;
            }

            List<S> anchorSchedules = roomSchedules.stream()
                .filter(s -> hasPosition(s, anchorPositionId))
                .sorted(Comparator.comparing(LiveRoomScheduleResponse::getWorkDay).thenComparing(LiveRoomScheduleResponse::getStartWork))
                .toList();
            if (anchorSchedules.isEmpty()) {
                continue;
            }

            List<S> nonAnchorSchedules = roomSchedules.stream()
                .filter(s -> !hasPosition(s, anchorPositionId))
                .toList();

            for (S anchorSchedule : anchorSchedules) {
                TimeRange anchorRange = toRange(anchorSchedule);
                if (anchorRange == null) {
                    continue;
                }

                for (S sourceSchedule : nonAnchorSchedules) {
                    TimeRange sourceRange = toRange(sourceSchedule);
                    if (sourceRange == null || !isOverlap(anchorRange, sourceRange)) {
                        continue;
                    }

                    // 仅合并非主播排班中的岗位人员：把 sourceSchedule 的岗位树并入 anchorSchedule
                    mergeNonAnchorPositions(anchorSchedule, sourceSchedule, anchorPositionId);
                }
            }

            results.addAll(anchorSchedules);
        }

        return results.stream()
            .sorted(Comparator
                .comparing(LiveRoomSchedulePageResponse::getLiveRoomId)
                .thenComparing(LiveRoomScheduleResponse::getWorkDay)
                .thenComparing(LiveRoomScheduleResponse::getStartWork))
            .toList();
    }

    /**
     * 将排班列表按天拆分。若排班的 endWork <= startWork（跨天），则拆分为两条：
     * <ul>
     *   <li>Day1: workDay 不变，startWork 保持，endWork = 23:59:59</li>
     *   <li>Day2: workDay + 1，startWork = 00:00:00，endWork = 原始 endWork</li>
     * </ul>
     * 拆分后两条记录共享相同的岗位与人员列表，不跨天的排班原样保留。
     *
     * @param schedules 原始排班列表
     * @param <S>       具体返回类型
     * @return 拆分后的排班列表（按 workDay + startWork 排序）
     */
    public static <S extends LiveRoomScheduleResponse> List<S> splitByDay(List<S> schedules) {
        if (EmptyUtil.isEmpty(schedules)) {
            return List.of();
        }
        List<S> result = new ArrayList<>(schedules.size());
        for (S schedule : schedules) {
            if (isCrossDay(schedule)) {
                result.add(buildDaySplit(schedule, schedule.getWorkDay(),
                    schedule.getStartWork(), LocalTime.of(23, 59, 59)));
                if (schedule.getEndWork().equals(LocalTime.MIDNIGHT)) {
                    continue;
                }
                result.add(buildDaySplit(schedule, schedule.getWorkDay().plusDays(1),
                    LocalTime.of(0, 0, 0), schedule.getEndWork()));
            } else {
                result.add(schedule);
            }
        }
        result.sort(Comparator.comparing(LiveRoomScheduleResponse::getWorkDay)
            .thenComparing(LiveRoomScheduleResponse::getStartWork));
        return result;
    }

    /**
     * 判断排班是否跨天（endWork <= startWork 且两者均非空）。
     */
    private static boolean isCrossDay(LiveRoomScheduleResponse schedule) {
        LocalTime start = schedule.getStartWork();
        LocalTime end = schedule.getEndWork();
        return start != null && end != null && !end.isAfter(start);
    }

    /**
     * 复制排班并赋值指定天的 workDay / startWork / endWork，岗位列表共享引用。
     */
    @SuppressWarnings("unchecked")
    private static <S extends LiveRoomScheduleResponse> S buildDaySplit(S source, LocalDate workDay,
                                                                         LocalTime startWork, LocalTime endWork) {
        try {
            S copy = (S) source.getClass().getDeclaredConstructor().newInstance();
            copy.setId(source.getId());
            copy.setLiveRoomId(source.getLiveRoomId());
            copy.setWorkDay(workDay);
            copy.setStartWork(startWork);
            copy.setEndWork(endWork);
            copy.setScheduleDuration(source.getScheduleDuration());
            copy.setRestDuration(source.getRestDuration());
            copy.setRemark(source.getRemark());
            if (source.getPositions() != null) {
                copy.setPositions(new ArrayList<>(source.getPositions()));
            }
            if (source instanceof LiveRoomSchedulePageResponse srcPage
                && copy instanceof LiveRoomSchedulePageResponse copyPage) {
                copyPage.setLiveRoomName(srcPage.getLiveRoomName());
                copyPage.setScheduleEmployeeId(srcPage.getScheduleEmployeeId());
                copyPage.setEmployeeId(srcPage.getEmployeeId());
                copyPage.setPositionId(srcPage.getPositionId());
            }
            return copy;
        } catch (Exception e) {
            throw new RuntimeException("拆分排班复制失败: id=" + source.getId(), e);
        }
    }

    /**
     * 在指定排班中查找某个岗位（positionId）的岗位节点。
     *
     * @param schedule   排班响应对象
     * @param positionId 岗位ID
     *
     * @return 岗位节点；不存在则返回 null
     */
    private static SchedulePositionVO findPosition(LiveRoomScheduleResponse schedule, Long positionId) {
        if (schedule == null || positionId == null || EmptyUtil.isEmpty(schedule.getPositions())) {
            return null;
        }
        for (SchedulePositionVO position : schedule.getPositions()) {
            if (position != null && Objects.equals(position.getPositionId(), positionId)) {
                return position;
            }
        }
        return null;
    }

    /**
     * 判断排班是否包含指定岗位节点。
     *
     * @param schedule   排班响应对象
     * @param positionId 岗位ID
     *
     * @return true 表示包含该岗位
     */
    private static boolean hasPosition(LiveRoomScheduleResponse schedule, Long positionId) {
        return findPosition(schedule, positionId) != null;
    }

    /**
     * 将 sourceSchedule 的“非主播岗位”岗位树合并进 targetAnchorSchedule。
     * <p>
     * 该方法只合并岗位与人员，不修改排班时间范围。
     * </p>
     *
     * @param targetAnchorSchedule 主体主播排班
     * @param sourceSchedule       重叠的非主播排班
     * @param anchorPositionId     主播岗位ID
     */
    private static void mergeNonAnchorPositions(
        LiveRoomSchedulePageResponse targetAnchorSchedule,
        LiveRoomSchedulePageResponse sourceSchedule,
        Long anchorPositionId
    ) {
        if (targetAnchorSchedule == null || sourceSchedule == null || anchorPositionId == null) {
            return;
        }
        if (EmptyUtil.isEmpty(sourceSchedule.getPositions())) {
            return;
        }
        if (targetAnchorSchedule.getPositions() == null) {
            targetAnchorSchedule.setPositions(new ArrayList<>());
        }

        for (SchedulePositionVO sourcePosition : sourceSchedule.getPositions()) {
            if (sourcePosition == null) {
                continue;
            }
            if (Objects.equals(sourcePosition.getPositionId(), anchorPositionId)) {
                continue;
            }
            if (EmptyUtil.isEmpty(sourcePosition.getEmployees())) {
                continue;
            }

            SchedulePositionVO targetPosition = findOrCreatePosition(targetAnchorSchedule, sourcePosition);
            mergeEmployees(targetPosition, sourcePosition.getEmployees(), ScheduleEmployeeVO::getEmployeeId);
        }
    }

    /**
     * 在目标排班中查找岗位节点，不存在则创建并追加。
     *
     * @param targetSchedule 目标排班
     * @param sourcePosition 源岗位节点（用于复制岗位ID/名称）
     *
     * @return 目标排班中的岗位节点（已存在或新建）
     */
    private static SchedulePositionVO findOrCreatePosition(LiveRoomSchedulePageResponse targetSchedule, SchedulePositionVO sourcePosition) {
        Long positionId = sourcePosition.getPositionId();
        SchedulePositionVO existing = findPosition(targetSchedule, positionId);
        if (existing != null) {
            return existing;
        }

        SchedulePositionVO created = new SchedulePositionVO();
        created.setPositionId(sourcePosition.getPositionId());
        created.setPositionName(sourcePosition.getPositionName());
        created.setEmployees(new ArrayList<>());
        targetSchedule.getPositions().add(created);
        return created;
    }

    /**
     * 从排班中移除某个岗位节点。
     * <p>
     * 当“可消融排班”的岗位人员已分摊合并到其它排班后，应移除该岗位以避免重复展示。
     * </p>
     *
     * @param schedule   排班响应对象
     * @param positionId 岗位ID
     */
    private static void removePosition(LiveRoomScheduleResponse schedule, Long positionId) {
        if (schedule == null || positionId == null || EmptyUtil.isEmpty(schedule.getPositions())) {
            return;
        }
        List<SchedulePositionVO> newPositions = schedule.getPositions().stream()
            .filter(p -> p != null && !Objects.equals(p.getPositionId(), positionId))
            .toList();
        schedule.setPositions(newPositions);
    }

    /**
     * 将源岗位的员工列表合并到目标岗位下（按 employeeId 去重）。
     *
     * @param target          目标岗位节点（保留排班）
     * @param sourceEmployees 源岗位员工列表（可消融排班）
     * @param employeeIdGetter 员工ID提取函数（函数式抽象，便于复用/测试）
     */
    private static void mergeEmployees(
        SchedulePositionVO target,
        List<ScheduleEmployeeVO> sourceEmployees,
        GetFun<ScheduleEmployeeVO, Long> employeeIdGetter
    ) {
        if (target == null || EmptyUtil.isEmpty(sourceEmployees) || employeeIdGetter == null) {
            return;
        }
        List<ScheduleEmployeeVO> targetEmployees = target.getEmployees() == null ? new ArrayList<>() : new ArrayList<>(target.getEmployees());
        Set<Long> existingIds = targetEmployees.stream()
            .map(ScheduleEmployeeVO::getEmployeeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        for (ScheduleEmployeeVO vo : sourceEmployees) {
            Long employeeId = employeeIdGetter.get(vo);
            if (employeeId == null || existingIds.contains(employeeId)) {
                continue;
            }
            existingIds.add(employeeId);
            targetEmployees.add(vo);
        }
        target.setEmployees(targetEmployees);
    }

    /**
     * 将排班的 workDay + startWork/endWork 转换为可计算的绝对时间区间。
     * <p>
     * 规则：
     * - endTime <= startTime 时视为跨天，结束时间自动 +1 天；
     * - 返回为半开区间语义的基础数据（具体重叠判断在 {@link #isOverlap(TimeRange, TimeRange)}）。
     * </p>
     *
     * @param schedule 排班响应对象
     *
     * @return 时间区间；若日期/时间缺失则返回 null
     */
    private static TimeRange toRange(LiveRoomScheduleResponse schedule) {
        LocalDate day = schedule.getWorkDay();
        LocalTime start = schedule.getStartWork();
        LocalTime end = schedule.getEndWork();
        if (day == null || start == null || end == null) {
            return null;
        }
        LocalDateTime startTime = LocalDateTime.of(day, start);
        LocalDateTime endTime = LocalDateTime.of(day, end);
        if (!endTime.isAfter(startTime)) {
            // 跨天：结束时间小于等于开始时间时，按次日结束处理
            endTime = endTime.plusDays(1);
        }
        return new TimeRange(startTime, endTime);
    }

    /**
     * 判断两个时间区间是否重叠（按半开区间 [start, end)）。
     *
     * @param a 区间A
     * @param b 区间B
     *
     * @return true 表示存在重叠
     */
    private static boolean isOverlap(TimeRange a, TimeRange b) {
        LocalDateTime maxStart = a.start().isAfter(b.start()) ? a.start() : b.start();
        LocalDateTime minEnd = a.end().isBefore(b.end()) ? a.end() : b.end();
        return maxStart.isBefore(minEnd);
    }

    /**
     * 判断候选排班的开始时间是否落入“任意一个其它排班”的时间范围内。
     * <p>
     * 用于限制“只消融起点落入他人区间的排班”，避免把基准排班误判为可消融对象。
     * </p>
     *
     * @param candidate 候选区间
     * @param others    其它排班区间列表
     *
     * @return true 表示落入至少一个区间
     */
    private static boolean isStartInsideAny(TimeRange candidate, List<TimeRangeWithId> others) {
        for (TimeRangeWithId other : others) {
            if (other.range().start().isBefore(candidate.start()) && candidate.start().isBefore(other.range().end())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断候选排班是否被某一条其它排班完整包含。
     * <p>
     * 若被单一排班完整包含，则不做消融（保留完整排班语义，避免不必要的“岗位迁移”）。
     * </p>
     *
     * @param candidate 候选区间
     * @param others    其它排班区间列表
     *
     * @return true 表示被至少一个区间完整包含
     */
    private static boolean isContainedByAny(TimeRange candidate, List<TimeRangeWithId> others) {
        for (TimeRangeWithId other : others) {
            if (!other.range().start().isAfter(candidate.start()) && !other.range().end().isBefore(candidate.end())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断候选排班是否能被“其它排班的时间并集”完整覆盖。
     * <p>
     * 仅当其它排班合并后的区间中存在一个区间能够覆盖 candidate，才允许消融；
     * 否则会出现“覆盖不完整导致无法分摊”的情况。
     * </p>
     *
     * @param candidate 候选区间
     * @param others    其它排班区间列表
     *
     * @return true 表示被并集完整覆盖
     */
    private static boolean isCoveredByUnion(TimeRange candidate, List<TimeRangeWithId> others) {
        List<TimeRange> ranges = others.stream().map(TimeRangeWithId::range).toList();
        List<TimeRange> merged = mergeRanges(ranges);
        for (TimeRange r : merged) {
            if (!r.start().isAfter(candidate.start()) && !r.end().isBefore(candidate.end())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 合并时间区间列表，输出按开始时间有序且互不重叠的区间集合。
     * <p>
     * 合并规则：
     * - 重叠或首尾相接的区间会合并成一个更大的区间；
     * - 该方法用于“并集覆盖”判定。
     * </p>
     *
     * @param ranges 原始区间列表
     *
     * @return 合并后的区间列表
     */
    private static List<TimeRange> mergeRanges(List<TimeRange> ranges) {
        if (EmptyUtil.isEmpty(ranges)) {
            return List.of();
        }
        List<TimeRange> sorted = ranges.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(TimeRange::start).thenComparing(TimeRange::end))
            .toList();
        List<TimeRange> merged = new ArrayList<>();
        TimeRange current = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            TimeRange next = sorted.get(i);
            if (!next.start().isAfter(current.end())) {
                // 发生重叠或首尾相接：扩大当前区间
                LocalDateTime end = current.end().isAfter(next.end()) ? current.end() : next.end();
                current = new TimeRange(current.start(), end);
            } else {
                // 不相交：结算当前区间，开启新区间
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    /**
     * 时间区间（绝对时间）。
     *
     * @param start 开始时间
     * @param end   结束时间
     */
    private record TimeRange(LocalDateTime start, LocalDateTime end) {
    }

    /**
     * 带排班ID的时间区间包装（用于日志/判定时保留来源）。
     *
     * @param scheduleId 排班ID
     * @param range      时间区间
     */
    private record TimeRangeWithId(Long scheduleId, TimeRange range) {
    }
}
