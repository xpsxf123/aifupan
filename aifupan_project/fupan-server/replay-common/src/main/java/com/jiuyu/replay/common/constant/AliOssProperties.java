package com.jiuyu.replay.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
@ConfigurationProperties(prefix = "third.ali.oss")
public class AliOssProperties {

    /**
     * secretId
     */
    private String secretId;
    /**
     * secretKey
     */
    private String secretKey;
    /**
     * arn
     */
    private String arn;
    /**
     * 前缀文件夹
     */
    private String savePrefix;
    /**
     * 是否外网
     */
    private Boolean isOuterNet = false;
    /**
     * buckets
     */
    private List<Bucket> buckets;

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
         * 内网域名
         */
        private String endpointEms;
        /**
         * 外网域名
         */
        private String endpoint;
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
