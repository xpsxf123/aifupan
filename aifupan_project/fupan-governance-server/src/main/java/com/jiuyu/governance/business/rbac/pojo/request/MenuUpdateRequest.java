package com.jiuyu.governance.business.rbac.pojo.request;

import com.jiuyu.governance.business.rbac.pojo.constants.MenuType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * 修改菜单请求
 */
@Getter
@Setter
public class MenuUpdateRequest {

    /**
     * ID
     */
    @NotNull(message = "缺少菜单信息")
    private Long id;

    /**
     * 父菜单id
     */
    @NotNull(message = "请选择父菜单")
    private Long parentId;

    /**
     * 菜单名
     */
    @Size(max = 50, message = "菜单名最长不能超过50个字符")
    @NotBlank(message = "请输入菜单名")
    private String name;

    /**
     * 菜单url
     */
    @Size(max = 100, message = "菜单url最长不能超过100个字符")
    private String url;

    /**
     * 菜单类型 0：菜单 1：功能 2：目录
     */
    @NotNull(message = "请选择菜单类型")
    private MenuType type;

    /**
     * 排序
     */
    @NotNull(message = "请设置排序值")
    private Integer sort;

    /**
     * 图标
     */
    @Length(max = 100, message = "图标最长不能超过100个字符")
    private String img;

    /**
     * 权限码，功能类型菜单需要
     */
    @Length(max = 50, message = "权限码最长不能超过50个字符")
    private String permissionCode;
}
