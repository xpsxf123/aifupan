package com.jiuyu.governance.business.rbac.pojo.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 人员个人资料变更
 *
 * @author HeHui
 * @date 2026-03-18 16:03
 */
@Getter
@Setter
public class EmployeeProfileUpdateRequest {


    /**
     * 员工名称
     */
    @Size(max = 20, message = "员工名称最大长度要小于 20")
    @NotBlank(message = "请输入员工姓名")
    private String name;

    /**
     * 头像
     */
    @Size(max = 300, message = "头像最大长度要小于 300")
    private String userAvatar = "";


    /**
     * 邮箱
     */
    @Size(max = 100, message = "邮箱最大长度要小于 100")
    @Email(message = "请输入正确的邮箱格式")
    private String email = "";
}
