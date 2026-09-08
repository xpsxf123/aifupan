package com.jiuyu.replay.third.config;

import com.jiuyu.replay.third.constant.WeChatPayProperties;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;

//@Configuration
public class WeChatPayRSAConfig {

    @Resource
    private WeChatPayProperties weChatPayProperties;

    @Bean
    public Config weChatPayConfig() {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(weChatPayProperties.getMerchantId())
                .privateKeyFromPath(weChatPayProperties.getPrivateKeyPath())
                .merchantSerialNumber(weChatPayProperties.getMerchantSerialNumber())
                .apiV3Key(weChatPayProperties.getApiKey())
                .build();
    }
}
