package com.jiuyu.governance.business.performance.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.framework.shandard.PageRequest;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 按天维度排班业绩统计请求
 *
 * @author lj
 * @date 2026-03-28
 */
@Getter
@Setter
public class DailyPerformanceStatsRequest extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 直播开始时间（筛选条件，查询 start_time >= startTime）
     */
    private LocalDateTime startTime;

    /**
     * 直播结束时间（筛选条件，查询 end_time <= endTime）
     */
    private LocalDateTime endTime;

    /**
     * 主播名称（模糊匹配）
     */
    private String anchorName;

    /**
     * 直播间ID（精确匹配）
     */
    private Long liveRoomId;

    /**
     * 排序字段
     * 可选：statsDate, scheduleCount, liveDurationMinutes, viewCount, salesRevenue, refund, netSales, investment, roi
     */
    private String sortField = "statsDate";

    /**
     * 排序方式：ASC-升序，DESC-降序
     */
    private String sortOrder = "DESC";

    /**
     * 租户ID（由框架自动注入）
     */
    @JsonIgnore
    private Long tenantId;
}
