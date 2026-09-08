# 系统与配置 API

## ConfigController

路由前缀: `api/config`

> 注意: 此同步版 Controller 仅包含部分端点。**核心业务端点 (settoken/getuserinfo/开发模式/版本更新/磁盘检测 等 24 个) 在 ConfigAsyncController 中**，见下方章节。前端调用时由路由分发到不同类但路径相同的接口。

---

### GET /api/config/settoken

**关键初始化接口**: 设置认证 Token，启动 WebSocket、数据同步、自动分析、看门狗线程。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| token | string | 是 | 认证 Token |
| uId | string | 是 | 用户 ID |
| tId | string | 是 | 租户 ID |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/config/settoken?token=xxx&uId=user_001&tId=tenant_001
```

---

### GET /api/config/getuserinfo

获取当前登录用户信息。

**请求参数**: 无

**响应**: 动态结构 (用户信息对象)

**示例**:
```http
GET /api/config/getuserinfo
```

---

### GET /api/config/getUserObject

获取保存的账号密码。

**请求参数**: 无

**响应** — `AccountPasswordVo`:

| 字段 | 类型 | 说明 |
|---|---|---|
| userName | string | 用户名 |
| password | string | 密码 |
| saveStatus | bool | 是否已保存 |

**示例**:
```http
GET /api/config/getUserObject
```

```json
// 响应
{
  "userName": "admin",
  "password": "***",
  "saveStatus": true
}
```

---

### GET /api/config/putUserObject

保存账号密码。

**请求参数 (Query)** — `AccountPasswordVo`:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userName | string | 是 | 用户名 |
| password | string | 是 | 密码 |
| saveStatus | bool | 是 | 是否保存 |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/config/putUserObject?userName=admin&password=xxx&saveStatus=true
```

---

### GET /api/config/getmodel

获取客户端配置。

**请求参数**: 无

**响应** — `Config`:

| 字段 | 类型 | 说明 |
|---|---|---|
| Id | int | 主键 |
| SerialNumber | string | 序列号 |
| DetectionFre | int | 检测频率 |
| SavePath | string | 存储路径 |
| LiveSource | int | 直播源 |
| LimitType | int | 限制类型 |
| LimitValue | int | 限制值 |
| IsRocord | int | 是否录制 |
| IsSubection | int | 是否分段 |
| HideDuration | int | 隐藏时长 |
| HideSize | int | 隐藏大小 |
| LiveNotice | int | 直播通知 |
| OnlineNumber | int | 在线人数 |

**示例**:
```http
GET /api/config/getmodel
```

```json
// 响应
{
  "Id": 1,
  "SerialNumber": "SN-001",
  "SavePath": "D:\\ReviewAnalysis\\Data",
  "DetectionFre": 30,
  "LimitType": 0,
  "LimitValue": 0,
  "IsRocord": 1,
  "IsSubection": 0,
  "LiveNotice": 1,
  "OnlineNumber": 0
}
```

---

### POST /api/config/updatemodel

更新客户端配置。

**请求体 (JSON)** — `Config`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| Id | int | 是 | 主键 |
| SerialNumber | string | 否 | 序列号 |
| DetectionFre | int | 否 | 检测频率 |
| SavePath | string | 否 | 存储路径 |
| LiveSource | int | 否 | 直播源 |
| LimitType | int | 否 | 限制类型 |
| LimitValue | int | 否 | 限制值 |
| IsRocord | int | 否 | 是否录制 |
| IsSubection | int | 否 | 是否分段 |
| HideDuration | int | 否 | 隐藏时长 |
| HideSize | int | 否 | 隐藏大小 |
| LiveNotice | int | 否 | 直播通知 |
| OnlineNumber | int | 否 | 在线人数 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/config/updatemodel
Content-Type: application/json

