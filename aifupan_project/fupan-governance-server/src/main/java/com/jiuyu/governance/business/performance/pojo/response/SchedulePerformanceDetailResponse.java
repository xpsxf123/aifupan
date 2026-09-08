package com.jiuyu.governance.business.performance.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 班次业绩详情响应
 *
 * @author lj
 * @date 2026-03-28
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePerformanceDetailResponse {

    /**
     * 班次业绩ID
     */
    private Long id;

    /**
     * 排班ID
     */
    private Long scheduleId;

    /**
     * 直播间ID
     */
    private Long liveRoomId;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 主播SecUid
     */
    private String secUid;

    /**
     * 数据来源：1-系统，2-手动
     */
    private Integer source;

    /**
     * 人员列表
     */
    private List<SchedulePerformanceResponse.StaffInfo> staffList;

    /**
     * 业绩数据（含图片URL和修改状态）
     */
    private PerformanceData performanceData;

    /**
     * 创建时间
     */
    private LocalDateTime createDate;

    /**
     * 更新时间
     */
    private LocalDateTime updateDate;
}
