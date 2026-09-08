package com.jiuyu.governance.business.rbac.service;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.rbac.pojo.response.MenuTreeResponse;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.business.rbac.pojo.request.RoleAddRequest;
import com.jiuyu.governance.business.rbac.pojo.request.RolePageQueryRequest;
import com.jiuyu.governance.business.rbac.pojo.request.RoleUpdateRequest;
import com.jiuyu.governance.business.rbac.pojo.response.RoleDetailResponse;
import com.jiuyu.governance.business.rbac.pojo.response.RoleResponse;

import java.util.List;
import java.util.Map;

/**
 * 角色服务接口
 *
 * @author HeHui
 * @date 2026-03-18
 */
public interface RoleService {


    /**
     * 获取默认角色ID
     *
     * @param tenantId 租户ID
     *
     * @return 默认角色ID
     */
    long getDefaultRoleId(long tenantId);

    /**
     * 添加角色
     *
     * @param request  添加请求
     * @param tenantId 租户ID
     */
    ApiResponse<Void> addRole(RoleAddRequest request, Long tenantId);

    /**
     * 更新角色
     *
     * @param request  更新请求
     * @param tenantId 租户ID
     */
    ApiResponse<Void> updateRole(RoleUpdateRequest request, Long tenantId);

    /**
     * 删除角色
     *
     * @param id       角色ID
     * @param tenantId 租户ID
     */
    ApiResponse<Void> deleteRole(Long id, Long tenantId);

    /**
     * 分页查询角色
     *
     * @param request  查询请求
     * @param tenantId 租户ID
     *
     * @return 角色分页数据
     */
    PageData<RoleResponse> pageQueryRole(RolePageQueryRequest request, Long tenantId);


    /**
     * 判断角色是否存在
     *
     * @param roleId   角色ID
     * @param tenantId 租户ID
     *
     * @return 角色是否存在
     */
    boolean hasRole(long roleId, long tenantId);


    /**
     * 给角色分配菜单
     *
     * @param roleId   角色ID
     * @param menuIds  菜单id
     * @param tenantId 租户ID
     */
    void assignMenuToRole(long roleId, List<Long> menuIds, Long tenantId);


    /**
     * 获取角色树
     *
     * @param roleIds  角色ID列表
     * @param tenantId 租户ID
     *
     * @return 角色树
     */
    List<MenuTreeResponse> getRolesTree(List<Long> roleIds, long tenantId);


    /**
     * 获取角色权限码
     *
     * @param roleIds  角色ID列表
     * @param tenantId 租户ID
     *
     * @return 角色权限码列表
     */
    List<String> getRolesPermissionCode(List<Long> roleIds, long tenantId);

    /**
     * 获取角色详情
     *
     * @param roleId   角色ID
     * @param tenantId 租户ID
     *
     * @return 角色拥有的菜单ID列表
     */
    RoleDetailResponse getRoleMenus(Long roleId, Long tenantId);

    /**
     * 角色下拉选择
     *
     * @param keyword  关键字
     * @param tenantId 租户ID
     * @param limit    限制数量
     *
     * @return 角色下拉选项
     */
    List<LabelOption> options(String keyword, long tenantId, Integer limit);

    /**
     * 获取角色名称映射
     *
     * @param ids 角色ID列表
     * @return 角色ID到名称的映射
     */
    Map<Long, String> getRoleNameMap(List<Long> ids);


    /**
     * 清空所有缓存
     */
    void clearAllCache();

    /**
     * 判断角色是否为默认角色
     *
     * @param roleIds  角色ID列表
     * @param tenantId 租户ID
     */
    boolean hasDefaultRole(List<Long> roleIds, long tenantId);
}
