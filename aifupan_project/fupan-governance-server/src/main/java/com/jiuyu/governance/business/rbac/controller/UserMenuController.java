package com.jiuyu.governance.business.rbac.controller;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.business.rbac.service.MenuService;
import com.jiuyu.governance.business.rbac.service.RoleService;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import com.jiuyu.governance.business.rbac.pojo.response.MenuTreeResponse;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 企业端-用户菜单API
 *
 * @author HeHui
 * @date 2026-03-18 15:15
 */
@GovernanceUser
@RestController
@RequestMapping("/api/governance/user-menu")
public class UserMenuController {

    private final MenuService menuService;

    private final EmployeeService employeeService;

    private final RoleService roleService;

    public UserMenuController(MenuService menuService, EmployeeService employeeService, RoleService roleService) {
        this.menuService = menuService;
        this.employeeService = employeeService;
        this.roleService = roleService;
    }

    /**
     *  获取当前用户的菜单树
     *
     * @return ApiResponse包含树形结构列表
     */
    @GetMapping("/tree")
    public ApiResponse<List<MenuTreeResponse>> getMenuTree(AccessUser accessUser) {
        // todo 目前租户管理员获取所有菜单权限
        if (this.isTenantAdmin(accessUser)) {
            return ApiResponse.success(menuService.getMenuTree());
        }
        List<Long> roleIds = employeeService.getEmployeeRoleIds(accessUser.userId(), accessUser.currentTenantId());
        return ApiResponse.success(roleService.getRolesTree(roleIds, accessUser.currentTenantId()));
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
