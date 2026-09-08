package com.jiuyu.governance.business.performance.pojo.bo;

import lombok.Data;

/**
 * 员工业绩统计BO
 *
 * @author lj
 * @date 2026-03-30
 */
@Data
public class EmployeePerformanceBO {

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 员工头像URL
     */
    private String userAvatar;

    /**
     * 岗位ID
     */
    private Long positionId;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 小组ID
     */
    private Long teamId;

    /**
     * 直播间ID列表（逗号分隔）
     */
    private String liveRoomIds;
}
