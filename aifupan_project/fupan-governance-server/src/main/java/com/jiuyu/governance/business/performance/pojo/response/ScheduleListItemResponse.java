package com.jiuyu.governance.business.performance.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排班列表项响应
 *
 * @author lj
 * @date 2026-05-09
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleListItemResponse {

    /** 排班ID */
    private Long scheduleId;

    /** 直播间ID */
    private Long roomId;

    /** 直播间 secUid */
    private String secUid;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 人员列表 */
    private List<WorkUserInfo> workUserList;

    /** 是否已有业绩：1-有，0-无 */
    private Integer hasPerformance;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkUserInfo {
        private Long employeeId;
        private String employeeName;
        private Long positionId;
        private String positionName;
        private String positionCode;
    }
}
