package com.jiuyu.replay.video.project.vo.hotsearch;

import com.jiuyu.replay.common.validated.EnumValue;
import com.jiuyu.replay.video.project.bo.influencer.VideoInfluencerInfoBo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

/**
 * 爆款搜索同步视频信息VO
 * 用于C#客户端同步视频数据时的入参
 *
 * @author RayChou
 * @date 2025-08-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款搜索同步视频信息VO")
public class VideoHotSearchSyncVideoVo {

    // ==================== 视频基础信息 ====================

    private Long videoId;

    /**
     * 平台类型: 1-抖音, 2-快手, 3-视频号, 4-本地上传
     */
    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号, 4-本地上传", example = "1")
    @NotNull(message = "平台类型不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法", groups = VideoInfluencerInfoBo.Add.class)
    private Byte platformType;

    /**
     * 平台视频ID
     */
    @Schema(description = "平台视频ID", example = "7123456789")
    @NotBlank(message = "平台视频ID不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 100, message = "平台视频ID不合法", groups = VideoInfluencerInfoBo.Add.class)
    private String platformVideoId;

    /**
     * 视频文件HASH值 mongodb存储关联值内容
     */
    @Schema(description = "视频文件HASH值 mongodb存储关联值内容", example = "abc123def456")
    @NotBlank(message = "视频文件HASH值不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 100, message = "视频文件HASH值不合法", groups = VideoInfluencerInfoBo.Add.class)
    private String videoHash;

    /**
     * 视频标题
     */
    @Schema(description = "视频标题", example = "美食制作教程")
    @NotBlank(message = "视频标题不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 500, message = "标题不合法", groups = VideoInfluencerInfoBo.Add.class)
    private String title;

    /**
     * 视频描述
     */
    @Schema(description = "视频描述", example = "教你制作美味的家常菜")
    @NotBlank(message = "视频描述不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private String description;

    /**
     * 封面图片URL
     */
    @Schema(description = "封面图片URL", example = "https://example.com/cover.jpg")
    @NotBlank(message = "封面图片不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 1000, message = "封面图片url太长", groups = VideoInfluencerInfoBo.Add.class)
    private String coverUrl;

    /**
     * 视频播放URL
     */
    @Schema(description = "视频播放URL", example = "https://example.com/video.mp4")
    @NotBlank(message = "视频播放url不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 1000, message = "视频播放url太长", groups = VideoInfluencerInfoBo.Add.class)
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
    @NotNull(message = "点赞数不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Long likeCount;

    /**
     * 评论数
     */
    @Schema(description = "评论数", example = "100")
    @NotNull(message = "评论数不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Long commentCount;

    /**
     * 分享数
     */
    @Schema(description = "分享数", example = "50")
    @NotNull(message = "分享数不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Long shareCount;

    /**
     * 收藏数
     */
    @Schema(description = "收藏数", example = "1000000")
    @NotNull(message = "收藏数不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Long collectCount;

    /**
     * 视频时长(秒)
     */
    @Schema(description = "视频时长(秒)", example = "300")
    @NotNull(message = "视频时长(秒)不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Integer duration;

    /**
     * 发布时间
     */
    @Schema(description = "发布时间", example = "2025-08-11 10:30:00")
    @NotNull(message = "视频发布时间不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private LocalDateTime publishTime;


    // ==================== 达人信息（新增字段） ====================

    @Schema(description = "达人平台类型: 1-抖音, 2-快手, 3-视频号", example = "1")
    @NotNull(message = "平台类型不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法", groups = VideoInfluencerInfoBo.Add.class)
    private Byte influencerPlatformType;

    @Schema(description = "达人平台用户ID", example = "influencer123456")
    @NotBlank(message = "平台用户ID不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 100, message = "平台用户ID长度不能超过100个字符", groups = VideoInfluencerInfoBo.Add.class)
    private String influencerPlatformUserId;

    @Schema(description = "达人昵称", example = "美食达人小王")
    @NotBlank(message = "昵称不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 100, message = "昵称长度不能超过100个字符", groups = VideoInfluencerInfoBo.Add.class)
    private String influencerNickname;

    @Schema(description = "达人头像URL", example = "https://example.com/influencer_avatar.jpg")
    @NotBlank(message = "达人头像URL不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 1000, message = "达人头像URL太长", groups = VideoInfluencerInfoBo.Add.class)
    private String influencerAvatar;

    @Schema(description = "达人粉丝数", example = "100000")
    @NotNull(message = "达人粉丝数不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Long influencerFollowersCount;
}
