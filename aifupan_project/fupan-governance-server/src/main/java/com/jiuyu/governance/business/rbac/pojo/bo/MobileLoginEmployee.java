package com.jiuyu.governance.business.rbac.pojo.bo;

import com.jiuyu.governance.business.rbac.pojo.constants.AccountStatus;
import com.jiuyu.governance.business.rbac.pojo.entity.Employee;
import lombok.Getter;
import lombok.Setter;

/**
 *  手机号码对应的人员
 * @author HeHui
 * @date 2026-05-07 15:15
 */
@Getter
@Setter
public class MobileLoginEmployee {

    /**
     * 人员
     */
    private Employee employee;


    /**
     * 租户状态
     */
    private AccountStatus tenantStatus;


    public static MobileLoginEmployee of(Employee employee, AccountStatus tenantStatus) {
        MobileLoginEmployee mobileLoginEmployee = new MobileLoginEmployee();
        mobileLoginEmployee.setEmployee(employee);
        mobileLoginEmployee.setTenantStatus(tenantStatus);
        return mobileLoginEmployee;
    }
}
