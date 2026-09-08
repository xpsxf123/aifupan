package com.jiuyu.governance.business.room.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jiuyu.framework.shandard.Range;
import com.jiuyu.governance.business.room.handler.WorkTimeHandler;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 班次排班表
 */
@Getter
@Setter
@TableName(value = "work_schedule")
public class WorkSchedule {
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
     * 租户ID
     */
    @TableField(value = "tenant_id")
    @NotNull(message = "租户ID不能为null")
    private Long tenantId;

    /**
     * 班次归属天
     */
    @TableField(value = "work_day")
    @NotNull(message = "班次归属天不能为null")
    private LocalDate workDay;

    /**
     * 开始时间，内容格式 月天小时分钟
     */
    @TableField(value = "start_work")
    @NotNull(message = "开始时间，内容格式 月天小时分钟不能为null")
    private Integer startWork;

    /**
     * 结束时间，内容格式 月天小时分钟
     */
    @TableField(value = "end_work")
    @NotNull(message = "结束时间，内容格式 月天小时分钟不能为null")
    private Integer endWork;

    /**
     * 直播间ID
     */
    @TableField(value = "live_room_id")
    @NotNull(message = "直播间ID不能为null")
    private Long liveRoomId;

    /**
     * 备注
     */
    @TableField(value = "remark")
    @Size(max = 100, message = "备注最大长度要小于 100")
    @NotBlank(message = "备注不能为空")
    private String remark;

    /**
     * 班次时长 分钟
     */
    @TableField(value = "schedule_duration")
    @NotNull(message = "班次时长 分钟不能为null")
    private Integer scheduleDuration;

    /**
     * 休息时长 分钟
     */
    @TableField(value = "rest_duration")
    @NotNull(message = "休息时长 分钟不能为null")
    private Integer restDuration;


    /**
     * 班次时间范围
     */
    public Range<LocalDateTime> workRange() {
        LocalDateTime start = this.workDay.atTime(this.parseStartWork());
        LocalDateTime end = this.workDay.atTime(this.parseEndWork());
        if (end.isBefore(start) || end.equals(start)) {
            end = end.plusDays(1);
        }
        return new Range<>(start, end);
    }

    /**
     * 轮班开始时间, HH:mm
     */
    public LocalTime parseStartWork() {
        return WorkTimeHandler.parseTime(this.startWork);
    }

    /**
     * 轮班结束时间, HH:mm
     */
    public LocalTime parseEndWork() {
        return WorkTimeHandler.parseTime(this.endWork);
    }


    /**
     * 轮班开始时间, HH:mm
     */
    public void formatStartWork(LocalTime time) {
        this.startWork = WorkTimeHandler.formatTime(time);
    }

    /**
     * 轮班结束时间, HH:mm
     */
    public void formatEndWork(LocalTime time) {
        this.endWork = WorkTimeHandler.formatTime(time);
    }
}
