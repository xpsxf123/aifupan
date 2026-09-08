package com.jiuyu.governance.plugins.oss.core;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.jiuyu.governance.plugins.oss.config.OssBucketProperties;
import com.jiuyu.governance.plugins.oss.config.OssProperties;
import com.jiuyu.governance.plugins.oss.enums.OssBucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * OSS 操作模板
 * <p>
 * 封装所有 OSS 底层操作，自动处理路径前缀和网络选择
 * </p>
 *
 * @author lj
 */
@Slf4j
public class OssTemplate {

    private final OssClientManager clientManager;

    public OssTemplate(OssClientManager clientManager) {
        this.clientManager = clientManager;
    }

    // ==================== 上传操作 ====================

    /**
     * 上传文件
     *
     * @param bucket   桶枚举
     * @param objectKey 对象键（不含环境前缀，由模板自动添加）
     * @param file     本地文件
     * @return 完整的对象键（含环境前缀）
     */
    public String upload(OssBucket bucket, String objectKey, File file) {
        String fullKey = buildFullKey(bucket, objectKey);
        OssBucketProperties props = clientManager.getBucketProperties(bucket);
        OSS client = clientManager.getClient(bucket, false);

        try {
            client.putObject(props.getBucketName(), fullKey, file);
            log.debug("[OSS] 上传成功 bucket={}, key={}", bucket.getAlias(), fullKey);
            return fullKey;
        } catch (Exception e) {
            throw new OssException(bucket.getAlias(), fullKey, "上传文件失败", e);
        }
    }

