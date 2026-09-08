package com.jiuyu.governance.business.rbac.pojo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * 手机验证码登录
 *
 * @author HeHui
 * @date 2026-03-27 16:13
 */
@Getter
@Setter
public class EmployeeMobileLoginRequest extends EmployeeMobileRequest {


    /**
     * 验证码
     */
    @NotBlank(message = "请输入验证码")
    @Length(min = 6, max = 6, message = "请输入正确的验证码")
    private String code;
}
