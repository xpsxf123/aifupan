package com.jiuyu.governance.plugins.oauth.data;


import java.util.List;
import java.util.Map;

/**
 * 业务数据权限验证
 *
 * @author HeHui
 * @date 2025-03-27 15:11
 */
public interface BusinessPermissions {


    /**
     * 支持的业务
     *
     * @return boolean
     */
    String support();

    /**
     * 验证
     *
     * @param tenantId          机构ID
     * @param userId            用户id
     * @param userPermissionMap 用户数据权限
     * @param businessType      业务类型
     * @param businessId        业务ID
     *
     * @return boolean
     */
    boolean verify(Long tenantId, Long userId, Map<String, List<Long>> userPermissionMap, String businessType, Long businessId);
}
