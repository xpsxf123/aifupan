package com.jiuyu.governance.business.performance.pojo.request;

import com.jiuyu.governance.business.performance.pojo.request.SchedulePerformanceDataRequest;
import com.jiuyu.governance.business.performance.pojo.response.SchedulePerformanceResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 班次业绩保存请求
 *
 * @author lj
 * @date 2026-03-28
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePerformanceSaveRequest {

    /**
     * 班次业绩ID
     * 为空则新增，有值则修改
     */
    private Long id;

    /**
     * 排班ID（直播班次ID）
     * 非必填，如果填写则从班次获取时间
     */
    private Long scheduleId;

    /**
     * 直播间ID
     */
    @NotNull(message = "直播间不能为空")
    private Long liveRoomId;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 主播SecUid
     */
    private String secUid;

    /**
     * 人员列表
     */
    @NotEmpty(message = "至少需要添加一个直播人员")
    @Valid
    private List<SchedulePerformanceResponse.StaffInfo> staffList;

    /**
     * 业绩数据（含凭证图片）
     */
    @Valid
    private SchedulePerformanceDataRequest performanceData;
}
