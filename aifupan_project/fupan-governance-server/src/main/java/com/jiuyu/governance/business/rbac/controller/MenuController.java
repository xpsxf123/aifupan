package com.jiuyu.governance.business.rbac.controller;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.business.rbac.service.RoleService;
import com.jiuyu.governance.plugins.oauth.SystemUser;
import com.jiuyu.governance.business.rbac.pojo.request.MenuAddRequest;
import com.jiuyu.governance.business.rbac.pojo.request.MenuUpdateRequest;
import com.jiuyu.governance.business.rbac.pojo.response.MenuTreeResponse;
import com.jiuyu.governance.business.rbac.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台端-菜单管理
 *
 * @author HeHui
 * @date 2026-03-18
 */
@SystemUser
@RestController
@RequestMapping("/api/governance/menu")
public class MenuController {

    private final MenuService menuService;

    private final RoleService roleService;

    public MenuController(MenuService menuService, RoleService roleService) {
        this.menuService = menuService;
        this.roleService = roleService;
    }

    /**
     * 新增菜单
     *
     * @param request 新增请求参数
     * @return ApiResponse
     */
    @PostMapping("/add")
    public ApiResponse<Void> addMenu(@Valid @RequestBody MenuAddRequest request) {
        return menuService.addMenu(request);
    }

    /**
     * 修改菜单
     *
     * @param request 修改请求参数
     * @return ApiResponse
     */
    @PostMapping("/update")
    public ApiResponse<Void> updateMenu(@Valid @RequestBody MenuUpdateRequest request) {
        ApiResponse<Void> apiResponse = menuService.updateMenu(request);
        if (apiResponse.ok()) {
            roleService.clearAllCache();
        }
        return apiResponse;
    }

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     * @return ApiResponse
     */
    @PostMapping("/delete/{id}")
    public ApiResponse<Void> deleteMenu(@PathVariable("id") Long id) {
        ApiResponse<Void> apiResponse = menuService.deleteMenu(id);
        if (apiResponse.ok()) {
            roleService.clearAllCache();
        }
        return apiResponse;
    }

    /**
     * 获取菜单树形结构
     *
     * @return ApiResponse包含树形结构列表
     */
    @GetMapping("/tree")
    public ApiResponse<List<MenuTreeResponse>> getMenuTree() {
        return ApiResponse.success(menuService.getMenuTree());
    }

}
