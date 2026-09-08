# addAnchorEnd 事件通知文档

## 概述

`action=addAnchorEnd` 是 `AddOrUpdateAnchor` 方法通过 `FrontNotice.NoticeJs` 推送给前端的 WebSocket 事件，用于通知前端添加/修改主播操作的结果。

## 通知格式

```json
{
    "code": 0,
    "status": 200,
    "action": "addAnchorEnd",
    "msg": "string",
    "data": { ... }
}
```

## 顶层字段说明

| 字段 | 类型 | 必返回 | 说明 |
|------|------|--------|------|
| `code` | int | 是 | 响应码。`0`=成功，`-1`=通用失败，其他值为 `CustomException` 透传的业务错误码 |
| `status` | int | 是 | HTTP 状态码。`200`=成功，`500`=失败 |
| `action` | string | 是 | 固定值 `"addAnchorEnd"` |
| `msg` | string | 否 | 错误原因描述，**仅失败时返回** |
| `data` | object | 是 | 主播完整信息（`AnchorInfo` 对象），失败时为 `null` |

## 场景说明

### 场景 1：添加成功

主播不存在，成功获取主播信息、同步服务端并加入缓存。

```json
{
    "code": 0,
    "status": 200,
    "action": "addAnchorEnd",
    "data": { /* 完整 AnchorInfo */ }
}
```

### 场景 2：主播已存在

主播已在录制列表中（`IsRemoveRecord == 0`），直接返回缓存数据，**不重复添加**。

特征：`data.Id` 固定为 `-999`。

```json
{
    "code": 0,
    "status": 200,
    "action": "addAnchorEnd",
    "data": { "Id": -999, ... }
}
```

### 场景 3：获取主播数据失败

`BuildAddAnchor` 返回 null，通常是直播链接无效或平台数据接口无响应。

```json
{
    "code": -1,
    "status": 500,
    "action": "addAnchorEnd",
    "msg": "获取主播数据失败",
    "data": null
}
```

### 场景 4：添加过程异常

添加过程中抛出异常。`code` 和 `msg` 透传异常信息。

```json
{
    "code": 70007,
    "status": 500,
    "action": "addAnchorEnd",
    "msg": "主播位已达上限，请购买资源",
    "data": null
}
```

常见错误码：

| code | msg 示例 | 说明 |
|------|----------|------|
| 70007 | 主播位已达上限，请购买资源 | 资源不足 |
| -1 | 链接异常，url 不能为空 | 直播链接为空 |

### 场景 5：修改时主播不存在

修改已有主播信息时，缓存中找不到该主播（`secUid` 不匹配或已被移除）。

```json
{
    "code": -1,
    "status": 500,
    "action": "addAnchorEnd",
    "msg": "主播不存在或可能被移除",
    "data": null
}
```

### 场景 6：修改成功

修改操作无独立通知 — 修改流程中没有 `addAnchorEnd` 通知。前端通过修改接口的 HTTP 返回判断结果。

---

## data 字段 (AnchorInfo) 说明

`data` 仅在 code=0 时有值，类型为 `AnchorInfo`。以下按用途分组列出核心字段：

### 基础信息

| 字段 | 类型 | 说明 |
|------|------|------|
| `Id` | int | 主键，已存在主播固定为 `-999`，新主播为实际 ID |
| `AnchorName` | string | 直播间名称 |
| `AnchorAvatar` | string | 主播头像 URL |
| `SecUid` | string | 主播在各平台的唯一标识 |
| `AnchorUserId` | string | 主播 userId（快手为 webId） |
| `anchorNumber` | string | 主播抖音号 / 快手自定义 ID |
| `FolderName` | string | 本地存储文件夹名（主播名去特殊符号） |
| `RemarksName` | string | 主播备注名称 |
| `TradeId` | string | 行业 ID |
| `HomeUrl` | string | 主播个人主页 URL |
| `LiveUrl` | string | 直播地址 |
| `AppShareUrl` | string | App 分享直播地址 |
| `SourceUrl` | string | 直播源地址 |
| `StreamUrl` | string | 最新一次直播流地址 |
| `AnchorPlatform` | string | 平台标识：`DouYinLive`=抖音、`TiktokLive`=国际版抖音、`KuaiShouLive`=快手、`WeChatChannelsLive`=微信视频号 |
| `platform` | int | 平台类型：`0`=抖音、`1`=快手、`2`=视频号 |

### 直播状态

| 字段 | 类型 | 说明 |
|------|------|------|
| `LiveStatus` | int | 直播状态：`0`=未检测、`2`=直播中、`4`=未直播 |
| `OnlineNumber` | string | 当前在线人数（字符串） |
| `BatchNumber` | string | 当前正在直播的批次编号 |

### 录制配置

| 字段 | 类型 | 说明 |
|------|------|------|
| `RecordStatus` | int | 录制状态：`0`=未开始、`1`=正在录制、`2`=手动停止、`3`=录制完成、`4`=手动开启 |
| `IsAutoRecord` | int | 是否自动录制：`0`=否、`1`=是 |
| `IsAutoUploadCloud` | int | 是否自动上传云空间：`0`=否、`1`=是 |
| `recordDefinition` | int? | 录制清晰度：`-1`=跟随系统、`0`=标清、`1`=高清、`2`=超清、`3`=蓝光 |
| `recordLimitType` | int? | 录制形式：`-1`=跟随系统、`0`=无限制、`1`=限制时长(只录一段)、`2`=时长分段 |
| `recordLimitValue` | int? | 录制时长上限（分钟） |
| `recordTimeMode` | int? | 录制时间模式：`0`=按视频时长、`1`=按北京时间(默认)、`2`=按时间点分段 |
| `segmentTimePoints` | string | 时间点分段录制的时间点列表，格式 `"09:30,10:40,11:20"` |
| `RecordTime` | string | 录制时间范围，格式 `"06:00:00-19:00:00"` |
| `pureRecordOnlineNum` | int? | 纯录制版是否获取在线人数：`0`=否、`1`=是 |

