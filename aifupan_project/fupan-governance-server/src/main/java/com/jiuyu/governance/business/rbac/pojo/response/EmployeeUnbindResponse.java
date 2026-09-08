package com.jiuyu.governance.business.rbac.pojo.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *  人员解绑通知
 * @author HeHui
 * @date 2026-05-07 14:36
 */
@Getter
@Setter
public class EmployeeUnbindResponse {

    /**
     * 被解绑的人员ID列表
     */
    private List<Long> employeeIds;

    /**
     * 被解绑的租户ID列表(如果解绑的是主账户的话)
     */
    private List<Long> tenantIds;


    public static EmployeeUnbindResponse of(List<Long> employeeIds, List<Long> tenantIds) {
        EmployeeUnbindResponse response = new EmployeeUnbindResponse();
        response.setEmployeeIds(employeeIds);
        return response;
    }
}
