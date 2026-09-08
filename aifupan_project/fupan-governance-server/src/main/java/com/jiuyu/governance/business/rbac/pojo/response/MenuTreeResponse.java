package com.jiuyu.governance.business.rbac.pojo.response;

import com.jiuyu.framework.shandard.TreeNode;
import com.jiuyu.governance.business.rbac.pojo.constants.MenuType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树形视图对象
 */
@Getter
@Setter
public class MenuTreeResponse implements TreeNode<Long, Integer, MenuTreeResponse> {
    /**
     * 菜单ID
     */
    private Long id;

    /**
     * 父菜单id
     */
    private Long parentId;

    /**
     * 菜单名
     */
    private String name;

    /**
     * 菜单url
     */
    private String url;

    /**
     * 菜单类型
     */
    private MenuType type;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 图标
     */
    private String img;

    /**
     * 权限码
     */
    private String permissionCode;

    /**
     * 创建时间
     */
    private LocalDateTime createDate;

    /**
     * 最后修改时间
     */
    private LocalDateTime updateDate;

    /**
     * 子节点
     */
    private List<MenuTreeResponse> children = new ArrayList<>();


}
