package com.jiuyu.replay.video.project.vo.subscription;

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
public class InfluencerSubscriptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订阅ID
     */
    @Schema(description = "订阅ID")
    private Long subscriptionId;

    /**
     * 达人ID
     */
    @Schema(description = "达人ID")
    private Long influencerId;

    /**
     * 平台类型: 1-抖音, 2-快手, 3-视频号
     */
    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号", example = "1")
    private Byte platformType;

    /**
     * 平台用户ID
     */
    @Schema(description = "平台用户ID", example = "123456789")
    private String platformUserId;

    /**
     * 平台账号（抖音号/快手号/视频号）
     */
    @Schema(description = "平台账号（抖音号/快手号/视频号）", example = "douyin123")
    private String platformAccount;

    /**
     * 达人头像
     */
    @Schema(description = "达人头像", example = "https://example.com/avatar.jpg")
    private String avatar;

    /**
     * 达人昵称
     */
    @Schema(description = "达人昵称", example = "美食达人王老师")
    private String nickname;

    /**
     * 行业名称
     */
    @Schema(description = "行业名称", example = "数码科技")
    private String industry;

    /**
     * 行业ID
     */
    @Schema(description = "行业ID", example = "12332")
    private Long industryId;

    /**
     * 分组ID
     */
    @Schema(description = "分组ID", example = "12332")
    private Long groupId;

    /**
     * 自动提取文案开关
     */
    @Schema(description = "自动提取文案开关", example = "true")
    private Boolean autoSyncEnabled;

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
     * 总视频数
     */
    @Schema(description = "总视频数", example = "100")
    private Integer videoCount;

    /**
     * 今日新增
     */
    @Schema(description = "今日新增", example = "5")
    private Integer todayIncrement;

    /**
     * 今日更新时间
     */
    @Schema(description = "今日更新时间", example = "2025-08-26 10:30:00")
    private LocalDateTime todaySyncTime;

    /**
     * 3日新增
     */
    @Schema(description = "3日新增", example = "15条")
    private Integer threeDayIncrement;

    /**
     * 3日更新时间
     */
    @Schema(description = "3日更新时间", example = "2025-08-26 08:00:00")
    private LocalDateTime threeDayIncrementTime;

    /**
     * 添加人
     */
    @Schema(description = "添加人")
    private String operator;

    @Schema(description = "用户ID")
    private Long userId;
}