{
  "Id": 1,
  "SavePath": "E:\\ReviewAnalysis\\Data",
  "DetectionFre": 60,
  "LimitType": 1,
  "LimitValue": 120,
  "LiveNotice": 1
}
```

---

### GET /api/config/checkfilebox

打开文件夹选择对话框。

**请求参数**: 无

**响应**: `string` — 选择的文件夹路径

**示例**:
```http
GET /api/config/checkfilebox
```

```json
// 响应
"D:\\ReviewAnalysis\\Data"
```

---

### GET /api/config/getdisksize

获取存储盘剩余空间。

**请求参数**: 无

**响应**: 动态结构 (磁盘空间信息)

**示例**:
```http
GET /api/config/getdisksize
```

---

### GET /api/config/getmaxdisk

推荐更大空间的磁盘。

**请求参数**: 无

**响应**: 动态结构 (磁盘推荐信息)

**示例**:
```http
GET /api/config/getmaxdisk
```

---

### POST /api/config/setClientVersionConfig

切换客户端版本。

**请求体 (JSON)** — `ClientVersionConfigVo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| clientVersion | string | 是 | 版本号 |
| isVersionSelect | int? | 否 | 是否版本选择 |
| userType | int? | 否 | 用户类型 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/config/setClientVersionConfig
Content-Type: application/json

{ "clientVersion": "2.6.0", "isVersionSelect": 1, "userType": 0 }
```

---

### GET /api/config/getTimeAccurate

检查系统时间是否准确。

**请求参数**: 无

**响应**: 动态结构 (时间精度信息)

**示例**:
```http
GET /api/config/getTimeAccurate
```

---

### GET /api/config/getVersionUpdate

检查应用更新。

**请求参数**: 无

**响应**: 动态结构 (更新信息)

**示例**:
```http
GET /api/config/getVersionUpdate
```

---

### GET /api/config/updateProgram

触发应用更新。

**请求参数**: 无

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/config/updateProgram
```

---

### GET /api/config/openDevTools

打开 Chrome DevTools。

**请求参数**: 无

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/config/openDevTools
```

---

## SystemController

路由前缀: `api/system`

---

### GET /api/system/getMachineCode

获取机器硬件唯一码。

**请求参数**: 无

**响应**: `string` — 机器码

**示例**:
```http
GET /api/system/getMachineCode
```

```json
// 响应
"A1B2C3D4E5F67890ABCDEF1234567890"
```

---

### GET /api/system/openOfficial

打开官方网站。

**请求参数**: 无

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/system/openOfficial
```

---

### POST /api/system/openUrl

打开 URL (尝试默认浏览器 → Edge → 内置浏览器)。

**请求体 (JSON)** — `OpenUrlBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| url | string | 是 | 要打开的 URL |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/system/openUrl
Content-Type: application/json

{ "url": "https://example.com" }
```

---

### GET /api/system/openGovernanceWeb

打开企业管理页面。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| key | string | 是 | 页面标识 |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/system/openGovernanceWeb?key=dashboard
```

---

### POST /api/system/htmlPrintPDF

HTML 转 PDF。

**请求体 (JSON)** — `HtmlPrintPDFBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| htmlContent | string | 是 | HTML 内容 |
| fileName | string | 是 | 输出文件名 |
| uploadType | int? | 否 | 上传类型 (默认0) |
| sourceType | int? | 否 | 来源类型 (默认0) |
| sourceId | string | 否 | 来源 ID |
| notFolder | int? | 否 | 不创建文件夹 (默认0) |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/system/htmlPrintPDF
Content-Type: application/json

{
  "htmlContent": "<html><body><h1>诊断报告</h1>...</body></html>",
  "fileName": "诊断报告_20260423",
  "uploadType": 1,
  "sourceType": 0,
  "sourceId": "video_001"
}
```

---

## FormController

路由前缀: `api/form`

---

### GET /api/form/togglemaxsize

切换窗口最大化/还原。

**请求参数**: 无

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/form/togglemaxsize
```

---

### GET /api/form/minsize

最小化窗口。

**请求参数**: 无

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/form/minsize
```

---

### GET /api/form/close

停止所有录制并关闭应用。

**请求参数**: 无

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/form/close
```

---

## ExportFileController

路由前缀: `api/export`

---

### GET /api/export/alysesCloudTxt

导出云端分析为 TXT。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type | string | 是 | 导出类型 |
| id | string | 是 | 资源 ID |
| name | string | 是 | 文件名 |

**响应**: 无返回体 (void)，触发文件保存对话框

**示例**:
```http
GET /api/export/alysesCloudTxt?type=0&id=video_001&name=分析报告
```

---

### GET /api/export/alysestxt

