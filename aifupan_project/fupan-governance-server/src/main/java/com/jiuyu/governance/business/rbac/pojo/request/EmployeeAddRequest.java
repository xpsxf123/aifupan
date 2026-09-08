package com.jiuyu.governance.business.rbac.pojo.request;

import com.jiuyu.governance.business.rbac.pojo.constants.JobType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 添加人员参数
 *
 * @author HeHui
 * @date 2026-03-18 16:01
 */
@Getter
@Setter
public class EmployeeAddRequest {

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
    private String userAvatar;

    /**
     * 工号
     */
    @Size(max = 10, message = "工号最大长度要小于 10")
    @NotBlank(message = "请输入工号")
    private String staffNumber;

    /**
     * 手机号码
     */
    @Size(max = 20, message = "手机号码最大长度要小于 20")
    @NotBlank(message = "请输入手机号码")
    private String mobile;

    /**
     * 邮箱
     */
    @Size(max = 100, message = "邮箱最大长度要小于 100")
    @Email(message = "请输入正确的邮箱格式")
    private String email;

    /**
     * 所属公司ID
     */
    @NotNull(message = "请选择所属公司")
    @Min(value = 1, message = "请选择所属公司")
    private Long companyId;

    /**
     * 所属部门ID
     */
    private Long deptId = 0L;

    /**
     * 所属小组ID
     */
    private Long teamId = 0L;

    /**
     * 所属岗位ID
     */
    private Long positionId = 0L;

    /**
     * 开启录制权限
     */
    @NotNull(message = "请选择是否开启录制权限")
    private Boolean onRec;


    /**
     * 角色ID
     */
    @NotNull(message = "请选择角色")
    private Long roleId;


    /**
     * 就职类型 1全职，2兼职
     */
    private JobType jobType;
}
