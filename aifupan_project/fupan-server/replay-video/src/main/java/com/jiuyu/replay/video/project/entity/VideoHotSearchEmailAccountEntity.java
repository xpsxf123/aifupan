package com.jiuyu.replay.video.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 热搜视频邮箱账号主表
 *
 * @author RayChou
 * @since 2025-11-25
 */
@Getter
@Setter
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_video_hot_search_email_account")
@Schema(description = "热搜视频邮箱账号主表")
public class VideoHotSearchEmailAccountEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花ID）
     */
    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "主键ID（雪花ID）", example = "1")
    private Long id;

    /**
     * 邮箱账号
     */
    @TableField("email")
    @Schema(description = "邮箱账号", example = "test@gmail.com")
    private String email;

    /**
     * 邮箱密码（加密存储）
     */
    @TableField("email_password")
    @Schema(description = "邮箱密码（加密存储）", example = "encrypted_password")
    private String emailPassword;

    /**
     * 账号类型：1-Gmail 2-Outlook 3-QQ邮箱 4-163邮箱 5-其他
     */
    @TableField("account_type")
    @Schema(description = "账号类型：1-Gmail 2-Outlook 3-QQ邮箱 4-163邮箱 5-其他", example = "1")
    private Integer accountType;

    /**
     * 账号归属城市（用于同城优先分配）
     */
    @TableField("city")
    @Schema(description = "账号归属城市", example = "上海")
    private String city;

    /**
     * 账号状态：0-不可用 1-可用 2-使用中 3-已禁用
     */
    @TableField("account_status")
    @Schema(description = "账号状态：0-不可用 1-可用 2-使用中 3-已禁用", example = "1")
    private Integer accountStatus;

    /**
     * 最后失败时间（用于自动修复判断）
     */
    @TableField("last_failure_time")
    @Schema(description = "最后失败时间", example = "2025-11-25 10:00:00")
    private LocalDateTime lastFailureTime;

    /**
     * 最后使用时间
     */
    @TableField("last_use_time")
    @Schema(description = "最后使用时间", example = "2025-11-25 10:30:00")
    private LocalDateTime lastUseTime;

    /**
     * 最后使用的客户端IP
     */
    @TableField("last_use_client_ip")
    @Schema(description = "最后使用的客户端IP", example = "192.168.1.100")
    private String lastUseClientIp;

    /**
     * 使用超时时间（分钟），超过此时间自动释放
     */
    @TableField("use_timeout_minutes")
    @Schema(description = "使用超时时间（分钟）", example = "60")
    private Integer useTimeoutMinutes;

    /**
     * 当前使用用户数（最多5个）
     */
    @TableField("current_user_count")
    @Schema(description = "当前使用用户数", example = "2")
    private Integer currentUserCount;

    /**
     * 最大并发用户数
     */
    @TableField("max_concurrent_users")
    @Schema(description = "最大并发用户数", example = "5")
    private Integer maxConcurrentUsers;

    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注", example = "测试账号")
    private String remark;

    /**
     * 创建时间
     */
    @TableField("created_date")
    @Schema(description = "创建时间", example = "2025-11-25 10:00:00")
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    @TableField("update_date")
    @Schema(description = "更新时间", example = "2025-11-25 10:30:00")
    private LocalDateTime updateDate;

    /**
     * 是否删除：0-未删除 1-已删除
     */
    @TableField("is_deleted")
    @Schema(description = "是否删除：0-未删除 1-已删除", example = "0")
    private Byte isDeleted;
}

