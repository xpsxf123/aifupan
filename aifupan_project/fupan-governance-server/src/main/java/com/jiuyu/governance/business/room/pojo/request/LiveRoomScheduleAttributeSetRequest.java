package com.jiuyu.governance.business.room.pojo.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;
import java.util.List;

/**
 * 设置直播间排班配置属性请求
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Getter
@Setter
public class LiveRoomScheduleAttributeSetRequest {

    /**
     * 直播间ID
     */
    @NotNull(message = "缺少直播间ID")
    private Long id;

    /**
     * 轮班开始时间
     */
    @NotNull(message = "请选择轮班开始时间")
    @DateTimeFormat(pattern = "HH:mm", fallbackPatterns = {"HH:mm:ss"})
    private LocalTime startPlan;

    /**
     * 轮班结束时间
     */
    @NotNull(message = "请选择轮班结束时间")
    @DateTimeFormat(pattern = "HH:mm", fallbackPatterns = {"HH:mm:ss"})
    private LocalTime endPlan;

    /**
     * 班次时长（分钟）可选项
     */
    @NotEmpty(message = "请选择班次时长可选项")
    @Size(max = 10, message = "班次时长最多添加10个选项")
    private List<Integer> shiftOptions;

    /**
     * 休息时长（分钟）可选项
     */
    @NotEmpty(message = "请选择休息时长可选项")
    @Size(max = 10, message = "休息时长最多添加10个选项")
    private List<Integer> restOptions;

    /**
     * 岗位可选项
     */
    @NotEmpty(message = "岗位可选项不能为空")
    @Size(max = 10, message = "岗位最多添加10个选项")
    private List<Long> positionOptions;
}
