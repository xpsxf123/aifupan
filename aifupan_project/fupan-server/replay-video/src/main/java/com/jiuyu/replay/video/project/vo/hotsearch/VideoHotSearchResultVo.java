package com.jiuyu.replay.video.project.vo.hotsearch;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 爆款搜索结果视图对象
 *
 * @author RayChou
 * @date 2025-08-29
 * @description 爆款搜索结果的前端展示对象，包含视频信息和文案提取状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款搜索结果视图对象")
public class VideoHotSearchResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 视频ID
     */
    @Schema(description = "视频ID", example = "1001")
    private Long videoId;

    /**
     * 文案提取ID
     */
    @Schema(description = "文案提取ID", example = "1")
    private Long extractId;

    /**
     * 平台类型: 1-抖音, 2-快手, 3-视频号
     */
    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号", example = "1")
    private Byte platformType;

    /**
     * 平台视频ID
     */
    @Schema(description = "平台视频ID", example = "7123456789")
    private String platformVideoId;

    /**
     * 视频标题
     */
    @Schema(description = "视频标题", example = "美食制作教程")
    private String videoTitle;

    /**
     * 视频封面URL
     */
    @Schema(description = "视频封面URL", example = "https://example.com/cover.jpg")
    private String videoCover;

    /**
     * 视频链接
     */
    @Schema(description = "视频链接URL", example = "https://baidu.com/aa.mp4")
    private String videoUrl;

    /**
     * 视频时长(秒)
     */
    @Schema(description = "视频时长(秒)", example = "300")
    private Integer duration;

    /**
     * 发布时间
     */
    @Schema(description = "发布时间", example = "2025-08-11 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    /**
     * 达人平台用户ID
     */
    @Schema(description = "达人平台ID", example = "douyin_123")
    private String influencerPlatformUserId;

    /**
     * 达人昵称
     */
    @Schema(description = "达人昵称", example = "美食达人")
    private String influencerNickname;


    /**
     * 达人头像URL
     */
    @Schema(description = "达人头像URL", example = "https://example.com/avatar.jpg")
    private String influencerAvatar;

    /**
     * 粉丝数
     */
    @Schema(description = "粉丝数", example = "100000")
    private Long followersCount;

    /**
     * 点赞数
     */
    @Schema(description = "点赞数", example = "50000")
    private Long likeCount;

    /**
     * 评论数
     */
    @Schema(description = "评论数", example = "1000")
    private Long commentCount;

    /**
     * 分享数
     */
    @Schema(description = "分享数", example = "500")
    private Long shareCount;

    /**
     * 收藏数
     */
    @Schema(description = "收藏数", example = "800")
    private Long collectCount;

    /**
     * 评赞比（评论数/点赞数）
     */
    @Schema(description = "评赞比（评论数/点赞数，保留两位小数）", example = "0.15")
    private String commentLikeRatio;

    /**
     * 文案提取状态: 0-未提取 1-待处理, 2-处理中, 3-已完成, 4-失败
     */
    @Schema(description = "文案提取状态: 0-未提取 1-待处理, 2-处理中, 3-已完成, 4-失败", example = "0")
    private Byte extractStatus;
}
