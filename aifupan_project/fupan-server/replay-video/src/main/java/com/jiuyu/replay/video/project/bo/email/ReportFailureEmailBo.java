package com.jiuyu.replay.video.project.bo.email;

import com.jiuyu.replay.common.validated.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 上报不可用邮箱账号BO
 *
 * @author RayChou
 * @since 2025-11-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "上报不可用邮箱账号BO")
public class ReportFailureEmailBo {

    /**
     * 账号ID
     */
    @NotNull(message = "账号ID不能为空")
    @Schema(description = "账号ID", example = "1234567890", required = true)
    private Long accountId;

    /**
     * 邮箱账号
     */
    @NotBlank(message = "邮箱账号不能为空")
    @Schema(description = "邮箱账号", example = "test@gmail.com", required = true)
    private String email;

    /**
     * 客户端IP
     */
    @Schema(description = "客户端IP", example = "192.168.1.100")
    private String clientIp;

    /**
     * 失败类型：1-密码错误 2-账号被封（永久禁用，不进行自动修复） 3-网络超时 4-验证码错误 5-其他
     */
    @NotNull(message = "失败类型不能为空")
    @EnumValue(intValues = {1, 2, 3, 4, 5}, message = "失败类型不合法")
    @Schema(description = "失败类型：1-密码错误 2-账号被封（永久禁用，不进行自动修复） 3-网络超时 4-验证码错误 5-其他", example = "1", required = true)
    private Integer failureType;

    /**
     * 失败原因详情
     */
    @Schema(description = "失败原因详情", example = "密码验证失败，无法登录")
    private String failureReason;

    /**
     * 错误码
     */
    @Schema(description = "错误码", example = "AUTH_FAILED")
    private String errorCode;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息", example = "Authentication failed: Invalid credentials")
    private String errorMessage;
}

