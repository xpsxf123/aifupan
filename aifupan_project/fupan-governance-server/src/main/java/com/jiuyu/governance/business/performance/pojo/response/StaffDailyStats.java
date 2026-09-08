package com.jiuyu.governance.business.performance.pojo.response;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 人员每日统计信息
 *
 * @author lj
 * @date 2026-03-28
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDailyStats {

    /**
     * 日期
     */
    private LocalDate statsDate;

    /**
     * 岗位ID
     */
    private Long positionId;

    /**
     * 岗位名称
     */
    private String positionName;

    /**
     * 人员ID
     */
    private Long employeeId;

    /**
     * 人员名称
     */
    private String employeeName;

    /**
     * 该员工当天参与的场次数
     */
    private Integer scheduleCount;

    /**
     * 显示名称
     * @return 显示名称 格式：岗位:名称 xxx场
     */
    public String showName() {
        if (ObjUtil.isNotEmpty(positionName) && ObjUtil.isNotEmpty(employeeName)) {
            return StrUtil.format("{}:{} {}场", positionName, employeeName, ObjUtil.defaultIfNull(scheduleCount, 0));
        }
        return null;
    }
}
