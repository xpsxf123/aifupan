package com.jiuyu.governance.business.rbac.pojo.request;

import cn.hutool.core.util.NumberUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 员工搜索选择
 *
 * @author HeHui
 * @date 2026-03-23 13:51
 */
@Getter
@Setter
public class EmployeeOptionSearchRequest implements DataPermissionsRequest  {

    /**
     * 姓名 + 手机号码 纯数字时搜索手机号码
     */
    private String keyword;


    /**
     * 返回条数
     */
    private Integer limit = 10;


    /**
     * 手机号码
     */
    @JsonIgnore
    private String mobile;


    /**
     * 所属岗位ID
     */
    private List<Long> positionIds;


    /**
     * 岗位编码
     * <pre>
     * {@code
     *     ANCHOR("anchor", "主播"),
     *     SUB_ANCHOR("sub_anchor", "副播"),
     *     OPERATION("operation", "运营"),
     *     CONTROL("control", "中控"),
     *     TOU_CHER("tou_cher", "投手"),
     *     EDITOR("editor", "剪辑"),
     *     GUEST("guest", "嘉宾"),
     *     COMPLIANCE_SPECIALIST("compliance_specialist", "合规专员"),
     *     COMPLIANCE_MANAGER("compliance_manager", "合规经理"),
     *     COMPLIANCE_LEADER("compliance_leader", "合规负责人"),
     *
     * }
     * </pre>
     */
    private List<String> positionCodeList;



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

    @JsonIgnore
    private List<Long> dataCompanyIds;

    @JsonIgnore
    private List<Long> dataDeptIds;

    @JsonIgnore
    private List<Long> dataTeamIds;

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

    @Override
    public void ifSystemUser(boolean systemUser) {
        this.systemUser = systemUser;
    }


    public String getMobile() {
        if (EmptyUtil.isNotEmpty(keyword) && keyword.length() >= 3 && NumberUtil.isLong(keyword)) {
            this.mobile = keyword;
        }
        return mobile;
    }
}
