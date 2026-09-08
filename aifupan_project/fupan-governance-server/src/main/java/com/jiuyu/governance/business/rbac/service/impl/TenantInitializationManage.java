package com.jiuyu.governance.business.rbac.service.impl;

import com.jiuyu.framework.lock.ResourceLock;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.common.init.InitializationTenant;
import com.jiuyu.governance.common.init.TenantInitContext;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 租户初始化管理
 *
 * @author HeHui
 * @date 2026-04-07 18:23
 */
@Component
@Slf4j
public class TenantInitializationManage {

    private final List<InitializationTenant> initializationTenants;


    /**
     * 构造方法
     *
     * @param initializationTenantProvider 初始化租户提供者
     */
    public TenantInitializationManage(ObjectProvider<InitializationTenant> initializationTenantProvider) {
        this.initializationTenants = initializationTenantProvider.orderedStream().toList();
    }


    /**
     * 执行租户初始化操作
     * <p>
     * 遍历所有配置的租户初始化器，依次对指定租户执行初始化。
     * 如果未配置任何初始化器，则返回失败响应。
     *
     * @param accessUser 当前访问用户信息，用于初始化过程中的权限校验和操作记录
     * @param mainAccountId 主账户ID，需要初始化的租户标识
     * @return ApiResponse<Void> 初始化结果，成功返回 SUCCESS，失败返回错误信息
     */
    @Transactional(rollbackFor = Throwable.class)
    @ResourceLock(prefix = "tenant:init", key = "#mainAccountId")
    public ApiResponse<Void> runInit(AccessUser accessUser, long mainAccountId) {
        if (EmptyUtil.isEmpty(initializationTenants)) {
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "未配置初始化租户");
        }
        TenantInitContext context = new TenantInitContext(mainAccountId);
        context.setTenantId(accessUser.currentTenantId());
        initializationTenants.forEach(initializationTenant -> initializationTenant.init(context, accessUser));
        return ApiResponse.success();
    }
}
