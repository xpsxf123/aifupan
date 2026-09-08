package com.jiuyu.governance.business.rbac.handler;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.common.init.InitializationTenant;
import com.jiuyu.governance.common.init.TenantInitContext;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 主账户身份同步子账户
 * <p>
 * 在 Employee 初始化（order=10）之后执行，使用主账户身份调用
 * {@link EmployeeService#syncSubAccount(AccessUser)} 拉取并落库 replay 侧的子账户。
 * 同步失败不应阻断整个租户初始化流程，因此异常被吞掉并仅以日志方式报告。
 *
 * @author HeHui
 * @date 2026-05-23
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SubAccountInitializationTenant implements InitializationTenant {

    private final EmployeeService employeeService;

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void init(TenantInitContext tenantContext, AccessUser accessUser) {
        try {
            AccessUser mainAccount = OauthConstant.buildAccessUser(tenantContext.getTenantId(),
                tenantContext.getMainAccountId(), OauthConstant.CLIENT_USER);
            ApiResponse<Void> result = employeeService.syncSubAccount(mainAccount);
            log.info("[租户初始化] syncSubAccount tenant: {}, mainAccountId: {}, code: {}, msg: {}",
                tenantContext.getTenantId(), tenantContext.getMainAccountId(), result.getCode(), result.getMsg());
        } catch (Exception e) {
            log.warn("[租户初始化] syncSubAccount failed, tenant: {}, mainAccountId: {}, err: {}",
                tenantContext.getTenantId(), tenantContext.getMainAccountId(), e.getMessage(), e);
        }
    }
}
