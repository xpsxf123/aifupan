package com.jiuyu.governance.business.rbac.pojo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * 角色新增请求
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Getter
@Setter
public class RoleAddRequest {

    /**
     * 角色名
     */
    @Length(max = 100, message = "角色名最长不能超过100个字符")
    @NotBlank(message = "请输入角色名")
    private String name;

    /**
     * 菜单ID列表
     */
    @Size(max = 3000, message = "菜单ID列表不能超过3000个")
    private List<Long> menuIds;
}
