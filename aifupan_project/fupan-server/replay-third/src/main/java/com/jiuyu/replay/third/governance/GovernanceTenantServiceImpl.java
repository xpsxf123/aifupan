package com.jiuyu.replay.third.governance;

import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.http.governance.GovernanceHttpServer;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.governance.response.TenantStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 企业后台租户服务实现
 *
 * @author HeHui
 * @date 2026-04-07 17:06
 */
@Service
@Slf4j
public class GovernanceTenantServiceImpl implements GovernanceTenantService {

    private final GovernanceHttpServer httpServer;

    private final ParameterizedTypeReference<R<List<TenantStatusResponse>>> statusType = new ParameterizedTypeReference<R<List<TenantStatusResponse>>>() {
    };

    public GovernanceTenantServiceImpl(GovernanceHttpServer httpServer) {
        this.httpServer = httpServer;
    }

    /**
     * 获取租户状态列表 - 需要登录
     *
     * @param tenantIds 租户ID列表
     *
     * @return 状态列表
     */
    @Override
    public List<TenantStatusResponse> getTenantStatusList(Collection<Long> tenantIds) {
        if (EmptyUtil.isEmpty(tenantIds)) {
            return List.of();
        }
        R<List<TenantStatusResponse>> response = httpServer.post("/api/governance/tenant/status", tenantIds, null)
            .retrieve().body(statusType);
        if (response == null) {
            log.warn("[企业租户] get tenant status rows empty, {}", tenantIds);
            return List.of();
        }
        if (response.fail()) {
            log.warn("[企业租户] get tenant status rows fail, {}, {}", response.getMsg(), tenantIds);
            return List.of();
        }
        return response.getData();
    }
}
