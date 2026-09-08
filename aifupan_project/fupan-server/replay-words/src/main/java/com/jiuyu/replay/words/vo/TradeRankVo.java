package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author RayChou
 * @date 2025/10/23 10:20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "行业热榜视图对象")
public class TradeRankVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Schema(description = "主键ID", example = "1234567890")
    private Long id;

    /**
     * 主播平台类型 1:抖音 2：快手 3：视频号
     */
    @Schema(description = "主播平台类型 1:抖音 2：快手 3：视频号", example = "1")
    private Byte anchorPlatformType;

    /**
     * 主播昵称
     */
    @Schema(description = "主播昵称", example = "张三直播间")
    private String anchorName;

    /**
     * 主播头像
     */
    @Schema(description = "主播头像URL", example = "https://example.com/avatar.jpg")
    private String anchorAvatar;

    /**
     * 主播平台账号
     */
    @Schema(description = "主播平台账号", example = "douyin_123456")
    private String anchorPlatformAccount;

    /**
     * 禅妈妈唯一id
     */
    @Schema(description = "禅妈妈唯一id", example = "1234567890")
    private String anchorId;

    /**
     * 账号热度
     */
    @Schema(description = "账号热度", example = "98.5")
    private String accountHeat;

    /**
     * 粉丝量
     */
    @Schema(description = "粉丝量", example = "1000000")
    private Long followerCount;

    /**
     * 销售额
     */
    @Schema(description = "销售额", example = "5000000.00")
    private String totalAmount;

    /**
     * 平均在线
     */
    @Schema(description = "平均场观", example = "50000")
    private String liveAverageUser;

    /**
     * 数据更新时间
     */
    @Schema(description = "数据更新时间", example = "2025-10-23 10:30:00")
    private LocalDateTime updateTime;
}
