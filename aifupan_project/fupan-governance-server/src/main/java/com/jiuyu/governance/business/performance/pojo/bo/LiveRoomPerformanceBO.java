package com.jiuyu.governance.business.performance.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 直播间业绩聚合BO
 * 用于 LEFT JOIN session_performance + GROUP BY 的查询结果
 *
 * @author lj
 * @date 2026-06-25
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LiveRoomPerformanceBO extends PerformanceAggregationBO {

    private String name;
    private String anchorAvatar;
}
