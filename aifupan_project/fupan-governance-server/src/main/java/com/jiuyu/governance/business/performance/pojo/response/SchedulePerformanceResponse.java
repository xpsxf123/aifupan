package com.jiuyu.governance.business.performance.pojo.response;

import cn.hutool.core.util.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排班业绩分页查询响应
 *
 * @author lj
 * @date 2026-03-27
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePerformanceResponse {

    /**
     * 排班业绩ID
     */
    private Long id;

    /**
     * 直播间ID
     */
    private Long liveRoomId;

    /**
     * 主播SecUid
     */
    private String secUid;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 人员信息列表
     */
    private List<StaffInfo> staffList;

    /**
     * 数据来源：1-系统，2-手动
     */
    private Integer source;

    /**
     * 业绩数据（含原始值对比）
     */
    private PerformanceData performanceData;

    /**
     * 人员信息
     */
    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffInfo {

        /**
         * 人员记录ID（schedule_performance_staff 表主键）
         */
        private Long id;

        /**
         * 岗位ID
         */
        private Long positionId;

        /**
         * 岗位名称
         */
        private String positionName;

        /**
         * 员工ID
         */
        private Long employeeId;

        /**
         * 员工名称
         */
        private String employeeName;

        /**
         * 是否排班中的人员
         */
        private Boolean isScheduleStaff;

        public String showName() {
            if (ObjUtil.isNotEmpty(positionName) && ObjUtil.isNotEmpty(employeeName)) {
                return positionName + ":" + employeeName;
            }
            return null;
        }
    }
}
