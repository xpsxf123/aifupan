# 业务逻辑层 (Bll)

## 概述

Bll 层是系统的核心业务逻辑层，位于 Controller 和数据层之间。采用缓存优先设计，大量使用 CacheManager 提升性能。

## 核心编排类

### AnchorBll
**文件**: `Bll/Anchor/AnchorBll.cs`
**职责**: 主播生命周期、检测录制、平台授权的中央编排器

**关键方法**:
- `StartDetectionAll()` — 启动全部主播检测录制
- `StopDecectorAll()` — 停止全部检测
- `StartRecord(secUid)` — 启动单个主播录制
- `RemoveAnchor(secUid)` — 移除主播
- `AddOrUpdateAnchor(bo)` — 添加/更新主播
- `AuthorizeJuliang/Qianchuan/Life/Enterprise/AnchorLive()` — 各平台授权

**状态管理**: 维护 `unRecordList` (待录制) 和 `recordingList` (录制中) 两个集合

### AnchorRecordBll
**文件**: `Bll/Anchor/AnchorRecordBll.cs`
**职责**: 单个主播的录制状态机和生命周期管理

**关键方法**:
- `StartDetectionOnline()` — 检测主播是否在线
- `StartRecord()` — 开始录制
- `RecordEndHandle()` — 录制结束处理
- `StopRecord()` — 停止录制

**录制模式**: 普通录制、排班录制、北京时间模式、时间点分段录制
**底层调用**: `CppRecordUtils` (C++ 录制库)

### OperationAnchorBll
**文件**: `Bll/OperationAnchorBll.cs`
**职责**: 主播操作门面，提供高层业务接口

**关键方法**:
- `GetPage()` — 分页查询
- `syncServerAnchor()` — 同步服务器主播到本地
- `PullJuliangDataAsync()` — 拉取巨量数据
- `UpdateBarrageMonitoring()` — 弹幕监控开关
- `topAnchor()` — 置顶操作

## 视频分析类

### AnchorVideoBll
**文件**: `Bll/AnchorVideoBll.cs`
**职责**: 视频分析核心逻辑

**关键方法**:
- `AutoAnalysis()` — 自动分析 (后台线程持续运行)
- `Analysis()` / `AnalysisVideo()` — 执行分析
- `LockAnalysis()` — 获取分析详情
- `ReAnalysisByTrade()` — 按行业重新分析
- `Delete()` / `DeleteLocalVideoByIds()` — 删除视频

**前置检查**: 分析余额、磁盘空间、MP4 转换队列

### UploadFileBll
**文件**: `Bll/UploadFileBll.cs`
**职责**: 用户上传文件分析 (视频/音频/文本)

**关键方法**:
- `Analysis()` — 执行分析
- `Save()` — 保存上传文件
- `AnalysisVideoOrAudio()` — 视频/音频分析 (含 MP4 转换、音频切片、ASR)
- `AnalysisText()` — 文本分析

**资源管理**: 扣减用户分析时长/字数配额

### VideoContrastBll
**文件**: `Bll/VideoContrastBll.cs`
**职责**: 视频对比分析

**关键方法**:
- `Save()` — 保存对比任务
- `ShareAnalysis()` — 生成分享 URL
- `lockCloudContrast()` — 下载云端对比数据 (ZIP)

## 数据采集类

### OnlineNumBll
**文件**: `Bll/OnlineNumBll.cs`
**职责**: 实时在线人数持久化，格式 `time@count_time@count...`

### TotalOnlineNumBll
**文件**: `Bll/TotalOnlineNumBll.cs`
**职责**: 按 BatchNumber (直播场次) 聚合总在线人数

### VideoViewershipNumBll
**文件**: `Bll/VideoViewershipNumBll.cs`
**职责**: 视频观看数和弹幕数统计

## 平台特定类

### DouYinAnchorBll
**文件**: `Bll/Anchor/DouYinAnchorBll.cs`
**职责**: 抖音平台集成

**关键方法**:
- `GetOnlineStatus()` — 检测在线状态
- `GetLiveRoomIdIfOnline()` — 获取直播间 ID
- `GetDouYinAnchorInfo()` — 获取主播信息

**特性**: 解析 HTML/JSON、多画质流 (SD/HD/超清/蓝光)、代理 IP 轮换

### KuaiShouAnchorBll
**文件**: `Bll/Anchor/KuaiShouAnchorBll.cs`
**职责**: 快手平台集成

**关键方法**:
- `GetLiveAnchorInfo()` — 获取直播信息
- `GetIsOnline()` — 检测在线状态

**特性**: 多画质支持、ID 映射 (originUserId ↔ webId ↔ customId)

### WeChatChannelsAnchorBll
**文件**: `Bll/Anchor/WeChatChannelsAnchorBll.cs`
**职责**: 微信视频号集成

**关键方法**:
- `GetOnlineStatus()` — 心跳 API 检测在线
- `GetLiveAnchorInfo()` — 获取直播信息

**特性**: 仅 M3U8 格式、OAuth 授权状态管理

## AI 与内容类

### AiRelatedBll
**文件**: `Bll/AiRelatedBll.cs`
**职责**: AI 对话、历史、结构化数据管理

**关键方法**:
- `AskStream()` / `Ask()` — AI 问答
- `addHistoryParagraph()` / `historyParagraphList()` — 历史管理
- `addStructure()` / `structurePage()` — 结构化数据
- `exportAiConfig()` — 导出配置
- `generateHtml()` — 生成 HTML/PDF 报告

### ShortVideoBll
**文件**: `Bll/ShortVideoBll.cs`
**职责**: 短视频提取和达人追踪

**关键方法**:
- `selectLocalVideo()` — 选择本地视频
- `collectDouYinVideoData()` — 采集抖音视频数据
- `captureInfluencer()` — 搜索/订阅达人

## 工具类

| Bll | 文件 | 职责 |
|---|---|---|
| ConfigBll | `Bll/ConfigBll.cs` | 系统配置读写 (原子写入 + 备份恢复) |
| DiagnosisBll | `Bll/DiagnosisBll.cs` | 诊断报告 PDF 下载 |
| CommonBll | `Bll/CommonBll.cs` | 清理失败分析数据 |
| DashboardBll | `Bll/DashboardBll.cs` | 按平台路由仪表盘数据 |
| SystemBll | `Bll/SystemBll.cs` | HTML 转 PDF |
| WordDocumentBll | `Bll/WordDocumentBll.cs` | Word 文档文本提取 (SpireDoc, 限 500 段) |

## 架构模式

1. **缓存优先**: AnchorCacheManager / VideoCacheManager / OnlineNumCacheManager 等
2. **服务器同步**: 本地变更同时同步到后端 API
3. **平台抽象**: 平台特定逻辑隔离在独立 Bll 类中
4. **资源校验**: 分析前检查余额、磁盘空间、用户配额
5. **错误恢复**: 备份文件、重试逻辑、优雅降级
