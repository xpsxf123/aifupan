package com.jiuyu.governance.business.performance.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 原始值表
 * 记录业绩数据首次修改前的原始值，支持多种来源类型
 *
 * @author lj
 * &#064;date  2026-03-24
 */
@Getter
@Setter
@TableName("session_original_value")
public class SessionOriginalValue extends BasePerformanceEntity {

    /** 来源类型：live_session */
    public static final int SOURCE_TYPE_SESSION = 0;
    /** 来源类型：session_performance */
    public static final int SOURCE_TYPE_SESSION_PERFORMANCE = 1;
    /** 来源类型：schedule_performance */
    public static final int SOURCE_TYPE_SCHEDULE_PERFORMANCE = 2;

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
     * 来源ID
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 来源类型：0-live_session，1-session_performance，2-schedule_performance
     */
    @TableField("source_type")
    private Integer sourceType;

    /**
     * 数据来源：1-系统，2-手动
     */
    @TableField("source")
    private Integer source;

    /**
     * 创建时间
     */
    @TableField(value = "create_date", fill = FieldFill.INSERT)
    private LocalDateTime createDate;

    /**
     * 创建人
     */
    @TableField("create_by")
    private Long createBy;
}
