package com.jiuyu.governance.business.rbac.pojo.response;

import lombok.Data;

import java.util.List;

/**
 * 角色详情响应
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Data
public class RoleDetailResponse {
    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 角色名
     */
    private String name;

    /**
     * 菜单树
     */
    private List<MenuTreeResponse> menuTree;
}
