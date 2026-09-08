package com.jiuyu.governance.business.rbac.pojo.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 人员基本信息
 *
 * @author HeHui
 * @date 2026-03-19 10:05
 */
@Getter
@Setter
public class EmployeeBaseInfo {

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
}
