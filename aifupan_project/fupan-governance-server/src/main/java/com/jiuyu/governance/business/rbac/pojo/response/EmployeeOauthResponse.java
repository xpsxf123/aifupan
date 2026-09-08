package com.jiuyu.governance.business.rbac.pojo.response;

import com.jiuyu.governance.business.rbac.pojo.constants.JobType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 人员登录响应
 *
 * @author HeHui
 * @date 2026-03-24 17:08
 */
@Getter
@Setter
public class EmployeeOauthResponse {

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
     * 访问令牌
     */
    private String accessToken;


    /**
     * 租户ID
     */
    private Long tenantId;

}
