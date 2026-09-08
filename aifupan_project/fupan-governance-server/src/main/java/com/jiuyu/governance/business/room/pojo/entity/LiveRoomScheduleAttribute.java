package com.jiuyu.governance.business.room.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jiuyu.framework.mybatispuls.type.IntListTypeHandler;
import com.jiuyu.framework.mybatispuls.type.LongListTypeHandler;
import com.jiuyu.governance.business.room.handler.WorkTimeHandler;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 直播间排班配置属性
 */
@Getter
@Setter
@TableName(value = "live_room_schedule_attribute", autoResultMap = true)
public class LiveRoomScheduleAttribute {
    /**
     * 直播间ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    @NotNull(message = "直播间ID不能为null")
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
     * 轮班开始时间, HH:mm
     */
    @TableField(value = "start_plan")
    @NotNull(message = "轮班开始时间, HH:mm不能为null")
    private Integer startPlan;

    /**
     * 轮班结束时间, HH:mm
     */
    @TableField(value = "end_plan")
    @NotNull(message = "轮班结束时间, HH:mm不能为null")
    private Integer endPlan;

    /**
     * 班次时长（分钟）可选项, 使用逗号拼接。最多添加10个
     */
    @TableField(value = "shift_options", typeHandler = IntListTypeHandler.class)
    @Size(max = 100, message = "班次时长（分钟）可选项, 使用逗号拼接。最多添加10个最大长度要小于 100")
    @NotBlank(message = "班次时长（分钟）可选项, 使用逗号拼接。最多添加10个不能为空")
    private List<Integer> shiftOptions;

    /**
     * 休息时长（分钟）可选项, 使用逗号拼接。最多添加10个
     */
    @TableField(value = "rest_options", typeHandler = IntListTypeHandler.class)
    @Size(max = 100, message = "休息时长（分钟）可选项, 使用逗号拼接。最多添加10个最大长度要小于 100")
    @NotBlank(message = "休息时长（分钟）可选项, 使用逗号拼接。最多添加10个不能为空")
    private List<Integer> restOptions;

    /**
     * 岗位可选项, 使用逗号拼接。最多添加10个
     */
    @TableField(value = "position_options", typeHandler = LongListTypeHandler.class)
    @Size(max = 400, message = "岗位可选项, 使用逗号拼接。最多添加10个最大长度要小于 400")
    @NotBlank(message = "岗位可选项, 使用逗号拼接。最多添加10个不能为空")
    private List<Long> positionOptions;




    /**
     * 轮班开始时间, HH:mm
     */
    public LocalTime parseStartPlan() {
        return WorkTimeHandler.parseTime(this.startPlan);
    }

    /**
     * 轮班结束时间, HH:mm
     */
    public LocalTime parseEndPlan() {
        return WorkTimeHandler.parseTime(this.endPlan);
    }


    /**
     * 轮班开始时间, HH:mm
     */
    public void formatStartPlan(LocalTime time) {
        this.startPlan = WorkTimeHandler.formatTime(time);
    }

    /**
     * 轮班结束时间, HH:mm
     */
    public void formatEndPlan(LocalTime time) {
        this.endPlan = WorkTimeHandler.formatTime(time);
    }



}
