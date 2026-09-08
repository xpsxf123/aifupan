package com.jiuyu.governance.business.room.pojo.request.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 新增直播间排班请求
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Getter
@Setter
public class LiveRoomScheduleAddRequest {

    /**
     * 直播间ID
     */
    @NotNull(message = "请选择直播间")
    private Long liveRoomId;

    /**
     * 班次日期
     */
    @NotNull(message = "请选择班次日期")
    private LocalDate workDay;

    /**
     * 班次列表
     */
    @NotEmpty(message = "请添加至少一个班次")
    @Valid
    private List<ScheduleSessionRequest> sessions;

    /**
     * 批量排班配置
     */
    @Valid
    private ScheduleBatchRequest batchConfig;
}
