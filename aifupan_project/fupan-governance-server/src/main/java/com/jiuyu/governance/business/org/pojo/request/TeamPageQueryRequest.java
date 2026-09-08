package com.jiuyu.governance.business.org.pojo.request;

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
 * 小组分页查询请求
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Getter
@Setter
public class TeamPageQueryRequest extends PageRequest implements DataPermissionsRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 所属公司ID
     */
    private Long companyId;

    /**
     * 小组名称关键字
     */
    private String name;

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
            return  null;
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
