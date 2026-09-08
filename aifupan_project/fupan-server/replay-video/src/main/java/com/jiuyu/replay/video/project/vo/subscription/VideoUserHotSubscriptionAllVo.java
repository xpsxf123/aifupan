package com.jiuyu.replay.video.project.vo.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author RayChou
 * @date 2025/9/1 19:16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款订阅对象视图")
public class VideoUserHotSubscriptionAllVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订阅ID
     */
    @Schema(description = "订阅ID")
    private Long id;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID")
    private Long tenantId;

    /**
     * 分组ID 逻辑关联tb_video_user_subscription_group.id，null表示虚拟默认分组
     */
    @Schema(description = "组ID 逻辑关联tb_video_user_subscription_group.id，null表示虚拟默认分组")
    private Long groupId;

    /**
     * 关键词
     */
    @Schema(description = "关键词")
    private String keyword;

    /**
     * 平台类型: 1-抖音, 2-快手, 3-视频号
     */
    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号")
    private Byte platformType;

    /**
     * 行业ID
     */
    @Schema(description = "行业ID")
    private Long industryId;

    /**
     * 订阅点赞阈值
     */
    @Schema(description = "订阅点赞阈值")
    private Integer subscriptionLikeCountThreshold;

    /**
     * 是否启用自动同步文案: 0-否, 1-是
     */
    @Schema(description = "是否启用自动同步文案: 0-否, 1-是")
    private Byte isEnabled;

    /**
     * 自动提取文案点赞阈值
     */
    @Schema(description = "自动提取文案点赞阈值")
    private Integer likeCountThreshold;

    /**
     * 自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月
     */
    @Schema(description = "自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月")
    private Byte updateTimeCondition;

    /**
     * 监控频率(小时)
     */
    @Schema(description = "监控频率(小时)")
    private Integer monitorFrequency;

    /**
     * 最后同步时间
     */
    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateDate;

}