### 监控与分析

| 字段 | 类型 | 说明 |
|------|------|------|
| `IsBarrageMonitoring` | int | 是否保存弹幕：`0`=不保存、`1`=保存 |
| `IsDataViewing` | int | 是否开启数据看板：`0`=否、`1`=是 |
| `isAutoAnalysis` | int? | 是否自动分析视频：`0`=否、`1`=是 |
| `isAutoDiagnosis` | int | 是否自动诊断：`0`=否、`1`=是 |
| `isDataDiagnosis` | int? | 是否开启数据诊断：`0`=否、`1`=是 |
| `diagnosisGenerateNum` | int? | 自动生成数据诊断剩余场次 |
| `isScriptQualityInspection` | int? | 话术质检开关：`0`=关闭、`1`=开启 |
| `isScriptFidelityMonitor` | int? | 话术还原度开关：`0`=关闭、`1`=开启 |
| `isInteractionPatrol` | int? | 互动巡检开关：`0`=关闭、`1`=开启 |
| `standardScriptId` | long? | 已确认标准直播稿 ID |
| `isStatisticsPerformance` | int? | 是否统计业绩：`0`=否、`1`=是 |
| `isScheduleRecord` | int? | 是否按排班录制：`0`=否、`1`=是 |
| `SmsTip` | int | 上下播短信提醒：`0`=不提醒、`1`=上播提醒、`2`=下播提醒、`3`=上下播提醒 |

### 平台授权状态

| 字段 | 类型 | 值范围 | 说明 |
|------|------|--------|------|
| `juliangAuthStatus` | int | 巨量百应授权状态：`0`=未授权、`1`=已授权、`2`=授权过期、`3`=授权失败、`4`=授权中、`5`=授权抖音号不匹配 |
| `qianchuanAuthStatus` | int | 千川授权状态：`0`=未授权、`1`=已授权、`2`=授权过期、`3`=授权失败、`4`=授权中、`5`=授权抖音号不匹配 |
| `lifeAuthStatus` | int | 来客授权状态 |
| `enterpriseAuthStatus` | int | 企业号授权状态：`0`=未授权、`1`=已授权、`2`=授权过期、`3`=授权失败、`4`=授权中、`5`=授权抖音号不匹配、`6`=子账号无权限 |
| `anchorLiveAuthStatus` | int | 主播后台授权状态：`0`=未授权、`1`=已授权、`2`=授权过期、`3`=授权失败、`4`=授权中 |
| `WeChatChannelsAuthStatus` | int | 微信视频号授权状态：`0`=未授权、`1`=已授权、`2`=微信后台取消授权、`3`=用户取消授权 |

> 来客 (`lifeAuthStatus`) 授权状态码与巨量百应一致：`0`=未授权、`1`=已授权、`2`=授权过期、`3`=授权失败、`4`=授权中、`5`=授权抖音号不匹配

### 账号运营

| 字段 | 类型 | 说明 |
|------|------|------|
| `AccountType` | int | 账号归属：`0`=自有账号、`1`=同行账号 |
| `premiereDate` | string | 首播日期 |
| `accountStage` | int? | 账号阶段（字典 `account_stage`） |
| `accountWaterLevel` | int? | 账号水平（字典 `account_water_level`） |
| `accountFlow` | int? | 浏览结构（字典 `account_flow`） |
| `livingTarget` | int? | 直播目标（字典 `living_target`） |
| `livingModality` | int? | 直播方式（字典 `living_modality`） |
| `marketing` | int? | 营销方式（字典 `marketing`） |
| `optimizeDirection` | string | 优化方向（字典 `optimize_direction`） |
| `learning` | string | 学习方向（字典 `learning`） |
| `livingMode` | int? | 直播模式（字典 `living_mode`） |
| `AnchorSituation` | string | 账号情况描述 |

### 其他

| 字段 | 类型 | 说明 |
|------|------|------|
| `AddTime` | string | 添加主播时间 |
| `StartTime` | string | 开始录制时间 |
| `LastRecordTime` | string | 最后开始录制时间 |
| `IsTop` | int | 是否置顶：`0`=否、`1`=是 |
| `AddTopTime` | string | 加入置顶的时间 |
| `IsRemoveRecord` | int | 移除状态：`0`=正常、`1`=已移除、`2`=已从恢复列表删除 |
| `deleteDate` | string | 从录制列表移除的时间 |
| `WebSocketId` | string | WebSocket 连接 ID |
| `engSerViceType` | string | 语音识别引擎模型，如 `"16k_zh"` |
| `diagnosisParams` | object | 诊断参数 |
| `dataDiagnosisParams` | object | 数据诊断参数 |
| `YesterdayRecordList` | array | 昨日录制列表 |
| `YesterdayRecordNum` | int | 昨日录制数量 |
| `YesterdayAverageObservationNum` | int | 昨日平均场观 |
| `YesterdayAverageVolumeStart` | int | 昨日平均销售额区间-起始 |
| `YesterdayAverageVolumeEnd` | int | 昨日平均销售额区间-结束 |
| `DayBeforeRecordList` | array | 前日录制列表 |
| `DayBeforeRecordNum` | int | 前日录制数量 |
| `DayBeforeAverageObservationNum` | int | 前日平均场观 |
| `DayBeforeAverageVolumeStart` | int | 前日平均销售额区间-起始 |
| `DayBeforeAverageVolumeEnd` | int | 前日平均销售额区间-结束 |
| `YesterdayRecordTime` | long? | 昨日录制数据时间戳 |
