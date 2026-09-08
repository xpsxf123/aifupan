package com.jiuyu.governance.plugins.oss.config;

import lombok.Data;

/**
 * OSS 桶配置属性
 *
 * @author lj
 */
@Data
public class OssBucketProperties {

    /**
     * 实际桶名称
     */
    private String bucketName;

    /**
     * 外网 Endpoint（如 https://oss-cn-hangzhou.aliyuncs.com）
     */
    private String endpoint;

    /**
     * 内网 Endpoint（如 https://oss-cn-hangzhou-internal.aliyuncs.com）
     */
    private String internalEndpoint;

    /**
     * 区域（如 cn-hangzhou）
     */
    private String region;

    /**
     * Access Key ID（可选，不填则使用全局配置）
     */
    private String accessKeyId;

    /**
     * Access Key Secret（可选，不填则使用全局配置）
     */
    private String accessKeySecret;

    /**
     * 公开访问 URL（如 https://replay-ai-data.oss-cn-hangzhou.aliyuncs.com）
     */
    private String accessUrl;

    /**
     * 自定义域名/CDN域名（可选，优先级高于 accessUrl）
     */
    private String domain;

    /**
     * 桶级别路径前缀（可选，覆盖全局配置）
     */
    private String pathPrefix;

    /**
     * 桶级别是否使用内网（可选，覆盖全局配置）
     */
    private Boolean useInternal;

    /**
     * 获取公开访问基础URL
     * 优先使用 domain，其次使用 accessUrl
     */
    public String getPublicBaseUrl() {
        if (domain != null && !domain.isEmpty()) {
            return domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
        }
        if (accessUrl != null && !accessUrl.isEmpty()) {
            return accessUrl.endsWith("/") ? accessUrl.substring(0, accessUrl.length() - 1) : accessUrl;
        }
        return null;
    }
}
