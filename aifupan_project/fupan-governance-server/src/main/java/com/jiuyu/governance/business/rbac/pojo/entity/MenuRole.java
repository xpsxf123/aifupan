package com.jiuyu.governance.business.rbac.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 菜单-角色-中间表
 */
@Getter
@Setter
@TableName(value = "menu_role")
public class MenuRole {
    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @NotNull(message = "ID不能为null")
    private Long id;

    /**
     * 关联的角色id
     */
    @TableField(value = "role_id")
    @NotNull(message = "关联的角色id不能为null")
    private Long roleId;

    /**
     * 关联的菜单id
     */
    @TableField(value = "menu_id")
    @NotNull(message = "关联的菜单id不能为null")
    private Long menuId;

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
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    @NotNull(message = "是否已删除不能为null")
    private Boolean isDeleted;
}
