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
 * 后台管理-爆款订阅信息VO
 *
 * @author RayChou
 * @date 2025-11-04
 * @description 用于后台管理系统查询用户的爆款订阅信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "后台管理-爆款订阅信息VO")
public class AdminHotSubscriptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订阅ID
     */
    @Schema(description = "订阅ID", example = "1234567890")
    private Long subscriptionId;

    /**
     * 关键词
     */
    @Schema(description = "关键词", example = "美食")
    private String keyword;

    /**
     * 所属行业
     */
    @Schema(description = "所属行业", example = "餐饮美食")
    private String industryName;

    /**
     * 订阅点赞阈值
     */
    @Schema(description = "订阅点赞阈值", example = "1000")
    private Integer subscriptionLikeCountThreshold;

    /**
     * 是否启用自动同步文案: 0-否, 1-是
     */
    @Schema(description = "是否启用自动同步文案: 0-否, 1-是", example = "1")
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
    @Schema(description = "短视频数量", example = "150")
    private Integer videoCount;

    /**
     * 订阅时间
     */
    @Schema(description = "订阅时间", example = "2025-10-01 10:30:00")
    private LocalDateTime createdDate;
}

