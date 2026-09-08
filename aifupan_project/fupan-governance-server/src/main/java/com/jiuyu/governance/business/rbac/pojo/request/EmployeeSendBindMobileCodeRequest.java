package com.jiuyu.governance.business.rbac.pojo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * 发送员工绑定新手机号码验证码
 *
 * @author HeHui
 * @date 2026-03-28 17:18
 */
@Getter
@Setter
public class EmployeeSendBindMobileCodeRequest {

    /**
     * 新手机号码
     */
    @NotBlank(message = "请输入新手机号码")
    @Length(min = 11, max = 11, message = "手机号码格式错误")
    private String newMobile;
}
