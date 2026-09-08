package com.jiuyu.governance.business.room.service;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.rbac.pojo.constants.AccountStatus;
import com.jiuyu.governance.business.room.pojo.bo.LiveRoomInfo;
import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.business.room.pojo.request.ClientLiveRoomAddRequest;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomAddRequest;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomQueryRequest;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomScheduleAttributeSetRequest;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomSearchQueryRequest;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomSyncTenantAnchorsRequest;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomUpdateRequest;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomOrgResponse;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomResponse;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomScheduleAttributeResponse;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomSyncResultResponse;
import com.jiuyu.governance.common.pojo.bo.LabelOption;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 直播间服务接口
 * <p>
 * 提供直播间的全生命周期管理，包括与第三方平台的对接、组织架构绑定、以及基础的 CRUD 操作。
 * </p>
 *
 * @author HeHui
 * @date 2026-03-25
 */
public interface LiveRoomService {

    /**
     * 新增直播间
     * <p>
     * 创建直播间前会调用 {@link com.jiuyu.governance.openfeign.collect.LiveAnchorOpenService}
     * 获取第三方平台主播的公开信息并自动填充落库。
     * </p>
     *
     * @param request  新增请求入参对象
     * @param tenantId 当前登录用户的租户ID
     * @param userId   当前操作人ID (用于审计字段)
     *
     * @return 响应结果，如果第三方接口调用失败或校验不通过将返回 failed
     */
    ApiResponse<Void> addLiveRoom(LiveRoomAddRequest request, long tenantId, long userId);


    /**
     * 新增主播
     *
     * @param request  新增请求入参对象
     * @param tenantId 当前登录用户的租户ID
     *
     * @return 响应结果
     */
    ApiResponse<Void> addAnchor(ClientLiveRoomAddRequest request, long tenantId);


    /**
     * 修改直播间基础信息与关联关系
     *
     * @param request  修改请求入参对象
     * @param tenantId 当前登录用户的租户ID
     * @param userId   当前操作人ID
     *
     * @return 响应结果
     */
    ApiResponse<Void> updateLiveRoom(LiveRoomUpdateRequest request, long tenantId, long userId);

    /**
     * 逻辑删除直播间
     *
     * @param id       要删除的直播间主键ID
     * @param tenantId 当前登录用户的租户ID
     * @param userId   当前操作人ID
     *
     * @return 响应结果
     */
    ApiResponse<Void> deleteLiveRoom(long id, long tenantId, long userId);

    /**
     * 启用或停用直播间状态
     *
     * @param id       要操作的直播间主键ID
     * @param tenantId 当前登录用户的租户ID
     * @param userId   当前操作人ID
     *
     * @return 响应结果
     */
    ApiResponse<Void> enableLiveRoom(long id, long tenantId, long userId, AccountStatus status);

    /**
     * 同步租户主播到直播间
     * <p>
     * 基于 replay 侧租户主播数据拉取结果，在单租户范围内按 platform + secUid 对账，
     * 执行批量新增或批量更新直播间基础信息，并可选绑定直播间负责人。
     * </p>
     *
     * @param request  同步请求参数
     * @param tenantId 当前登录用户的租户ID
     * @param userId   当前操作人ID
     *
     * @return 同步统计结果
     */
    ApiResponse<LiveRoomSyncResultResponse> syncTenantAnchors(LiveRoomSyncTenantAnchorsRequest request, long tenantId,
        long userId);

    /**
     * 分页多条件查询直播间列表
     * <p>
     * 包含基于组织架构维度的数据权限隔离。
     * 列表中会通过 Complete 工具类自动组装所属的组织名称和管理员信息。
     * </p>
     *
     * @param request  分页与条件请求对象
     * @param tenantId 当前登录用户的租户ID
     *
     * @return 组装完成的直播间分页数据
     */
    PageData<LiveRoomResponse> pageQueryLiveRoom(LiveRoomQueryRequest request, long tenantId);

