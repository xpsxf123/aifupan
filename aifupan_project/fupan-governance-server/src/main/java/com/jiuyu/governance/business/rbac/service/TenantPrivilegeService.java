package com.jiuyu.governance.business.rbac.service;


import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.business.rbac.pojo.response.TenantStatusResponse;

import java.util.List;

/**
 * 租户权益服务
 *
 * @author HeHui
 * @date 2026-03-27 15:15
 */
public interface TenantPrivilegeService {

    /**
     * 判断租户是否可用
     *
     * @param tenantId 租户 ID
     *
     * @return true 表示可用，false 表示不可用
     */
    boolean available(long tenantId);


    /**
     * 获取可用租户
     *
     * @param tenantIds      租户 ID 列表
     * @param forceRefresh   是否强制刷新
     *
     * @return 可用租户 ID 列表
     */
    List<Long> availableTenantIds(List<Long> tenantIds, boolean forceRefresh);


    /**
     * 设置租户可用
     *
     * @param tenantId 租户 ID
     */
    void setAvailable(long tenantId);


    /**
     * 租户版本过期
     *
     * @param tenantIds 租户 ID 列表
     */
    void expired(List<Long> tenantIds);


    /**
     * 加载可用租户
     *
     * @param idx   索引
     * @param limit 限制
     *
     * @return {@link List }<{@link Long }>
     */
    List<Long> loadAvailable(Long idx, int limit);


    /**
     * 冻结租户
     *
     * @param tenantId 租户 ID
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    ApiResponse<Void> freeze(long tenantId);

    /**
     * 获取租户状态
     *
     * @param tenantIds 租户 ID 列表
     *
     * @return {@link List }<{@link TenantStatusResponse }>
     */
    List<TenantStatusResponse> getTenantStatus(List<Long> tenantIds);

    /**
     * 判断租户是否存在
     *
     * @param tenantId 租户 ID
     *
     * @return true 存在，false 不存在
     */
    boolean hasTenant(long tenantId);
}
