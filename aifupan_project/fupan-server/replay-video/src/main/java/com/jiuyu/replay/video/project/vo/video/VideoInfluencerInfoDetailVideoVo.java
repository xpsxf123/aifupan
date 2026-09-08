package com.jiuyu.replay.video.project.vo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author RayChou
 * @date 2025/8/22 15:03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "达人详情视频视图对象")
public class VideoInfluencerInfoDetailVideoVo {
    /**
     * 主键ID（雪花ID）
     */
    @Schema(description = "主键ID（雪花ID）", example = "1")
    private Long id;

    /**
     * 提取文案ID
     */
    @Schema(description = "提取文案ID", example = "1")
    private Long extractId;

    /**
     * 平台类型: 1-抖音, 2-快手, 3-视频号, 4-本地上传
     */
    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号, 4-本地上传", example = "1")
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
     * 视频封面URL
     */
    @Schema(description = "视频封面url", example = "https://douyin.com/coverUrl.jpg")
    private String coverUrl;

    /**
     * 视频地址URL
     */
    @Schema(description = "视频URL", example = "https://douyin.com/videoUrl.mp4")
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
    private LocalDateTime publishTime;

    @Schema(description = "点赞数")
    private List<VideoInfoIncrementVo> likeCountIncrements;

    @Schema(description = "评论数")
    private List<VideoInfoIncrementVo> commentCountIncrements;

    @Schema(description = "转发数")
    private List<VideoInfoIncrementVo> shareCountIncrements;

    @Schema(description = "收藏数")
    private List<VideoInfoIncrementVo> collectCountIncrements;

    /**
     * 评赞比（评论数/点赞数）
     */
    @Schema(description = "评赞比（评论数/点赞数，保留两位小数）")
    private List<VideoCommentLikeRatioVo> commentLikeRatios;

    @Schema(description = "文案提取状态: 0-未提取 1-待处理, 2-处理中, 3-已完成, 4-失败", example = "1")
    private Byte extractStatus;
}
