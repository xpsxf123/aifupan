package com.jiuyu.governance.business.rbac.pojo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 人员基于手机号码 + 密码登录
 *
 * @author HeHui
 * @date 2026-03-24 17:10
 */
@Getter
@Setter
public class EmployeeOauthPasswordLoginRequest {

    /**
     * 手机号码
     */
    @NotBlank(message = "请输入手机号码")
    private String mobile;


    /**
     * 密码
     */
    @NotBlank(message = "请输入密码")
    private String password;
}
