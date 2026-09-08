package com.jiuyu.replay.third.config;

import com.jiuyu.replay.third.constant.LianLuProperties;
import com.jiuyu.replay.third.shlianlu.LianLuSmsHandler;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/19 下午7:14
 */
@Configuration
public class LianLuConfig {

    @Resource
    private LianLuProperties lianLuProperties;

    @Bean
    public LianLuSmsHandler lianLuSmsHandler(ObjectProvider<RestClient.Builder> restClientBuilder) {
        return new LianLuSmsHandler(
                lianLuProperties.getMsg(),
                restClientBuilder
        );
    }


}
