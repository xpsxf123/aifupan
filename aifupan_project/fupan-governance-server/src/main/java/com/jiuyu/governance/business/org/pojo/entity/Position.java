package com.jiuyu.governance.business.org.pojo.entity;

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
 * 岗位
 */
@Getter
@Setter
@TableName(value = "`position`")
public class Position {
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
     * 岗位名称
     */
    @TableField(value = "`name`")
    @Size(max = 100,message = "岗位名称最大长度要小于 100")
    @NotBlank(message = "岗位名称不能为空")
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
     * 创建人
     */
    @TableField(value = "create_by")
    @NotNull(message = "创建人不能为null")
    private Long createBy;

    /**
     * 修改人
     */
    @TableField(value = "update_by")
    @NotNull(message = "修改人不能为null")
    private Long updateBy;

    /**
     * 排序
     */
    @TableField(value = "sort")
    @NotNull(message = "排序不能为null")
    private Integer sort;

    /**
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    @NotNull(message = "是否已删除不能为null")
    private Boolean isDeleted;

    /**
     * 是否默认岗位
     */
    @TableField(value = "is_default")
    @NotNull(message = "是否默认岗位不能为null")
    private Boolean isDefault;

    /**
     * 岗位code， 默认岗位才有的编码，用于系统业务中通过枚举查询岗位
     */
    @TableField(value = "position_code")
    @Size(max = 30,message = "岗位code， 默认岗位才有的编码，用于系统业务中通过枚举查询岗位最大长度要小于 30")
    private String positionCode;
}
