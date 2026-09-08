package com.jiuyu.governance.plugins.oauth.data;


import java.util.List;

/**
 * 数据权限抽象请求接口
 *
 * @author HeHui
 * @date 2026-03-18 16:30
 */
public interface DataPermissionsRequest {

    /**
     * 数据权限类型维度
     *
     * @return {@link List }<{@link String }>
     */
    List<String> dataTypes();


    /**
     * 设置租户id
     *
     * @param tenantId 租户id
     */
    void setTenantId(Long tenantId);


    /**
     * 设置当前用户id
     *
     * @param currentUserId 当前用户id
     */
    void initCurrentUserId(Long currentUserId);


    /**
     * 设置系统用户
     *
     * @param systemUser 系统用户
     */
    default void ifSystemUser(boolean systemUser) {
    }


    /**
     * 获取数据权限id
     *
     * @param dataType 数据权限类型维度
     *
     * @return {@link List }<{@link Long }>
     */
    List<Long> findDataIds(String dataType);


    /**
     * 设置数据id
     *
     * @param dataType 数据权限类型维度
     * @param dataIds  数据id
     */
    void toDataIds(String dataType, List<Long> dataIds);
}
