package com.jiuyu.governance.business.rbac.task;

import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.pojo.entity.Employee;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.business.rbac.service.TenantPrivilegeService;
import com.jiuyu.governance.business.rbac.service.impl.EmployeeOauthManage;
import com.jiuyu.governance.common.init.InitializationTenant;
import com.jiuyu.governance.openfeign.replay.UserAccountService;
import com.jiuyu.governance.openfeign.replay.response.ReplayAccountDetailResponse;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 租户版本套餐过期任务
 *
 * @author HeHui
 * @date 2026-03-30 11:28
 */
@Component
@Slf4j
public class TenantVersionExpiredJob {

    private final TenantPrivilegeService tenantPrivilegeService;

    private final EmployeeService employeeService;

    private final UserAccountService accountService;

    private final EmployeeOauthManage oauthManage;

    public TenantVersionExpiredJob(TenantPrivilegeService tenantPrivilegeService, EmployeeService employeeService, UserAccountService accountService, EmployeeOauthManage oauthManage) {
        this.tenantPrivilegeService = tenantPrivilegeService;
        this.employeeService = employeeService;
        this.accountService = accountService;
        this.oauthManage = oauthManage;
    }

    /**
     * 租户版本套餐过期任务
     */
    @XxlJob("tenantVersionExpired")
    public void execute() throws Exception {
        log.info("[定时任务] start >>>> tenantVersionExpired");
        BatchQuery<Long, Employee> batchQuery = new BatchQuery<>((limit, idx) -> {
            log.info("[定时任务] load tenant hold employee...  idx: {}, limit: {}", idx, limit);
            return employeeService.loadTenantHoldEmployee(idx, limit, null);
        }, Employee::getId);


        batchQuery.consumer(employees -> {
            log.info("[定时任务] load tenant hold employee... size: {}", employees.size());
            List<Long> expiredTenantIds = new ArrayList<>(employees.size());
            employees.forEach(employee -> {
                log.info("[定时任务] load tenant hold employee... tenant: {}, employee: {}, queryAccount Package", employee.getTenantId(), employee.getName());
                ApiResponse<ReplayAccountDetailResponse> apiResponse = accountService.getMainAccountDetail(employee.getConnectorClientUserId());
                if (apiResponse.failed()) {
                    log.warn("[定时任务] get main account detail failed, tenant: {}, accountId: {}, {} . msg: {}", employee.getTenantId(), employee.getConnectorClientUserId(), employee.getName(), apiResponse.getMsg());
                    return;
                }
                if (apiResponse.getData() == null) {
                    log.warn("[定时任务] get main account detail result empty, tenant: {}, accountId: {}, {} ", employee.getTenantId(), employee.getConnectorClientUserId(), employee.getName());
                    expiredTenantIds.add(employee.getTenantId());
                    return;
                }
                ReplayAccountDetailResponse account = apiResponse.getData();
                if (account.getPackageLevel() == null) {
                    log.warn("[定时任务] get main account detail result packageLevel null, tenant: {}, accountId: {}, {} ", employee.getTenantId(), employee.getConnectorClientUserId(), employee.getName());
                    expiredTenantIds.add(employee.getTenantId());
                    return;
                }
                if (account.getPackageLevel() < InitializationTenant.MIN_GOVERNANCE_LEVEL) {
                    log.warn("[定时任务] get main account detail result {} packageLevel {} < {}, tenant: {}, accountId: {}, {} ", account.getPackageName(), account.getPackageLevel(), InitializationTenant.MIN_GOVERNANCE_LEVEL, employee.getTenantId(), employee.getConnectorClientUserId(), employee.getName());
                    expiredTenantIds.add(employee.getTenantId());
                    return;
                }
                // 可用
                tenantPrivilegeService.setAvailable(employee.getTenantId());
            });
            if (EmptyUtil.isNotEmpty(expiredTenantIds)) {
                tenantPrivilegeService.expired(expiredTenantIds);
                oauthManage.forcedForTenant(expiredTenantIds);
                log.info("[定时任务] tenant expired... size: {}", expiredTenantIds.size());
            }
        });
        long count = batchQuery.run();
        log.info("[定时任务] tenant expired... done, count: {}", count);
    }
}
