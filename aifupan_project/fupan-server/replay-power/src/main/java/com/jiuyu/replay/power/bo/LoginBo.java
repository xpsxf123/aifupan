package com.jiuyu.replay.power.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
@Schema
public class LoginBo {

    /**
     * 账号
     */
    @Schema(description = "账号")
    @NotBlank(message = "账号不能为空")
    private String username;
    /**
     * 密码
     */
    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    private String password;
    /**
     * 手机号
     */
    @Schema(description = "手机号")
    @NotBlank(message = "手机号不能为空")
    private String phone;
    /**
     * 验证码
     */
    @Schema(description = "验证码")
    @NotBlank(message = "验证码不能为空")
    private String code;
    /**
     * 用户类型
     */
    @Schema(description = "用户类型")
    @NotNull(message = "用户类型不能为空")
    private Integer userType;
}
