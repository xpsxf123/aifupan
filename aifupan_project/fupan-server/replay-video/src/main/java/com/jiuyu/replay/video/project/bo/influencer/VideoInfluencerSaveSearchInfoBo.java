package com.jiuyu.replay.video.project.bo.influencer;

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

/**
 * 达人信息视图对象
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 达人信息的前端展示对象，包含达人基础信息和统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "达人搜索业务对象")
public class VideoInfluencerSaveSearchInfoBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-小红书", example = "1")
    @NotNull(message = "平台类型不能为空")
    @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法")
    private Byte platformType;

    @Schema(description = "平台用户ID", example = "douyin_123")
    @NotBlank(message = "平台用户ID不能为空")
    @Length(max = 100, message = "平台用户ID长度不能超过100个字符")
    private String platformUserId;

    @Schema(description = "平台账号（抖音号/快手号/视频号）", example = "douyin123")
    @NotBlank(message = "平台用户账号不能为空")
    @Length(max = 100, message = "平台用户账号长度不能超过100个字符")
    private String platformAccount;

    @Schema(description = "昵称", example = "达人昵称")
    @NotBlank(message = "昵称不能为空")
    @Length(max = 100, message = "昵称长度不能超过100个字符")
    private String nickname;

    @Schema(description = "头像URL", example = "头像URL")
    @NotBlank(message = "头像URL不能为空")
    @Length(max = 500, message = "头像URL不能超过500个字符")
    private String avatar;

    @Schema(description = "个人简介", example = "个人简介")
    //@NotBlank(message = "个人简介不能为空")
    @Length(max = 10000, message = "个人简介不能超过10000个字符")
    private String description;

    @Schema(description = "粉丝数", example = "粉丝数")
    @NotNull(message = "粉丝数不能为空")
    private Long followersCount;

    @Schema(description = "关注数", example = "关注数")
    @NotNull(message = "关注数不能为空")
    private Long followingCount;

    @Schema(description = "作品数", example = "作品数")
    @NotNull(message = "作品数不能为空")
    private Integer videoCount;

    @Schema(description = "点赞数", example = "点赞数")
    @NotNull(message = "点赞数不能为空")
    private Long likeCount;

    @Schema(description = "认证状态: 0-未认证, 1-个人认证, 2-企业/机构认证, 3-政府/官方组织认证, 4-媒体/特殊认证", example = "0")
    @NotNull(message = "认证状态: 0-未认证, 1-个人认证, 2-企业/机构认证, 3-政府/官方组织认证, 4-媒体/特殊认证")
    @EnumValue(byteValues = {0, 1, 2, 3, 4}, message = "认证状态不合法")
    private Byte verificationStatus;

    @Schema(description = "认证信息", example = "认证信息")
    private String verificationInfo;
}
