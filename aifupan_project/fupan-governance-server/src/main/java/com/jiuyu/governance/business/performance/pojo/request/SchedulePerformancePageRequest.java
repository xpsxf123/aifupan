package com.jiuyu.governance.business.performance.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.framework.shandard.PageRequest;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 排班业绩分页查询请求
 *
 * @author lj
 * @date 2026-03-27
 */
@Getter
@Setter
public class SchedulePerformancePageRequest extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 直播开始时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    private LocalDateTime startTime;

    /**
     * 直播结束时间（格式：yyyy-MM-dd HH:mm:ss）
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
     * 租户ID（由框架自动注入）
     */
    @JsonIgnore
    private Long tenantId;
}
