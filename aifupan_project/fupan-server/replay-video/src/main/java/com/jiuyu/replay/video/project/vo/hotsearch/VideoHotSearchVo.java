package com.jiuyu.replay.video.project.vo.hotsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 爆款搜索视图对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 爆款搜索结果显示数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款搜索视图对象")
public class VideoHotSearchVo implements Serializable {

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
    @Schema(description = "视频标题", example = "这名应届毕业生了，他明明很有才呢？#比熊 #狗狗和狗狗的日常 #主任后了也要孩子")
    private String videoTitle;

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
    @Schema(description = "粉丝数", example = "271.9w")
    private Long followersCount;

    /**
     * 点赞数
     */
    @Schema(description = "点赞数", example = "143.2w")
    private Long likeCount;

    /**
     * 评论数
     */
    @Schema(description = "评论数", example = "55.4w")
    private Long commentCount;

    /**
     * 转发数
     */
    @Schema(description = "转发数", example = "1,049")
    private Long shareCount;

    /**
     * 收藏数
     */
    @Schema(description = "收藏数", example = "526")
    private Long collectCount;
}
