package com.jiuyu.replay.video.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 爆款订阅表
 * </p>
 *
 * @author RayChou
 * @since 2025-08-30
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_video_user_hot_subscription")
public class VideoUserHotSubscriptionEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花ID）
     */
    @TableId("id")
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 分组ID 逻辑关联tb_video_user_subscription_group.id，null表示虚拟默认分组
     */
    @TableField(value = "group_id", updateStrategy = FieldStrategy.ALWAYS) // 忽略字段策略，允许null值更新
    private Long groupId;

    /**
     * 关键词
     */
    @TableField("keyword")
    private String keyword;

    /**
     * 平台类型: 1-抖音, 2-快手, 3-视频号
     */
    @TableField("platform_type")
    private Byte platformType;

    /**
     * 行业ID
     */
    @TableField("industry_id")
    private Long industryId;

    /**
     * 订阅点赞阈值
     */
    @TableField("subscription_like_count_threshold")
    private Integer subscriptionLikeCountThreshold;

    /**
     * 是否启用自动同步文案: 0-否, 1-是
     */
    @TableField("is_enabled")
    private Byte isEnabled;

    /**
     * 自动提取文案点赞阈值
     */
    @TableField(value = "like_count_threshold", updateStrategy = FieldStrategy.ALWAYS) // 忽略字段策略，允许null值更新
    private Integer likeCountThreshold;

    /**
     * 自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月
     */
    @TableField(value = "update_time_condition", updateStrategy = FieldStrategy.ALWAYS) // 忽略字段策略，允许null值更新
    private Byte updateTimeCondition;

    /**
     * 监控频率(小时)
     */
    @TableField("monitor_frequency")
    private Integer monitorFrequency;

    /**
     * 最后同步时间
     */
    @TableField("last_sync_time")
    private LocalDateTime lastSyncTime;

    /**
     * 创建时间
     */
    @TableField("created_date")
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    @TableField("update_date")
    private LocalDateTime updateDate;

    /**
     * 是否删除: 0-未删除, 1-已删除
     */
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
