package com.jiuyu.governance.plugins.oauth.data;

import com.jiuyu.framework.lock.SpelParseHandler;
import com.jiuyu.governance.business.org.service.impl.ManagerConnectorProcessor;
import com.jiuyu.governance.plugins.oauth.data.aop.DataPermissionExecuteAspect;
import com.jiuyu.governance.plugins.oauth.data.aop.DataPermissionsAspect;
import com.jiuyu.governance.plugins.oauth.data.aop.DataPermissionsQueryAdvisor;
import com.jiuyu.governance.plugins.oauth.data.aop.DataPermissionsQueryPredicate;
import com.jiuyu.governance.plugins.oauth.data.provider.UserPermissionProvider;
import com.jiuyu.governance.plugins.oauth.data.supports.DataPermissionsHandler;
import com.jiuyu.governance.plugins.oauth.data.supports.DataPermissionsParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据权限自动配置
 *
 * @author HeHui
 * @date 2026-03-19 12:18
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(DataPermissionsParameter.class)
public class DataPermissionsAuthConfiguration {


    /**
     * 配置数据权限业务处理器
     *
     * @param connectorProcessor 各类型管理员连接器
     *
     * @return {@link DataPermissionsHandler }
     */
    @Bean
    @ConditionalOnMissingBean
    public DataPermissionsHandler dataPermissionsHandler(ManagerConnectorProcessor connectorProcessor,
                                                         ObjectProvider<BusinessPermissions> businessPermissionsProvider,
                                                         ObjectProvider<UserPermissionProvider> userPermissionProviders) {
        // 创建DataPermissionsHandler实例，并注入处理数据权限的逻辑
        log.info("[数据权限] load DataPermissionsHandler for default 数据权限查询处理工具");
        return new DataPermissionsHandler(businessPermissionsProvider, userPermissionProviders, connectorProcessor);
    }

    /**
     * 数据权限执行切面
     *
     * @param dataPermissionsHandler 数据权限处理程序
     * @param parseHandler           解析处理程序
     *
     * @return {@link DataPermissionExecuteAspect }
     */
    @Bean
    @ConditionalOnMissingBean
    public DataPermissionExecuteAspect dataPermissionExecuteAspect(DataPermissionsHandler dataPermissionsHandler, SpelParseHandler parseHandler) {
        log.info("[数据权限] load dataPermissionExecuteAspect for {}", BeforePermission.class);
        return new DataPermissionExecuteAspect(dataPermissionsHandler, parseHandler);
    }


    /**
     * 查询数据权限切面
     *
     * @return {@link DataPermissionsAspect }
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = DataPermissionsParameter.PREFIX, name = "enable-query", havingValue = "true", matchIfMissing = true)
    public DataPermissionsAspect dataPermissionsAspect(DataPermissionsHandler dataPermissionsHandler, DataPermissionsParameter parameter) {
        log.info("[数据权限] load dataPermissionExecuteAspect for {}", DataPermissionsRequest.class);
        return new DataPermissionsAspect(new DataPermissionsQueryAdvisor(null, dataPermissionsHandler), new DataPermissionsQueryPredicate(parameter.getIgnoreClassRegular(), parameter.getBasePackages(), parameter.getReturnTypes()));
    }
}
