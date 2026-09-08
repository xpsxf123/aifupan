package com.jiuyu.governance.business.rbac.pojo.response;

import com.jiuyu.governance.business.room.pojo.bo.LiveRoomInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 员工资料
 *
 * @author HeHui
 * @date 2026-03-28 15:54
 */
@Getter
@Setter
public class EmployeeProfileResponse extends EmployeeInfoResponse {

    /**
     * 员工直播间信息
     */
    private List<LiveRoomInfo> roomInfos;


    public EmployeeProfileResponse() {
    }

    public EmployeeProfileResponse(EmployeeInfoResponse employeeInfoResponse) {
        if (employeeInfoResponse == null) {
            return;
        }
        this.setId(employeeInfoResponse.getId());
        this.setName(employeeInfoResponse.getName());
        this.setUserAvatar(employeeInfoResponse.getUserAvatar());
        this.setStaffNumber(employeeInfoResponse.getStaffNumber());
        this.setMobile(employeeInfoResponse.getMobile());
        this.setEmail(employeeInfoResponse.getEmail());
        this.setCompanyId(employeeInfoResponse.getCompanyId());
        this.setCompanyName(employeeInfoResponse.getCompanyName());
        this.setDeptId(employeeInfoResponse.getDeptId());
        this.setDeptName(employeeInfoResponse.getDeptName());
        this.setTeamId(employeeInfoResponse.getTeamId());
        this.setTeamName(employeeInfoResponse.getTeamName());
        this.setPositionId(employeeInfoResponse.getPositionId());
        this.setPositionName(employeeInfoResponse.getPositionName());
        this.setRoleId(employeeInfoResponse.getRoleId());
        this.setRoleName(employeeInfoResponse.getRoleName());
        this.setOnRec(employeeInfoResponse.getOnRec());
        this.setJobType(employeeInfoResponse.getJobType());
        this.setAccountStatus(employeeInfoResponse.getAccountStatus());
        this.setHoldTenant(employeeInfoResponse.getHoldTenant());
    }
}