    /**
     * 分页查询直播间（带今日业绩排序）
     *
     * @param request  分页与条件请求对象
     * @param tenantId 当前登录用户的租户ID
     *
     * @return 按今日业绩排序的直播间分页数据
     */
    PageData<LiveRoomResponse> pageQueryLiveRoomWithTodayPerformance(LiveRoomQueryRequest request, long tenantId);

    /**
     * 获取指定直播间的详细信息
     *
     * @param id       直播间主键ID
     * @param tenantId 当前登录用户的租户ID
     *
     * @return 直播间详情（包含完整关联数据），若不存在则返回 null
     */
    LiveRoomResponse getLiveRoomDetail(long id, long tenantId);

    /**
     * 直播间搜索查询
     *
     * @param request 搜索查询请求对象
     *
     * @return {@link List }<{@link LabelOption }>
     */
    List<LabelOption> options(LiveRoomSearchQueryRequest request);

    /**
     * 获取直播间组织架构
     *
     * @param request 搜索查询请求对象
     *
     * @return {@link List }<{@link LiveRoomOrgResponse }>
     */
    List<LiveRoomOrgResponse> getLiveRoomOrg(LiveRoomSearchQueryRequest request);

    /**
     * 设置直播间排班配置
     *
     * @param request  设置请求参数
     * @param tenantId 当前登录用户的租户ID
     * @param userId   当前操作人ID
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    ApiResponse<Void> setScheduleAttribute(LiveRoomScheduleAttributeSetRequest request, long tenantId, long userId);

    /**
     * 获取直播间排班配置
     *
     * @param id       直播间主键ID
     * @param tenantId 当前登录用户的租户ID
     *
     * @return {@link LiveRoomScheduleAttributeResponse }
     */
    LiveRoomScheduleAttributeResponse getScheduleAttribute(long id, long tenantId);


    /**
     * 获取员工关联的直播间
     *
     * @param employeeId  员工ID
     * @param positionId  职位ID
     * @param deadlineDay 截止日期
     *
     * @return {@link List }<{@link LiveRoomInfo }>
     */
    List<LiveRoomInfo> getEmployeeJoinRooms(long employeeId, Long positionId, LocalDate deadlineDay);



    /**
     * 批量获取直播间信息
     *
     * @param roomIds 直播间ID列表
     *
     * @return {@link List }<{@link LiveRoomInfo }>
     */
    List<LiveRoomInfo> getRoomInfos(Collection<Long> roomIds);

    /**
     * 批量获取员工关联的直播间
     *
     * @param employeeIds 员工ID列表
     * @param positionId  职位ID
     * @param deadlineDay 截止日期
     *
     * @return {@link Map }<{@link Long }, {@link List }<{@link LiveRoomInfo }>>
     */
    Map<Long, List<LiveRoomInfo>> getEmployeeJoinRooms(List<Long> employeeIds, Long positionId, LocalDate deadlineDay);

    /**
     * 获取直播间名称映射
     *
     * @param roomIds 直播间ID列表
     *
     * @return {@link Map }<{@link Long }, {@link String }>
     */
    Map<Long, String> getLiveRoomNameMap(List<Long> roomIds);



    /**
     * 获取组织架构下直播间数量
     *
     * @param orgIds      组织架构ID列表
     * @param managerType 管理类型
     * @param tenantId    当前登录用户的租户ID
     *
     * @return {@link Map }<{@link Long }, {@link Integer }>
     */
    Map<Long, Integer> countOrgRoomMap(List<Long> orgIds, ManagerType managerType, long tenantId);


    /**
     * 获取直播间所属租户ID映射
     *
     * @param platformType 平台类型
     * @param secUids      主播 secUid 列表
     *
     * @return {@link Map }<{@link String }, {@link List }<{@link Long }>>
     */
    Map<String, List<Long>> getRoomTenantMap(LivePlatformType platformType, List<String> secUids);
}
