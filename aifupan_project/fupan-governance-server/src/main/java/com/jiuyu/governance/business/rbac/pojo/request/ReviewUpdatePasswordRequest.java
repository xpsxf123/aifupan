package com.jiuyu.governance.business.rbac.pojo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 爱复盘更改手机号码请求
 *
 * @author HeHui
 * @date 2026-03-27 20:05
 */
@Getter
@Setter
public class ReviewUpdatePasswordRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;
}
