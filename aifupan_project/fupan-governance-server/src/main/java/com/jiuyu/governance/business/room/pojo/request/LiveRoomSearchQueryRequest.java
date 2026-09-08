package com.jiuyu.governance.business.room.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.List;

/**
 * 直播间搜索查询请求
 *
 * @author HeHui
 * @date 2026-03-25
 */
@Getter
@Setter
public class LiveRoomSearchQueryRequest implements DataPermissionsRequest {


    /**
     * 返回条数
     */
    private Integer limit = 50;

    /**
     * 直播平台类型 (0: 抖音, 1: 快手, 2: 视频号)
     */
    private Integer platform;

    /**
     * 关键字
     */
    private String keyword;


    /**
     * 直播间账号状态 (0: 停用, 1: 正常)
     */
    private Integer accountStatus;

    /**
     * 按所属公司ID精确过滤
     */
    private Long companyId;

    /**
     * 按所属部门ID精确过滤
     */
    private Long deptId;

    /**
     * 按所属小组ID精确过滤
     */
    private Long teamId;


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
    @JsonIgnore
    private List<Long> companyIds;


    /**
     * 所属部门ID
     */
    @JsonIgnore
    private List<Long> deptIds;


    /**
     * 所属小组ID
     */
    @JsonIgnore
    private List<Long> teamIds;

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
            case OauthConstant.DEPT -> this.deptIds = dataIds;
            case OauthConstant.TEAM -> this.teamIds = dataIds;
            case OauthConstant.COMPANY -> this.companyIds = dataIds;
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
        return EmptyUtil.isEmpty(this.companyIds) && EmptyUtil.isEmpty(this.deptIds) && EmptyUtil.isEmpty(this.teamIds);
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
            return false;
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
