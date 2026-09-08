package com.jiuyu.replay.api.controller.openapi.governance.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * 基于手机号码绑定子账户
 *
 * @author HeHui
 * @date 2026-03-23 19:40
 */
@Getter
@Setter
public class BindMobileSubAccountRequest {


    /**
     * 主账户ID
     */
    @NotNull(message = "缺少主账户ID")
    private Long currentUserId;


    /**
     * 手机号码
     */
    @NotBlank(message = "缺少手机号码")
    private String mobile;

    /**
     * 昵称
     */
    @Length(max = 30, message = "昵称长度不能超过30个字符")
    private String nickname;
}
