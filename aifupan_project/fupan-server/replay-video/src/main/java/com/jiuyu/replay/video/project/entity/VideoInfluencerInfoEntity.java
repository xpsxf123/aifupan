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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 达人信息表
 * </p>
 *
 * @author RayChou
 * @since 2025-08-13
 */
@Getter
@Setter
@Accessors(chain = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_video_influencer_info")
@Schema(description = "达人信息表")
public class VideoInfluencerInfoEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花ID）
     */
    @TableId("id")
    @Schema(description = "主键ID（雪花ID）", example = "1")
    private Long id;

    /**
     * 平台类型: 1-抖音, 2-快手, 3-视频号
     */
    @TableField("platform_type")
    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号", example = "1")
    @NotNull(message = "平台类型不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法", groups = VideoInfluencerInfoBo.Add.class)
    private Byte platformType;

    /**
     * 平台用户ID
     */
    @TableField("platform_user_id")
    @Schema(description = "平台用户ID", example = "123456789")
    @NotBlank(message = "平台用户ID不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 100, message = "平台用户ID长度不能超过100个字符", groups = VideoInfluencerInfoBo.Add.class)
    private String platformUserId;

    /**
     * 平台账号（抖音号/快手号/视频号）
     */
    @TableField("platform_account")
    @Schema(description = "平台账号（抖音号/快手号/视频号）", example = "douyin123")
    @NotBlank(message = "平台用户账号不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 100, message = "平台用户账号长度不能超过100个字符", groups = VideoInfluencerInfoBo.Add.class)
    private String platformAccount;

    /**
     * 昵称
     */
    @TableField("nickname")
    @Schema(description = "昵称", example = "美食达人小王")
    @NotBlank(message = "昵称不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 100, message = "昵称长度不能超过100个字符", groups = VideoInfluencerInfoBo.Add.class)
    private String nickname;

    /**
     * 头像URL
     */
    @TableField("avatar")
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    //@NotBlank(message = "头像URL不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 500, message = "头像URL不能超过500个字符", groups = VideoInfluencerInfoBo.Add.class)
    private String avatar;

    /**
     * 个人简介
     */
    @TableField("influencer_description")
    @Schema(description = "个人简介", example = "专注美食分享的达人")
    //@NotBlank(message = "个人简介不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @Length(max = 10000, message = "个人简介不能超过10000个字符", groups = VideoInfluencerInfoBo.Add.class)
    private String description;

    /**
     * 粉丝数
     */
    @TableField("followers_count")
    @Schema(description = "粉丝数", example = "100000")
    @NotNull(message = "粉丝数不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Long followersCount;

    /**
     * 关注数
     */
    @TableField("following_count")
    @Schema(description = "关注数", example = "500")
    @NotNull(message = "关注数不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Long followingCount;

    /**
     * 作品数
     */
    @TableField("video_count")
    @Schema(description = "作品数", example = "200")
    @NotNull(message = "作品数不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Integer videoCount;

    /**
     * 获赞数
     */
    @TableField("like_count")
    @Schema(description = "获赞数", example = "1000000")
    @NotNull(message = "点赞数不能为空", groups = VideoInfluencerInfoBo.Add.class)
    private Long likeCount;

    /**
     * 认证状态: 0-未认证, 1-个人认证, 2-企业/机构认证, 3-政府/官方组织认证, 4-媒体/特殊认证
     */
    @TableField("verification_status")
    @Schema(description = "认证状态: 0-未认证, 1-个人认证, 2-企业/机构认证, 3-政府/官方组织认证, 4-媒体/特殊认证", example = "1")
    @NotNull(message = "认证状态不能为空", groups = VideoInfluencerInfoBo.Add.class)
    @EnumValue(byteValues = {0, 1, 2, 3, 4}, message = "认证状态不合法", groups = VideoInfluencerInfoBo.Add.class)
    private Byte verificationStatus;

    /**
     * 认证信息
     */
    @TableField("verification_info")
    @Schema(description = "认证信息", example = "美食博主")
    private String verificationInfo;

    /**
     * 最后同步时间
     */
    @TableField("last_sync_time")
    @Schema(description = "最后同步时间", example = "2025-08-11 10:30:00")
    private LocalDateTime lastSyncTime;

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
