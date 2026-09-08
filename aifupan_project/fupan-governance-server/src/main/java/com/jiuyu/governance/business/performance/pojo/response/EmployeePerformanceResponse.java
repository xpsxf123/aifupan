package com.jiuyu.governance.business.performance.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 人员业绩分页响应
 *
 * @author lj
 * @date 2026-03-30
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePerformanceResponse {

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
     * 岗位名称
     */
    private String positionName;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 小组ID
     */
    private Long teamId;

    /**
     * 小组名称
     */
    private String teamName;

    /**
     * 关联的直播间列表
     */
    private List<LiveRoomInfo> liveRoomList;

    /**
     * 业绩统计（六时段）
     */
    private PerformancePeriodStatsResponse performanceStatistics;

    /**
     * 直播间信息
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveRoomInfo {
        /**
         * 直播间ID
         */
        private Long id;

        /**
         * 直播间名称
         */
        private String name;
    }
}
