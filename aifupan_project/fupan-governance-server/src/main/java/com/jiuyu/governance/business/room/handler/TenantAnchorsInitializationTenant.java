package com.jiuyu.governance.business.room.handler;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomSyncTenantAnchorsRequest;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomSyncResultResponse;
import com.jiuyu.governance.business.room.service.LiveRoomService;
import com.jiuyu.governance.common.init.InitializationTenant;
import com.jiuyu.governance.common.init.TenantInitContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 同步租户主播到直播间
 * <p>
 * 在主账户身份就绪之后执行，调用 {@link LiveRoomService#syncTenantAnchors} 将 replay
 * 侧的租户主播同步为直播间。同步失败不应阻断整个租户初始化流程，因此异常被吞掉并仅以日志方式报告。
 *
 * @author HeHui
 * @date 2026-05-23
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TenantAnchorsInitializationTenant implements InitializationTenant {

    private final LiveRoomService liveRoomService;

    @Override
    public int getOrder() {
        return 110;
    }

    @Override
    public void init(TenantInitContext tenantContext, AccessUser accessUser) {
        try {
            Long adminId = tenantContext.getTenantAdminId();
            long operatorId = adminId == null ? 0L : adminId;
            LiveRoomSyncTenantAnchorsRequest request = new LiveRoomSyncTenantAnchorsRequest();
            ApiResponse<LiveRoomSyncResultResponse> result = liveRoomService.syncTenantAnchors(request,
                tenantContext.getTenantId(), operatorId);
            log.info("[租户初始化] syncTenantAnchors tenant: {}, operator: {}, code: {}, msg: {}, data: {}",
                tenantContext.getTenantId(), operatorId, result.getCode(), result.getMsg(), result.getData());
        } catch (Exception e) {
            log.warn("[租户初始化] syncTenantAnchors failed, tenant: {}, err: {}",
                tenantContext.getTenantId(), e.getMessage(), e);
        }
    }
}
