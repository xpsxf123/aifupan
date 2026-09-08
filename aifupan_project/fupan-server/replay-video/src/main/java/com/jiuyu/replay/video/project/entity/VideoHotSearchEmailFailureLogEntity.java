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
 * 热搜视频邮箱账号失败记录表
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
@TableName("tb_video_hot_search_email_failure_log")
@Schema(description = "热搜视频邮箱账号失败记录表")
public class VideoHotSearchEmailFailureLogEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花ID）
     */
    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "主键ID（雪花ID）", example = "1")
    private Long id;

    /**
     * 邮箱账号ID
     */
    @TableField("email_account_id")
    @Schema(description = "邮箱账号ID", example = "1001")
    private Long emailAccountId;

    /**
     * 邮箱账号
     */
    @TableField("email")
    @Schema(description = "邮箱账号", example = "test@gmail.com")
    private String email;

    /**
     * 客户端IP
     */
    @TableField("client_ip")
    @Schema(description = "客户端IP", example = "192.168.1.100")
    private String clientIp;

    /**
     * 客户端城市
     */
    @TableField("client_city")
    @Schema(description = "客户端城市", example = "上海")
    private String clientCity;

    /**
     * 失败类型：1-密码错误 2-账号被封 3-网络超时 4-验证码错误 5-其他
     */
    @TableField("failure_type")
    @Schema(description = "失败类型：1-密码错误 2-账号被封 3-网络超时 4-验证码错误 5-其他", example = "1")
    private Integer failureType;

    /**
     * 失败原因详情
     */
    @TableField("failure_reason")
    @Schema(description = "失败原因详情", example = "密码验证失败")
    private String failureReason;

    /**
     * 错误码
     */
    @TableField("error_code")
    @Schema(description = "错误码", example = "AUTH_FAILED")
    private String errorCode;

    /**
     * 错误信息
     */
    @TableField("error_message")
    @Schema(description = "错误信息", example = "Authentication failed")
    private String errorMessage;

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

