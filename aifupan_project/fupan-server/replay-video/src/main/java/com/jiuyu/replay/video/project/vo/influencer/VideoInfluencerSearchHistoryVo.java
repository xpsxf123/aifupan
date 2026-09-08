package com.jiuyu.replay.video.project.vo.influencer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 达人搜索结果视图对象
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 达人搜索结果的前端展示对象，包含搜索信息和结果列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索达人历史记录业务视图对象")
public class VideoInfluencerSearchHistoryVo {

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
     * 昵称
     */
    @Schema(description = "昵称", example = "美食达人小王")
    private String nickname;

    /**
     * 头像URL
     */
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    /**
     * 平台账号（抖音号/快手号/视频号）
     */
    @Schema(description = "平台账号（抖音号/快手号/视频号）", example = "douyin123")
    private String platformAccount;

    /**
     * 作品数
     */
    @Schema(description = "作品数", example = "200")
    private Integer videoCount;

    /**
     * 粉丝数
     */
    @Schema(description = "粉丝数", example = "100000")
    private Long followersCount;
}
