package com.jiuyu.governance.business.performance.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 场次表
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
@TableName("live_session")
@NoArgsConstructor
public class LiveSession extends BasePerformanceEntity {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 直播间ID
     */
    @TableField("live_room_id")
    private Long liveRoomId;

    /**
     * 直播批次号
     */
    @TableField("batch_number")
    private String batchNumber;

    /**
     * 场次开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 场次结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 总时长（秒）
     */
    @TableField("duration")
    private Integer duration;

    /**
     * 数据来源：1-系统推送，2-手动录入
     */
    @TableField("source")
    private Integer source;

    /**
     * 实时数据OSS地址
     */
    @TableField("oss_url")
    private String ossUrl;

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
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;

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
     * 逻辑删除：0-正常，-1-已删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
