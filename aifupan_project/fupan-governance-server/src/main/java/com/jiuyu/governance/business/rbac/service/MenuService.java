package com.jiuyu.governance.business.rbac.service;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.business.rbac.pojo.entity.Menu;
import com.jiuyu.governance.business.rbac.pojo.request.MenuAddRequest;
import com.jiuyu.governance.business.rbac.pojo.request.MenuUpdateRequest;
import com.jiuyu.governance.business.rbac.pojo.response.MenuTreeResponse;

import java.util.List;

/**
 * 菜单服务接口
 *
 * @author HeHui
 * @date 2026-03-18
 */
public interface MenuService {

    /**
     * 添加菜单
     *
     * @param request 添加请求
     */
    ApiResponse<Void> addMenu(MenuAddRequest request);

    /**
     * 更新菜单
     *
     * @param request 更新请求
     */
    ApiResponse<Void> updateMenu(MenuUpdateRequest request);

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     */
    ApiResponse<Void> deleteMenu(Long id);

    /**
     * 获取菜单树
     *
     * @return 菜单树列表
     */
    List<MenuTreeResponse> getMenuTree();


    /**
     * 构建树形结构
     *
     * @param menuIds 菜单ID列表
     * @return 树形结构列表
     */
    List<MenuTreeResponse> buildTree(List<Long> menuIds);

    /**
     * 根据ID列表查询菜单
     *
     * @param menuIds 菜单ID列表
     * @return 菜单列表
     */
    List<MenuTreeResponse> listInIds(List<Long> menuIds);

    /**
     * 批量插入菜单
     *
     * @param menuList 菜单列表
     */
    void batchInsert(List<Menu> menuList);

    /**
     * 删除所有菜单
     */
    void deleteAll();

    /**
     * 列出所有菜单ID
     */
    List<Long> listAllIds();


    /**
     * 列出普通菜单ID
     */
    List<Long> getOrdinaryMenuIds();
}
