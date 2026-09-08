package com.jiuyu.governance.business.room.pojo.request.schedule;

import com.jiuyu.framework.shandard.PageRequest;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 个人排班查询
 *
 * @author HeHui
 * @date 2026-04-10 20:14
 */
@Getter
@Setter
public class EmployeeSchedulePageQueryRequest extends PageRequest {


    @Serial
    private static final long serialVersionUID = -788509716360270354L;


    /**
     * 员工名称
     */
    private String employeeName;


    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 岗位ID
     */
    private Long positionId;


    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 小组ID
     */
    private Long teamId;

    /**
     * 公司ID
     */
    private Long companyId;
}
