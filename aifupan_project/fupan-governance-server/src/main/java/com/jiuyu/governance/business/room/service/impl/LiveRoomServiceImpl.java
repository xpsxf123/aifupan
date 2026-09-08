package com.jiuyu.governance.business.room.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.mybatispuls.util.CustomPage;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.org.pojo.entity.Position;
import com.jiuyu.governance.business.org.service.DeptService;
import com.jiuyu.governance.business.org.service.PositionService;
import com.jiuyu.governance.business.org.service.SubCompanyService;
import com.jiuyu.governance.business.org.service.TeamService;
import com.jiuyu.governance.business.org.service.impl.ManagerConnectorProcessor;
import com.jiuyu.governance.business.rbac.pojo.constants.AccountStatus;
import com.jiuyu.governance.business.room.mapper.LiveRoomMapper;
import com.jiuyu.governance.business.room.mapper.LiveRoomScheduleAttributeMapper;
import com.jiuyu.governance.business.room.pojo.bo.LiveRoomInfo;
import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoom;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoomScheduleAttribute;
import com.jiuyu.governance.business.room.pojo.entity.ScheduleEmployee;
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
import com.jiuyu.governance.business.room.service.LiveRoomService;
import com.jiuyu.governance.common.pojo.ErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.pojo.bo.CountData;
import com.jiuyu.governance.common.pojo.bo.IdName;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.openfeign.collect.LiveAnchorOpenService;
import com.jiuyu.governance.openfeign.collect.pojo.response.DouyinAnchorBo;
import com.jiuyu.governance.openfeign.collect.pojo.response.DouyinOpenResult;
import com.jiuyu.governance.openfeign.replay.AnchorInfoService;
import com.jiuyu.governance.openfeign.replay.request.TenantAnchorQueryRequest;
import com.jiuyu.governance.openfeign.replay.response.TenantAnchorInfoResponse;
import com.jiuyu.governance.plugins.oauth.data.BeforePermission;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 直播间服务实现类
 *
 * @author HeHui
 * @date 2026-03-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveRoomServiceImpl extends ServiceImpl<LiveRoomMapper, LiveRoom> implements LiveRoomService {

    private static final int DEFAULT_PLATFORM = 0;

    private static final int OWN_ACCOUNT_TYPE = 0;

    private static final long DEFAULT_DEPT_ID = 0L;

    private static final long DEFAULT_TEAM_ID = 0L;

    private final SubCompanyService companyService;
    private final DeptService deptService;
    private final PositionService positionService;
    private final TeamService teamService;
    private final ManagerConnectorProcessor connectorProcessor;
    private final LiveAnchorOpenService liveAnchorOpenService;
    private final LiveRoomScheduleAttributeMapper scheduleAttributeMapper;
    private final AnchorInfoService anchorInfoService;

    /**
     * 新增直播间
     * <p>
     * 1. 校验公司、部门、小组是否存在。<br>
     * 2. 校验同一个平台下该 secUid 是否已存在，防止重复创建。<br>
     * 3. 调用 LiveAnchorOpenService 查询该主播的第三方数据（如昵称、头像），如未查到则阻断。<br>
     * 4. 保存到数据库，并使用 ManagerConnectorProcessor 绑定关联的负责人。
     * </p>
     *
     * @param request  新增请求入参对象
     * @param tenantId 当前登录用户的租户ID
     * @param userId   当前操作人ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission.Multiple(enableLevel = true,
        value = {
            @BeforePermission(type = OauthConstant.COMPANY, dataId = "#request.companyId"),
            @BeforePermission(type = OauthConstant.DEPT, dataId = "#request.deptId", ignoreEmpty = true),
            @BeforePermission(type = OauthConstant.TEAM, dataId = "#request.teamId", ignoreEmpty = true)
        })
    public ApiResponse<Void> addLiveRoom(LiveRoomAddRequest request, long tenantId, long userId) {
        // todo 验证 上层组织是否有下层组织来决定是否必填？
        // 2. 调用第三方 API 获取主播信息
        DouyinOpenResult<DouyinAnchorBo> openResult = liveAnchorOpenService.searchAnchor(LivePlatformType.getByValue(request.getPlatform()), request.getAnchorNumber(), true);
        if (openResult == null || openResult.getCode() != 0 || openResult.getData() == null) {
            log.warn("[直播间管理] 新增失败，无法获取主播信息：platform: {}, anchorNumber: {}, tenantId: {}", request.getPlatform(), request.getAnchorNumber(), tenantId);
            return ApiResponse.failed(SystemErrorCode.UNPROCESSABLE_ENTITY.getCode(), "无法获取主播信息，请检查账号是否正确");
        }
        // 处理默认值
        if (request.getDeptId() == null) {
            request.setDeptId(0L);
        }
        if (request.getTeamId() == null) {
            request.setTeamId(0L);
        }
        DouyinAnchorBo anchorInfo = openResult.getData();

        // 3. 校验 secUid 是否租户内唯一
        boolean exists = super.lambdaQuery()
            .eq(LiveRoom::getTenantId, tenantId)
            .eq(LiveRoom::getPlatform, request.getPlatform())
            .eq(LiveRoom::getSecUid, anchorInfo.getSec_uid())
            .eq(LiveRoom::getIsDeleted, false)
            .exists();
        if (exists) {
            log.warn("[直播间管理] 新增失败，该直播间已存在：platform: {}, secUid: {}, tenantId: {}", request.getPlatform(), anchorInfo.getSec_uid(), tenantId);
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "该直播间已存在");
        }


        // 4. 构建实体
        LiveRoom liveRoom = BeanUtil.copyProperties(request, LiveRoom.class);
        liveRoom.setTenantId(tenantId);
        liveRoom.setSecUid(anchorInfo.getSec_uid());
        liveRoom.setPlatform(request.getPlatform());

        // 填充第三方数据
        liveRoom.setAnchorName(anchorInfo.getNickname());
        liveRoom.setAnchorAvatar(anchorInfo.getAvatar());
        liveRoom.setHomeUrl(EmptyUtil.isEmpty(anchorInfo.getAweme_url()) ? "" : anchorInfo.getAweme_url());
        liveRoom.setLiveUrl(""); // 抖音未返回 live_url，默认空

        liveRoom.setAccountStatus(AccountStatus.NORMAL);
        liveRoom.setCreateBy(userId);
        liveRoom.setUpdateBy(userId);
        liveRoom.setCreateDate(LocalDateTime.now());
        liveRoom.setUpdateDate(LocalDateTime.now());
        liveRoom.setIsDeleted(false);


        this.save(liveRoom);
        log.info("[直播间管理] 新增直播间：id: {}, platform: {}, anchorName: {}, tenantId: {}", liveRoom.getId(), request.getPlatform(), anchorInfo.getNickname(), tenantId);

        // 5. 绑定管理员
        if (EmptyUtil.isNotEmpty(request.getManagerUserIds())) {
            connectorProcessor.connector(ManagerType.LIVE_ROOM, liveRoom.getId(), request.getManagerUserIds());
        }
        connectorProcessor.clearTenantCache(tenantId, List.of(ManagerType.LIVE_ROOM));
        connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, List.of(liveRoom.getCompanyId()), ManagerType.DEPT, List.of(liveRoom.getDeptId()), ManagerType.TEAM, List.of(liveRoom.getTeamId())));
        return ApiResponse.success();
    }


    /**
     * 新增主播
     *
     * @param request  新增请求入参对象
     * @param tenantId 当前登录用户的租户ID
     *
     * @return 响应结果
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ApiResponse<Void> addAnchor(ClientLiveRoomAddRequest request, long tenantId) {
        boolean exists = super.lambdaQuery()
            .eq(LiveRoom::getTenantId, tenantId)
            .eq(LiveRoom::getPlatform, request.getPlatform())
            .eq(LiveRoom::getSecUid, request.getSecUid())
            .eq(LiveRoom::getIsDeleted, false)
            .exists();
        if (exists) {
            log.warn("[直播间管理] 新增失败，该直播间已存在：platform: {}, secUid: {}, tenantId: {}", request.getPlatform(), request.getSecUid(), tenantId);
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "该直播间已存在");
        }
        // 4. 构建实体
        LiveRoom liveRoom = BeanUtil.copyProperties(request, LiveRoom.class);
        liveRoom.setTenantId(tenantId);
        liveRoom.setSecUid(request.getSecUid());
        liveRoom.setPlatform(request.getPlatform());

        // 填充第三方数据
        liveRoom.setAnchorName(request.getAnchorName());
        liveRoom.setAnchorAvatar(EmptyUtil.isEmpty(request.getAnchorAvatar()) ? "" : request.getAnchorAvatar());
        liveRoom.setHomeUrl(EmptyUtil.isEmpty(request.getHomeUrl()) ? "" : request.getHomeUrl());
        liveRoom.setLiveUrl(EmptyUtil.isEmpty(request.getLiveUrl()) ? "" : request.getLiveUrl()); // 抖音未返回 live_url，默认空

        liveRoom.setAccountStatus(AccountStatus.NORMAL);
        liveRoom.setCreateBy(0L);
        liveRoom.setUpdateBy(0L);
        liveRoom.setCreateDate(LocalDateTime.now());
        liveRoom.setUpdateDate(LocalDateTime.now());
        liveRoom.setIsDeleted(false);


        this.save(liveRoom);
        log.info("[直播间管理] 新增直播间：id: {}, platform: {}, anchorName: {}, tenantId: {}", liveRoom.getId(), request.getPlatform(), request.getAnchorName(), tenantId);


        connectorProcessor.clearTenantCache(tenantId, List.of(ManagerType.LIVE_ROOM));
        connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, List.of(liveRoom.getCompanyId())));
        return ApiResponse.success();
    }

    /**
     * 修改直播间基础信息与关联关系
     *
     * @param request  修改请求入参对象
     * @param tenantId 当前登录用户的租户ID
     * @param userId   当前操作人ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission(type = OauthConstant.LIVE_ROOM, dataId = "#request.id")
    public ApiResponse<Void> updateLiveRoom(LiveRoomUpdateRequest request, long tenantId, long userId) {
        LiveRoom liveRoom = super.getById(request.getId());
        if (liveRoom == null) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "直播间不存在");
        }
        // 处理默认值
        if (request.getDeptId() == null) {
            request.setDeptId(0L);
        }
        if (request.getTeamId() == null) {
            request.setTeamId(0L);
        }


        LiveRoom updateEntity = BeanUtil.copyProperties(request, LiveRoom.class);
        updateEntity.setUpdateBy(userId);
        updateEntity.setUpdateDate(LocalDateTime.now());


        this.updateById(updateEntity);
        log.info("[直播间管理] 修改直播间：id: {}, tenantId: {}", request.getId(), tenantId);

        if (EmptyUtil.isEmpty(request.getManagerUserIds())) {
            connectorProcessor.disconnectAll(ManagerType.LIVE_ROOM, request.getId());
        } else {
            connectorProcessor.connector(ManagerType.LIVE_ROOM, request.getId(), request.getManagerUserIds());
        }
        connectorProcessor.clearTenantCache(tenantId, List.of(ManagerType.LIVE_ROOM));
        connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, List.of(request.getCompanyId(), liveRoom.getCompanyId()), ManagerType.DEPT, List.of(request.getDeptId(), liveRoom.getDeptId()), ManagerType.TEAM, List.of(request.getTeamId(), liveRoom.getTeamId())));
        return ApiResponse.success();
    }

    /**
     * 逻辑删除直播间
     * <p>
     * TODO: 需要前置校验该直播间下是否有未执行的排班。
     * 删除后，同时解除与负责人的关联关系。
     * </p>
     *
     * @param id       要删除的直播间主键ID
     * @param tenantId 当前登录用户的租户ID
     * @param userId   当前操作人ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission(type = OauthConstant.LIVE_ROOM, dataId = "#id")
    public ApiResponse<Void> deleteLiveRoom(long id, long tenantId, long userId) {
        LiveRoom existing = this.getById(id);
        if (existing == null || existing.getIsDeleted() || !existing.getTenantId().equals(tenantId)) {
            log.warn("[直播间管理] 删除失败，直播间不存在：id: {}, tenantId: {}", id, tenantId);
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "直播间不存在");
        }

        // TODO: 校验是否有未执行的排班 (依赖 schedule 模块)

        LiveRoom updateEntity = new LiveRoom();
        updateEntity.setId(id);
        updateEntity.setIsDeleted(true);
        updateEntity.setUpdateBy(userId);
        updateEntity.setUpdateDate(LocalDateTime.now());
        this.updateById(updateEntity);
        log.info("[直播间管理] 删除直播间：id: {}, tenantId: {}", id, tenantId);

        connectorProcessor.disconnectAll(ManagerType.LIVE_ROOM, id);
        connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, List.of(existing.getCompanyId()), ManagerType.DEPT, List.of(existing.getDeptId()), ManagerType.TEAM, List.of(existing.getTeamId())));
        connectorProcessor.clearTenantCache(tenantId, List.of(ManagerType.LIVE_ROOM));
        return ApiResponse.success();
    }

    /**
     * 启用或停用直播间状态
     *
     * @param id       要操作的直播间主键ID
     * @param tenantId 当前登录用户的租户ID
     * @param userId   当前操作人ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission(type = OauthConstant.LIVE_ROOM, dataId = "#id")
    public ApiResponse<Void> enableLiveRoom(long id, long tenantId, long userId, AccountStatus accountStatus) {

        LiveRoom updateEntity = new LiveRoom();
        updateEntity.setId(id);
        updateEntity.setAccountStatus(accountStatus);
        updateEntity.setUpdateBy(userId);
        updateEntity.setUpdateDate(LocalDateTime.now());
        this.updateById(updateEntity);
        log.info("[直播间管理] 修改直播间状态：id: {}, accountStatus: {}, tenantId: {}", id, accountStatus, tenantId);

        return ApiResponse.success();
    }

    /**
     * 同步租户主播到直播间
     * <p>
     * 1. 调用 replay 接口获取租户主播信息（参数 tenantId/accountType 固定）。<br>
     * 2. 远端数据按 platform + secUid 去重后，在单租户内对账批量新增/更新直播间。<br>
     * 3. 可选绑定直播间负责人，并清理相关缓存。
     * </p>
     *
     * @param request  同步请求参数
     * @param tenantId 当前登录用户租户ID
     * @param userId   当前操作人ID
     *
     * @return 同步统计结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ApiResponse<LiveRoomSyncResultResponse> syncTenantAnchors(LiveRoomSyncTenantAnchorsRequest request, long tenantId,
        long userId) {
        LiveRoomSyncTenantAnchorsRequest realRequest = request == null ? new LiveRoomSyncTenantAnchorsRequest() : request;
        int platform = realRequest.getPlatform() == null ? DEFAULT_PLATFORM : realRequest.getPlatform();

        Optional<Long> companyIdOptional = companyService.getTenantFirst(tenantId);
        if (companyIdOptional.isEmpty()) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "请先创建公司");
        }
        long companyId = companyIdOptional.get();

        List<TenantAnchorInfoResponse> remoteAnchors;
        try {
            TenantAnchorQueryRequest queryRequest = this.buildTenantAnchorQueryRequest(realRequest, tenantId, platform);
            remoteAnchors = anchorInfoService.getTenantAnchors(queryRequest);
        } catch (Throwable e) {
            log.warn("[直播间管理] 同步失败，拉取租户主播异常：tenantId: {}, platform: {}", tenantId, platform, e);
            return ApiResponse.failed(SystemErrorCode.UNPROCESSABLE_ENTITY.getCode(), "同步失败，请稍后重试");
        }

        if (EmptyUtil.isEmpty(remoteAnchors)) {
            return ApiResponse.success(this.buildSyncResult(0, 0, 0));
        }

        // 去重
        Map<String, TenantAnchorInfoResponse> uniqueRemoteMap = this.deduplicateRemoteAnchors(platform, remoteAnchors);
        if (EmptyUtil.isEmpty(uniqueRemoteMap)) {
            return ApiResponse.success(this.buildSyncResult(0, 0, 0));
        }

        // 提取secUid
        List<String> secUids = uniqueRemoteMap.values().stream().map(TenantAnchorInfoResponse::getSecUid)
            .filter(EmptyUtil::isNotEmpty).distinct().toList();
        if (EmptyUtil.isEmpty(secUids)) {
            return ApiResponse.success(this.buildSyncResult(0, 0, 0));
        }

        // 获取已存在的直播间
        List<LiveRoom> existingRooms = this.listExistingRooms(tenantId, platform, secUids);
        Map<String, LiveRoom> existingRoomMap = EmptyUtil.isEmpty(existingRooms) ? Map.of()
            : existingRooms.stream().collect(Collectors.toMap(LiveRoom::getSecUid, Function.identity(), (a, b) -> a));

        List<LiveRoom> needSaveList = new ArrayList<>();
        List<LiveRoom> needUpdateList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 遍历远程数据
        for (TenantAnchorInfoResponse remote : uniqueRemoteMap.values()) {
            if (remote == null || EmptyUtil.isEmpty(remote.getSecUid())) {
                continue;
            }
            // 获取已存在的直播间
            LiveRoom existing = existingRoomMap.get(remote.getSecUid());
            if (existing == null) {
                LiveRoom newRoom = this.buildNewLiveRoom(realRequest, tenantId, userId, now, platform, companyId, remote);
                needSaveList.add(newRoom);
            } else {
                LiveRoom updateRoom = this.buildUpdateLiveRoom(realRequest, userId, now, existing, remote);
                needUpdateList.add(updateRoom);
            }
        }

        // 保存
        if (EmptyUtil.isNotEmpty(needSaveList)) {
            this.saveBatch(needSaveList);
        }
        // 更新
        if (EmptyUtil.isNotEmpty(needUpdateList)) {
            this.updateBatchById(needUpdateList);
        }

        // 绑定管理员
        if (EmptyUtil.isNotEmpty(realRequest.getManagerUserIds())) {
            List<Long> roomIds = new ArrayList<>(needSaveList.size() + needUpdateList.size());
            for (LiveRoom room : needSaveList) {
                if (room.getId() != null) {
                    roomIds.add(room.getId());
                }
            }
            for (LiveRoom room : needUpdateList) {
                if (room.getId() != null) {
                    roomIds.add(room.getId());
                }
            }
            roomIds = roomIds.stream().distinct().toList();
            for (Long roomId : roomIds) {
                connectorProcessor.connector(ManagerType.LIVE_ROOM, roomId, realRequest.getManagerUserIds());
            }
        }

        connectorProcessor.clearTenantCache(tenantId, List.of(ManagerType.LIVE_ROOM));
        connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, List.of(companyId)));

        // 统计结果
        LiveRoomSyncResultResponse result = this.buildSyncResult(uniqueRemoteMap.size(), needSaveList.size(),
            needUpdateList.size());
        log.info("[直播间管理] 同步直播间完成：tenantId: {}, platform: {}, total: {}, created: {}, updated: {}", tenantId, platform,
            result.getTotal(), result.getCreated(), result.getUpdated());
        return ApiResponse.success(result);
    }

    /**
     * 构建租户主播查询请求对象
     * <p>
     * 根据同步请求参数、租户ID和平台信息，构造查询远程主播信息的请求对象。
     * 设置账号类型为自有账号，并根据条件可选地设置行业ID和负责人ID过滤条件。
     * </p>
     *
     * @param request  同步请求参数
     * @param tenantId 租户ID
     * @param platform 平台标识
     *
     * @return 构造完成的租户主播查询请求对象
     */
    private TenantAnchorQueryRequest buildTenantAnchorQueryRequest(LiveRoomSyncTenantAnchorsRequest request, long tenantId,
        int platform) {
        TenantAnchorQueryRequest queryRequest = new TenantAnchorQueryRequest();
        // 设置基础查询条件：租户、账号类型、平台
        queryRequest.setTenantId(tenantId);
        queryRequest.setAccountType(OWN_ACCOUNT_TYPE);
        queryRequest.setPlatformList(List.of(platform));

        // 根据请求参数设置可选的过滤条件
        if (request.getTradeId() != null) {
            queryRequest.setTradeIds(List.of(request.getTradeId()));
        }
        if (EmptyUtil.isNotEmpty(request.getManagerUserIds())) {
            queryRequest.setUserIds(request.getManagerUserIds());
        }
        return queryRequest;
    }

    /**
     * 对远程主播列表进行去重处理
     * <p>
     * 根据平台和主播安全UID生成唯一键，过滤掉空值和重复的主播信息。
     * 使用LinkedHashMap保持插入顺序，确保相同平台+secUid的主播只保留第一个。
     * </p>
     *
     * @param platform   平台标识
     * @param remoteList 远程主播信息列表
     *
     * @return 去重后的主播信息Map，key为"platform:secUid"格式
     */
    private Map<String, TenantAnchorInfoResponse> deduplicateRemoteAnchors(int platform, List<TenantAnchorInfoResponse> remoteList) {
        Map<String, TenantAnchorInfoResponse> uniqueMap = new LinkedHashMap<>();
        // 遍历主播列表，过滤无效数据并按platform:secUid去重
        for (TenantAnchorInfoResponse remote : remoteList) {
            if (remote == null || EmptyUtil.isEmpty(remote.getSecUid())) {
                continue;
            }
            String key = platform + ":" + remote.getSecUid();
            uniqueMap.putIfAbsent(key, remote);
        }
        return uniqueMap;
    }

    /**
     * 构建新直播间对象
     * <p>
     * 根据远程主播信息和请求参数，创建一个新的直播间实体对象。
     * 设置租户、平台、主播基本信息、组织架构、账号状态等必要字段。
     * </p>
     *
     * @param request   同步请求参数
     * @param tenantId  租户ID
     * @param userId    当前操作人ID
     * @param now       当前时间
     * @param platform  平台标识
     * @param companyId 公司ID
     * @param remote    远程主播信息响应对象
     *
     * @return 新建的直播间实体对象（包含所有必填字段）
     */
    private LiveRoom buildNewLiveRoom(LiveRoomSyncTenantAnchorsRequest request, long tenantId, long userId, LocalDateTime now,
        int platform, long companyId, TenantAnchorInfoResponse remote) {
        LiveRoom entity = new LiveRoom();
        // 设置租户、平台和主播唯一标识
        entity.setTenantId(tenantId);
        entity.setPlatform(platform);
        entity.setSecUid(remote.getSecUid());

        // 设置主播基本信息，空值则设置为空字符串
        entity.setAnchorName(EmptyUtil.isEmpty(remote.getAnchorName()) ? "" : remote.getAnchorName());
        entity.setAnchorAvatar(EmptyUtil.isEmpty(remote.getAnchorAvatar()) ? "" : remote.getAnchorAvatar());
        entity.setHomeUrl(EmptyUtil.isEmpty(remote.getHomeUrl()) ? "" : remote.getHomeUrl());
        entity.setLiveUrl(EmptyUtil.isEmpty(remote.getLiveUrl()) ? "" : remote.getLiveUrl());
        entity.setAnchorNumber(EmptyUtil.isEmpty(remote.getAnchorNumber()) ? "" : remote.getAnchorNumber());

        // 设置组织架构和行业信息
        entity.setTradeId(this.resolveTradeIdForCreate(request, remote));
        entity.setCompanyId(companyId);
        entity.setDeptId(DEFAULT_DEPT_ID);
        entity.setTeamId(DEFAULT_TEAM_ID);

        // 设置账号状态和审计字段
        entity.setAccountStatus(AccountStatus.NORMAL);
        entity.setCreateBy(userId);
        entity.setUpdateBy(userId);
        entity.setCreateDate(now);
        entity.setUpdateDate(now);
        entity.setIsDeleted(false);
        return entity;
    }


    /**
     * 构建直播间更新对象
     * <p>
     * 根据远程主播信息和现有直播间记录，构建需要更新的直播间对象。
     * 仅更新非空的字段信息，行业ID通过专门的方法解析处理。
     * </p>
     *
     * @param request  同步请求参数
     * @param userId   当前操作人ID
     * @param now      当前时间
     * @param existing 现有的直播间记录
     * @param remote   远程主播信息响应对象
     *
     * @return 待更新的直播间对象（包含ID和更新字段）
     */
    private LiveRoom buildUpdateLiveRoom(LiveRoomSyncTenantAnchorsRequest request, long userId, LocalDateTime now, LiveRoom existing,
        TenantAnchorInfoResponse remote) {
        LiveRoom update = new LiveRoom();
        update.setId(existing.getId());
        update.setUpdateBy(userId);
        update.setUpdateDate(now);

        // 更新主播基本信息，空值则设置为空字符串
        update.setAnchorName(EmptyUtil.isEmpty(remote.getAnchorName()) ? "" : remote.getAnchorName());
        update.setAnchorAvatar(EmptyUtil.isEmpty(remote.getAnchorAvatar()) ? "" : remote.getAnchorAvatar());
        update.setHomeUrl(EmptyUtil.isEmpty(remote.getHomeUrl()) ? "" : remote.getHomeUrl());
        update.setLiveUrl(EmptyUtil.isEmpty(remote.getLiveUrl()) ? "" : remote.getLiveUrl());
        update.setAnchorNumber(EmptyUtil.isEmpty(remote.getAnchorNumber()) ? "" : remote.getAnchorNumber());

        // 解析并设置行业ID（仅在需要更新时设置）
        Long tradeId = this.resolveTradeIdForUpdate(request, remote, existing);
        if (tradeId != null) {
            update.setTradeId(tradeId);
        }

        return update;
    }

    /**
     *  计算出创建行业ID
     *
     * @param request 请求
     * @param remote 远程
     * @return {@link Long }
     */
    private Long resolveTradeIdForCreate(LiveRoomSyncTenantAnchorsRequest request, TenantAnchorInfoResponse remote) {
        if (request.getTradeId() != null) {
            return request.getTradeId();
        }
        if (EmptyUtil.isNotEmpty(remote.getTradeIds())) {
            return remote.getTradeIds().stream().filter(Objects::nonNull).findFirst().orElse(remote.getSystemTradeId());
        }
        if (remote.getSystemTradeId() != null) {
            return remote.getSystemTradeId();
        }
        return 0L;
    }

    /**
     *  计算出修改行业ID
     *
     * @param request 请求
     * @param remote 远程
     * @param existing 现有
     * @return {@link Long }
     */
    private Long resolveTradeIdForUpdate(LiveRoomSyncTenantAnchorsRequest request, TenantAnchorInfoResponse remote, LiveRoom existing) {
        if (existing != null && existing.getTradeId() != null && existing.getTradeId() > 0) {
            return null;
        }

        if (request.getTradeId() != null) {
            return request.getTradeId();
        }
        if (EmptyUtil.isNotEmpty(remote.getTradeIds())) {
            return remote.getTradeIds().stream().filter(Objects::nonNull).findFirst().orElse(remote.getSystemTradeId());
        }
        if (remote.getSystemTradeId() != null) {
            return remote.getSystemTradeId();
        }
        return null;
    }


    /**
     * 生成同步结果
     *
     * @param total 总计
     * @param created 创建
     * @param updated 更新
     * @return {@link LiveRoomSyncResultResponse }
     */
    private LiveRoomSyncResultResponse buildSyncResult(int total, int created, int updated) {
        LiveRoomSyncResultResponse response = new LiveRoomSyncResultResponse();
        response.setTotal(total);
        response.setCreated(created);
        response.setUpdated(updated);
        return response;
    }

    /**
     * 查询已存在的直播间列表
     * <p>
     * 根据租户ID、平台和主播安全UID列表，查询未删除的直播间记录。
     * </p>
     *
     * @param tenantId 租户ID
     * @param platform 平台标识
     * @param secUids  主播安全UID列表
     *
     * @return 已存在的直播间列表，若secUids为空则返回空列表
     */
    protected List<LiveRoom> listExistingRooms(long tenantId, int platform, List<String> secUids) {
        if (EmptyUtil.isEmpty(secUids)) {
            return List.of();
        }
        return super.lambdaQuery()
            .eq(LiveRoom::getTenantId, tenantId)
            .eq(LiveRoom::getPlatform, platform)
            .in(LiveRoom::getSecUid, secUids)
            .eq(LiveRoom::getIsDeleted, false)
            .list();
    }



    /**
     * 分页多条件查询直播间列表
     * <p>
     * 基于入参的条件以及数据权限 (companyIds, deptIds, teamIds) 进行动态过滤。<br>
     * 使用 Complete 工具类装配关联的组织名称和负责人信息。
     * </p>
     *
     * @param request  分页与条件请求对象
     * @param tenantId 当前登录用户的租户ID
     *
     * @return 组装完成的直播间分页数据
     */
    @Override
    public PageData<LiveRoomResponse> pageQueryLiveRoom(LiveRoomQueryRequest request, long tenantId) {
        PageData<LiveRoomResponse> pageData = CustomPage.execute(request, (page, req) -> {
            return super.getBaseMapper().pageQueryLiveRoom(page, req, tenantId);
        });

        Complete.start(pageData.getList())
            .build(LiveRoomResponse::getCompanyId, LiveRoomResponse::setCompanyName, companyService::getNameMap)
            .then()
            .build(LiveRoomResponse::getDeptId, LiveRoomResponse::setDeptName, deptService::getDeptNameMap)
            .then()
            .build(LiveRoomResponse::getTeamId, LiveRoomResponse::setTeamName, teamService::getTeamNameMap)
            .then()
            .build(LiveRoomResponse::getId, LiveRoomResponse::setManagerUserInfos, ids -> connectorProcessor.getConnectorManagerInfos(ManagerType.LIVE_ROOM, ids))
            .then()
            .over();
        return pageData;
    }

    @Override
    public PageData<LiveRoomResponse> pageQueryLiveRoomWithTodayPerformance(LiveRoomQueryRequest request, long tenantId) {
        PageData<LiveRoomResponse> pageData = CustomPage.execute(request, (page, req) -> {
            return super.getBaseMapper().pageQueryLiveRoomWithTodayPerformance(page, req, tenantId);
        });

        Complete.start(pageData.getList())
            .build(LiveRoomResponse::getCompanyId, LiveRoomResponse::setCompanyName, companyService::getNameMap)
            .then()
            .build(LiveRoomResponse::getDeptId, LiveRoomResponse::setDeptName, deptService::getDeptNameMap)
            .then()
            .build(LiveRoomResponse::getTeamId, LiveRoomResponse::setTeamName, teamService::getTeamNameMap)
            .then()
            .build(LiveRoomResponse::getId, LiveRoomResponse::setManagerUserInfos, ids -> connectorProcessor.getConnectorManagerInfos(ManagerType.LIVE_ROOM, ids))
            .then()
            .over();
        return pageData;
    }

    /**
     * 获取指定直播间的详细信息
     *
     * @param id       直播间主键ID
     * @param tenantId 当前登录用户的租户ID
     *
     * @return 直播间详情（包含完整关联数据），若不存在则返回 null
     */
    @Override
    @BeforePermission(type = OauthConstant.LIVE_ROOM, dataId = "#id")
    public LiveRoomResponse getLiveRoomDetail(long id, long tenantId) {
        LiveRoom existing = super.lambdaQuery()
            .eq(LiveRoom::getId, id)
            .eq(LiveRoom::getTenantId, tenantId)
            .eq(LiveRoom::getIsDeleted, false)
            .one();

        if (existing == null) {
            return null;
        }

        LiveRoomResponse response = BeanUtil.copyProperties(existing, LiveRoomResponse.class);

        Complete.start(java.util.List.of(response))
            .build(LiveRoomResponse::getCompanyId, LiveRoomResponse::setCompanyName, companyService::getNameMap)
            .then()
            .build(LiveRoomResponse::getDeptId, LiveRoomResponse::setDeptName, deptService::getDeptNameMap)
            .then()
            .build(LiveRoomResponse::getTeamId, LiveRoomResponse::setTeamName, teamService::getTeamNameMap)
            .then()
            .build(LiveRoomResponse::getId, LiveRoomResponse::setManagerUserInfos, ids -> connectorProcessor.getConnectorManagerInfos(ManagerType.LIVE_ROOM, ids))
            .then()
            .over();

        return response;
    }


    /**
     * 直播间搜索查询
     *
     * @param request 搜索查询请求对象
     *
     * @return {@link List }<{@link LabelOption }>
     */
    @Override
    public List<LabelOption> options(LiveRoomSearchQueryRequest request) {
        return super.getBaseMapper().listLiveRooms(request)
            .stream()
            .map(item -> new LabelOption(item.getId(), item.getAnchorName() + "(" + item.getAnchorNumber() + ")"))
            .collect(Collectors.toList());
    }


    /**
     * 获取直播间组织架构
     *
     * @param request 搜索查询请求对象
     *
     * @return {@link List }<{@link LiveRoomOrgResponse }>
     */
    @Override
    public List<LiveRoomOrgResponse> getLiveRoomOrg(LiveRoomSearchQueryRequest request) {
        return super.getBaseMapper().listLiveRooms(request).stream().map(lr -> {
            LiveRoomOrgResponse liveRoomOrgResponse = new LiveRoomOrgResponse();
            liveRoomOrgResponse.setRoomId(lr.getId());
            liveRoomOrgResponse.setCompanyId(lr.getCompanyId());
            liveRoomOrgResponse.setDeptId(lr.getDeptId());
            liveRoomOrgResponse.setTeamId(lr.getTeamId());
            return liveRoomOrgResponse;
        }).toList();
    }

    /**
     * 设置直播间排班配置
     *
     * @param request  设置请求参数
     * @param tenantId 当前登录用户的租户ID
     * @param userId   当前操作人ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission(type = OauthConstant.LIVE_ROOM, dataId = "#request.id")
    public ApiResponse<Void> setScheduleAttribute(LiveRoomScheduleAttributeSetRequest request, long tenantId, long userId) {
        // 1. 验证直播间是否存在且属于当前租户
        LiveRoom liveRoom = super.lambdaQuery()
            .eq(LiveRoom::getId, request.getId())
            .eq(LiveRoom::getTenantId, tenantId)
            .eq(LiveRoom::getIsDeleted, false)
            .one();

        if (liveRoom == null) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "直播间不存在");
        }

        // 2. 转换数据
        LiveRoomScheduleAttribute entity = new LiveRoomScheduleAttribute();
        entity.setId(request.getId());
        entity.formatStartPlan(request.getStartPlan());
        entity.formatEndPlan(request.getEndPlan());
        entity.setShiftOptions(request.getShiftOptions());
        entity.setRestOptions(request.getRestOptions());
        entity.setPositionOptions(request.getPositionOptions());

        // 3. 验证排班配置是否存在，决定新增还是修改
        boolean exists = ChainWrappers.lambdaQueryChain(scheduleAttributeMapper)
            .eq(LiveRoomScheduleAttribute::getId, request.getId())
            .exists();
        if (!exists) {
            entity.setCreateBy(userId);
            entity.setUpdateBy(userId);
            entity.setCreateDate(LocalDateTime.now());
            entity.setUpdateDate(LocalDateTime.now());
            scheduleAttributeMapper.insert(entity);
            log.info("[直播间管理] 新增直播间排班配置：id: {}, tenantId: {}", request.getId(), tenantId);
        } else {
            entity.setUpdateBy(userId);
            entity.setUpdateDate(LocalDateTime.now());
            scheduleAttributeMapper.updateById(entity);
            log.info("[直播间管理] 修改直播间排班配置：id: {}, tenantId: {}", request.getId(), tenantId);
        }

        return ApiResponse.success();
    }

    /**
     * 获取直播间排班配置
     *
     * @param id       直播间主键ID
     * @param tenantId 当前登录用户的租户ID
     *
     * @return 直播间排班配置详情
     */
    @BeforePermission(type = OauthConstant.LIVE_ROOM, dataId = "#id")
    @Override
    public LiveRoomScheduleAttributeResponse getScheduleAttribute(long id, long tenantId) {
        LiveRoomScheduleAttribute existing = scheduleAttributeMapper.selectById(id);
        if (existing == null) {
            return null;
        }

        LiveRoomScheduleAttributeResponse response = new LiveRoomScheduleAttributeResponse();
        response.setId(existing.getId());
        response.setStartPlan(existing.parseStartPlan());
        response.setEndPlan(existing.parseEndPlan());
        response.setShiftOptions(existing.getShiftOptions());
        response.setRestOptions(existing.getRestOptions());
        List<Long> positionIds = existing.getPositionOptions();
        Map<Long, Position> positionMap = positionService.getPositionMap(positionIds);
        response.setPositionOptions(positionIds.stream().map(positionId -> {
            Position position = positionMap.get(positionId);
            return new IdName(positionId, position == null ? "已删除" : position.getName());
        }).sorted((o1, o2) -> {
            Position position1 = positionMap.get(o1.getId());
            Position position2 = positionMap.get(o2.getId());
            if (position1 == null && position2 == null) {
                return 0;
            }
            if (position1 == null) {
                return 1;
            }
            if (position2 == null) {
                return -1;
            }
            return Integer.compare(position1.getSort(), position2.getSort());
        }).toList());
        return response;
    }


    /**
     * 获取员工关联的直播间
     *
     * @param employeeId  员工ID
     * @param positionId  职位ID
     * @param deadlineDay 截止日期
     *
     * @return {@link List }<{@link LiveRoomInfo }>
     */
    @Override
    public List<LiveRoomInfo> getEmployeeJoinRooms(long employeeId, Long positionId, LocalDate deadlineDay) {
        List<Long> roomIds = super.getBaseMapper().getEmployeeJoinRoomIds(employeeId, positionId, deadlineDay);
        if (EmptyUtil.isEmpty(roomIds)) {
            return List.of();
        }
        return getRoomInfos(roomIds);
    }

    /**
     * 批量获取直播间信息
     *
     * @param roomIds 直播间ID列表
     *
     * @return {@link List }<{@link LiveRoomInfo }>
     */
    @Override
    public List<LiveRoomInfo> getRoomInfos(Collection<Long> roomIds) {
        if (EmptyUtil.isEmpty(roomIds)) {
            return List.of();
        }
        return super.lambdaQuery()
            .in(LiveRoom::getId, roomIds)
            .eq(LiveRoom::getIsDeleted, false)
            .select(LiveRoom::getId, LiveRoom::getAnchorName, LiveRoom::getAnchorNumber, LiveRoom::getHomeUrl, LiveRoom::getLiveUrl, LiveRoom::getAnchorAvatar)
            .list().stream()
            .map(item -> {
                LiveRoomInfo liveRoomInfo = new LiveRoomInfo();
                liveRoomInfo.setId(item.getId());
                liveRoomInfo.setPlatform(item.getPlatform());
                liveRoomInfo.setAnchorNumber(item.getAnchorNumber());
                liveRoomInfo.setSecUid(item.getSecUid());
                liveRoomInfo.setHomeUrl(item.getHomeUrl());
                liveRoomInfo.setLiveUrl(item.getLiveUrl());
                liveRoomInfo.setAnchorName(item.getAnchorName());
                liveRoomInfo.setAnchorAvatar(item.getAnchorAvatar());
                return liveRoomInfo;
            })
            .collect(Collectors.toList());
    }

    /**
     * 批量获取员工关联的直播间
     *
     * @param employeeIds 员工ID列表
     * @param positionId  职位ID
     * @param deadlineDay 截止日期
     *
     * @return {@link Map }<{@link Long }, {@link List }<{@link LiveRoomInfo }>>
     */
    @Override
    public Map<Long, List<LiveRoomInfo>> getEmployeeJoinRooms(List<Long> employeeIds, Long positionId, LocalDate deadlineDay) {
        if (EmptyUtil.isEmpty(employeeIds)) {
            return Map.of();
        }
        List<ScheduleEmployee> employeeJoinRoomInfos = super.getBaseMapper().getEmployeeJoinRoomInfos(employeeIds, positionId, deadlineDay);
        if (EmptyUtil.isEmpty(employeeJoinRoomInfos)) {
            return Map.of();
        }
        Map<Long, List<Long>> employeeRoomMap = employeeJoinRoomInfos.stream().collect(Collectors.groupingBy(ScheduleEmployee::getEmployeeId, Collectors.mapping(ScheduleEmployee::getLiveRoomId, Collectors.toList())));
        Map<Long, LiveRoomInfo> roomMap = this.getRoomInfos(employeeRoomMap.values().stream().flatMap(Collection::stream).distinct().toList()).stream().collect(Collectors.toMap(LiveRoomInfo::getId, Function.identity()));
        return employeeRoomMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            List<LiveRoomInfo> list = entry.getValue().stream().map(roomMap::get).filter(Objects::nonNull).toList();
            if (list == null) {
                return List.of();
            }
            return list;
        }));
    }

    /**
     * 获取直播间名称映射
     *
     * @param roomIds 直播间ID列表
     *
     * @return {@link Map }<{@link Long }, {@link String }>
     */
    @Override
    public Map<Long, String> getLiveRoomNameMap(List<Long> roomIds) {
        if (EmptyUtil.isEmpty(roomIds)) {
            return Map.of();
        }
        return super.lambdaQuery()
            .in(LiveRoom::getId, roomIds)
            .eq(LiveRoom::getIsDeleted, false)
            .select(LiveRoom::getId, LiveRoom::getAnchorName, LiveRoom::getAnchorNumber)
            .list().stream()
            .collect(Collectors.toMap(LiveRoom::getId, live -> live.getAnchorName() + "(" + live.getAnchorNumber() + ")"));
    }

    /**
     * 获取组织架构下直播间数量
     *
     * @param orgIds      组织架构ID列表
     * @param managerType 管理类型
     * @param tenantId    当前登录用户的租户ID
     *
     * @return {@link Map }<{@link Long }, {@link Integer }>
     */
    @Override
    public Map<Long, Integer> countOrgRoomMap(List<Long> orgIds, ManagerType managerType, long tenantId) {
        if (EmptyUtil.isEmpty(orgIds) || managerType == null || ManagerType.EMPLOYEE.equals(managerType) || ManagerType.LIVE_ROOM.equals(managerType)) {
            return Map.of();
        }
        List<CountData> countDataList = super.getBaseMapper().countOrgRoomMap(orgIds, managerType.getValue(), tenantId);
        if (EmptyUtil.isEmpty(countDataList)) {
            return Map.of();
        }
        return countDataList.stream().collect(Collectors.toMap(CountData::getId, c -> c.getCount().intValue()));
    }

    /**
     * 获取直播间所属租户ID映射
     *
     * @param platformType 平台类型
     * @param secUids      主播 secUid 列表
     *
     * @return {@link Map }<{@link String }, {@link List }<{@link Long }>>
     */
    @Override
    public Map<String, List<Long>> getRoomTenantMap(LivePlatformType platformType, List<String> secUids) {
        if (EmptyUtil.isEmpty(secUids)) {
            return Map.of();
        }
        List<LiveRoom> roomTenants = super.lambdaQuery()
            .eq(LiveRoom::getPlatform, platformType.getValue())
            .in(LiveRoom::getSecUid, secUids)
            .eq(LiveRoom::getIsDeleted, false)
            .select(LiveRoom::getSecUid, LiveRoom::getTenantId)
            .list();
        if (EmptyUtil.isEmpty(roomTenants)) {
            return Map.of();
        }
        return roomTenants.stream().collect(Collectors.groupingBy(LiveRoom::getSecUid, Collectors.mapping(LiveRoom::getTenantId, Collectors.toList())));
    }
}
