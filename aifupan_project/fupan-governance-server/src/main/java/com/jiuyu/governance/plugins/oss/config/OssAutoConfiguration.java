package com.jiuyu.governance.plugins.oss.config;

import com.jiuyu.governance.plugins.oss.core.OssClientManager;
import com.jiuyu.governance.plugins.oss.core.OssTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OSS 自动配置
 * <p>
 * 当配置了 jiuyu.oss.access-key-id 时自动启用
 * </p>
 *
 * @author lj
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(OssProperties.class)
@ConditionalOnProperty(prefix = "jiuyu.oss", name = "access-key-id")
public class OssAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OssClientManager ossClientManager(OssProperties ossProperties) {
        log.info("[OSS] 初始化 OssClientManager, 桶数量={}", ossProperties.getBuckets().size());
        return new OssClientManager(ossProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OssTemplate ossTemplate(OssClientManager ossClientManager) {
        log.info("[OSS] 初始化 OssTemplate");
        return new OssTemplate(ossClientManager);
    }
}