    /**
     * 上传流
     */
    public String upload(OssBucket bucket, String objectKey, InputStream inputStream, String contentType) {
        String fullKey = buildFullKey(bucket, objectKey);
        OssBucketProperties props = clientManager.getBucketProperties(bucket);
        OSS client = clientManager.getClient(bucket, false);

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            if (StringUtils.hasText(contentType)) {
                metadata.setContentType(contentType);
            }
            client.putObject(props.getBucketName(), fullKey, inputStream, metadata);
            log.debug("[OSS] 上传成功 bucket={}, key={}", bucket.getAlias(), fullKey);
            return fullKey;
        } catch (Exception e) {
            throw new OssException(bucket.getAlias(), fullKey, "上传流失败", e);
        }
    }

    /**
     * 上传字节数组
     */
    public String upload(OssBucket bucket, String objectKey, byte[] bytes, String contentType) {
        return upload(bucket, objectKey, new ByteArrayInputStream(bytes), contentType);
    }

    /**
     * 生成预上传链接（PUT）
     */
    public String generatePresignedUploadUrl(OssBucket bucket, String objectKey, Duration expiration) {
        return generatePresignedUploadUrl(bucket, objectKey, expiration, null);
    }

    /**
     * 生成预上传链接（PUT）带 Content-Type
     */
    public String generatePresignedUploadUrl(OssBucket bucket, String objectKey, Duration expiration, String contentType) {
        String fullKey = buildFullKey(bucket, objectKey);
        OssBucketProperties props = clientManager.getBucketProperties(bucket);
        OSS client = clientManager.getClient(bucket, true); // 预签名用外网

        try {
            Date expirationDate = new Date(System.currentTimeMillis() + expiration.toMillis());

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    props.getBucketName(), fullKey, HttpMethod.PUT);
            request.setExpiration(expirationDate);

            if (StringUtils.hasText(contentType)) {
                request.setContentType(contentType);
            }

            URL url = client.generatePresignedUrl(request);
            log.debug("[OSS] 生成预上传链接 bucket={}, key={}, expires={}", bucket.getAlias(), fullKey, expirationDate);
            return url.toString();
        } catch (Exception e) {
            throw new OssException(bucket.getAlias(), fullKey, "生成预上传链接失败", e);
        }
    }

    // ==================== 下载操作 ====================

    /**
     * 下载为流
     * <p>下载操作不加环境前缀，因为存储的 objectKey 已包含完整路径</p>
     */
    public InputStream download(OssBucket bucket, String objectKey) {
        OssBucketProperties props = clientManager.getBucketProperties(bucket);
        OSS client = clientManager.getClient(bucket, false);

        try {
            OSSObject ossObject = client.getObject(props.getBucketName(), objectKey);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            throw new OssException(bucket.getAlias(), objectKey, "下载文件失败", e);
        }
    }

    /**
     * 下载为字节数组
     */
    public byte[] downloadAsBytes(OssBucket bucket, String objectKey) {
        try (InputStream is = download(bucket, objectKey);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new OssException(bucket.getAlias(), objectKey, "下载文件失败", e);
        }
    }

    /**
     * 下载到本地文件
     * <p>下载操作不加环境前缀，因为存储的 objectKey 已包含完整路径</p>
     */
    public void downloadToFile(OssBucket bucket, String objectKey, File destFile) {
        OssBucketProperties props = clientManager.getBucketProperties(bucket);
        OSS client = clientManager.getClient(bucket, false);

        try {
            client.getObject(new GetObjectRequest(props.getBucketName(), objectKey), destFile);
            log.debug("[OSS] 下载成功 bucket={}, key={}, dest={}", bucket.getAlias(), objectKey, destFile.getAbsolutePath());
        } catch (Exception e) {
            throw new OssException(bucket.getAlias(), objectKey, "下载文件失败", e);
        }
    }

    /**
     * 生成预下载链接（GET）
     */
    public String generatePresignedDownloadUrl(OssBucket bucket, String objectKey, Duration expiration) {
        return generatePresignedDownloadUrl(bucket, objectKey, expiration, null);
    }

    /**
     * 生成预下载链接（GET）带文件名
     * <p>下载操作不加环境前缀，因为存储的 objectKey 已包含完整路径</p>
     */
    public String generatePresignedDownloadUrl(OssBucket bucket, String objectKey, Duration expiration, String filename) {
        OssBucketProperties props = clientManager.getBucketProperties(bucket);
        OSS client = clientManager.getClient(bucket, true); // 预签名用外网

        try {
            Date expirationDate = new Date(System.currentTimeMillis() + expiration.toMillis());

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    props.getBucketName(), objectKey, HttpMethod.GET);
            request.setExpiration(expirationDate);

            // 设置 Content-Disposition 以指定下载文件名
            if (StringUtils.hasText(filename)) {
                String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                        .replace("+", "%20");
                ResponseHeaderOverrides headers = new ResponseHeaderOverrides();
                headers.setContentDisposition("attachment; filename*=UTF-8''" + encodedFilename);
                request.setResponseHeaders(headers);
            }

            URL url = client.generatePresignedUrl(request);
            log.debug("[OSS] 生成预下载链接 bucket={}, key={}, expires={}", bucket.getAlias(), objectKey, expirationDate);
            return url.toString();
        } catch (Exception e) {
            throw new OssException(bucket.getAlias(), objectKey, "生成预下载链接失败", e);
        }
    }

    // ==================== 管理操作 ====================

    /**
     * 判断文件是否存在
     */
    public boolean exists(OssBucket bucket, String objectKey) {
        String fullKey = buildFullKey(bucket, objectKey);
        OssBucketProperties props = clientManager.getBucketProperties(bucket);
        OSS client = clientManager.getClient(bucket, false);

        try {
            return client.doesObjectExist(props.getBucketName(), fullKey);
        } catch (Exception e) {
            throw new OssException(bucket.getAlias(), fullKey, "检查文件存在失败", e);
        }
    }

    /**
     * 删除文件
     */
    public void delete(OssBucket bucket, String objectKey) {
        String fullKey = buildFullKey(bucket, objectKey);
        OssBucketProperties props = clientManager.getBucketProperties(bucket);
        OSS client = clientManager.getClient(bucket, false);

        try {
            client.deleteObject(props.getBucketName(), fullKey);
            log.debug("[OSS] 删除成功 bucket={}, key={}", bucket.getAlias(), fullKey);
        } catch (Exception e) {
            throw new OssException(bucket.getAlias(), fullKey, "删除文件失败", e);
        }
    }

    /**
     * 批量删除
     */
    public void delete(OssBucket bucket, List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }

        OssBucketProperties props = clientManager.getBucketProperties(bucket);
        OSS client = clientManager.getClient(bucket, false);

        List<String> fullKeys = objectKeys.stream()
                .map(key -> buildFullKey(bucket, key))
                .toList();

        try {
            DeleteObjectsRequest request = new DeleteObjectsRequest(props.getBucketName());
            request.setKeys(fullKeys);
            client.deleteObjects(request);
            log.debug("[OSS] 批量删除成功 bucket={}, count={}", bucket.getAlias(), fullKeys.size());
        } catch (Exception e) {
            throw new OssException(bucket.getAlias(), null, "批量删除失败", e);
        }
    }

    /**
     * 复制文件
     */
    public String copy(OssBucket bucket, String sourceKey, String destKey) {
        String sourceFullKey = buildFullKey(bucket, sourceKey);
        String destFullKey = buildFullKey(bucket, destKey);
        OssBucketProperties props = clientManager.getBucketProperties(bucket);
        OSS client = clientManager.getClient(bucket, false);

        try {
            client.copyObject(props.getBucketName(), sourceFullKey, props.getBucketName(), destFullKey);
            log.debug("[OSS] 复制成功 bucket={}, source={}, dest={}", bucket.getAlias(), sourceFullKey, destFullKey);
            return destFullKey;
        } catch (Exception e) {
            throw new OssException(bucket.getAlias(), sourceFullKey, "复制文件失败", e);
        }
    }

    // ==================== URL 生成 ====================

    /**
     * 获取公开访问 URL
     */
    public String getPublicUrl(OssBucket bucket, String objectKey) {
        OssBucketProperties props = clientManager.getBucketProperties(bucket);

        String baseUrl = props.getPublicBaseUrl();
        if (baseUrl == null) {
            // 使用默认的 OSS 域名
            baseUrl = String.format("https://%s.%s",
                    props.getBucketName(),
                    props.getEndpoint().replace("https://", "").replace("http://", ""));
        }

        return baseUrl + "/" + objectKey;
    }

    /**
     * 获取内网访问 URL
     */
    public String getInternalUrl(OssBucket bucket, String objectKey) {
        OssBucketProperties props = clientManager.getBucketProperties(bucket);

        String internalEndpoint = props.getInternalEndpoint();
        if (internalEndpoint == null || internalEndpoint.isEmpty()) {
            internalEndpoint = props.getEndpoint();
        }

        String baseUrl = String.format("https://%s.%s",
                props.getBucketName(),
                internalEndpoint.replace("https://", "").replace("http://", ""));

        return baseUrl + "/" + objectKey;
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建完整的对象键（添加环境前缀）
     *
     * @param bucket    桶枚举
     * @param objectKey 对象键（不含环境前缀）
     * @return 完整对象键（含环境前缀）
     */
    public String buildFullKey(OssBucket bucket, String objectKey) {
        String pathPrefix = clientManager.getOssProperties().getPathPrefix(bucket.getAlias());

        // 清理路径
        objectKey = objectKey.replaceAll("^/+", "");

        if (StringUtils.hasText(pathPrefix)) {
            pathPrefix = pathPrefix.replaceAll("^/+|/+$", "");
            return pathPrefix + "/" + objectKey;
        }

        return objectKey;
    }

    /**
     * 获取客户端管理器
     */
    public OssClientManager getClientManager() {
        return clientManager;
    }
}
