package com.jiuyu.replay.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 签名验证配置
 *
 * @author RayChou
 * @date 2025/6/9 14:28
 */
@Configuration
@ConfigurationProperties(prefix = "signature")
public class SignatureConfig {

    /**
     * 是否启用签名验证
     */
    private boolean enabled = true;

    /**
     * 签名过期时间，单位毫秒，默认15分钟
     */
    private long expireTime = 15 * 60 * 1000;

    /**
     * 需要排除的路径
     */
    private List<String> excludePaths = new ArrayList<>();

    /**
     * 需要引入的路径
     */
    private List<String> includePaths = new ArrayList<>();

    /**
     * APP_ID和对应的秘钥映射
     */
    private List<AppConfig> apps = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public List<String> getIncludePaths() {
        return includePaths;
    }

    public void setIncludePaths(List<String> includePaths) {
        this.includePaths = includePaths;
    }

    public List<AppConfig> getApps() {
        return apps;
    }

    public void setApps(List<AppConfig> apps) {
        this.apps = apps;
    }

    /**
     * 应用配置
     */
    public static class AppConfig {
        private String appId;
        private String appSecret;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }
    }
} 