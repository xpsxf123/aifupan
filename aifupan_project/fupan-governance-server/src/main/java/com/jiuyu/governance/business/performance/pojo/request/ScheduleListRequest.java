package com.jiuyu.governance.business.performance.pojo.request;

import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.plugins.webmvc.serializer.EnumDesc;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 排班列表查询请求
 *
 * @author lj
 * @date 2026-05-09
 */
@Getter
@Setter
public class ScheduleListRequest {

    /**
     * 排班日期，必填
     */
    @NotNull(message = "日期不能为空")
    private LocalDate date;

    /**
     * 直播间 secUid，必填
     */
    @NotNull(message = "直播间不能为空")
    private String secUid;

    /**
     * 平台类型 0抖音，1快手，2视频号
     */
    @NotNull(message = "平台类型不能为空")
    @EnumDesc(LivePlatformType.class)
    private Integer platformType;

    /**
     * 排班业绩ID，选填
     */
    private Long schedulePerformanceId;
}
