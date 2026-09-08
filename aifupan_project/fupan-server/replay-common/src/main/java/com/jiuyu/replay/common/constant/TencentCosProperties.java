package com.jiuyu.replay.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "third.tencent.cos")
public class TencentCosProperties {
    /**
     * secretId
     */
    private String secretId;
    /**
     * secretKey
     */
    private String secretKey;
    /**
     * 私有读写
     */
    private Bucket privateBucket;
    /**
     * 公共读写
     */
    private Bucket publicBucket;


    @Data
    public static class Bucket{
        /**
         * cos临时凭证 redis-key
         */
        private String redisCosTempTokenKeyPrefix;
        /**
         * 桶名称
         */
        private String bucketName;
        /**
         * 地区
         */
        private String region;
        /**
         * 访问地址
         */
        private String accessUrl;
    }
}
