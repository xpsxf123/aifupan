package com.jiuyu.governance.business.performance.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 排班业绩人员表
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
@TableName("schedule_performance_staff")
public class SchedulePerformanceStaff {

    /**
     * 主键，雪花ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 排班业绩主表ID
     */
    @TableField("schedule_performance_id")
    private Long schedulePerformanceId;

    /**
     * 员工ID
     */
    @TableField("employee_id")
    private Long employeeId;

    /**
     * 员工名称（冗余字段）
     */
    @TableField("employee_name")
    private String employeeName;

    /**
     * 岗位ID
     */
    @TableField("position_id")
    private Long positionId;

    /**
     * 岗位名称（冗余字段）
     */
    @TableField("position_name")
    private String positionName;

    /**
     * 岗位编码
     */
    @TableField("position_code")
    private String positionCode;

    /**
     * 是否排班中的人员：0-否，1-是
     */
    @TableField("is_schedule_staff")
    private Boolean isScheduleStaff;

    /**
     * 创建时间
     */
    @TableField(value = "create_date", fill = FieldFill.INSERT)
    private LocalDateTime createDate;

    /**
     * 更新时间
     */
    @TableField(value = "update_date", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;

    /**
     * 创建人
     */
    @TableField("create_by")
    private Long createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private Long updateBy;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean isDeleted;
}
