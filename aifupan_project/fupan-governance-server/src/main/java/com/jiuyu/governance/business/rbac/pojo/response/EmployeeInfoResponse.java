package com.jiuyu.governance.business.rbac.pojo.response;

import com.jiuyu.governance.business.rbac.pojo.constants.AccountStatus;
import com.jiuyu.governance.business.rbac.pojo.constants.JobType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 人员信息响应
 *
 * @author HeHui
 * @date 2026-03-18 16:12
 */
@Getter
@Setter
public class EmployeeInfoResponse {

    /**
     * ID
     */
    private Long id;

    /**
     * 员工名称
     */
    private String name;

    /**
     * 头像
     */
    private String userAvatar;

    /**
     * 工号
     */
    private String staffNumber;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 所属公司ID
     */
    private Long companyId;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 所属小组ID
     */
    private Long teamId;


    /**
     * 小组名称
     */
    private String teamName;

    /**
     * 所属岗位ID
     */
    private Long positionId;

    /**
     * 岗位名称
     */
    private String positionName;

    /**
     * 角色ID
     */
    @NotNull(message = "请选择角色")
    private Long roleId;

    /**
     * 角色名
     */
    private String roleName;

    /**
     * 开启录制权限
     */
    private Boolean onRec;

    /**
     * 就职类型 1全职 2兼职
     */
    private JobType jobType;

    /**
     * 账户状态 0停用 1正常
     */
    private AccountStatus accountStatus;


    /**
     * 是否租户管理员
     */
    private Boolean holdTenant;
}
