package com.jiuyu.governance.plugins.oss.core;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.jiuyu.governance.plugins.oss.config.OssBucketProperties;
import com.jiuyu.governance.plugins.oss.config.OssProperties;
import com.jiuyu.governance.plugins.oss.enums.OssBucket;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSS 客户端管理器
 * <p>
 * 管理多个桶的 OSSClient 实例，支持内网/外网切换
 * </p>
 *
 * @author lj
 */
@Slf4j
public class OssClientManager {

    private final OssProperties ossProperties;

    /**
     * 外网客户端缓存 (key: bucketAlias)
     */
    private final Map<String, OSS> externalClients = new ConcurrentHashMap<>();

    /**
     * 内网客户端缓存 (key: bucketAlias)
     */
    private final Map<String, OSS> internalClients = new ConcurrentHashMap<>();

    public OssClientManager(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    /**
     * 获取指定桶的 OSS 客户端（使用全局网络策略）
     *
     * @param bucket 桶枚举
     * @param forPresignedUrl 是否用于生成预签名链接
     * @return OSS 客户端
     */
    public OSS getClient(OssBucket bucket, boolean forPresignedUrl) {
        boolean useInternal = forPresignedUrl
                ? ossProperties.getNetwork().isPresignedUrlInternal()
                : ossProperties.getNetwork().isServerOperationInternal();
        return getClient(bucket.getAlias(), useInternal);
    }

    /**
     * 获取指定桶的 OSS 客户端
     *
     * @param bucketAlias 桶别名
     * @param useInternal 是否使用内网
     * @return OSS 客户端
     */
    public OSS getClient(String bucketAlias, boolean useInternal) {
        // 检查桶级别配置是否覆盖
        OssBucketProperties bucketProps = getBucketProperties(bucketAlias);
        final boolean finalUseInternal;
        if (bucketProps.getUseInternal() != null) {
            finalUseInternal = bucketProps.getUseInternal();
        } else {
            finalUseInternal = useInternal;
        }

        Map<String, OSS> clientCache = finalUseInternal ? internalClients : externalClients;

        return clientCache.computeIfAbsent(bucketAlias, key -> createClient(bucketAlias, finalUseInternal));
    }

    /**
     * 获取桶配置
     *
     * @param bucketAlias 桶别名
     * @return 桶配置
     */
    public OssBucketProperties getBucketProperties(String bucketAlias) {
        OssBucketProperties props = ossProperties.getBuckets().get(bucketAlias);
        if (props == null) {
            throw new OssException("未找到桶配置: " + bucketAlias);
        }
        return props;
    }

    /**
     * 获取桶配置
     *
     * @param bucket 桶枚举
     * @return 桶配置
     */
    public OssBucketProperties getBucketProperties(OssBucket bucket) {
        return getBucketProperties(bucket.getAlias());
    }

    /**
     * 获取配置属性
     */
    public OssProperties getOssProperties() {
        return ossProperties;
    }

    /**
     * 创建 OSS 客户端
     */
    private OSS createClient(String bucketAlias, boolean useInternal) {
        OssBucketProperties bucketProps = getBucketProperties(bucketAlias);
        String endpoint = useInternal ? bucketProps.getInternalEndpoint() : bucketProps.getEndpoint();

        if (endpoint == null || endpoint.isEmpty()) {
            throw new OssException("桶 " + bucketAlias + " 的 " + (useInternal ? "内网" : "外网") + " endpoint 未配置");
        }

        String accessKeyId = ossProperties.getAccessKeyId(bucketAlias);
        String accessKeySecret = ossProperties.getAccessKeySecret(bucketAlias);

        if (accessKeyId == null || accessKeySecret == null) {
            throw new OssException("桶 " + bucketAlias + " 的 AccessKey 未配置");
        }

        log.info("[OSS] 创建客户端 bucket={}, endpoint={}, useInternal={}", bucketAlias, endpoint, useInternal);
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    /**
     * 关闭所有客户端
     */
    @PreDestroy
    public void shutdown() {
        log.info("[OSS] 关闭所有客户端...");
        externalClients.values().forEach(client -> {
            try {
                client.shutdown();
            } catch (Exception e) {
                log.warn("[OSS] 关闭客户端异常", e);
            }
        });
        internalClients.values().forEach(client -> {
            try {
                client.shutdown();
            } catch (Exception e) {
                log.warn("[OSS] 关闭客户端异常", e);
            }
        });
        externalClients.clear();
        internalClients.clear();
    }
}
