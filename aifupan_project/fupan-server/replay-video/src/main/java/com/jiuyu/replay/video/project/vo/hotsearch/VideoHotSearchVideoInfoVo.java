package com.jiuyu.replay.video.project.vo.hotsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author RayChou
 * @date 2025/9/3 15:32
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜爆款历史数据组合视图对象")
public class VideoHotSearchVideoInfoVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 达人平台类型: 1-抖音, 2-快手, 3-视频号
     */
    @Schema(description = "达人平台类型: 1-抖音, 2-快手, 3-视频号", example = "1")
    private Byte influencerPlatformType;

    /**
     * 达人平台用户ID
     */
    @Schema(description = "达人平台用户ID", example = "douyin_123")
    private String influencerPlatformUserId;

    /**
     * 达人昵称
     */
    @Schema(description = "达人昵称", example = "天元王刚")
    private String influencerNickname;

    /**
     * 达人头像URL
     */
    @Schema(description = "达人头像URL", example = "https://douyin.com/avatar.jpg")
    private String influencerAvatar;

    /**
     * 达人粉丝数
     */
    @Schema(description = "达人粉丝数", example = "10000")
    private Long influencerFollowersCount;


    /**
     * 视频平台类型: 1-抖音, 2-快手, 3-视频号, 4-本地上传
     */
    @Schema(description = "视频平台类型: 1-抖音, 2-快手, 3-视频号, 4-本地上传", example = "1")
    private Byte platformType;

    /**
     * 平台视频ID
     */
    @Schema(description = "平台视频ID", example = "7123456789")
    private String platformVideoId;

    /**
     * 视频文件HASH值 mongodb存储关联值内容
     */
    @Schema(description = "视频文件HASH值 mongodb存储关联值内容", example = "abc123def456")
    private String videoHash;

    /**
     * 视频标题
     */
    @Schema(description = "视频标题", example = "美食制作教程")
    private String title;

    /**
     * 视频描述
     */
    @Schema(description = "视频描述", example = "教你制作美味的家常菜")
    private String description;

    /**
     * 封面图片URL
     */
    @Schema(description = "封面图片URL", example = "https://example.com/cover.jpg")
    private String coverUrl;

    /**
     * 视频播放URL
     */
    @Schema(description = "视频播放URL", example = "https://example.com/video.mp4")
    private String videoUrl;

    /**
     * 作者ID 如果是本地上传：user_id 平台：influencer_id
     */
    @Schema(description = "作者ID 如果是本地上传：user_id 平台：influencer_id", example = "author123")
    private String authorId;

    /**
     * 作者名称
     */
    @Schema(description = "作者名称", example = "美食达人小王")
    private String authorName;

    /**
     * 点赞数
     */
    @Schema(description = "点赞数", example = "500")
    private Long likeCount;

    /**
     * 评论数
     */
    @Schema(description = "评论数", example = "100")
    private Long commentCount;

    /**
     * 分享数
     */
    @Schema(description = "分享数", example = "50")
    private Long shareCount;

    /**
     * 收藏数
     */
    @Schema(description = "收藏数", example = "1000000")
    private Long collectCount;

    /**
     * 视频时长(秒)
     */
    @Schema(description = "视频时长(秒)", example = "300")
    private Integer duration;

    /**
     * 发布时间
     */
    @Schema(description = "发布时间", example = "2025-08-11 10:30:00")
    private LocalDateTime publishTime;

}
