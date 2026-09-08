# ReviewAnalysis 项目总览

## 1. 项目定位

ReviewAnalysis 是一款 **直播电商数据分析与监控桌面客户端**，主要面向抖音（Douyin）生态，提供主播直播间数据采集、视频内容分析、AI 智能诊断、广告投放数据对接等功能。

## 2. 技术栈

| 层级 | 技术选型 |
|---|---|
| 语言 | C# |
| 框架 | .NET Framework 4.7.2, Windows Forms |
| 嵌入式浏览器 | CefSharp 135.0 (Chromium Embedded Framework) |
| 数据库 | SQLite + Entity Framework 6 |
| HTTP 客户端 | RestSharp 112.1 |
| 内嵌 HTTP 服务 | System.Net.HttpListener (自研路由) |
| JSON 序列化 | Newtonsoft.Json 13.0 + System.Text.Json 8.0 |
| JS 引擎 | Jint 4.4 (JavaScript 解释器) |
| 视频/图像处理 | OpenCvSharp4 4.11, MediaInfo, FFmpeg |
| 语音识别 (ASR) | 腾讯云 ASR SDK |
| AI/LLM | ChatCompletions API (大模型对话) |
| 云存储 | 腾讯云 COS, 七牛云 |
| 文档处理 | PDFsharp, Spire.Doc |
| 浏览器自动化 | Microsoft.Playwright 1.29 |
| HTML 解析 | AngleSharp 1.3 |
| 容错/重试 | Polly 8.6 |

## 3. 应用架构

```
┌─────────────────────────────────────────────────────────┐
│                    WinForms UI 层                        │
│  FormMain (主窗口) + CefSharp 嵌入式浏览器               │
│  SplashScreen / JuliangForm / AnchorLiveForm / ...      │
├─────────────────────────────────────────────────────────┤
│                  Controller 层 (REST API)                │
│  AnchorInfoController / AnchorVideoController /          │
│  DiagnosisController / ShortVideoController / ...        │
├─────────────────────────────────────────────────────────┤
│                  Business Logic 层 (Bll)                 │
│  AnchorBll / AnchorVideoBll / ShortVideoBll / ...        │
├─────────────────────────────────────────────────────────┤
│               平台数据采集层 (plugins)                    │
│  PlatformDataManager → Collectors → Adapters → Processors│
│  (巨量/千川/来客/企业号/主播后台/微信视频号)               │
├─────────────────────────────────────────────────────────┤
│                  数据访问层 (Db)                          │
│  SQLiteHelper 系列 (Audio/Video/Record/Upload/...)       │
├─────────────────────────────────────────────────────────┤
│                  基础设施层                               │
│  HttpServer / Websocket / TcpHelp / Asr / Ai / Utils    │
└─────────────────────────────────────────────────────────┘
```

## 4. 核心模块说明

### 4.1 平台对接模块

| 模块目录 | 对接平台 | 功能 |
|---|---|---|
| `anchorLive/` | 抖音主播后台 | 直播间概览、流量漏斗、实时数据采集 |
| `juliang/` | 巨量引擎 | 广告投放数据、授权管理 |
| `qianchuan/` | 千川 | 电商广告数据 |
| `life/` | 抖音来客 | 本地生活数据 |
| `enterprise/` | 抖音企业号 | 企业账号数据 |
| `WeChatChannels/` | 微信视频号 | 视频号数据采集 |
| `douyin/` | 抖音通用 | 授权、Cookie 管理、通用接口 |

### 4.2 数据采集架构 (plugins/)

采用插件化架构，统一管理多平台数据采集：

- `PlatformDataManager` — 统一调度入口
- `collectors/` — 各平台采集器 (JuliangApiCollector, LifeCollector, QianchuanCollector, AnchorLiveCollector, EnterpriseCollector)
- `adapters/` — 数据适配器，将各平台数据统一为内部格式
- `processors/` — 数据处理器

### 4.3 AI 智能分析 (Ai/)

- `ChatCompletionsAsk` — LLM 对话接口
- `AiFactory` — AI 服务工厂
- `diagnosis/` — 数据诊断报告生成
- `auto/` — 自动化分析任务
- `AiAutoTimer` — 定时 AI 分析

