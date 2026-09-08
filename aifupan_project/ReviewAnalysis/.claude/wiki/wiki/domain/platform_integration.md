# 平台对接域 (Platform Integration)

## 概述

系统对接多个抖音生态平台和微信视频号，各平台模块采用统一的架构模式：
`Form(授权) → CollectionManager(调度) → Poller(轮询) → Api(HTTP) → DataHandle(持久化)`

所有平台模块共享 Cookie 认证模式（微信视频号除外），通过 CefSharp 浏览器窗口完成登录授权。

## 平台模块

### Douyin 抖音通用 (douyin/)

抖音平台通用认证模块，提供 Cookie 管理和授权窗口。

**认证方式**: CefSharp 浏览器扫码/手机登录
**核心 Cookie**: 授权有效性判据为名称含 `sessionid` 的 cookie（如 `sessionid_ss`）

**关键文件**:
- `douyin/DouyinAuthForm.cs` — 通用授权窗口
- `douyin/DouyinAuthUtils.cs` — Cookie 提取、登录状态检测

### Juliang 巨量引擎 (juliang/)

字节跳动广告投放数据平台（巨量百应/罗盘）。

**认证方式**: Cookie 认证，核心 Cookie 为 `COMPASS_LUOPAN_DT`
**登录入口**: `buyin.jinritemai.com`

**采集数据**:
- 直播大屏数据 (实时/汇总)
- 用户画像 (年龄/性别/省份)
- 流量/订单来源分解
- 商品/支付数据
- 违规/合规数据
- GMV / ROI / 投放消耗

**关键文件**:
- `JuliangForm.cs` — CefSharp 授权窗口
- `JuliangDataHandle.cs` — 数据持久化
- `JuliangReachData.cs` — 轮询采集引擎

**数据存储**: `dataCollect\juliang\realTime\` 和 `dataCollect\juliang\finish\`

### Juliang API (juliangApi/)

巨量引擎的无头 HTTP 采集模式，不依赖浏览器 UI，通过 API 直接拉取数据。

**认证方式**: 复用本地 Cookie + `a_bogus` JS 签名 (script/juliang.js)
**轮询间隔**: 30 秒

**关键文件**:
- `JuliangApiDataCollectionManager.cs` — 检测调度
- `JuliangApiDataPoller.cs` — 轮询器
- `JuliangApiDataApi.cs` — HTTP 调用层

### Qianchuan 千川 (qianchuan/)

抖音电商广告投放平台。

**认证方式**: Cookie 认证，核心 Cookie 为 `sessionid`
**轮询间隔**: 30 秒 (检测 60 秒)

**采集数据**:
- 千川账户列表
- 总览面板数据
- 净成交订单数 / 订单成本
- 支付金额 / 结算率
- 退款数/额/率
- 曝光/观看/互动指标
- 消耗 / ROI / 在线人数

**关键文件**:
- `QianchuanDataCollectionManager.cs` — 检测调度
- `QianchuanDataPoller.cs` — 轮询器
- `QianchuanDataApi.cs` — HTTP 调用层

### Life 来客 (life/)

抖音本地生活服务平台。

**认证方式**: Cookie 认证，核心 Cookie 为 `sessionid_ls` / `sessionid_ss_ls`
**轮询间隔**: 30 秒

**关键文件**:
- `LifeDataCollectionManager.cs` — 检测调度
- `LifeDataPoller.cs` — 轮询器
- `LifeDataApi.cs` — HTTP 调用层

### Enterprise 企业号 (enterprise/)

抖音企业账号数据。

**认证方式**: Cookie 认证，核心 Cookie 为 `sessionid`
**轮询间隔**: 30 秒

**关键文件**:
- `EnterpriseDataCollectionManager.cs` — 检测调度
- `EnterpriseDataPoller.cs` — 轮询器
- `EnterpriseDataApi.cs` — HTTP 调用层

### AnchorLive 主播后台 (anchorLive/)

抖音主播后台数据采集模块。

**认证方式**: Cookie 认证，Cookie 文件通过 MD5(secUid) 存储
**轮询间隔**: 可配置，默认 30 秒
**登录入口**: `anchor.douyin.com`

**采集数据**:
- 直播间概览 (overview)
- 流量漏斗 (funnel)

**授权状态** (AnchorLiveAuthStatusEnum):
- `unAuth = 0` — 未授权
- `auth = 1` — 已授权
- `authExpires = 2` — 授权过期
- `needRefresh = 3` — 需刷新

**数据存储**: `dataCollect\AnchorLive\realTime\{roomId}_{videoId}.txt` (JSONL)

### WeChatChannels 微信视频号 (WeChatChannels/)

微信视频号数据采集。

**认证方式**: 服务令牌 + 签名认证 (非 Cookie)
- `WeChatChannelsServiceToken` — 令牌管理
- `WeChatChannelsServiceSignatureUtil` — 请求签名

**授权状态** (AnchorEntity.WeChatChannelsAuthStatus):
- `0` — 未授权
- `1` — 已授权
- `2` — 微信后台取消授权
- `3` — 用户取消授权

**采集数据**: 直播状态/心跳、仪表盘数据、账号/商务信息

## 统一数据采集架构 (plugins/)

### PlatformDataManager
统一多平台数据文件管理器，负责文件生命周期，不负责轮询调度。

**职责**:
- 文件创建/合并/归档
- 待上传文件查找
- 上传后归档到 `dataCollect\uploaded\{platform}Up\{date}\`
- 失败文件标记 `_unUp` 后缀

**关键方法**:
- `SavePlatformData()` — 保存/合并平台数据
- `MergeProcessData()` — 合并增量数据
- `ArchiveUploadedFile()` — 归档已上传文件

### Collector (采集器)
各平台采集器包装，均实现 `IPlatformCollector` 接口：

| 采集器 | 平台 |
|---|---|
| JuliangedCollector | 巨量引擎 (浏览器) |
| QianchuanCollectorSimple | 千川 |
| LifeCollectorSimple | 来客 |
| EnterpriseCollectorSimple | 企业号 |
| AnchorLiveCollectorSimple | 主播后台 |

### Adapter (适配器)
将各平台异构数据转换为 `UnifiedDataModel` 统一格式：
- JuliangAdapter / QianchuanAdapter / LifeAdapter / EnterpriseAdapter / AnchorLiveAdapter
