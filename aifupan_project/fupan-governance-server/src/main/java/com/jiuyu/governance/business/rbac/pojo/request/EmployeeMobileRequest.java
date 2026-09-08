package com.jiuyu.governance.business.rbac.pojo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * 员工手机号码请求
 *
 * @author HeHui
 * @date 2026-03-27 16:12
 */
@Getter
@Setter
public class EmployeeMobileRequest {

    /**
     * 手机号码
     */
    @NotBlank(message = "请输入手机号码")
    @Length(min = 11, max = 11, message = "请输入正确的手机号码")
    private String mobile;
}