导出本地分析为 TXT。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type | string | 是 | 导出类型 |
| id | string | 是 | 资源 ID |
| name | string | 是 | 文件名 |

**响应**: 无返回体 (void)，触发文件保存对话框

**示例**:
```http
GET /api/export/alysestxt?type=0&id=video_001&name=分析报告
```

---

## ConfigAsyncController

路由前缀: `api/config` (与 ConfigController 共享路由，使用 `[RestAsyncController]` 标记)

> 这是 ConfigController 的异步增强版，包含核心业务逻辑。ConfigController 为同步版本，此类为异步版本，两者共存。

---

### GET /api/config/settoken

**关键初始化接口 (异步版)**: 设置认证 Token，初始化 FristPageIni 后台任务编排。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| token | string | 是 | 认证 Token |
| userId | string | 是 | 用户 ID |
| activeTenantId | long | 是 | 当前租户 ID |

**响应**: 无返回体 (void)

**内部逻辑**: 使用 AsyncLock 防重入 → 启动定时用户信息轮询 (8秒) → 同步本地数据到服务器

---

### GET /api/config/getuserinfo

获取当前用户信息 (异步版)，**同时执行账号保护逻辑**:
- 用户 Status==1 (冻结) → 停止录制 + 清除 Token + 通知前端跳登录页
- 租户 ID 变更 → 触发 `UserFrozenAsync` 强制登出

**请求参数**: 无

**响应**: `UserVo` — 用户信息对象

---

### GET /api/config/pin

CefSharp 网页打印为 PDF。

**请求参数**: 无

**响应**: 无返回体 (void)，输出到 `D:\AiFuPan\output.pdf`

---

### GET /api/config/testSetProxy

测试设置抖音 HTTP 代理 IP。

**请求参数**: 无

**响应**: 无返回体 (void)

---

### GET /api/config/tesTdouyinWarnStopRecord

测试通知前端"抖音警告停止录制"弹窗。

**内部逻辑**: 若 `config.IsRocord==1` → `AnchorBll.StopDecectorAll()` → 前端弹窗 `action=douyinWarnStopRecord`

---

### GET /api/config/openUpdateVersion / /updateVersion

测试通知前端打开更新弹窗。

**请求参数**: 无

**响应**: 无返回体 (void)，前端弹窗 `action=openUpdate`

---

### GET /api/config/handUpdateVersion

手动触发所有主播的版本更新通知。

---

### GET /api/config/getTimeAccurate

检查本地系统时间与服务器时间的偏差。

**响应**: `bool` — true=准确

---

### GET /api/config/getVersionUpdate

获取版本更新信息。

**响应**: `VersionUpdateVo` (仅当服务器版本 ≠ 客户端版本时返回)

---

### GET /api/config/getCreateTime

启动定时版本更新检测线程。

**响应**: `VersionUpdateVo`

---

### GET /api/config/getUrl

获取环境拼接后的完整 URL。

**响应**: `string` — `API地址 + Web地址 + 在线地址`

---

### GET /api/config/getUserObject

获取保存的账号密码。

**响应**: `AccountPasswordVo`

---

### GET /api/config/putUserObject

保存账号密码 (带参数校验)。

**请求参数 (Query)**: `AccountPasswordVo` (userName 不能为空)

---

### GET /api/config/getmodel

获取客户端配置 (异步版)。

**响应**: `Config`

---

### POST /api/config/updatemodel

更新客户端配置 (异步版)。

**请求体 (JSON)**: `Config`

---

### GET /api/config/checkfilebox

打开文件夹选择对话框 (线程安全)。

**响应**: `string` — 选择的路径，取消返回 null

---

### GET /api/config/closedialog

关闭文件夹选择对话框 (线程安全, 使用 lock)。

---

### GET /api/config/getdisksize

获取存储盘的总容量和剩余空间。

**响应**: `Dictionary<string, long>`

---

### GET /api/config/getmaxdisk

检测存储路径所在盘符，推荐剩余空间最大的盘符。

**响应**: `string` — 提示文案 (不满足条件返回空字符串)

---

### GET /api/config/updateProgram

触发主程序更新 (检查更新包是否正在下载)。

---

### GET /api/config/setClientVersion

切换客户端版本 (replay ↔ record)。

---

