package com.jiuyu.replay.third.governance;


import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.third.governance.response.TenantStatusResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企业后台租户服务
 *
 * @author HeHui
 * @date 2026-04-07 17:01
 */
public interface GovernanceTenantService {


    /**
     * 获取租户状态列表 - 需要登录
     *
     * @param tenantIds 租户ID列表
     * @return 状态列表
     */
    List<TenantStatusResponse> getTenantStatusList(Collection<Long> tenantIds);


    /**
     * 获取租户状态 - 需要登录
     *  转为用户列表所需状态: -1不允许开通，0待开通， 1停用（过期），2正常, 3冻结
     *  <p/>
     * {@code com.jiuyu.replay.power.vo.UserListVo#governanceStatus }
     * @param tenantIds 租户ID
     *
     * @return {@link Map }<{@link Long }, {@link Integer }>
     */
    default Map<Long, Integer> getTenantStatusForUserMap(Collection<Long> tenantIds) {
        if (EmptyUtil.isEmpty(tenantIds)) {
            return Map.of();
        }
        Map<Long, Integer> map = getTenantStatusList(tenantIds).stream().collect(Collectors.toMap(TenantStatusResponse::getId, TenantStatusResponse::getAccountStatus));
        return tenantIds.stream().collect(Collectors.toMap(id -> id, id -> {
            Integer status = map.get(id);
            if (status == null) {
                return 0;
            }
            if (status == 0) {
                return 1;
            }
            if (status == 1) {
                return 2;
            }
            if (status == 2) {
                return 3;
            }
            return status;
        }));
    }
}
