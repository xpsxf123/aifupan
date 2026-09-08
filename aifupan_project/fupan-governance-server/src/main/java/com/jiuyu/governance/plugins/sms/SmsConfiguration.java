package com.jiuyu.governance.plugins.sms;

import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.common.replay.ReplayHttpServer;
import com.jiuyu.governance.openfeign.replay.SmsService;
import com.jiuyu.governance.openfeign.replay.impl.SmsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 短信服务配置
 *
 * @author HeHui
 * @date 2026-04-01 10:04
 */
@Configuration
@EnableConfigurationProperties(SmsParameter.class)
public class SmsConfiguration {



    /**
     * 创建模拟验证码提供者
     * 仅在配置开启 mock 功能时创建此 Bean，用于测试环境提供模拟验证码
     *
     * @param parameter  短信参数配置，用于获取固定验证码等配置信息
     * @return           模拟验证码提供者实例，当配置了固定验证码时使用该验证码，否则使用随机验证码
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = SmsParameter.PREFIX, name = "mock", havingValue = "true")
    public MockCodeProvide mockCodeProvide(SmsParameter parameter) {
        if (EmptyUtil.isNotEmpty(parameter.getFixedCode())) {
            return new FixedMockCodeProvide(parameter.getFixedCode());
        }
        return new FixedMockCodeProvide();
    }


    /**
     * 创建短信服务
     *
     * @param httpServer       重放 HTTP 服务器，用于处理短信发送的 HTTP 请求
     * @param redisTemplate    Redis 模板，用于缓存短信验证码等相关数据
     * @param parameter        短信参数配置，包含模板、超时时间、缓存前缀等配置信息
     * @param mockCodeProvide  模拟验证码提供者（可选），用于测试环境提供模拟验证码
     * @return                 短信服务实例，用于发送短信验证码
     */
    @Bean
    public SmsService smsService(ReplayHttpServer httpServer, StringRedisTemplate redisTemplate, SmsParameter parameter, @Autowired(required = false) MockCodeProvide mockCodeProvide) {
        return new SmsServiceImpl(httpServer, redisTemplate, parameter.getTemplates(), parameter.getCodeTimeout(), parameter.getCachePrefix(), parameter.getErrorMax(), mockCodeProvide);
    }
}
