package com.jiuyu.replay.api.controller.openapi.governance.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 更改手机号码请求
 *
 * @author HeHui
 * @date 2026-03-27 20:05
 */
@Getter
@Setter
public class UpdatePasswordRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String rawPassword;

    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;
}
