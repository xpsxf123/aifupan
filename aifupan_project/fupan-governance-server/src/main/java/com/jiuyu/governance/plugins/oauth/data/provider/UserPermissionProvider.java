package com.jiuyu.governance.plugins.oauth.data.provider;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.governance.business.org.service.impl.ManagerConnectorProcessor;

import java.util.List;
import java.util.Map;

/**
 * 用户权限提供者策略接口
 * 用于根据不同的用户类型或维度提供对应的数据权限ID集合
 *
 * @author HeHui
 * @date 2026-03-18
 */
public interface UserPermissionProvider {

    /**
     * 是否支持处理当前用户
     *
     * @param accessUser 当前访问用户
     * @return true if supported
     */
    boolean supports(AccessUser accessUser);

    /**
     * 获取用户在该维度下的数据ID列表
     *
     * @param accessUser         当前访问用户
     * @param dimension          权限维度
     * @param connectorProcessor 连接处理器，用于拉取上游数据
     *
     * @return {@link Map }<{@link String }, {@link List }<{@link Long }>>
     */
    Map<String, List<Long>> getPermissionIds(AccessUser accessUser, List<String> dimension, ManagerConnectorProcessor connectorProcessor);

}
