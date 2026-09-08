package com.jiuyu.governance.business.rbac.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jiuyu.governance.business.rbac.pojo.constants.AccountStatus;
import com.jiuyu.governance.business.rbac.pojo.constants.JobType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 人员
 */
@Getter
@Setter
@TableName(value = "employee")
public class Employee {
    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @NotNull(message = "ID不能为null")
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "create_date")
    private LocalDateTime createDate;

    /**
     * 最后修改时间
     */
    @TableField(value = "update_date")
    private LocalDateTime updateDate;

    /**
     * 创建人
     */
    @TableField(value = "create_by")
    private Long createBy;

    /**
     * 修改人
     */
    @TableField(value = "update_by")
    private Long updateBy;

    /**
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    private Boolean isDeleted;

    /**
     * 租户ID
     */
    @TableField(value = "tenant_id")
    private Long tenantId;

    /**
     * 是否租户管理员
     */
    @TableField(value = "hold_tenant")
    private Boolean holdTenant;

    /**
     * 员工名称
     */
    @TableField(value = "`name`")
    @Size(max = 20, message = "员工名称最大长度要小于 20")
    @NotBlank(message = "员工名称不能为空")
    private String name;

    /**
     * 头像
     */
    @TableField(value = "user_avatar")
    @Size(max = 300, message = "头像最大长度要小于 300")
    private String userAvatar;

    /**
     * 工号
     */
    @TableField(value = "staff_number")
    @Size(max = 10, message = "工号最大长度要小于 10")
    @NotBlank(message = "工号不能为空")
    private String staffNumber;

    /**
     * 手机号码
     */
    @TableField(value = "mobile")
    @Size(max = 20, message = "手机号码最大长度要小于 20")
    @NotBlank(message = "手机号码不能为空")
    private String mobile;

    /**
     * 邮箱
     */
    @TableField(value = "email")
    @Size(max = 100, message = "邮箱最大长度要小于 100")
    @Email(message = "请输入正确的邮箱格式")
    private String email;

    /**
     * 所属公司ID
     */
    @TableField(value = "company_id")
    @NotNull(message = "请选择所属公司ID")
    private Long companyId;

    /**
     * 所属部门ID
     */
    @TableField(value = "dept_id")
    private Long deptId;

    /**
     * 所属小组ID
     */
    @TableField(value = "team_id")
    private Long teamId;

    /**
     * 所属岗位ID
     */
    @TableField(value = "position_id")
    private Long positionId;

    /**
     * 开启录制权限
     */
    @TableField(value = "on_rec")
    @NotNull(message = "请选择是否开启录制权限")
    private Boolean onRec;

    /**
     * 关联客户端账户ID
     */
    @TableField(value = "connector_client_user_id")
    private Long connectorClientUserId;

    /**
     * 岗位类型
     */
    @TableField(value = "job_type")
    private JobType jobType;


    /**
     * 账户状态
     */
    @TableField(value = "account_status")
    private AccountStatus accountStatus;


    /**
     * 密码
     */
    @TableField(value = "password")
    private String password;
}
