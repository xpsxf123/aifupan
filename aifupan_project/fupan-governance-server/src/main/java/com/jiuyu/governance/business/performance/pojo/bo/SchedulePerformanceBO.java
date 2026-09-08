package com.jiuyu.governance.business.performance.pojo.bo;

import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceMetrics;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 排班业绩分页查询 BO
 *
 * @author lj
 * @date 2026-03-27
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePerformanceBO extends BasePerformanceMetrics {

    /**
     * 排班业绩ID
     */
    private Long id;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 直播间ID
     */
    private Long liveRoomId;

    /**
     * 主播SecUid
     */
    private String secUid;

    /**
     * 数据来源：1-系统，2-手动
     */
    private Integer source;
}
