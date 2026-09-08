package com.jiuyu.replay.video.project.vo.influencer;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 达人信息视图对象
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 达人信息的前端展示对象，包含达人基础信息和统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "达人信息视图对象")
public class VideoInfluencerSearchInfoVo {

    @Schema(description = "达人ID", example = "123456789")
    private Long influencerId;

    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-小红书", example = "1")
    private Integer platformType;

    @Schema(description = "平台类型名称", example = "抖音")
    private String platformTypeName;

    @Schema(description = "平台用户ID", example = "douyin_123")
    private String platformUserId;

    @Schema(description = "平台账号（抖音号/快手号/视频号）", example = "douyin123")
    private String platformAccount;

    @Schema(description = "昵称", example = "达人昵称")
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 粉丝数
     */
    private Long followerCount;

    /**
     * 粉丝数显示文本
     */
    private String followerCountText;

    /**
     * 作品数
     */
    private Long videoCount;

    /**
     * 个人简介
     */
    private String description;

    /**
     * 认证状态: 0-未认证, 1-个人认证, 2-企业认证
     */
    private Integer verificationStatus;

    /**
     * 认证状态名称
     */
    private String verificationStatusName;

    /**
     * 认证信息
     */
    private String verificationInfo;

    /**
     * 是否已订阅
     */
    private Boolean isSubscribed;

    /**
     * 订阅ID（如果已订阅）
     */
    private String subscriptionId;

    /**
     * 所属分组ID（默认分组为0）
     */
    private Long groupId;

    /**
     * 所属分组名称
     */
    private String groupName;

    /**
     * 近3日更新视频数
     */
    private Integer recentVideoCount;

    /**
     * 总获赞数
     */
    private Long totalLikeCount;

    /**
     * 总获赞数显示文本
     */
    private String totalLikeCountText;

    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdateTime;
}
