package com.jiuyu.governance.business.rbac.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色
 */
@Getter
@Setter
@TableName(value = "`role`")
public class Role {
    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @NotNull(message = "ID不能为null")
    private Long id;

    /**
     * 租户ID
     */
    @TableField(value = "tenant_id")
    @NotNull(message = "租户ID不能为null")
    private Long tenantId;

    /**
     * 角色名
     */
    @TableField(value = "`name`")
    @Size(max = 100,message = "角色名最大长度要小于 100")
    @NotBlank(message = "角色名不能为空")
    private String name;

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

    /**
     * 是否默认角色
     */
    @TableField(value = "is_default")
    @NotNull(message = "是否默认角色不能为null")
    private Boolean isDefault;
}
