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
 * 部门
 */
@Getter
@Setter
@TableName(value = "dept")
public class Dept {
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
     * 部门名称
     */
    @TableField(value = "`name`")
    @Size(max = 40,message = "部门名称最大长度要小于 40")
    @NotBlank(message = "部门名称不能为空")
    private String name;

    /**
     * 所属公司ID
     */
    @TableField(value = "company_id")
    @NotNull(message = "所属公司ID不能为null")
    private Long companyId;

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
}
