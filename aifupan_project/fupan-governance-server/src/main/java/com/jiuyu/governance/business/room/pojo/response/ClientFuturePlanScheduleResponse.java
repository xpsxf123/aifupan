package com.jiuyu.governance.business.room.pojo.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 面向客户端的未来排班计划响应
 */
@Getter
@Setter
public class ClientFuturePlanScheduleResponse {

    /**
     * 排班ID
     */
    private Long scheduleId;

    /**
     * 主播SecUid
     */
    private String secUid;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

}
