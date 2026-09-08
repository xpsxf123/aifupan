package com.jiuyu.governance.business.room.service;


import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.Range;
import com.jiuyu.governance.business.rbac.pojo.constants.DefaultPosition;
import com.jiuyu.governance.business.room.pojo.bo.EmployeeLiveRoomScheduleRawDto;
import com.jiuyu.governance.business.room.pojo.bo.RoomScheduleBo;
import com.jiuyu.governance.business.room.pojo.bo.RoomSchedulesRawBo;
import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.business.room.pojo.request.schedule.AnchorQueryScheduleRequest;
import com.jiuyu.governance.business.room.pojo.request.schedule.EmployeeScheduleQueryRequest;
import com.jiuyu.governance.business.room.pojo.request.schedule.ScheduleReplaceEmployeeRequest;
import com.jiuyu.governance.business.room.pojo.response.ClientFuturePlanScheduleResponse;
import com.jiuyu.governance.business.room.pojo.response.ClientLiveRoomSchedulesResponse;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomScheduleAlignResponse;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomSchedulePageResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 直播间排班服务
 *
 * @author HeHui
 * @date 2026-03-25 16:54
 */
public interface LiveRoomScheduleService {


    /**
     * 获取直播间排班
     *
     * @param tenantId     租户ID
     * @param platformType 直播平台类型
     * @param secUidList   直播间唯一ID列表
     * @param startTime    开始时间
     * @param endTime      结束时间
     * @param positions    岗位 不传查所有
     *
     * @return {@link List }<{@link RoomScheduleBo }>
     */
    default List<RoomScheduleBo> getRangeTimeLiveSchedule(long tenantId, LivePlatformType platformType, Collection<String> secUidList, LocalDateTime startTime, LocalDateTime endTime, DefaultPosition... positions) {
        return getLiveSchedule(tenantId, platformType, secUidList, List.of(new Range<>(startTime, endTime)), positions);
    }

    /**
     * 获取直播间排班
     *
     * @param tenantId     租户ID
     * @param platformType 直播平台类型
     * @param secUidList   直播间唯一ID列表
     * @param day          排班日期
     * @param positions    岗位 不传查所有
     *
     * @return {@link List }<{@link RoomScheduleBo }>
     */
    default List<RoomScheduleBo> getDayLiveSchedule(long tenantId, LivePlatformType platformType, Collection<String> secUidList, LocalDate day, DefaultPosition... positions) {
        return getLiveSchedule(tenantId, platformType, secUidList, List.of(new Range<>(day.atStartOfDay(), day.atTime(LocalTime.of(23, 59, 59)))), positions);
    }


    /**
     * 获取直播间排班
     *
     * @param tenantId     租户ID
     * @param platformType 直播平台类型
     * @param secUidList   直播间唯一ID列表
     * @param timeRanges   排班范围区间
     * @param positions    岗位 不传查所有
     *
     * @return {@link List }<{@link RoomScheduleBo }>
     */
    List<RoomScheduleBo> getLiveSchedule(long tenantId, LivePlatformType platformType, Collection<String> secUidList, List<Range<LocalDateTime>> timeRanges, DefaultPosition... positions);

    /**
     * 获取直播间排班
     *
     * @param day      排班日期
     * @param tenantId 租户ID
     *
     * @return {@link List }<{@link ClientLiveRoomSchedulesResponse }>
     */
    List<ClientLiveRoomSchedulesResponse> getLiveRoomSchedules(LocalDate day, long tenantId);

    /**
     * 获取直播间未来排班
     *
     * @param futureDay  未来排班日期
     * @param tenantId   租户ID
     *
     * @return 排班数据列表
     */
    List<ClientFuturePlanScheduleResponse> getLiveRoomFuturePlanSchedules(LocalDate futureDay, long tenantId);

    /**
     * 获取直播间排班
     *
     * @param tenantId 租户ID
     * @param roomIds  直播间ID
     * @param range    时间范围
     *
     * @return {@link List }<{@link RoomSchedulesRawBo }>
     */
    List<RoomSchedulesRawBo> getLiveRoomSchedules(long tenantId, Collection<Long> roomIds, Range<LocalDate> range);



    /**
     * 获取员工排班
     *
     * @param employeeIds 员工ID
     * @param range       时间范围
     *
     * @return {@link Map }<{@link Long }, {@link List }<{@link EmployeeLiveRoomScheduleRawDto }>>
     */
    Map<Long, List<RoomSchedulesRawBo>> getEmployeeSchedules(Collection<Long> employeeIds, Range<LocalDate> range);


    /**
     * 查询个人排班
     *
     * @param request  查询请求
     * @param tenantId 租户ID
     * @param employeeId 人员ID
     *
     * @return 分页数据
     */
    List<LiveRoomSchedulePageResponse> employeeScheduleList(EmployeeScheduleQueryRequest request, long tenantId, long employeeId);

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
    List<LiveRoomSchedulePageResponse> queryAnchorRoomSchedule(LivePlatformType platformType, String secUid, LocalDateTime startTime, LocalDateTime endTime, long tenantId);

    /**
     * 批量查询主播排班
     *
     * @param requests 批量查询请求集合
     * @param tenantId 租户ID
     *
     * @return 排班数据列表
     */
    List<LiveRoomScheduleAlignResponse> batchQueryAnchorRoomSchedule(List<AnchorQueryScheduleRequest> requests, long tenantId);


    /**
     * 获取排班员工
     *
     * @param scheduleId 排班ID
     *
     * @return 排班员工列表
     */
    List<RoomScheduleBo.WorkUser> listScheduleEmployees(long scheduleId);


    /**
     * 批量更换排班人员
     *
     * @param request  更换请求
     * @param tenantId 租户ID
     * @param userId   操作人ID
     * @return 响应结果
     */
    ApiResponse<Void> replaceEmployees(ScheduleReplaceEmployeeRequest request, long tenantId, long userId);
}
