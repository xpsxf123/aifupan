package com.jiuyu.governance.business.performance.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 排班业绩表
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
@TableName("schedule_performance")
public class SchedulePerformance extends BasePerformanceEntity {

    /**
     * 主键，雪花ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 排班ID
     */
    @TableField("schedule_id")
    private Long scheduleId;

    /**
     * 直播间ID
     */
    @TableField("live_room_id")
    private Long liveRoomId;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 主播SecUid
     */
    @TableField("sec_uid")
    private String secUid = "";

    /**
     * 数据来源：1-系统，2-手动
     */
    @TableField("source")
    private Integer source;

    /**
     * 公司ID
     */
    @TableField("company_id")
    private Long companyId;

    /**
     * 部门ID
     */
    @TableField("dept_id")
    private Long deptId;

    /**
     * 小组ID
     */
    @TableField("team_id")
    private Long teamId;

    /**
     * 创建时间
     */
    @TableField(value = "create_date", fill = FieldFill.INSERT)
    private LocalDateTime createDate;

    /**
     * 更新时间
     */
    @TableField(value = "update_date", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;

    /**
     * 创建人
     */
    @TableField("create_by")
    private Long createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private Long updateBy;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean isDeleted;
}
