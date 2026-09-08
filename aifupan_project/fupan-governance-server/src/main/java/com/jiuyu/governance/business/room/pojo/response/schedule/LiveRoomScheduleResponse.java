package com.jiuyu.governance.business.room.pojo.response.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 直播间排班响应
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Getter
@Setter
public class LiveRoomScheduleResponse {

    /**
     * ID
     */
    private Long id;

    /**
     * 直播间ID
     */
    private Long liveRoomId;

    /**
     * 班次归属天
     */
    private LocalDate workDay;

    /**
     * 轮班开始时间
     */
    @DateTimeFormat(pattern = "HH:mm", fallbackPatterns = {"HH:mm:ss"})
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startWork;

    /**
     * 轮班结束时间
     */
    @DateTimeFormat(pattern = "HH:mm", fallbackPatterns = {"HH:mm:ss"})
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endWork;

    /**
     * 班次时长
     */
    private Integer scheduleDuration;

    /**
     * 休息时长
     */
    private Integer restDuration;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排班岗位列表 (包含岗位下的人员)
     */
    private List<SchedulePositionVO> positions;
}
