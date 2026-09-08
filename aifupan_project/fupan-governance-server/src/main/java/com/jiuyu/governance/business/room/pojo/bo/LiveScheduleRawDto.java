package com.jiuyu.governance.business.room.pojo.bo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 排班原始数据查询 DTO
 * <p>
 * 用于从数据库查询原始排班数据，包含排班、直播间、人员等完整信息。
 * 通常用于批量查询场景，支持按多个条件过滤。
 * </p>
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Getter
@Setter
public class LiveScheduleRawDto {
    /**
     * 排班 ID
     */
    private Long scheduleId;
    
    /**
     * 直播间 ID
     */
    private Long liveRoomId;
    
    /**
     * 直播间平台唯一标识（sec_uid）
     */
    private String secUid;
    
    /**
     * 直播平台类型（1-抖音，2-快手，3-淘宝等）
     */
    private Integer platformType;
    
    /**
     * 工作日期
     */
    private LocalDate workDay;
    
    /**
     * 开始工作时间（格式：HHmm，例如 900 表示 9:00）
     */
    private Integer startWork;
    
    /**
     * 结束工作时间（格式：HHmm，例如 1800 表示 18:00）
     */
    private Integer endWork;
    
    /**
     * 排班时长（分钟）
     */
    private Integer scheduleDuration;
    
    /**
     * 休息时长（分钟）
     */
    private Integer restDuration;
    
    /**
     * 员工 ID（关联的人员，可能为空）
     */
    private Long employeeId;

    /**
     * 员工名称
     */
    private String employeeName;

    /**
     * 岗位 ID（关联的岗位，可能为空）
     */
    private Long positionId;
    
    /**
     * 人员排班关联 ID
     */
    private Long scheduleEmployeeId;
}
