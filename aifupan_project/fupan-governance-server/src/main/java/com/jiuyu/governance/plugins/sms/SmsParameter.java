package com.jiuyu.governance.plugins.sms;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 短信服务配置参数
 *
 * @author HeHui
 * @date 2026-04-01 09:58
 */
@Getter
@Setter
@ConfigurationProperties(prefix = SmsParameter.PREFIX)
public class SmsParameter {

    public static final String PREFIX = "jiuyu.sms";


    /**
     * 开始mock验证码
     */
    private Boolean mock = false;

    /**
     * 固定验证码
     */
    private String fixedCode;


    /**
     * 验证码有效期
     */
    private Duration codeTimeout = Duration.ofMinutes(10); // 10分钟


    /**
     * 最大错误次数
     */
    private int errorMax = 4;

    /**
     * 缓存前缀
     */
    private String cachePrefix = "governance:sms-code";


    /**
     * 模版
     */
    private Templates templates = new Templates();


    /**
     * 短信模版
     */
    @Getter
    @Setter
    public static class Templates {

        /**
         * 验证码短信模版
         */
        private String verification = "verification_code";
    }
}
