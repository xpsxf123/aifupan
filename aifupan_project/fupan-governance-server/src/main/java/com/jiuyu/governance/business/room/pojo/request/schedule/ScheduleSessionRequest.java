package com.jiuyu.governance.business.room.pojo.request.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;
import java.util.List;

/**
 * 班次请求
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Getter
@Setter
public class ScheduleSessionRequest {

    /**
     * 上播时间
     */
    @NotNull(message = "请选择上播时间")
    @DateTimeFormat(pattern = "HH:mm", fallbackPatterns = {"HH:mm:ss"})
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startWork;

    /**
     * 下播时间
     */
    @NotNull(message = "请选择下播时间")
    @DateTimeFormat(pattern = "HH:mm", fallbackPatterns = {"HH:mm:ss"})
    @JsonFormat(pattern = "HH:mm")
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

    /**
     * 排班人员列表
     */
    @NotEmpty(message = "请选择排班人员")
    @Valid
    private List<ScheduleEmployeeRequest> employees;
}
