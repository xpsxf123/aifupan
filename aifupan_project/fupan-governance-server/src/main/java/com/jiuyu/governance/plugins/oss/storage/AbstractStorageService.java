package com.jiuyu.governance.plugins.oss.storage;

import com.jiuyu.governance.plugins.oss.core.OssTemplate;
import com.jiuyu.governance.plugins.oss.enums.OssBucket;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

/**
 * 抽象存储服务
 * <p>
 * 提供存储服务的通用实现，子类只需要：
 * <ul>
 *     <li>实现 {@link #getBucket()} 指定使用的桶</li>
 *     <li>可选：重写 {@link #getPathPrefix()} 自定义路径前缀</li>
 *     <li>可选：添加业务特有的便捷方法</li>
 * </ul>
 * </p>
 *
 * @author lj
 */
public abstract class AbstractStorageService implements StorageService {

    protected final OssTemplate ossTemplate;

    public AbstractStorageService(OssTemplate ossTemplate) {
        this.ossTemplate = ossTemplate;
    }

    /**
     * 子类实现：指定使用的桶
     *
     * @return 桶枚举
     */
    @Override
    public abstract OssBucket getBucket();

    /**
     * 子类可重写：自定义路径前缀
     * <p>
     * 例如视频存储服务可返回 "live"，则文件会存储在 {env}/live/{bizPath}
     * </p>
     *
     * @return 路径前缀，默认为空
     */
    protected String getPathPrefix() {
        return "";
    }

    /**
     * 构建完整业务路径
     *
     * @param bizPath 业务路径
     * @return 完整路径（包含服务级别前缀）
     */
    protected String buildFullPath(String bizPath) {
        String prefix = getPathPrefix();
        if (StringUtils.hasText(prefix)) {
            // 去掉前后的斜杠，避免重复
            prefix = prefix.replaceAll("^/+|/+$", "");
            bizPath = bizPath.replaceAll("^/+", "");
            return prefix + "/" + bizPath;
        }
        return bizPath.replaceAll("^/+", "");
    }

    // ==================== 上传操作 ====================

    @Override
    public String upload(String bizPath, File file) {
        String fullPath = buildFullPath(bizPath);
        return ossTemplate.upload(getBucket(), fullPath, file);
    }

    @Override
    public String upload(String bizPath, InputStream inputStream, String contentType) {
        String fullPath = buildFullPath(bizPath);
        return ossTemplate.upload(getBucket(), fullPath, inputStream, contentType);
    }

    @Override
    public String upload(String bizPath, byte[] bytes, String contentType) {
        String fullPath = buildFullPath(bizPath);
        return ossTemplate.upload(getBucket(), fullPath, bytes, contentType);
    }

    @Override
    public PresignedUploadResult generatePresignedUploadUrl(String bizPath, Duration expiration) {
        String fullPath = buildFullPath(bizPath);
        String uploadUrl = ossTemplate.generatePresignedUploadUrl(getBucket(), fullPath, expiration);
        String ossKey = ossTemplate.buildFullKey(getBucket(), fullPath);
        return new PresignedUploadResult(uploadUrl, ossKey);
    }

    @Override
    public PresignedUploadResult generatePresignedUploadUrl(String bizPath, Duration expiration, String contentType) {
        String fullPath = buildFullPath(bizPath);
        String uploadUrl = ossTemplate.generatePresignedUploadUrl(getBucket(), fullPath, expiration, contentType);
        String ossKey = ossTemplate.buildFullKey(getBucket(), fullPath);
        return new PresignedUploadResult(uploadUrl, ossKey);
    }

    // ==================== 下载操作 ====================
    // 注意：下载方法接收的是完整的 OSS Key（含环境前缀和路径前缀），不再拼接路径

    @Override
    public InputStream download(String ossKey) {
        return ossTemplate.download(getBucket(), ossKey);
    }

    @Override
    public byte[] downloadAsBytes(String ossKey) {
        return ossTemplate.downloadAsBytes(getBucket(), ossKey);
    }

    @Override
    public void downloadToFile(String ossKey, File destFile) {
        ossTemplate.downloadToFile(getBucket(), ossKey, destFile);
    }

    @Override
    public String generatePresignedDownloadUrl(String ossKey, Duration expiration) {
        return ossTemplate.generatePresignedDownloadUrl(getBucket(), ossKey, expiration);
    }

    @Override
    public String generatePresignedDownloadUrl(String ossKey, Duration expiration, String filename) {
        return ossTemplate.generatePresignedDownloadUrl(getBucket(), ossKey, expiration, filename);
    }

    // ==================== 管理操作 ====================

    @Override
    public boolean exists(String bizPath) {
        String fullPath = buildFullPath(bizPath);
        return ossTemplate.exists(getBucket(), fullPath);
    }

    @Override
    public void delete(String bizPath) {
        String fullPath = buildFullPath(bizPath);
        ossTemplate.delete(getBucket(), fullPath);
    }

    @Override
    public void delete(List<String> bizPaths) {
        List<String> fullPaths = bizPaths.stream()
                .map(this::buildFullPath)
                .toList();
        ossTemplate.delete(getBucket(), fullPaths);
    }

    @Override
    public String copy(String sourceBizPath, String destBizPath) {
        String sourceFullPath = buildFullPath(sourceBizPath);
        String destFullPath = buildFullPath(destBizPath);
        return ossTemplate.copy(getBucket(), sourceFullPath, destFullPath);
    }

    // ==================== 路径与 URL 生成 ====================

    @Override
    public String getFullOssKey(String bizPath) {
        String fullPath = buildFullPath(bizPath);
        return ossTemplate.buildFullKey(getBucket(), fullPath);
    }

    @Override
    public String getPublicUrl(String bizPath) {
        String fullPath = buildFullPath(bizPath);
        return ossTemplate.getPublicUrl(getBucket(), fullPath);
    }

    @Override
    public String getInternalUrl(String bizPath) {
        String fullPath = buildFullPath(bizPath);
        return ossTemplate.getInternalUrl(getBucket(), fullPath);
    }
}
