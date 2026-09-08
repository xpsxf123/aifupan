package com.jiuyu.governance.business.rbac.pojo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * 更改密码请求
 *
 * @author HeHui
 * @date 2026-03-28 17:10
 */
@Getter
@Setter
public class EmployeeUpdatePasswordRequest {

    /**
     * 验证码
     */
    @NotBlank(message = "请输入验证码")
    @Length(min = 6, max = 6, message = "验证码错误")
    private String code;

    /**
     * 新密码
     */
    @NotBlank(message = "请输入新密码")
    @Length(min = 6, max = 20, message = "密码长度为6-20位")
    private String newPassword;
}
