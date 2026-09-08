package com.jiuyu.replay.video.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jiuyu.replay.common.validated.EnumValue;
import com.jiuyu.replay.video.project.bo.influencer.VideoInfluencerInfoBo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 视频基础信息表
 * </p>
 *
 * @author RayChou
 * @since 2025-08-13
 */
@Getter
@Setter
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_video_info")
@Schema(description = "视频基础信息表")
public class VideoInfoEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花ID）
     */
    @TableId("id")
    @Schema(description = "主键ID（雪花ID）", example = "1")
    private Long id;

    /**
     * 平台类型: 1-抖音, 2-快手, 3-视频号, 4-本地上传
     */
    @TableField("platform_type")
    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号, 4-本地上传", example = "1")
    @NotNull(message = "平台类型不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法", groups = VideoInfluencerInfoBo.Add.class)
    private Byte platformType;

    /**
     * 平台视频ID
     */
    @TableField("platform_video_id")
    @Schema(description = "平台视频ID", example = "7123456789")
    @NotBlank(message = "平台视频ID不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 100, message = "平台视频ID不合法", groups = VideoInfluencerInfoBo.Add.class)
    private String platformVideoId;

    /**
     * 视频文件HASH值 mongodb存储关联值内容
     */
    @TableField("video_hash")
    @Schema(description = "视频文件HASH值 mongodb存储关联值内容", example = "abc123def456")
    @NotBlank(message = "视频文件HASH值不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 100, message = "视频文件HASH值不合法", groups = VideoInfluencerInfoBo.Add.class)
    private String videoHash;

    /**
     * 视频标题
     */
    @TableField("title")
    @Schema(description = "视频标题", example = "美食制作教程")
    @NotBlank(message = "视频标题不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 500, message = "标题不合法", groups = VideoInfluencerInfoBo.Add.class)
    private String title;

    /**
     * 视频描述
     */
    @TableField("video_description")
    @Schema(description = "视频描述", example = "教你制作美味的家常菜")
    @NotBlank(message = "视频描述不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private String description;

    /**
     * 封面图片URL
     */
    @TableField("cover_url")
    @Schema(description = "封面图片URL", example = "https://example.com/cover.jpg")
    @NotBlank(message = "封面图片不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 1000, message = "封面图片url太长", groups = VideoInfluencerInfoBo.Add.class)
    private String coverUrl;

    /**
     * 视频播放URL
     */
    @TableField("video_url")
    @Schema(description = "视频播放URL", example = "https://example.com/video.mp4")
    @NotBlank(message = "视频播放url不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 1000, message = "视频播放url太长", groups = VideoInfluencerInfoBo.Add.class)
    private String videoUrl;

    /**
     * 作者ID 如果是本地上传：user_id 平台：influencer_id
     */
    @TableField("author_id")
    @Schema(description = "作者ID 如果是本地上传：user_id 平台：influencer_id", example = "author123")
    private String authorId;

    /**
     * 作者名称
     */
    @TableField("author_name")
    @Schema(description = "作者名称", example = "美食达人小王")
    private String authorName;

    /**
     * 点赞数
     */
    @TableField("like_count")
    @Schema(description = "点赞数", example = "500")
    @NotNull(message = "点赞数不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Long likeCount;

    /**
     * 评论数
     */
    @TableField("comment_count")
    @Schema(description = "评论数", example = "100")
    @NotNull(message = "评论数不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Long commentCount;

    /**
     * 分享数
     */
    @TableField("share_count")
    @Schema(description = "分享数", example = "50")
    @NotNull(message = "分享数不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Long shareCount;

    /**
     * 收藏数
     */
    @TableField("collect_count")
    @Schema(description = "收藏数", example = "1000000")
    @NotNull(message = "收藏数不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Long collectCount;

    /**
     * 视频时长(秒)
     */
    @TableField("duration")
    @Schema(description = "视频时长(秒)", example = "300")
    @NotNull(message = "视频时长(秒)不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Integer duration;

    /**
     * 发布时间
     */
    @TableField("publish_time")
    @Schema(description = "发布时间", example = "2025-08-11 10:30:00")
    @NotNull(message = "视频发布时间不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private LocalDateTime publishTime;

    /**
     * 文案提取状态: 0-未提取 1-已提取
     */
    @TableField("extract_status")
    @Schema(description = "文案提取状态: 0-未提取 1-已提取", example = "1")
    private Byte extractStatus;

    /**
     * 文案提取时间
     */
    @Schema(description = "文案提取时间", example = "2025-08-11 10:30:00")
    private LocalDateTime extractTime;

    /**
     * AI分析状态:  0-未分析 1-已分析
     */
    @TableField("analysis_status")
    @Schema(description = "AI分析状态: 0-未分析 1-已分析", example = "1")
    private Byte analysisStatus;

    /**
     * AI分析时间
     */
    @Schema(description = "AI分析时间", example = "2025-08-11 10:30:00")
    private LocalDateTime analysisTime;

    /**
     * 创建时间
     */
    @TableField("created_date")
    @Schema(description = "创建时间", example = "2025-08-11 10:30:00")
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    @TableField("update_date")
    @Schema(description = "更新时间", example = "2025-08-11 10:30:00")
    private LocalDateTime updateDate;

    /**
     * 是否删除: 0-未删除, 1-已删除
     */
    @TableField("is_deleted")
    @TableLogic
    @Schema(description = "是否删除: 0-未删除, 1-已删除", example = "0")
    private Byte isDeleted;
}
