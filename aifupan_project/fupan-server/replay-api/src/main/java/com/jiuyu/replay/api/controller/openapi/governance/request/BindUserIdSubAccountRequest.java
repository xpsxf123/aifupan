package com.jiuyu.replay.api.controller.openapi.governance.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * 基于客户端用户ID绑定子账户
 *
 * @author HeHui
 * @date 2026-03-23 19:40
 */
@Getter
@Setter
public class BindUserIdSubAccountRequest {


    /**
     * 主账户ID
     */
    @NotNull(message = "缺少主账户ID")
    private Long currentUserId;


    /**
     * 子账户ID
     */
    @NotNull(message = "缺少子账户ID")
    private Long userId;
}