### 4.4 语音识别 (Asr/)

- 集成腾讯云 ASR 服务
- 直播录音转文字
- 语音内容分析

### 4.5 短视频模块 (ShortVideo/)

- `DouyinSpider` — 抖音视频爬虫
- `ShortVideoService` — 短视频服务层
- `SearchVideoByAweme` — 按 aweme_id 搜索视频

### 4.6 内嵌 HTTP 服务 (HttpServer/)

- 本地 HTTP 服务器 (HttpListener)
- 为 CefSharp 嵌入页面提供本地 API
- Controller 层通过自定义属性路由

### 4.7 WebSocket 通信 (Websocket/)

- 实时数据推送
- 抖音直播间弹幕/数据流

### 4.8 录制与弹幕 (BarrageGrabExe/)

- 内嵌 BarrageGrab.exe 弹幕抓取工具
- 直播录制功能 (librecord.dll)

## 5. 数据层

### 5.1 数据库

SQLite 文件数据库，通过多个 SQLiteHelper 类管理不同数据表：

| Helper 类 | 数据域 |
|---|---|
| SQLiteHelper | 通用数据操作 |
| SQLiteHelperAudio | 音频数据 |
| SQLiteHelperVideoRecord | 视频录制记录 |
| SQLiteHelperUploadFile | 上传文件管理 |
| SQLiteHelperOnlineNum | 在线人数统计 |
| SQLiteHelperVideoViewershipNum | 视频观看数据 |
| SQLiteHelperTotalOnlineNum | 总在线人数 |
| SQLiteHelperVideoContrast | 视频对比数据 |
| SQLiteHelperAudioAnalysis | 音频分析结果 |
| SQLiteHelperUploadFileAnalysis | 上传文件分析 |

### 5.2 实体模型

- `AnchorEntity` — 主播信息 (录制设置、授权状态、诊断配置)
- `VideoEntity` — 视频元数据
- `UploadFileEntity` — 上传文件跟踪

### 5.3 缓存

- `CommonCache` — 全局缓存
- `BeanCache/` — 业务对象缓存
- `DataCache/` — 数据缓存

## 6. API 路由

| Controller | 路由前缀 | 主要端点 |
|---|---|---|
| AnchorInfoController | `/api/anchorinfo` | 主播管理、授权、录制控制 |
| AnchorVideoController | `/api/anchorvideo` | 视频分析、修复 |
| DiagnosisController | — | 数据诊断 |
| ShortVideoController | — | 短视频操作 |
| SystemController | — | 系统配置 |
| UploadFileController | — | 文件上传 |

## 7. 入口与启动流程

1. `Program.cs` → `Main()` 初始化全局异常处理, 单实例 Mutex 检测
2. `FormMain` 启动 (WinForms Application.Run)
3. `Form1_Load` → 依次执行:
   - 启动 HTTP 资源文件服务 (端口 8080)
   - 初始化 HttpListener 服务器 (端口 6606)
   - 加载配置 (`ConfigBll.init()`)
   - 初始化 CefSharp 浏览器, 加载前端页面
   - 同步服务器数据 (主播列表/配置)
   - 清理视频缓存, 更新录制状态
   - 启动自动分析线程 + 看门狗线程
   - 启动用户轮询 (8 秒间隔)
   - 初始化弹幕采集与 WebSocket 管理
4. 看门狗: 每 5 分钟检查自动分析线程, 异常时自动重启
5. 账号保护: 用户冻结或租户变更 → 停止录制 → 清除 Token → 强制登出

**客户端版本**: v2.6.0 (支持 replay 和 record 两种模式)
**环境配置**: dev/test/yz/prod 四套环境, 通过 `Constant.env` 控制 API 地址和 Web 地址切换

## 8. 构建与部署

- 构建工具: MSBuild (Visual Studio)
- 平台目标: x64 (Debug) / AnyCPU (Release)
- 输出类型: Windows 可执行文件 (WinExe)
- 自动更新: `Update/` + `updateExe/` 模块