### GET /api/config/openDevelopmentMode

开启开发者模式 (developmentMode=0)。

**响应**: `int` — 0

---

### GET /api/config/closeDevMode

关闭开发者模式 (developmentMode=1)，同时关闭 DevTools。

**响应**: `int` — 1

---

### GET /api/config/getClientMode

获取当前客户端模式。

**响应**: `int` — 0=开发模式, 1=正常模式

---

### GET /api/config/openDevTools

打开 Chrome DevTools (仅开发模式下可用)。

---

### GET /api/config/setSockectProxy

设置获取 WebSocket 地址时是否使用代理。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| proxy | int | 是 | 1=使用代理, 0=不使用 |

---

## QianchuanDataController

路由前缀: `api/qianchuan`

> 千川广告平台数据接口，负责拉取千川广告投放数据和账户管理。

---

### GET /api/qianchuan/pullQianchuan

手动触发拉取千川投放数据。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| roomId | string | 是 | 直播间 ID |
| aavid | string | 是 | 千川广告账户 ID |
| anchorId | string | 是 | 主播 ID |

**前置校验**:
1. 四个参数均不能为空
2. 主播信息必须存在于缓存
3. 主播 `qianchuanAuthStatus` 必须为 2 (已授权)

**响应**: 无返回体 (void)，fire-and-forget 异步执行

---

### GET /api/qianchuan/getQianchuanAccountList

获取千川广告账户列表。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| roomId | string | 是 | 直播间 ID |

**内部逻辑**:
1. 校验主播存在且已授权 (qianchuanAuthStatus==2)
2. 从本地文件读取千川 Cookie
3. 调用千川 API `GetAccountUserList`

**响应**:
```json
// 成功
{ "code": 0, "status": 200, "data": [...] }

// 失败
{ "code": -1, "status": 500, "message": "错误信息" }
```

---

## UserPropertyController / UserPropertyAsyncController

路由前缀: `api/userproperty`

> 用户资产查询接口。UserPropertyController 使用 `[RestAsyncController]` 标记(同步版)，UserPropertyAsyncController 使用 `[RestController]` 标记(异步版)，均只有 1 个端点。

---

### GET /api/userproperty/info

获取用户资产信息 (分析余额、可用时长、字数配额)。

**请求参数**: 无

**响应**: `UserPropertyEntity`

---

## SyncDataController

路由前缀: `api/sync`

> **状态: 已废弃。** 所有方法均已被注释，无可用端点。原始功能为本地数据 ↔ 服务器同步(主播、视频分析、文件分析数据的上传下载重试)。

---

## TestController

路由前缀: `api/test`

> 开发调试接口，仅用于开发过程中的功能验证和手动测试。

---

### GET /api/test/test1111

手动触发诊断报告生成。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |

**内部逻辑**: 调用 `DiagnosisAuto.generateDiagnosis`

---

### GET /api/test/openDiagnosis

打开诊断报告 (空实现)。

---

### GET /api/test/dataUpload

手动触发巨量引擎 + 微信视频号数据上传。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| batchNumber | string | 是 | 批次号 |
| videoId | string | 是 | 视频 ID |
| secUid | string | 是 | 主播 SecUid |

---

### GET /api/test/getSessionId

获取有效的抖音 Session ID (用于 Cookie 维护)。

**响应**: `string`

---

### POST /api/test/testWeChatChannels

测试微信视频号解绑。

**请求体 — `SecUidModel`**:

| 字段 | 类型 | 说明 |
|---|---|---|
| SecUid | string | 主播 SecUid |

---

### POST /api/test/QueryLiveInfo

测试视频号直播信息查询。

**请求体 — `SecUidModel`**

---

### POST /api/test/QueryLiveHeartbeat

测试视频号直播心跳检测。

**请求体 — `ExportIdModel`**:

| 字段 | 类型 | 说明 |
|---|---|---|
| ExportId | string | 视频号 Export ID |

---

### POST /api/test/DownloadFile

测试腾讯云 COS 文件下载。

---

### GET /api/test/testHtml

测试 OSS 上传 HTML 文件:
1. 获取签名上传 URL
2. 上传本地 HTML 到 OSS
3. 返回 OSS Key

**响应**: `string` — OSS 对象 Key
