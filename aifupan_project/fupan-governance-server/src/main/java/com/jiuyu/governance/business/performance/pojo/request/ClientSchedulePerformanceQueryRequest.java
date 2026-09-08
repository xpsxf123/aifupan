package com.jiuyu.governance.business.performance.pojo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 客户端查询排班业绩请求
 *
 * @author lj
 * @date 2026-04-21
 */
@Getter
@Setter
public class ClientSchedulePerformanceQueryRequest {

    /**
     * 主播SecUid
     */
    @NotBlank(message = "secUid不能为空")
    private String secUid;

    /**
     * 视频开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 视频结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}
