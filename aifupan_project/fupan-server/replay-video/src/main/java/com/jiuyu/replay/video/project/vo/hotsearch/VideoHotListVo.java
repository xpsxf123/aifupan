package com.jiuyu.replay.video.project.vo.hotsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 爆款视频列表视图对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 爆款视频列表显示数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款视频列表视图对象")
public class VideoHotListVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 视频ID
     */
    @Schema(description = "视频ID", example = "1001")
    private Long videoId;

    /**
     * 视频封面
     */
    @Schema(description = "视频封面", example = "https://example.com/cover.jpg")
    private String videoCover;

    /**
     * 视频标题
     */
    @Schema(description = "视频标题", example = "谁家不大 先记住科目的大学生日常")
    private String videoTitle;

    /**
     * 发布时间
     */
    @Schema(description = "发布时间", example = "2025-05-28 11:30")
    private String publishTime;

    /**
     * 时长（秒）
     */
    @Schema(description = "时长（秒）", example = "120")
    private Integer duration;

    /**
     * 达人头像
     */
    @Schema(description = "达人头像", example = "https://example.com/avatar.jpg")
    private String influencerAvatar;

    /**
     * 达人昵称
     */
    @Schema(description = "达人昵称", example = "小明明-")
    private String influencerNickname;

    /**
     * 粉丝数
     */
    @Schema(description = "粉丝数", example = "2719000")
    private Long followersCount;

    /**
     * 点赞数
     */
    @Schema(description = "点赞数", example = "1432000")
    private Long likeCount;

    /**
     * 评论数
     */
    @Schema(description = "评论数", example = "554000")
    private Long commentCount;

    /**
     * 转发数
     */
    @Schema(description = "转发数", example = "1049")
    private Long shareCount;

    /**
     * 收藏数
     */
    @Schema(description = "收藏数", example = "526")
    private Long collectCount;
}
