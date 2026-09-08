package com.jiuyu.governance.business.room.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 直播间排班的人员
 */
@Getter
@Setter
@TableName(value = "schedule_employee")
public class ScheduleEmployee {
    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
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
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    @NotNull(message = "是否已删除不能为null")
    private Boolean isDeleted;

    /**
     * 直播间ID
     */
    @TableField(value = "live_room_id")
    @NotNull(message = "直播间ID不能为null")
    private Long liveRoomId;

    /**
     * 用户ID
     */
    @TableField(value = "employee_id")
    @NotNull(message = "用户ID不能为null")
    private Long employeeId;

    /**
     * 岗位ID
     */
    @TableField(value = "position_id")
    @NotNull(message = "岗位ID不能为null")
    private Long positionId;

    /**
     * 排班计划ID
     */
    @TableField(value = "schedule_id")
    @NotNull(message = "排班计划ID不能为null")
    private Long scheduleId;

    /**
     * 班次归属天
     */
    @TableField(value = "work_day")
    @NotNull(message = "班次归属天不能为null")
    private LocalDate workDay;
}
