package com.jiuyu.governance.business.performance.service;

import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.performance.pojo.request.DailyPerformanceStatsRequest;
import com.jiuyu.governance.business.performance.pojo.request.ScheduleListRequest;
import com.jiuyu.governance.business.performance.pojo.request.SchedulePerformancePageRequest;
import com.jiuyu.governance.business.performance.pojo.request.SchedulePerformanceSaveRequest;
import com.jiuyu.governance.business.performance.pojo.response.ScheduleListItemResponse;
import com.jiuyu.governance.business.performance.pojo.response.client.ClientPerformanceResponse;
import com.jiuyu.governance.business.performance.pojo.response.DailyPerformanceStatsResponse;
import com.jiuyu.governance.business.performance.pojo.response.LiveRoomPerformanceResponse;
import com.jiuyu.governance.business.performance.pojo.response.SchedulePerformanceDetailResponse;
import com.jiuyu.governance.business.performance.pojo.response.SchedulePerformanceResponse;
import com.jiuyu.governance.business.performance.pojo.response.client.ClientSchedulePerformanceResponse;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomResponse;

import java.util.List;

/**
 * 排班业绩服务接口
 *
 * @author lj
 * @date 2026-03-24
 */
public interface SchedulePerformanceService {

    /**
     * 排班业绩分页查询
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageData<SchedulePerformanceResponse> pageQuerySchedulePerformance(SchedulePerformancePageRequest request);

    /**
     * 按天维度统计排班业绩
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageData<DailyPerformanceStatsResponse> pageQueryDailyStats(DailyPerformanceStatsRequest request);

    /**
     * 班次业绩详情查询
     *
     * @param id       班次业绩ID
     * @param tenantId 租户ID
     * @return 班次业绩详情
     */
    SchedulePerformanceDetailResponse getDetailById(Long id, Long tenantId);

    /**
     * 保存班次业绩（新增或修改）
     *
     * @param request  保存请求
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     * @return 业绩ID
     */
    Long saveSchedulePerformance(SchedulePerformanceSaveRequest request, Long tenantId, Long userId);

    /**
     * 获取班次业绩数据
     *
     * @param result   直播间数据
     * @param tenantId 租户ID
     * @return 班次业绩数据
     */
    PageData<LiveRoomPerformanceResponse> LiveRoomPerformanceData(PageData<LiveRoomResponse> result, long tenantId);

    /**
     * 删除班次业绩（逻辑删除）
     *
     * @param id       班次业绩ID
     * @param tenantId 租户ID
     */
    void deleteSchedulePerformance(Long id, Long tenantId);

    /**
     * 根据主播secUid和时间范围查询排班业绩（最多返回1条）
     *
     * @param tenantId  租户ID
     * @param secUid    主播SecUid
     * @param startTime 视频开始时间
     * @param endTime   视频结束时间
     * @return 排班业绩响应，无匹配时返回null
     */
    ClientSchedulePerformanceResponse queryBySecUidAndTimeRange(Long tenantId, String secUid,
                                                                java.time.LocalDateTime startTime,
                                                                java.time.LocalDateTime endTime);

    /**
     * 更新班次业绩的人员
     *
     * @param staffInfoList 人员信息
     * @param scheduleId 排班id
     * @param tenantId 租户ID
     */
    void updateSchedulePerformanceStaff(List<SchedulePerformanceResponse.StaffInfo> staffInfoList, Long scheduleId, Long tenantId, Long userId);

    /**
     * 查询排班列表
     *
     * @param request  查询请求
     * @param tenantId 租户ID
     * @return 排班列表
     */
    List<ScheduleListItemResponse> queryScheduleList(ScheduleListRequest request, Long tenantId);
}
