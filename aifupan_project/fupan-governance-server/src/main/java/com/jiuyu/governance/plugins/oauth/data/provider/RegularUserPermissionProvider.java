package com.jiuyu.governance.plugins.oauth.data.provider;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.org.service.impl.ManagerConnectorProcessor;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 常规业务用户 权限策略
 * 通过 ManagerConnectorProcessor 拉取用户关联的具体数据权限ID
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Order(20)
@Component
public class RegularUserPermissionProvider implements UserPermissionProvider {

    private final Cache<String, List<Long>> cache;


    private final Boolean enableLocalCache;

    public RegularUserPermissionProvider() {
        this(false);
    }

    /**
     * 构造函数，创建一个常规用户权限提供者实例
     *
     * @param enableLocalCache 是否启用本地缓存
     */
    public RegularUserPermissionProvider(boolean enableLocalCache) {
        if (enableLocalCache) {
            this.cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
        } else {
            this.cache = null;
        }
        this.enableLocalCache = enableLocalCache;
    }


    @Override
    public boolean supports(AccessUser accessUser) {
        return Objects.equals(accessUser.userType(), OauthConstant.GOVERNANCE_USER);
    }

    /**
     * 获取用户在该维度下的数据ID列表
     *
     * @param accessUser         当前访问用户
     * @param dimension          权限维度
     * @param connectorProcessor 连接处理器，用于拉取上游数据
     *
     * @return {@link Map }<{@link String }, {@link List }<{@link Long }>>
     */
    @Override
    public Map<String, List<Long>> getPermissionIds(AccessUser accessUser, List<String> dimension, ManagerConnectorProcessor connectorProcessor) {
        Map<String, List<Long>> map = new HashMap<>();
        List<String> notCacheType = new ArrayList<>();
        dimension.forEach(type -> {
            if (!enableLocalCache) {
                notCacheType.add(type);
                return;
            }
            List<Long> ids = cache.getIfPresent(accessUser.userId() + "-" + type);
            if (ids != null) {
                map.put(type, ids);
            } else {
                notCacheType.add(type);
            }
        });
        if (EmptyUtil.isEmpty(notCacheType)) {
            return Collections.unmodifiableMap(map);
        }
        List<ManagerType> managerTypes = Arrays.stream(ManagerType.values())
            .filter(managerType -> notCacheType.contains(managerType.getCode())).toList();
        Map<String, List<Long>> userConnectorMap = connectorProcessor.getUserConnectorIds(managerTypes, accessUser.currentTenantId(), accessUser.userId()).entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getCode(), Map.Entry::getValue));
        notCacheType.forEach(type -> {
            List<Long> ids = userConnectorMap.get(type);
            if (ids != null) {
                map.put(type, ids);
            }
            if (enableLocalCache) {
                cache.put(accessUser.userId() + "-" + type, ids == null ? List.of() : ids);
            }
        });
        return Collections.unmodifiableMap(map);
    }
}
