package com.jiuyu.governance.plugins.oss.core;

/**
 * OSS 异常
 *
 * @author lj
 */
public class OssException extends RuntimeException {

    private final String bucketAlias;
    private final String objectKey;

    public OssException(String message) {
        super(message);
        this.bucketAlias = null;
        this.objectKey = null;
    }

    public OssException(String message, Throwable cause) {
        super(message, cause);
        this.bucketAlias = null;
        this.objectKey = null;
    }

    public OssException(String bucketAlias, String objectKey, String message) {
        super(String.format("[bucket=%s, key=%s] %s", bucketAlias, objectKey, message));
        this.bucketAlias = bucketAlias;
        this.objectKey = objectKey;
    }

    public OssException(String bucketAlias, String objectKey, String message, Throwable cause) {
        super(String.format("[bucket=%s, key=%s] %s", bucketAlias, objectKey, message), cause);
        this.bucketAlias = bucketAlias;
        this.objectKey = objectKey;
    }

    public String getBucketAlias() {
        return bucketAlias;
    }

    public String getObjectKey() {
        return objectKey;
    }
}
