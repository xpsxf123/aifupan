package com.jiuyu.governance.business.org.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.framework.shandard.PageRequest;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.List;

/**
 * 子公司分页查询请求
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Getter
@Setter
public class SubCompanyPageQueryRequest extends PageRequest implements DataPermissionsRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 公司名称关键字
     */
    private String name;


    /**
     * 公司id列表
     */
    @JsonIgnore
    private List<Long> companyIds;



    /**
     * 租户id
     */
    @JsonIgnore
    private Long tenantId;

    /**
     * 数据权限类型维度
     *
     * @return {@link List }<{@link String }>
     */
    @Override
    public List<String> dataTypes() {
        return List.of(OauthConstant.COMPANY);
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
        if (dataType.equals(OauthConstant.COMPANY)) {
            return companyIds;
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
        if (dataType.equals(OauthConstant.COMPANY)) {
            companyIds = dataIds;
        }
    }
}
