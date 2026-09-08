package com.jiuyu.governance.plugins.oauth.data.provider;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.org.service.impl.ManagerConnectorProcessor;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.pojo.ErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 系统管理员/租户管理员 权限策略
 * 这类用户拥有所有数据的访问权限
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Order(10)
@Component
public class AdminPermissionProvider implements UserPermissionProvider {


    /**
     * 判断是否支持该用户的权限验证
     * <p>
     * 支持以下两种用户类型：
     * 1. 系统管理员用户：直接返回 true，拥有所有数据访问权限
     * 2. 租户主用户：检查 metadata 中是否包含租户管理员标识（TENANT_ADMIN=true）
     *
     * @param accessUser 当前访问用户，包含用户类型和元数据信息
     *
     * @return true-表示支持该用户的权限验证（系统管理员或租户管理员）；false-表示不支持
     */
    @Override
    public boolean supports(AccessUser accessUser) {
        // 系统管理员用户，拥有所有权限
        if (OauthConstant.SYSTEM_USER.equals(accessUser.userType())) {
            return true;
        }
        // 治理平台用户，需要检查是否为租户管理员
        if (OauthConstant.GOVERNANCE_USER.equals(accessUser.userType())) {
            Map<String, String> metadata = accessUser.metadata();
            if (EmptyUtil.isEmpty(metadata)) {
                return false;
            }
            return Objects.equals(metadata.get(OauthConstant.TENANT_ADMIN), "true");
        }
        return false;
    }

    /**
     * 获取用户在该维度下的数据 ID 列表
     * <p>
     * 根据用户类型返回对应的权限数据 ID：
     * 1. 租户管理员用户：验证身份后，从连接器处理器查询租户下各维度的所有数据 ID
     * 2. 系统管理员用户：直接返回所有维度的全量权限（ID 为 0）
     *
     * @param accessUser         当前访问用户，包含用户类型和元数据信息
     * @param dimension          权限维度列表，如公司、部门、小组、直播间等
     * @param connectorProcessor 连接处理器，用于查询租户下的连接器 ID 数据
     *
     * @return Map<String, List<Long>> 各维度对应的数据 ID 列表；空 Map 表示该用户没有任何数据权限
     */
    @Override
    public Map<String, List<Long>> getPermissionIds(AccessUser accessUser, List<String> dimension, ManagerConnectorProcessor connectorProcessor) {
        // 企业用户，需要验证是否为租户管理员，租户管理员不设本地缓存，增加即时性
        if (OauthConstant.GOVERNANCE_USER.equals(accessUser.userType())) {
            Map<String, String> metadata = accessUser.metadata();
            if (EmptyUtil.isEmpty(metadata)) {
                throw new BusinessException(SystemErrorCode.CONFLICT, "metadata is empty");
            }
            boolean tenantAdmin = Objects.equals(metadata.get(OauthConstant.TENANT_ADMIN), "true");
            if (!tenantAdmin) {
                throw new BusinessException(SystemErrorCode.CONFLICT, "user is not tenant admin");
            }
            // 过滤出请求维度对应的管理类型，查询租户下的所有连接器 ID
            List<ManagerType> managerTypes = Arrays.stream(ManagerType.values())
                .filter(managerType -> dimension.contains(managerType.getCode())).toList();
            Map<ManagerType, List<Long>> managerIdMap = connectorProcessor.queryTenantAllConnectorIds(accessUser.currentTenantId(), managerTypes);
            if (EmptyUtil.isEmpty(managerIdMap)) {
                return Map.of();
            }
            return managerIdMap.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getCode(), Map.Entry::getValue));
        }

        // 系统管理员用户，返回所有维度的全量权限（ID=0 代表全部）
        List<Long> all = List.of(0L);
        return dimension.stream().collect(java.util.stream.Collectors.toMap(d -> d, d -> all));
    }
}
