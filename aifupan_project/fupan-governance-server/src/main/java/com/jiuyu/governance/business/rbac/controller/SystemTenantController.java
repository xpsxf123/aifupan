package com.jiuyu.governance.business.rbac.controller;

import com.jiuyu.governance.business.rbac.pojo.response.TenantStatusResponse;
import com.jiuyu.governance.business.rbac.service.TenantPrivilegeService;
import com.jiuyu.governance.business.rbac.service.impl.TenantInitializationManage;
import com.jiuyu.governance.common.pojo.BizErrorCode;

import com.jiuyu.framework.lock.ResourceLock;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.common.init.InitializationTenant;
import com.jiuyu.governance.common.init.TenantInitContext;
import com.jiuyu.governance.common.pojo.bo.IdRequest;
import com.jiuyu.governance.plugins.oauth.SystemUser;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台端 - 租户相关API
 *
 * @author HeHui
 * @date 2026-03-24 10:40
 */
@RestController
@RequestMapping("/api/governance/tenant")
@SystemUser
public class SystemTenantController {

    private final TenantInitializationManage tenantInitializationManage;

    private final EmployeeService employeeService;

    private final TenantPrivilegeService tenantPrivilegeService;

    public SystemTenantController(TenantInitializationManage tenantInitializationManage, EmployeeService employeeService, TenantPrivilegeService tenantPrivilegeService) {
        this.tenantInitializationManage = tenantInitializationManage;
        this.employeeService = employeeService;
        this.tenantPrivilegeService = tenantPrivilegeService;
    }






    /**
     * 租户初始化接口
     * <p>
     * 调用配置的租户初始化器对指定租户进行初始化操作，包括数据初始化、配置初始化等。
     * 使用资源锁保证同一租户的初始化操作串行执行，避免并发冲突。
     *
     * @param request    请求参数，包含主账户ID
     * @param accessUser 当前访问用户信息，用于初始化过程中的权限校验和操作记录
     *
     * @return ApiResponse<Void> 初始化结果，成功返回 SUCCESS，失败返回错误信息
     */
    @Transactional(rollbackFor = Throwable.class)
    @PostMapping("/init")
    @ResourceLock(prefix = "tenant", key = "#request.id")
    public ApiResponse<Void> init(@RequestBody @Validated IdRequest request, AccessUser accessUser) {
        return tenantInitializationManage.runInit(accessUser, request.getId());
    }


    /**
     * 查询存在主账户的用户ID
     *
     * @param userIds 用户id
     *
     * @return {@link ApiResponse }<{@link List }<{@link Long }>>
     */
    @PostMapping("/exist-main-account")
    public ApiResponse<List<Long>> mainAccountExist(@RequestBody List<Long> userIds) {
        return ApiResponse.success(employeeService.mainAccountExist(userIds));
    }


    /**
     * 获取租户状态
     *
     * @param tenantIds 租户id
     *
     * @return {@link ApiResponse }<{@link List }<{@link TenantStatusResponse }>>
     */
    @PostMapping("/status")
    public ApiResponse<List<TenantStatusResponse>> getTenantStatus(@RequestBody List<Long> tenantIds) {
        return ApiResponse.success(tenantPrivilegeService.getTenantStatus(tenantIds));
    }

}
