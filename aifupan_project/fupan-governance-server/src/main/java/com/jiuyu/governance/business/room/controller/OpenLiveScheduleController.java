package com.jiuyu.governance.business.room.controller;

import com.jiuyu.framework.oauth.client.annotation.APIKey;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.business.room.pojo.request.schedule.OpenAnchorScheduleQueryRequest;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomScheduleAlignResponse;
import com.jiuyu.governance.business.room.pojo.response.schedule.OpenAnchorScheduleResponse;
import com.jiuyu.governance.business.room.pojo.response.schedule.SchedulePositionVO;
import com.jiuyu.governance.business.room.service.LiveRoomScheduleService;
import com.jiuyu.governance.common.pojo.bo.IdName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 开放接口 - 直播间排班（主播）查询
 * <p>
 * 供外部应用（非同一应用、不同 IP）通过 {@code @APIKey} 调用。
 * 复用已有 {@link LiveRoomScheduleService#batchQueryAnchorRoomSchedule} 查询能力，
 * 在此仅做「按 videoId 聚合 + 抽取主播岗位人员 + 收集场次ID」的出参整形，
 * 不改动既有服务方法。
 * </p>
 *
 * @author HeHui
 * @date 2026-06-22
 */
@Slf4j
@RestController
@RequestMapping("/api/governance/live-schedule-open")
@RequiredArgsConstructor
@APIKey(clientName = "x-jiuyu-client-id")
public class OpenLiveScheduleController {

    private final LiveRoomScheduleService liveRoomScheduleService;

    private final EmployeeService employeeService;

    /**
     * 批量查询主播排班（按 videoId 聚合）
     *
     * @param request 查询请求（含身份字段 + 明细集合）
     *
     * @return 每个 videoId 对应的主播人员集合与场次ID集合
     */
    @PostMapping("/batch-query-anchor")
    public ApiResponse<List<OpenAnchorScheduleResponse>> batchQueryAnchor(@RequestBody @Validated OpenAnchorScheduleQueryRequest request) {
        log.info("[排班开放接口 - 批量查询主播排班] tenantId={}, userId={}, userType={}, items={}",
            request.getTenantId(), request.getUserId(), request.getUserType(),
            EmptyUtil.isEmpty(request.getItems()) ? 0 : request.getItems().size());

        // 数据隔离以 tenantId 为准（既有服务方法语义不变，仅按租户过滤）
        List<LiveRoomScheduleAlignResponse> aligns =
            liveRoomScheduleService.batchQueryAnchorRoomSchedule(request.getItems(), request.getTenantId());
        if (EmptyUtil.isEmpty(aligns)) {
            return ApiResponse.success(List.of());
        }

        return ApiResponse.success(aggregateByVideoId(aligns));
    }

    /**
     * 按 videoId 聚合：抽取主播岗位人员（id+name，按 id 去重）并收集命中的场次ID。
     */
    private List<OpenAnchorScheduleResponse> aggregateByVideoId(List<LiveRoomScheduleAlignResponse> aligns) {
        // 保持入参出现顺序
        Map<String, OpenAnchorScheduleResponse> byVideoId = new LinkedHashMap<>();
        // 去重辅助：videoId -> 已收集的员工ID / 场次ID
        Map<String, Set<Long>> seenAnchorIds = new HashMap<>();
        Map<String, Set<Long>> seenScheduleIds = new HashMap<>();

        for (LiveRoomScheduleAlignResponse align : aligns) {
            String videoId = align.getVideoId();
            if (EmptyUtil.isEmpty(videoId)) {
                continue;
            }

            OpenAnchorScheduleResponse agg = byVideoId.computeIfAbsent(videoId, k -> {
                OpenAnchorScheduleResponse r = new OpenAnchorScheduleResponse();
                r.setVideoId(k);
                r.setAnchors(new ArrayList<>());
                r.setScheduleIds(new ArrayList<>());
                return r;
            });
            Set<Long> anchorIdSet = seenAnchorIds.computeIfAbsent(videoId, k -> new HashSet<>());
            Set<Long> scheduleIdSet = seenScheduleIds.computeIfAbsent(videoId, k -> new  HashSet<>());

            // 收集场次ID
            if (align.getId() != null && scheduleIdSet.add(align.getId())) {
                agg.getScheduleIds().add(align.getId());
            }

            // 抽取主播岗位（anchorPosition=true）下的人员
            if (EmptyUtil.isEmpty(align.getPositions())) {
                continue;
            }
            for (SchedulePositionVO position : align.getPositions()) {
                if (position == null || !Boolean.TRUE.equals(position.getAnchorPosition()) || EmptyUtil.isEmpty(position.getEmployees())) {
                    continue;
                }
                position.getEmployees().stream()
                    .filter(e -> e != null && e.getEmployeeId() != null)
                    .filter(e -> anchorIdSet.add(e.getEmployeeId()))
                    .forEach(e -> agg.getAnchors().add(new OpenAnchorScheduleResponse.AnchorInfo(e.getEmployeeId(), e.getEmployeeName())));
            }
        }

        return new ArrayList<>(byVideoId.values());
    }


    /**
     * 获取员工名称列表
     *
     * @param ids 员工ID列表
     * @return {@link ApiResponse }<{@link List }<{@link IdName }>>
     */
    @PostMapping("/get-employee-name-list")
    public ApiResponse<List<IdName>> getEmployeeNameList(@RequestBody List<Long> ids) {
        List<IdName> employeeNameList = employeeService.getNameMap(ids).entrySet().stream().map(e -> new IdName(e.getKey(), e.getValue())).collect(Collectors.toList());
        return ApiResponse.success(employeeNameList);
    }
}
