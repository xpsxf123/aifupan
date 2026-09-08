package com.jiuyu.governance.business.room.pojo.request.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 主播排班查询
 *
 * @author HeHui
 * @date 2026-04-07 10:13
 */
@Getter
@Setter
public class AnchorQueryScheduleRequest {


    /**
     * 直播平台类型
     * 0 抖音，1快手，2视频号
     */
    @NotNull(message = "直播平台类型不能为空")
    private Integer livePlatformType;

    /**
     * 主播ID
     */
    @NotBlank(message = "主播ID不能为空")
    private String secUid;

    /**
     * videoId
     */
    @NotBlank(message = "videoId不能为空")
    private String videoId;

    /**
     * 排班开始时间
     */
    @NotNull(message = "排班开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 排班结束时间
     */
    private LocalDateTime endTime;
}
