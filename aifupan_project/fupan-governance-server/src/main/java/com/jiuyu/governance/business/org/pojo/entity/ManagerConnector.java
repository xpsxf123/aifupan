package com.jiuyu.governance.business.org.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 管理员连接
 */
@Getter
@Setter
@TableName(value = "manager_connector")
public class ManagerConnector {
    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @NotNull(message = "ID不能为null")
    private Long id;

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
     * 管理类型, 1公司，2部门，3小组，4直播间
     */
    @TableField(value = "manager_type")
    @NotNull(message = "管理类型, 1公司，2部门，3小组，4直播间不能为null")
    private ManagerType managerType;

    /**
     * 员工ID
     */
    @TableField(value = "employee_id")
    @NotNull(message = "员工ID不能为null")
    private Long employeeId;

    /**
     * 目标ID
     */
    @TableField(value = "target_id")
    private Long targetId;
}
