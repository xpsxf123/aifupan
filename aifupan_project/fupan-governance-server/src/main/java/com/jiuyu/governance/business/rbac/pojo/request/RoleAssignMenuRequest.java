package com.jiuyu.governance.business.rbac.pojo.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 角色分配菜单请求
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Getter
@Setter
public class RoleAssignMenuRequest {

    /**
     * 角色ID
     */
    @NotNull(message = "缺少角色ID")
    private Long roleId;

    /**
     * 菜单ID列表
     */
    @NotNull(message = "请选择菜单")
    @Size(max = 3000, message = "菜单ID列表不能超过3000个")
    private List<Long> menuIds;

}
