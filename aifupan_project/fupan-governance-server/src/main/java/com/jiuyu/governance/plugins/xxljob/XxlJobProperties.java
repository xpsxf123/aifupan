package com.jiuyu.governance.plugins.xxljob;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * xxl-job配置类
 *
 * @author HeHui
 * @date 2025-10-28 10:00
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobProperties {

    /**
     * 是否启用xxl-job
     */
    private Boolean enable = false;

    /**
     * 管理配置
     */
    private Admin admin = new Admin();

    /**
     * 执行器配置
     */
    private Executor executor = new Executor();

    /**
     * 管理配置属性
     */
    @Getter
    @Setter
    public static class Admin {
        /**
         * 访问令牌
         */
        private String accessToken;

        /**
         * 管理端地址
         */
        private String addresses;

        /**
         * 超时时间(秒)，默认3秒
         */
        private Integer timeout = 3;
    }

    /**
     * 执行器配置属性
     */
    @Getter
    @Setter
    public static class Executor {
        /**
         * 执行器地址，默认为空
         */
        private String address = "";

        /**
         * 应用名称
         */
        private String appName;

        /**
         * 执行器IP，默认为空
         */
        private String ip = "";

        /**
         * 日志存储路径
         */
        private String logPath;

        /**
         * 日志保留天数，默认30天
         */
        private Integer logRetentionDays = 30;

        /**
         * 执行器端口，默认9999
         */
        private Integer port = 9999;
    }
}
