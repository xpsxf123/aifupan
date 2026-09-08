package com.jiuyu.governance.business.rbac.controller;

import com.jiuyu.governance.common.pojo.BizErrorCode;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.Permissions;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.common.pojo.bo.IdRequest;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import com.jiuyu.governance.business.rbac.pojo.request.RoleAddRequest;
import com.jiuyu.governance.business.rbac.pojo.request.RoleAssignMenuRequest;
import com.jiuyu.governance.business.rbac.pojo.request.RolePageQueryRequest;
import com.jiuyu.governance.business.rbac.pojo.request.RoleUpdateRequest;
import com.jiuyu.governance.business.rbac.pojo.response.RoleDetailResponse;
import com.jiuyu.governance.business.rbac.pojo.response.RoleResponse;
import com.jiuyu.governance.business.rbac.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业端-角色管理
 * 限定只能后台管理人员进入
 *
 * @author HeHui
 * @date 2026-03-18
 */
@GovernanceUser
@RestController
@RequestMapping("/api/governance/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 新增角色
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Permissions("sys:role:add")
    @PostMapping("/add")
    public ApiResponse<Void> addRole(@Valid @RequestBody RoleAddRequest request, AccessUser accessUser) {
        return roleService.addRole(request, accessUser.currentTenantId());
    }

    /**
     * 修改角色
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Permissions({"sys:role:update", "sys:role:add"})
    @PostMapping("/update")
    public ApiResponse<Void> updateRole(@Valid @RequestBody RoleUpdateRequest request, AccessUser accessUser) {
        return roleService.updateRole(request, accessUser.currentTenantId());
    }

    /**
     * 删除角色
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Permissions("sys:role:delete")
    @PostMapping("/delete")
    public ApiResponse<Void> deleteRole(@Valid @RequestBody IdRequest request, AccessUser accessUser) {
        return roleService.deleteRole(request.getId(), accessUser.currentTenantId());
    }

    /**
     * 分页查询角色
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link PageData }<{@link RoleResponse }>>
     */
    @PostMapping("/list")
    public ApiResponse<PageData<RoleResponse>> pageQueryRole(@RequestBody RolePageQueryRequest request, AccessUser accessUser) {
        return ApiResponse.success(roleService.pageQueryRole(request, accessUser.currentTenantId()));
    }

    /**
     * 给角色分配菜单
     *
     * @param request    参数
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Permissions("sys:role:assign-menu")
    @PostMapping("/assign-menus")
    public ApiResponse<Void> assignMenus(@Valid @RequestBody RoleAssignMenuRequest request, AccessUser accessUser) {
        if (!roleService.hasRole(request.getRoleId(), accessUser.currentTenantId())) {
            return ApiResponse.failed(BizErrorCode.NO_POWER.getCode(), "没有权限");
        }
        roleService.assignMenuToRole(request.getRoleId(), request.getMenuIds(), accessUser.currentTenantId());
        return ApiResponse.success();
    }

    /**
     * 获取角色的菜单列表
     *
     * @param id         ID
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link RoleDetailResponse }>
     */
    @GetMapping("/detail")
    public ApiResponse<RoleDetailResponse> getRoleMenus(@RequestParam Long id, AccessUser accessUser) {
        return ApiResponse.success(roleService.getRoleMenus(id, accessUser.currentTenantId()));
    }


    /**
     * 角色下拉选择
     *
     * @param keyword 关键词
     * @param limit   限制
     *
     * @return {@link ApiResponse }<{@link List }<{@link LabelOption }>>
     */
    @GetMapping("/options")
    public ApiResponse<List<LabelOption>> options(@RequestParam(required = false) String keyword, @RequestParam(required = false, defaultValue = "100") Integer limit, AccessUser accessUser) {
        return ApiResponse.success(roleService.options(keyword, accessUser.currentTenantId(), limit));
    }

}
