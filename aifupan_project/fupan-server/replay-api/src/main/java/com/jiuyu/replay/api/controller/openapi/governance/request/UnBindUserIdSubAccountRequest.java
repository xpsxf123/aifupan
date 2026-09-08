package com.jiuyu.replay.api.controller.openapi.governance.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 基于客户端用户ID解绑子账户
 *
 * @author HeHui
 * @date 2026-03-23 19:40
 */
@Getter
@Setter
public class UnBindUserIdSubAccountRequest {


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
