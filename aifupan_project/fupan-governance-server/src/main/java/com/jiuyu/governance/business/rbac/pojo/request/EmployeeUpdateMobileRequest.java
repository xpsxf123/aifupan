package com.jiuyu.governance.business.rbac.pojo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * 更改手机号码请求
 *
 * @author HeHui
 * @date 2026-03-28 17:10
 */
@Getter
@Setter
public class EmployeeUpdateMobileRequest {

    /**
     * 验证码
     */
    @NotBlank(message = "请输入验证码")
    @Length(min = 6, max = 6, message = "验证码错误")
    private String code;

    /**
     * 新手机号码
     */
    @NotBlank(message = "请输入新手机号码")
    @Length(min = 11, max = 11, message = "手机号码格式错误")
    private String newMobile;
}
