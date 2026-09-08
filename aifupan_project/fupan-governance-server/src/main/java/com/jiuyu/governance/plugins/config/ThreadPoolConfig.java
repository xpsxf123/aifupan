package com.jiuyu.governance.plugins.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 *
 * @author lj
 * @date 2026-04-14
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 商品 OSS 上传专用线程池
     */
    @Bean("productUploadExecutor")
    public ThreadPoolTaskExecutor productUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("product-upload-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
