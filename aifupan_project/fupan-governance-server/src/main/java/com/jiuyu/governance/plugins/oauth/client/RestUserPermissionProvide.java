package com.jiuyu.governance.plugins.oauth.client;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.provides.UserPermissionProvide;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.business.rbac.service.RoleService;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 远程调用判断用户是否有权限
 *
 * @author HeHui
 * @date 2026-03-17 18:53
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class RestUserPermissionProvide implements UserPermissionProvide {

    private final EmployeeService employeeService;

    private final RoleService roleService;

    /**
     * 检查用户是否具有指定的权限
     * <p>
     * 权限判断逻辑（按优先级顺序）：
     * 1. 如果权限码集合为空，默认返回true
     * 2. 系统后台用户拥有所有权限
     * 3. 客户端用户没有任何权限
     * 4. 租户管理员拥有所有权限
     * 5. 普通用户根据角色权限进行匹配判断
     *
     * @param accessUser 访问用户信息，包含用户ID、用户类型、租户ID等
     * @param permissionCodes 需要检查的权限码集合
     * @param mode 权限匹配模式：true表示需要满足所有权限（AND模式），false表示只需满足任一权限（OR模式）
     *
     * @return true表示用户具有权限，false表示用户没有权限
     */
    @Override
    public boolean hasPermissions(AccessUser accessUser, Collection<String> permissionCodes, boolean mode) {
        if (EmptyUtil.isEmpty(permissionCodes)) {
            return true;
        }
        if (Objects.equals(accessUser.userType(), OauthConstant.SYSTEM_USER)) {
            // 爱复盘后台用户 不管啥情况都有权限
            return true;
        }
        if (Objects.equals(accessUser.userType(), OauthConstant.CLIENT_USER)) {
            // 客户端用户不管啥时候都没有权限
            return false;
        }
        // 如果当前用户为租户管理员
        if (isTenantAdmin(accessUser)) {
            return true;
        }
        // 获取当前用户的角色ID
        List<Long> roleIds = employeeService.getEmployeeRoleIds(accessUser.userId(), accessUser.currentTenantId());
        if (EmptyUtil.isEmpty(roleIds)) {
            return false;
        }
        // 获取当前用户的角色权限码
        List<String> rolesPermissionCode = roleService.getRolesPermissionCode(roleIds, accessUser.currentTenantId());
        if (EmptyUtil.isEmpty(rolesPermissionCode)) {
            return false;
        }
        // 过滤掉null的权限码，只统计有效权限
        long validPermissionCount = permissionCodes.stream().filter(Objects::nonNull).count();
        if (validPermissionCount == 0) {
            return true;
        }
        long hitCount = permissionCodes.stream().filter(Objects::nonNull).filter(rolesPermissionCode::contains).count();
        if (mode) {
            // AND模式：需要拥有所有有效权限
            return hitCount == validPermissionCount;
        } else {
            // OR模式：只需拥有任一权限
            return hitCount > 0;
        }
    }



    /**
     * 判断当前用户是否是租户管理员
     *
     * @param accessUser 访问用户
     *
     * @return {@code true} 是
     */
    private boolean isTenantAdmin(AccessUser accessUser) {
        Map<String, String> metadata = accessUser.metadata();
        if (EmptyUtil.isEmpty(metadata)) {
            return false;
        }
        return Objects.equals(metadata.get(OauthConstant.TENANT_ADMIN), "true");
    }
}
