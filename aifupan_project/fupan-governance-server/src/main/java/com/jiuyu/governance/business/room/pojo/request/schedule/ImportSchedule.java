package com.jiuyu.governance.business.room.pojo.request.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 导入排班 - 单个班次
 *
 * @author HeHui
 * @date 2026-08-20
 */
@Getter
@Setter
public class ImportSchedule {

    /**
     * 班次归属天
     */
    @NotNull(message = "缺少排班日期")
    private LocalDate workDay;

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
     * 班次时长（分钟）
     */
    @NotNull(message = "缺少班次时长")
    private Integer scheduleDuration;

    /**
     * 休息时长（分钟）
     */
    @NotNull(message = "缺少休息时长")
    private Integer restDuration;

    /**
     * 排班人员列表
     */
    @NotEmpty(message = "请选择排班人员")
    @Valid
    private List<ImportEmployee> employees;
}
