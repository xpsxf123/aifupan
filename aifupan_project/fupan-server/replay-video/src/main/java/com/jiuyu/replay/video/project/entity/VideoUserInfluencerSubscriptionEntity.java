package com.jiuyu.replay.video.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 达人订阅表
 * </p>
 *
 * @author RayChou
 * @since 2025-08-13
 */
@Getter
@Setter
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_video_user_influencer_subscription")
@Schema(description = "达人订阅表")
public class VideoUserInfluencerSubscriptionEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，雪花算法生成
     */
    @TableId("id")
    @Schema(description = "主键ID，雪花算法生成", example = "1")
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    /**
     * 分组ID，逻辑关联tb_video_user_subscription_group.id，null表示虚拟默认分组
     */
    @TableField(value = "group_id", updateStrategy = FieldStrategy.ALWAYS) // 忽略字段策略，允许null值更新
    @Schema(description = "分组ID，逻辑关联tb_video_user_subscription_group.id，null表示虚拟默认分组", example = "1001")
    private Long groupId;

    /**
     * 达人ID，逻辑关联tb_video_influencer.id
     */
    @TableField("influencer_id")
    @Schema(description = "达人ID，逻辑关联tb_video_influencer.id", example = "2001")
    private Long influencerId;

    /**
     * 行业ID
     */
    @TableField("industry_id")
    @Schema(description = "行业ID", example = "1")
    private Long industryId;

    /**
     * 是否启用自动提取文案: 0-否, 1-是
     */
    @TableField("is_enabled")
    @Schema(description = "是否启用: 0-否, 1-是", example = "1")
    private Byte isEnabled;

    /**
     * 自动提取文案点赞阈值
     */
    @TableField(value = "like_count_threshold", updateStrategy = FieldStrategy.ALWAYS) // 忽略字段策略，允许null值更新
    @Schema(description = "自动提取文案点赞阈值", example = "1")
    private Integer likeCountThreshold;

    /**
     * 自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月
     */
    @TableField(value = "update_time_condition", updateStrategy = FieldStrategy.ALWAYS) // 忽略字段策略，允许null值更新
    @Schema(description = "自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月", example = "1")
    private Byte updateTimeCondition;

    /**
     * 监控频率（小时），0表示不监控
     */
    @TableField("monitor_frequency")
    @Schema(description = "监控频率（小时），0表示不监控", example = "24")
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
     * 是否删除: 0-否, 1-是
     */
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
