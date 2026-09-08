package com.jiuyu.governance.business.rbac.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jiuyu.framework.mybatispuls.type.LongListTypeHandler;
import com.jiuyu.governance.business.rbac.pojo.constants.MenuType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单
 */
@Getter
@Setter
@TableName(value = "menu", autoResultMap = true)
public class Menu {
    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @NotNull(message = "ID不能为null")
    private Long id;

    /**
     * 父菜单id
     */
    @TableField(value = "parent_id")
    @NotNull(message = "父菜单id不能为null")
    private Long parentId;

    /**
     * 菜单名
     */
    @TableField(value = "`name`")
    @Size(max = 50, message = "菜单名最大长度要小于 50")
    @NotBlank(message = "菜单名不能为空")
    private String name;

    /**
     * 菜单url
     */
    @TableField(value = "url")
    @Size(max = 100, message = "菜单url最大长度要小于 100")
    private String url;

    /**
     * 0：菜单  1：功能 2：目录
     */
    @TableField(value = "`type`")
    @NotNull(message = "0：菜单  1：功能 2：目录不能为null")
    private MenuType type;

    /**
     * 排序
     */
    @TableField(value = "sort")
    @NotNull(message = "排序不能为null")
    private Integer sort;

    /**
     * 图标
     */
    @TableField(value = "img")
    @Size(max = 100, message = "图标最大长度要小于 100")
    private String img;

    /**
     * 创建时间
     */
    @TableField(value = "create_date")
    @NotNull(message = "创建时间不能为null")
    private LocalDateTime createDate;

    /**
     * 最后修改时间
     */
    @TableField(value = "update_date")
    @NotNull(message = "最后修改时间不能为null")
    private LocalDateTime updateDate;

    /**
     * 权限码
     */
    @TableField(value = "permission_code")
    @Size(max = 50, message = "权限码最大长度要小于 50")
    @NotBlank(message = "权限码不能为空")
    private String permissionCode;

    /**
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    @NotNull(message = "是否已删除不能为null")
    private Boolean isDeleted;


    /**
     * 所有父节点ID
     */
    @TableField(value = "parent_path_ids", typeHandler = LongListTypeHandler.class)
    private List<Long> parentPathIds;
}
