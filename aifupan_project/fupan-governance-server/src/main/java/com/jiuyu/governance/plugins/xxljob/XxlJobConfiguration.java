package com.jiuyu.governance.plugins.xxljob;

import com.jiuyu.framework.util.EmptyUtil;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * xxl-job 配置类
 *
 * @author HeHui
 * @date 2025-10-28 10:03
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(XxlJobProperties.class)
public class XxlJobConfiguration {

    /**
     * 初始化XXL-JOB执行器
     *
     * @param xxlJobProperties XXL-JOB配置属性
     *
     * @return XxlJobSpringExecutor XXL-JOB执行器实例
     */
    @ConditionalOnProperty(prefix = "xxl.job", name = "enable", havingValue = "true")
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor(XxlJobProperties xxlJobProperties) {
        log.info("[Xxl-Job] config init. >>>>>>>>>>> adminAddresses={}, appName={}, address={}, ip={}, port={}",
            xxlJobProperties.getAdmin().getAddresses(),
            xxlJobProperties.getExecutor().getAppName(),
            xxlJobProperties.getExecutor().getAddress(),
            xxlJobProperties.getExecutor().getIp(),
            xxlJobProperties.getExecutor().getPort());
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();

        // 设置调度中心地址
        xxlJobSpringExecutor.setAdminAddresses(xxlJobProperties.getAdmin().getAddresses());

        // 设置访问令牌
        xxlJobSpringExecutor.setAccessToken(xxlJobProperties.getAdmin().getAccessToken());

        // 设置应用名称
        xxlJobSpringExecutor.setAppname(xxlJobProperties.getExecutor().getAppName());

        // 设置执行器地址
        if (EmptyUtil.isNotEmpty(xxlJobProperties.getExecutor().getAddress())) {
            xxlJobSpringExecutor.setAddress(xxlJobProperties.getExecutor().getAddress());
        }

        // 设置执行器IP
        if (EmptyUtil.isNotEmpty(xxlJobProperties.getExecutor().getIp())) {
            xxlJobSpringExecutor.setIp(xxlJobProperties.getExecutor().getIp());
        }

        // 设置执行器端口
        if (xxlJobProperties.getExecutor().getPort() != null) {
            xxlJobSpringExecutor.setPort(xxlJobProperties.getExecutor().getPort());
        }

        // 设置日志路径
        if (StringUtils.hasText(xxlJobProperties.getExecutor().getLogPath())) {
            xxlJobSpringExecutor.setLogPath(xxlJobProperties.getExecutor().getLogPath());
        }

        // 设置日志保留天数
        if (xxlJobProperties.getExecutor().getLogRetentionDays() != null) {
            xxlJobSpringExecutor.setLogRetentionDays(xxlJobProperties.getExecutor().getLogRetentionDays());
        }

        return xxlJobSpringExecutor;
    }
}
