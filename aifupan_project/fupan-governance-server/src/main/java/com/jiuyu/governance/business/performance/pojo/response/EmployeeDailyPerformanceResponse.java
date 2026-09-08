package com.jiuyu.governance.business.performance.pojo.response;

import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceMetrics;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工业绩按天维度响应
 * <p>
 * 使用场景：员工业绩详情页-按天维度分页列表
 * 以天为维度统计员工业绩，每天一条记录
 * </p>
 *
 * @author lj
 * @date 2026-03-30
 */
@Getter
@Setter
@SuperBuilder
public class EmployeeDailyPerformanceResponse extends BasePerformanceMetrics {

    /**
     * 统计日期（格式：yyyy-MM-dd）
     */
    private LocalDate statsDate;

    /**
     * 当天参与的直播间列表
     */
    private List<LiveRoomInfo> liveRoomList;

    /**
     * 场次数量（当天总排班场次）
     */
    private Integer scheduleCount;

    /**
     * 直播时长（分钟，当天总计）
     */
    private Long liveDurationMinutes;

    /**
     * 直播间信息
     */
    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveRoomInfo {

        /**
         * 直播间ID
         */
        private Long liveRoomId;

        /**
         * 直播间名称（主播名称）
         */
        private String liveRoomName;
    }
}
