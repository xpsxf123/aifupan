---
name: "aliyun-oss"
description: "OSS对象存储模块：多桶管理、环境隔离、预签名链接、上传下载。开发文件存储相关功能时调用。"
---

# OSS 对象存储模块

阿里云 OSS 集成模块，提供配置化的多桶管理、环境路径隔离、预签名链接等功能。

## 1. 核心能力

| 能力 | 说明 |
|-----|------|
| 多桶支持 | 通过配置管理多个 OSS 桶，业务代码使用枚举指定 |
| 环境隔离 | 路径自动添加环境前缀（dev/local/prod），同一桶可多环境共用 |
| 网络策略 | 服务端操作默认内网，预签名链接默认外网 |
| 预签名链接 | 支持预上传（PUT）和预下载（GET）链接生成 |
| 抽象封装 | 接口 + 抽象类设计，业务只需继承并指定桶 |

## 2. 架构设计

### 2.1 类结构

```
plugins/oss/
├── config/
│   ├── OssProperties.java           # 全局配置属性
│   ├── OssBucketProperties.java     # 桶配置属性
│   └── OssAutoConfiguration.java    # 自动配置
├── core/
│   ├── OssTemplate.java             # 底层OSS操作
│   ├── OssClientManager.java        # Client管理器
│   └── OssException.java            # OSS异常
├── enums/
│   └── OssBucket.java               # 桶枚举
└── storage/
    ├── StorageService.java          # 存储服务接口
    ├── AbstractStorageService.java  # 抽象存储服务
    └── impl/                        # 具体实现
        ├── VideoStorageService.java
        ├── ImageStorageService.java
        └── DocumentStorageService.java
```

### 2.2 层次关系

```
业务代码
    ↓ 依赖
VideoStorageService / ImageStorageService / ...（具体存储服务）
    ↓ 继承
AbstractStorageService（抽象类，通用逻辑）
    ↓ 实现
StorageService（接口，标准方法定义）
    ↓ 调用
OssTemplate（底层OSS操作）
    ↓ 使用
OssClientManager（管理多个OSSClient实例）
```

## 3. 配置说明

### 3.1 配置示例

```yaml
jiuyu:
  oss:
    default-bucket: video
    path-prefix: ${spring.profiles.active}
    network:
      server-operation: internal    # 服务端操作用内网
      presigned-url: external       # 预签名链接用外网
    buckets:
      video:
        bucket-name: jiuyu-video-prod
        endpoint: oss-cn-hangzhou.aliyuncs.com
        internal-endpoint: oss-cn-hangzhou-internal.aliyuncs.com
        access-key-id: ENC(xxx)
        access-key-secret: ENC(xxx)
        domain: https://video-cdn.example.com
      image:
        bucket-name: jiuyu-image-prod
        endpoint: oss-cn-hangzhou.aliyuncs.com
        internal-endpoint: oss-cn-hangzhou-internal.aliyuncs.com
        access-key-id: ENC(xxx)
        access-key-secret: ENC(xxx)
        domain: https://img-cdn.example.com
      document:
        bucket-name: jiuyu-document-prod
        endpoint: oss-cn-hangzhou.aliyuncs.com
        internal-endpoint: oss-cn-hangzhou-internal.aliyuncs.com
        access-key-id: ENC(xxx)
        access-key-secret: ENC(xxx)
```

### 3.2 各环境配置差异

| 配置项 | local | dev | prod |
|-------|-------|-----|------|
| path-prefix | `local` | `dev` | `prod` |
| network.server-operation | `external` | `internal` | `internal` |
| bucket-name | 可共用 | 可共用 | 可共用 |

本地开发没有内网，需配置 `server-operation: external`。

### 3.3 存储路径规则

```
最终路径 = {环境前缀}/{服务前缀}/{业务路径}

示例：
- 环境前缀：dev（来自 path-prefix 配置）
- 服务前缀：live（来自 VideoStorageService.getPathPrefix()）
- 业务路径：2026/03/20/session_123.mp4（业务代码传入）
- 最终路径：dev/live/2026/03/20/session_123.mp4
```

## 4. 使用方式

### 4.1 创建业务存储服务

```java
@Service
public class VideoStorageService extends AbstractStorageService {

    public VideoStorageService(OssTemplate ossTemplate) {
        super(ossTemplate);
    }

    @Override
    public OssBucket getBucket() {
        return OssBucket.VIDEO;
    }

    @Override
    protected String getPathPrefix() {
        return "live";
    }

    public String uploadSessionVideo(Long sessionId, File file) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String bizPath = String.format("%s/session_%d.mp4", datePath, sessionId);
        return upload(bizPath, file);
    }

    public String getVideoDownloadUrl(String videoPath) {
        return generatePresignedDownloadUrl(videoPath, Duration.ofHours(2));
    }
}
```

### 4.2 业务代码调用

```java
@Service
public class LiveSessionService {

    @Autowired
    private VideoStorageService videoStorageService;

    public void handleVideoUpload(Long sessionId, MultipartFile file) {
        String path = videoStorageService.uploadSessionVideo(sessionId, toFile(file));
    }

    public String getDownloadUrl(String videoPath) {
        return videoStorageService.generatePresignedDownloadUrl(videoPath, Duration.ofHours(1));
    }
}
```

## 5. 接口方法说明

### 5.1 上传相关

| 方法 | 说明 |
|-----|------|
| `upload(bizPath, file)` | 上传本地文件 |
| `upload(bizPath, inputStream, contentType)` | 上传输入流 |
| `upload(bizPath, bytes, contentType)` | 上传二进制数据 |
| `generatePresignedUploadUrl(bizPath, expiration)` | 生成预上传链接 |
| `generatePresignedUploadUrl(bizPath, expiration, contentType)` | 生成带Content-Type的预上传链接 |

### 5.2 下载相关

| 方法 | 说明 |
|-----|------|
| `download(bizPath)` | 下载为 InputStream |
| `downloadAsBytes(bizPath)` | 下载为 byte[] |
| `downloadToFile(bizPath, destFile)` | 下载到本地文件 |
| `generatePresignedDownloadUrl(bizPath, expiration)` | 生成预下载链接 |
| `generatePresignedDownloadUrl(bizPath, expiration, filename)` | 生成带文件名的预下载链接 |

### 5.3 管理相关

| 方法 | 说明 |
|-----|------|
| `exists(bizPath)` | 判断文件是否存在 |
| `delete(bizPath)` | 删除单个文件 |
| `delete(List<bizPaths>)` | 批量删除 |
| `copy(sourceBizPath, destBizPath)` | 复制文件 |

### 5.4 URL 生成

| 方法 | 说明 |
|-----|------|
| `getPublicUrl(bizPath)` | 获取公开访问URL（使用CDN域名） |
| `getInternalUrl(bizPath)` | 获取内网访问URL |

## 6. 桶枚举说明

```java
public enum OssBucket {
    VIDEO("video", "视频存储"),
    IMAGE("image", "图片存储"),
    DOCUMENT("document", "文档存储");
}
```

新增桶类型时：
1. 在 `OssBucket` 枚举中添加新值
2. 在配置文件 `buckets` 下添加对应配置
3. 创建对应的 `XxxStorageService` 实现类
