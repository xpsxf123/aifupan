package com.jiuyu.governance.business.performance.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 视频表
 *
 * @author lj
 * @date 2026-03-19
 */
@Getter
@Setter
@TableName("live_video")
public class LiveVideo extends BasePerformanceEntity {

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
     * 直播批次号
     */
    @TableField("batch_number")
    private String batchNumber;

    /**
     * 视频唯一标识
     */
    @TableField("video_id")
    private String videoId;

    /**
     * 业绩数据是否存在 0-丢失业绩数据，1-完好
     */
    @TableField("has_performance")
    private Integer hasPerformance;

    /**
     * 视频文件OSS地址
     */
    @TableField("video_oss_url")
    private String videoOssUrl;

    /**
     * 视频开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 视频结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 主播secUid
     */
    @TableField("sec_uid")
    private String secUid;

    /**
     * 平台类型：0-抖音，1-快手，2-视频号
     */
    @TableField("platform")
    private Integer platform;

    /**
     * 处理状态：0-待处理，1-处理中，2-成功，3-失败
     */
    @TableField("process_status")
    private Integer processStatus;

    /**
     * 最近处理时间
     */
    @TableField("process_time")
    private LocalDateTime processTime;

    /**
     * 处理失败原因
     */
    @TableField("fail_reason")
    private String failReason;

    /**
     * 数据最新更新时间（最近一次业绩数据推送时间）
     */
    @TableField("data_update_time")
    private LocalDateTime dataUpdateTime;

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
     * 逻辑删除：0-正常，-1-已删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
