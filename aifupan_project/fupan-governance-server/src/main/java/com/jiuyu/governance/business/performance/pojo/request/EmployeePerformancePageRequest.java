package com.jiuyu.governance.business.performance.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.framework.shandard.PageRequest;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.List;

/**
 * 人员业绩分页查询请求
 *
 * @author lj
 * @date 2026-03-30
 */
@Getter
@Setter
public class EmployeePerformancePageRequest extends PageRequest implements DataPermissionsRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 公司ID（筛选） */
    private Long companyId;

    /** 部门ID（筛选） */
    private Long deptId;

    /** 小组ID（筛选） */
    private Long teamId;

    /** 岗位ID（筛选） */
    private Long positionId;

    /** 账号状态（筛选） */
    private Integer accountStatus;

    /** 员工姓名（模糊搜索） */
    private String employeeName;

    /** 手机号 */
    private String mobile;

    @JsonIgnore
    private Long tenantId;

    @JsonIgnore
    private Long currentUserId;

    /** 数据权限-公司ID列表 */
    @JsonIgnore
    private List<Long> dataCompanyIds;

    /** 数据权限-部门ID列表 */
    @JsonIgnore
    private List<Long> dataDeptIds;

    /** 数据权限-小组ID列表 */
    @JsonIgnore
    private List<Long> dataTeamIds;

    /** 是否为系统用户 */
    @JsonIgnore
    private Boolean systemUser;

    @Override
    public List<String> dataTypes() {
        return List.of(OauthConstant.DEPT, OauthConstant.TEAM, OauthConstant.COMPANY);
    }

    @Override
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public void initCurrentUserId(Long currentUserId) {
        this.currentUserId = currentUserId;
    }

    @Override
    public List<Long> findDataIds(String dataType) {
        if (EmptyUtil.isEmpty(dataType)) {
            return List.of();
        }
        return switch (dataType) {
            case OauthConstant.DEPT -> this.dataDeptIds;
            case OauthConstant.TEAM -> this.dataTeamIds;
            case OauthConstant.COMPANY -> this.dataCompanyIds;
            default -> List.of();
        };
    }

    @Override
    public void toDataIds(String dataType, List<Long> dataIds) {
        if (EmptyUtil.isEmpty(dataType)) {
            return;
        }
        switch (dataType) {
            case OauthConstant.DEPT -> this.dataDeptIds = dataIds;
            case OauthConstant.TEAM -> this.dataTeamIds = dataIds;
            case OauthConstant.COMPANY -> this.dataCompanyIds = dataIds;
        }
    }

    @Override
    public void ifSystemUser(boolean systemUser) {
        this.systemUser = systemUser;
    }

    /** 数据权限ID列表是否全部为空 */
    public boolean empty() {
        if (Boolean.TRUE.equals(this.systemUser)) {
            return false;
        }
        return EmptyUtil.isEmpty(this.dataCompanyIds) && EmptyUtil.isEmpty(this.dataDeptIds) && EmptyUtil.isEmpty(this.dataTeamIds);
    }

    /** 检查指定的筛选ID是否在数据权限范围内 */
    public boolean hasDataId(String dataType, Long dataId) {
        if (Boolean.TRUE.equals(this.systemUser)) {
            return true;
        }
        if (dataId == null) {
            return true;
        }
        List<Long> dataIds = this.findDataIds(dataType);
        if (EmptyUtil.isEmpty(dataIds)) {
            return true;
        }
        return dataIds.contains(dataId);
    }
}
