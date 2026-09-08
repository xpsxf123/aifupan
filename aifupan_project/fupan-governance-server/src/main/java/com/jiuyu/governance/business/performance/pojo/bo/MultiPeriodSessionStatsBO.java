package com.jiuyu.governance.business.performance.pojo.bo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 多时段场次统计BO（一次查询返回6个时段的场次数量和直播时长）
 *
 * @author lj
 * @date 2026-05-09
 */
@Getter
@Setter
@NoArgsConstructor
public class MultiPeriodSessionStatsBO {

    private Long liveRoomId;
    private Integer todaySessionCount;
    private Integer todayDuration;
    private Integer yesterdaySessionCount;
    private Integer yesterdayDuration;
    private Integer thisWeekSessionCount;
    private Integer thisWeekDuration;
    private Integer lastWeekSessionCount;
    private Integer lastWeekDuration;
    private Integer thisMonthSessionCount;
    private Integer thisMonthDuration;
    private Integer lastMonthSessionCount;
    private Integer lastMonthDuration;
}
