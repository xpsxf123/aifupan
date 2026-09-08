package com.jiuyu.governance.plugins.oss.storage;

import com.jiuyu.governance.plugins.oss.enums.OssBucket;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

/**
 * 存储服务接口
 * <p>
 * 定义对象存储的标准操作方法，由具体业务存储服务实现
 * </p>
 *
 * @author lj
 */
public interface StorageService {

    /**
     * 获取当前服务使用的桶
     *
     * @return 桶枚举
     */
    OssBucket getBucket();

    // ==================== 上传操作 ====================

    /**
     * 上传本地文件
     *
     * @param bizPath 业务路径（不含环境前缀）
     * @param file    本地文件
     * @return 完整存储路径
     */
    String upload(String bizPath, File file);

    /**
     * 上传输入流
     *
     * @param bizPath     业务路径（不含环境前缀）
     * @param inputStream 输入流
     * @param contentType 内容类型（如 image/jpeg）
     * @return 完整存储路径
     */
    String upload(String bizPath, InputStream inputStream, String contentType);

    /**
     * 上传二进制数据
     *
     * @param bizPath     业务路径（不含环境前缀）
     * @param bytes       二进制数据
     * @param contentType 内容类型（如 image/jpeg）
     * @return 完整存储路径
     */
    String upload(String bizPath, byte[] bytes, String contentType);

    /**
     * 生成预上传链接（PUT）
     *
     * @param bizPath    业务路径（不含环境前缀）
     * @param expiration 链接有效期
     * @return 预上传结果（含上传链接和完整 ossKey）
     */
    PresignedUploadResult generatePresignedUploadUrl(String bizPath, Duration expiration);

    /**
     * 生成预上传链接（带 Content-Type）
     *
     * @param bizPath     业务路径（不含环境前缀）
     * @param expiration  链接有效期
     * @param contentType 内容类型
     * @return 预上传结果（含上传链接和完整 ossKey）
     */
    PresignedUploadResult generatePresignedUploadUrl(String bizPath, Duration expiration, String contentType);

    // ==================== 下载操作 ====================

    /**
     * 下载文件为输入流
     *
     * @param bizPath 业务路径
     * @return 输入流（调用方负责关闭）
     */
    InputStream download(String bizPath);

    /**
     * 下载文件为字节数组
     *
     * @param bizPath 业务路径
     * @return 字节数组
     */
    byte[] downloadAsBytes(String bizPath);

    /**
     * 下载文件到本地
     *
     * @param bizPath  业务路径
     * @param destFile 目标文件
     */
    void downloadToFile(String bizPath, File destFile);

    /**
     * 生成预下载链接（GET）
     *
     * @param bizPath    业务路径
     * @param expiration 链接有效期
     * @return 预下载 URL
     */
    String generatePresignedDownloadUrl(String bizPath, Duration expiration);

    /**
     * 生成预下载链接（带文件名，用于浏览器下载）
     *
     * @param bizPath    业务路径
     * @param expiration 链接有效期
     * @param filename   下载时显示的文件名
     * @return 预下载 URL
     */
    String generatePresignedDownloadUrl(String bizPath, Duration expiration, String filename);

    // ==================== 管理操作 ====================

    /**
     * 判断文件是否存在
     *
     * @param bizPath 业务路径
     * @return true 存在，false 不存在
     */
    boolean exists(String bizPath);

    /**
     * 删除文件
     *
     * @param bizPath 业务路径
     */
    void delete(String bizPath);

    /**
     * 批量删除文件
     *
     * @param bizPaths 业务路径列表
     */
    void delete(List<String> bizPaths);

    /**
     * 复制文件
     *
     * @param sourceBizPath 源业务路径
     * @param destBizPath   目标业务路径
     * @return 目标完整路径
     */
    String copy(String sourceBizPath, String destBizPath);

    // ==================== 路径与 URL 生成 ====================

    /**
     * 获取完整的 OSS 对象键（含环境前缀和服务前缀）
     *
     * @param bizPath 业务路径
     * @return 完整的 OSS 对象键
     */
    String getFullOssKey(String bizPath);

    /**
     * 获取公开访问 URL
     * <p>
     * 如果配置了自定义域名（CDN），则使用自定义域名；否则使用 OSS 默认域名
     * </p>
     *
     * @param bizPath 业务路径
     * @return 公开访问 URL
     */
    String getPublicUrl(String bizPath);

    /**
     * 获取内网访问 URL
     *
     * @param bizPath 业务路径
     * @return 内网访问 URL
     */
    String getInternalUrl(String bizPath);
}
