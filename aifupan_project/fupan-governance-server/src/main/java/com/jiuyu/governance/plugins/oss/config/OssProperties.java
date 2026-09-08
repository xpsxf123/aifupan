package com.jiuyu.governance.plugins.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * OSS 配置属性
 * <p>
 * 配置示例：
 * <pre>
 * jiuyu:
 *   oss:
 *     access-key-id: xxx
 *     access-key-secret: xxx
 *     default-bucket: ai-data
 *     path-prefix: ${spring.profiles.active}
 *     network:
 *       server-operation: internal
 *       presigned-url: external
 *     buckets:
 *       ai-data:
 *         bucket-name: replay-ai-data
 *         endpoint: https://oss-cn-hangzhou.aliyuncs.com
 *         internal-endpoint: https://oss-cn-hangzhou-internal.aliyuncs.com
 *         region: cn-hangzhou
 *         access-url: https://replay-ai-data.oss-cn-hangzhou.aliyuncs.com
 * </pre>
 * </p>
 *
 * @author lj
 */
@Data
@ConfigurationProperties(prefix = "jiuyu.oss")
public class OssProperties {

    /**
     * 全局 Access Key ID
     */
    private String accessKeyId;

    /**
     * 全局 Access Key Secret
     */
    private String accessKeySecret;

    /**
     * 默认桶别名
     */
    private String defaultBucket;

    /**
     * 全局路径前缀（默认使用环境名）
     */
    private String pathPrefix;

    /**
     * 网络策略配置
     */
    private NetworkProperties network = new NetworkProperties();

    /**
     * 桶配置映射（key 为桶别名）
     */
    private Map<String, OssBucketProperties> buckets = new HashMap<>();

    /**
     * 获取指定桶的 AccessKeyId
     * 桶级别配置优先，否则使用全局配置
     */
    public String getAccessKeyId(String bucketAlias) {
        OssBucketProperties bucket = buckets.get(bucketAlias);
        if (bucket != null && bucket.getAccessKeyId() != null && !bucket.getAccessKeyId().isEmpty()) {
            return bucket.getAccessKeyId();
        }
        return accessKeyId;
    }

    /**
     * 获取指定桶的 AccessKeySecret
     * 桶级别配置优先，否则使用全局配置
     */
    public String getAccessKeySecret(String bucketAlias) {
        OssBucketProperties bucket = buckets.get(bucketAlias);
        if (bucket != null && bucket.getAccessKeySecret() != null && !bucket.getAccessKeySecret().isEmpty()) {
            return bucket.getAccessKeySecret();
        }
        return accessKeySecret;
    }

    /**
     * 获取指定桶的路径前缀
     * 桶级别配置优先，否则使用全局配置
     */
    public String getPathPrefix(String bucketAlias) {
        OssBucketProperties bucket = buckets.get(bucketAlias);
        if (bucket != null && bucket.getPathPrefix() != null && !bucket.getPathPrefix().isEmpty()) {
            return bucket.getPathPrefix();
        }
        return pathPrefix;
    }

    /**
     * 网络策略配置
     */
    @Data
    public static class NetworkProperties {

        /**
         * 服务端操作（上传/下载流）使用的网络
         * 可选值：internal（内网）、external（外网）
         * 默认：internal
         */
        private String serverOperation = "internal";

        /**
         * 预签名链接使用的网络
         * 可选值：internal（内网）、external（外网）
         * 默认：external（因为预签名链接是给外部使用的）
         */
        private String presignedUrl = "external";

        /**
         * 服务端操作是否使用内网
         */
        public boolean isServerOperationInternal() {
            return "internal".equalsIgnoreCase(serverOperation);
        }

        /**
         * 预签名链接是否使用内网
         */
        public boolean isPresignedUrlInternal() {
            return "internal".equalsIgnoreCase(presignedUrl);
        }
    }
}
