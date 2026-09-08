package com.jiuyu.governance.business.room.pojo.request.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/**
 * 排班修改请求
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Getter
@Setter
public class ScheduleUpdateRequest {

    /**
     * 排班ID
     */
    @NotNull(message = "缺少排班ID")
    private Long id;

    /**
     * 直播间ID
     */
    @NotNull(message = "缺少直播间ID")
    private Long liveRoomId;

    /**
     * 上播时间
     */
    @NotNull(message = "请选择上播时间")
    private LocalTime startWork;

    /**
     * 下播时间
     */
    @NotNull(message = "请选择下播时间")
    private LocalTime endWork;

    /**
     * 班次时长
     */
    @NotNull(message = "请选择班次时长")
    private Integer scheduleDuration;

    /**
     * 休息时长
     */
    @NotNull(message = "请选择休息时长")
    private Integer restDuration;
}
