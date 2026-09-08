package com.jiuyu.replay.video.project.bo.hotsearch;

import com.jiuyu.replay.common.validated.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 爆款搜索视频信息业务对象
 *
 * @author RayChou
 * @date 2025-08-29
 * @description 爆款搜索视频信息的业务对象，包含视频基础信息和统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款搜索视频信息业务对象")
public class VideoHotSearchInfoBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号", example = "1")
    @NotNull(message = "平台类型不能为空")
    @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法")
    private Byte platformType;

    @Schema(description = "平台视频ID", example = "7123456789")
    @NotBlank(message = "平台视频ID不能为空")
    @Length(max = 100, message = "平台视频ID长度不能超过100个字符")
    private String platformVideoId;

    @Schema(description = "平台用户ID", example = "douyin_123")
    @NotBlank(message = "平台用户ID不能为空")
    @Length(max = 100, message = "平台用户ID长度不能超过100个字符")
    private String platformUserId;

    @Schema(description = "视频标题", example = "美食制作教程")
    @NotBlank(message = "视频标题不能为空")
    @Length(max = 500, message = "视频标题长度不能超过500个字符")
    private String title;

    @Schema(description = "视频文件hash值", example = "8ewrwr82324242")
    @NotBlank(message = "视频文件hash值不能为空")
    @Length(max = 100, message = "视频文件hash值不能超过100个字符")
    private String videoHash;

    @Schema(description = "视频描述", example = "详细的美食制作过程")
    @NotBlank(message = "视频描述不能为空")
    @Length(max = 2000, message = "视频描述长度不能超过2000个字符")
    private String description;

    @Schema(description = "视频封面URL", example = "https://example.com/cover.jpg")
    @NotBlank(message = "视频封面URL不能为空")
    @Length(max = 500, message = "视频封面URL长度不能超过500个字符")
    private String coverUrl;

    @Schema(description = "视频播放URL", example = "https://example.com/video.mp4")
    @NotBlank(message = "视频播放URL不能为空")
    @Length(max = 500, message = "视频播放URL长度不能超过500个字符")
    private String videoUrl;

    @Schema(description = "作者ID", example = "author123")
    @NotBlank(message = "作者ID不能为空")
    @Length(max = 100, message = "作者ID长度不能超过100个字符")
    private String authorId;

    @Schema(description = "作者名称", example = "美食达人小王")
    @NotBlank(message = "作者名称不能为空")
    @Length(max = 100, message = "作者名称长度不能超过100个字符")
    private String authorName;

    @Schema(description = "达人头像URL", example = "https://example.com/avatar.jpg")
    @NotBlank(message = "达人头像URL不能为空")
    @Length(max = 500, message = "达人头像URL长度不能超过500个字符")
    private String influencerAvatar;

    @Schema(description = "达人粉丝数", example = "100000")
    @NotNull(message = "达人粉丝数不能为空")
    private Long influencerFollowersCount;

    @Schema(description = "视频时长(秒)", example = "300")
    @NotNull(message = "视频时长不能为空")
    private Integer duration;

    @Schema(description = "发布时间", example = "2025-08-11 10:30:00")
    @NotNull(message = "视频发布时间不能为空")
    private LocalDateTime publishTime;

    @Schema(description = "点赞数", example = "50000")
    @NotNull(message = "点赞数不能为空")
    private Long likeCount;

    @Schema(description = "评论数", example = "1000")
    @NotNull(message = "评论数不能为空")
    private Long commentCount;

    @Schema(description = "分享数", example = "500")
    @NotNull(message = "分享数不能为空")
    private Long shareCount;

    @Schema(description = "收藏数", example = "800")
    @NotNull(message = "收藏数不能为空")
    private Long collectCount;
}
