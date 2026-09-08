package com.jiuyu.governance.business.performance.pojo.response.client;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户端查询排班业绩响应
 *
 * @author lj
 * @date 2026-04-21
 */
@Getter
@Setter
public class ClientSchedulePerformanceResponse {

    /**
     * 排班业绩ID
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
     * 排班业绩响应
     */
    private List<ClientPerformanceResponse> clientPerformanceResponse;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 小组ID
     */
    private Long teamId;

    /**
     * 更新时间
     */
    private LocalDateTime updateDate;

    /**
     * 更新URL
     */
    private String updateUrl;
}
