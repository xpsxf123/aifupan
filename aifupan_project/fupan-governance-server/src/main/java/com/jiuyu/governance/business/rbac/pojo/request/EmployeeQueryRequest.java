package com.jiuyu.governance.business.rbac.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.framework.shandard.PageRequest;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.common.pojo.request.DataPermissionsPageRequest;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.List;

/**
 * 人员分页搜索
 *
 * @author HeHui
 * @date 2026-03-18 16:23
 */
@Getter
@Setter
public class EmployeeQueryRequest extends PageRequest implements DataPermissionsRequest {
    @Serial
    private static final long serialVersionUID = -5903990319063857416L;

    /**
     * 员工名称
     */
    private String name;

    /**
     * 员工ID
     */
    private Long id;

    /**
     * 工号
     */
    private String staffNumber;

    /**
     * 手机号码
     */
    private String mobile;


    /**
     * 所属岗位ID
     */
    private List<Long> positionIds;



    /**
     * 开启录制权限
     */
    private Boolean onRec;

    /**
     * 就职类型 1全职 2兼职
     */
    private Integer jobType;

    /**
     * 账户状态 0停用 1正常
     */
    private Integer accountStatus;


    /**
     * 是否开启主账户可见（主账户传true时才有效）
     */
    private Boolean openMain;



    /**
     * 租户id
     */
    @JsonIgnore
    private Long tenantId;

    /**
     * 当前用户id
     */
    @JsonIgnore
    private Long currentUserId;


    /**
     * 所属公司ID
     */
    private List<Long> companyIds;


    /**
     * 所属部门ID
     */
    private List<Long> deptIds;


    /**
     * 所属小组ID
     */
    private List<Long> teamIds;



    /**
     * 所属公司ID
     */
    @JsonIgnore
    private List<Long> dataCompanyIds;


    /**
     * 所属部门ID
     */
    @JsonIgnore
    private List<Long> dataDeptIds;


    /**
     * 所属小组ID
     */
    @JsonIgnore
    private List<Long> dataTeamIds;


    /**
     * 是否为系统用户
     */
    @JsonIgnore
    private Boolean systemUser;

    /**
     * 数据权限类型维度
     *
     * @return {@link List }<{@link String }>
     */
    @Override
    public List<String> dataTypes() {
        return List.of(OauthConstant.DEPT, OauthConstant.TEAM, OauthConstant.COMPANY);
    }

    /**
     * 设置租户id
     *
     * @param tenantId 租户id
     */
    @Override
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 设置当前用户id
     *
     * @param currentUserId 当前用户id
     */
    @Override
    public void initCurrentUserId(Long currentUserId) {
        this.currentUserId = currentUserId;
    }

    /**
     * 获取数据权限id
     *
     * @param dataType 数据权限类型维度
     *
     * @return {@link List }<{@link Long }>
     */
    @Override
    public List<Long> findDataIds(String dataType) {
        if (EmptyUtil.isEmpty(dataType)) {
            return List.of();
        }
        return switch (dataType) {
            case OauthConstant.DEPT -> this.deptIds;
            case OauthConstant.TEAM -> this.teamIds;
            case OauthConstant.COMPANY -> this.companyIds;
            default -> List.of();
        };
    }

    /**
     * 设置数据id
     *
     * @param dataType 数据权限类型维度
     * @param dataIds  数据id
     */
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



    /**
     * 是否为空
     *
     * @return boolean
     */
    public boolean empty() {
        if (Boolean.TRUE.equals(this.systemUser)) {
            return false;
        }
        return EmptyUtil.isEmpty(this.dataCompanyIds) && EmptyUtil.isEmpty(this.dataDeptIds) && EmptyUtil.isEmpty(this.dataTeamIds);
    }


    /**
     * 是否包含数据id
     *
     * @param dataType 数据权限类型维度
     * @param dataId   数据id
     *
     * @return boolean
     */
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

    /**
     * 设置系统用户
     *
     * @param systemUser 系统用户
     */
    @Override
    public void ifSystemUser(boolean systemUser) {
        this.systemUser = systemUser;
    }

}
