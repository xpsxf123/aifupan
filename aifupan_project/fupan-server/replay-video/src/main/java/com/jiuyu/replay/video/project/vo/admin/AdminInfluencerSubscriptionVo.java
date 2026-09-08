package com.jiuyu.replay.video.project.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台管理-达人订阅信息VO
 *
 * @author RayChou
 * @date 2025-11-04
 * @description 用于后台管理系统查询用户的达人订阅信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "后台管理-达人订阅信息VO")
public class AdminInfluencerSubscriptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订阅ID
     */
    @Schema(description = "订阅ID", example = "1234567890")
    private Long subscriptionId;

    /**
     * 达人昵称
     */
    @Schema(description = "达人昵称", example = "美食达人小王")
    private String nickname;

    /**
     * 平台类型: 1-抖音, 2-快手, 3-视频号
     */
    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号", example = "1")
    private Byte platformType;

    /**
     * 平台用户ID
     */
    @Schema(description = "平台用户ID", example = "douyin_123")
    private String platformUserId;

    /**
     * 抖音ID（平台账号）
     */
    @Schema(description = "抖音ID（平台账号）", example = "douyin123")
    private String platformAccount;

    /**
     * 所属行业
     */
    @Schema(description = "所属行业", example = "餐饮美食")
    private String industryName;

    /**
     * 是否启用自动提取文案: 0-否, 1-是
     */
    @Schema(description = "是否启用自动提取文案: 0-否, 1-是", example = "1")
    private Byte isEnabled;

    /**
     * 自动提取文案点赞阈值
     */
    @Schema(description = "自动提取文案点赞阈值", example = "500")
    private Integer likeCountThreshold;

    /**
     * 自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月
     */
    @Schema(description = "自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月", example = "3")
    private Byte updateTimeCondition;

    /**
     * 短视频数量
     */
    @Schema(description = "短视频数量", example = "200")
    private Integer videoCount;

    /**
     * 订阅时间
     */
    @Schema(description = "订阅时间", example = "2025-10-01 10:30:00")
    private LocalDateTime createdDate;
}

