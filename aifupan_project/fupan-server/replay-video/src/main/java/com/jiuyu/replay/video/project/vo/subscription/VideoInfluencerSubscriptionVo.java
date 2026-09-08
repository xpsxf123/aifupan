package com.jiuyu.replay.video.project.vo.subscription;

import com.jiuyu.replay.video.project.entity.VideoInfluencerInfoEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 达人订阅视图对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 达人订阅列表显示数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "达人订阅视图对象")
public class VideoInfluencerSubscriptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，雪花算法生成
     */
    @Schema(description = "主键ID，雪花算法生成", example = "1")
    private Long id;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    /**
     * 分组ID，逻辑关联tb_video_user_subscription_group.id，null表示虚拟默认分组
     */
    @Schema(description = "分组ID，逻辑关联tb_video_user_subscription_group.id，null表示虚拟默认分组", example = "1001")
    private Long groupId;

    /**
     * 达人ID，逻辑关联tb_video_influencer.id
     */
    @Schema(description = "达人ID，逻辑关联tb_video_influencer.id", example = "2001")
    private Long influencerId;

    /**
     * 行业ID
     */
    @Schema(description = "行业ID", example = "1")
    private Long industryId;

    /**
     * 是否启用自动提取文案: 0-否, 1-是
     */
    @Schema(description = "是否启用自动提取文案: 0-否, 1-是", example = "1")
    private Byte isEnabled;

    /**
     * 自动提取文案点赞阈值
     */
    @Schema(description = "自动提取文案点赞阈值", example = "1")
    private Integer likeCountThreshold;

    /**
     * 自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月
     */
    @Schema(description = "自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月", example = "1")
    private Byte updateTimeCondition;

    /**
     * 监控频率（小时），0表示不监控
     */
    @Schema(description = "监控频率（小时），0表示不监控", example = "24")
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
    @Schema(description = "修改时间")
    private LocalDateTime updateDate;

    /**
     * 是否删除: 0-否, 1-是
     */
    @Schema(description = "是否已删除")
    private Byte isDeleted;

    /**
     * 达人信息
     */
    @Schema(description = "达人详细信息")
    private VideoInfluencerInfoEntity videoInfluencerInfoVo;
}
