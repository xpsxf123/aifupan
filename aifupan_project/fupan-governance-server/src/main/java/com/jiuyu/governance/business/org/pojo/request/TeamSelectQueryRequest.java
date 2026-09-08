package com.jiuyu.governance.business.org.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 小组下拉选择
 *
 * @author HeHui
 * @date 2026-03-19 20:10
 */
@Getter
@Setter
public class TeamSelectQueryRequest implements DataPermissionsRequest {

    /**
     * 查询调试
     */
    private Integer limit = 100;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 部门名称关键字
     */
    private String keyword;


    /**
     * 排除的部门id列表
     */
    private List<Long> excludeIds;


    /**
     * 部门id列表
     */
    @JsonIgnore
    private List<Long> deptIds;


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
     * 数据权限类型维度
     *
     * @return {@link List }<{@link String }>
     */
    @Override
    public List<String> dataTypes() {
        return List.of(OauthConstant.DEPT);
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
        if (dataType.equals(OauthConstant.DEPT)) {
            return deptIds;
        } else {
            return null;
        }
    }

    /**
     * 设置数据id
     *
     * @param dataType 数据权限类型维度
     * @param dataIds  数据id
     */
    @Override
    public void toDataIds(String dataType, List<Long> dataIds) {
        if (dataType.equals(OauthConstant.DEPT)) {
            deptIds = dataIds;
        }
    }
}
