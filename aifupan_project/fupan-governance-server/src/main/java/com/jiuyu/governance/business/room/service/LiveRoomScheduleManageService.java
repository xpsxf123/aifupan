package com.jiuyu.governance.business.room.service;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.room.pojo.request.schedule.*;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomSchedulePageResponse;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomScheduleResponse;
import com.jiuyu.governance.common.pojo.bo.IdRequest;

import java.util.List;

/**
 * 直播间排班管理服务接口
 *
 * @author HeHui
 * @date 2026-03-26
 */
public interface LiveRoomScheduleManageService {

    /**
     * 新增直播间排班
     *
     * @param request  新增请求
     * @param tenantId 租户ID
     * @param userId   操作人ID
     * @return 响应结果
     */
    ApiResponse<Void> addSchedule(LiveRoomScheduleAddRequest request, Long tenantId, Long userId);

    /**
     * 指定时间范围查询排班数据
     *
     * @param request  查询请求
     * @param tenantId 租户ID
     * @return 排班数据列表
     */
    List<LiveRoomScheduleResponse> listRange(LiveRoomScheduleQueryRequest request, Long tenantId);

    /**
     * 分页查询排班数据
     *
     * @param request  查询请求
     * @param tenantId 租户ID
     * @return 分页数据
     */
    PageData<LiveRoomSchedulePageResponse> page(LiveRoomSchedulePageRequest request, Long tenantId);

    /**
     * 添加排班人员
     *
     * @param request  添加人员请求
     * @param tenantId 租户ID
     * @param userId   操作人ID
     * @return 响应结果
     */
    ApiResponse<Void> addEmployee(ScheduleAddEmployeeRequest request, Long tenantId, Long userId);

    /**
     * 移除排班人员
     *
     * @param request  移除人员请求
     * @param tenantId 租户ID
     * @param userId   操作人ID
     * @return 响应结果
     */
    ApiResponse<Void> removeEmployee(ScheduleRemoveEmployeeRequest request, Long tenantId, Long userId);

    /**
     * 修改单个排班
     *
     * @param request  修改请求
     * @param tenantId 租户ID
     * @param userId   操作人ID
     * @return 响应结果
     */
    ApiResponse<Void> updateSchedule(ScheduleUpdateRequest request, Long tenantId, Long userId);

    /**
     * 删除单个排班
     *
     * @param scheduleId 排班ID
     * @param tenantId 租户ID
     * @param userId   操作人ID
     * @return 响应结果
     */
    ApiResponse<Void> deleteSchedule(long scheduleId, Long tenantId, Long userId);

    /**
     * 批量更换排班人员
     *
     * @param request  更换请求
     * @param tenantId 租户ID
     * @param userId   操作人ID
     * @return 响应结果
     */
    ApiResponse<Void> replaceEmployees(ScheduleReplaceEmployeeRequest request, long tenantId, long userId);

    /**
     * 获取排班详情
     *
     * @param roomScheduleId 排班ID
     * @param tenantId 租户ID
     * @return 排班详情
     */
    LiveRoomScheduleResponse getAdminDetail(long roomScheduleId, long tenantId);

    /**
     * 导出直播间排班导入模板（矩阵式 .xlsx）
     *
     * @param liveRoomId 直播间ID
     * @param tenantId   租户ID
     * @return Excel 文件字节
     */
    byte[] exportTemplate(Long liveRoomId, Long tenantId);

    /**
     * 批量导入直播间排班（覆盖式）
     *
     * @param request  导入请求
     * @param tenantId 租户ID
     * @param userId   操作人ID
     * @return 响应结果
     */
    ApiResponse<Void> importSchedule(LiveRoomScheduleImportRequest request, Long tenantId, Long userId);
}
