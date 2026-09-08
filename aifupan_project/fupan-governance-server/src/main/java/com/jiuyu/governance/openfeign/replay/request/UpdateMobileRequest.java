package com.jiuyu.governance.openfeign.replay.request;

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
public class UpdateMobileRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 手机号码
     */
    @NotNull(message = "手机号码不能为空")
    private String mobile;

    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;
}
