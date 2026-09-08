package com.jiuyu.governance.business.room.pojo.request.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 直播间排班批量导入请求
 *
 * @author HeHui
 * @date 2026-08-20
 */
@Getter
@Setter
public class LiveRoomScheduleImportRequest {

    /**
     * 直播间ID
     */
    @NotNull(message = "请选择直播间")
    private Long liveRoomId;

    /**
     * 排班列表（有人的班次；可为空，仅当 emptyWorkDays 非空时用于纯删除）
     */
    @Valid
    private List<ImportSchedule> schedules;

    /**
     * 需要清空排班的空日期（该天所有时间段均未选人），导入时软删这些天的已有排班
     */
    private List<LocalDate> emptyWorkDays;
}
