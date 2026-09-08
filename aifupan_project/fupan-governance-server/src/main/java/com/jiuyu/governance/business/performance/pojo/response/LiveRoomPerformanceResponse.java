package com.jiuyu.governance.business.performance.pojo.response;

import com.jiuyu.governance.business.room.pojo.response.LiveRoomResponse;
import lombok.Getter;
import lombok.Setter;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/3/30 14:20
 */
@Getter
@Setter
public class LiveRoomPerformanceResponse extends LiveRoomResponse {

    /**
     * 业绩统计
     */
    private PerformancePeriodStatsResponse performanceStatistics;
}
