package com.jiuyu.governance.business.rbac.pojo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * 更改员工密码
 *
 * @author HeHui
 * @date 2026-03-28 17:10
 */
@Getter
@Setter
public class EditEmployeePasswordRequest {

    /**
     * 人员ID
     */
    @NotNull(message = "缺少人员")
    private Long employeeId;

    /**
     * 新密码
     */
    @NotBlank(message = "请输入新密码")
    @Length(min = 6, max = 20, message = "密码长度为6-20位")
    private String newPassword;
}
