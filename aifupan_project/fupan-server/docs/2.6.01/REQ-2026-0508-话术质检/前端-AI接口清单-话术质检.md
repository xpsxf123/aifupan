# 话术质检 — 前端 AI 接口清单

> 适用版本：2.6.01（B4 / B5 / B6 / B8 全套）
>
> 最后更新：2026-06-02

---

## 阅读说明

| 事项 | 说明 |
|---|---|
| 适用场景 | **话术质检阶段**。示例、流程、curl 均以话术质检（monitorType=0）为准 |
| 还原度 / 巡检 | 接口字段契约完整保留，业务暂未上线（调用返 **70014**），前端隐藏相关入口即可 |
| 登录态 | 所有接口均需 Header `Authorization: Bearer <jwt>`；`tenantId` / `userId` 后端从 JWT 自动获取，**前端不传** |
| Snowflake Long | 后端以 19 位整型存储 ID；JSON 序列化时部分字段已转 string（见各接口注）。凡标注"传 string 防精度丢失"的字段，**必须以字符串传输或接收**，不得用 JS number |
| 时间格式 | `ISO-8601 YYYY-MM-DDTHH:mm:ss`，无时区信息，按 Asia/Shanghai 解读 |

---

## 通用响应封装 `R<T>`

```json
{
  "code": 0,
  "msg": "success",
  "data": <T>
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| code | number | 0 = 成功；其他见末尾错误码表 |
| msg | string | 描述；成功时通常为 "success" |
| data | T | 业务数据，类型见每个接口的响应结构 |

**前端解包约定（参考）**

```typescript
async function call<T>(url: string, init?: RequestInit): Promise<T> {
  const r = await fetch(url, init).then(r => r.json());
  if (r.code !== 0) throw new ApiError(r.code, r.msg);
  return r.data as T;
}
```

---

## 接口列表（按业务流程顺序）

| # | 方法 | 路径 | 鉴权 | 简述 |
|---|---|---|---|---|
| 1 | POST | /replay/anchorurl/addOrUpdateAnchor | 登录 | 添加/修改直播间（含三个 AI 监控开关） |
| 2 | POST | /replay/script-monitor/setMonitorEnabled | 登录 + 防重复 | 单能力 toggle 开关（B8） |
| 3 | GET | /replay/script-monitor/anchorBasicConfig | 登录 | 拉直播间配置（含三开关 + standardScriptId） |
| 4 | GET | /replay/userproperty/monitorPositionStatistics | 登录 | 查监控位余额 |
| 5 | POST | /replay/script-monitor/triggerReport | 登录 + 防重复 | 手动触发质检报告 |
| 6 | POST | /replay/script-monitor/batchReportStatus | 登录 | 批量查报告状态（上限 100） |
| 7 | GET | /replay/script-monitor/reportStatus | 登录 | 单个查报告状态 |
| 8 | POST | /replay/script-monitor/confirmRead | 登录 | 已读确认（含 confirmRole 三角色勾选） |
| 9 | GET | /replay/script-monitor/qualityReportDetail | 登录 | 质检报告详情 |

---

## 接口详情

---

### 1. 添加/修改直播间

**POST** `/replay/anchorurl/addOrUpdateAnchor`

**描述**：新增或编辑直播间配置，同时写入三个 AI 监控开关。话术质检阶段前端传 `isScriptQualityInspection=1`，其余开关传 0 或 null（不变更）。

**鉴权**：Header `Authorization: Bearer <jwt>` 必传

**Content-Type**：`application/json`

**校验顺序（后端）**：账号切换保护(70007) → 还原度暂禁(70014) → 监控位不足(70002) → Token < 10万(70001) → 写库

#### 请求参数（Body，仅列 AI 监控相关及核心字段）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | Y | 主播唯一标识（抖音/快手等平台 secUid） |
| anchorName | string | N | 主播名称 |
| platform | number | N | 平台 0=抖音 1=快手 2=视频号 |
| homeUrl | string | N | 主页 URL |
| liveUrl | string | N | 直播间 URL |
| isScriptQualityInspection | number \| null | N | 话术质检开关 0=关 1=开，null=不变更 |
| isScriptFidelityMonitor | number \| null | N | 话术还原度开关 0=关 1=开，null=不变更。**暂禁**，传 1 返 70014 |
| isInteractionPatrol | number \| null | N | 互动巡检开关 0=关 1=开，null=不变更。**暂禁**，传 1 返 70014 |
| standardScriptId | string \| null | N | 标准稿 ID（Snowflake，**传 string 防精度丢失**）。B5 阶段可不传 |
| accountType | number | N | 账号归属 0=自有，非0=竞品。已开 AI 监控时不可改为竞品（70007） |
| isAutoRecord | number | N | 在线时自动录制 0=否 1=是 |
| recordTime | string | N | 录制时间段，格式 `"06:00:00-19:00:00"` |
| smsTip | number | N | 上下播提醒 0=不提醒 1=上播 2=下播 3=上下播 |
| isAutoAnalysis | number | N | 自动分析视频 0=否 1=是 |
| isAutoDiagnosis | number | N | 自动诊断 0=否 1=是 |
| isDataDiagnosis | number | N | 自动数据诊断 0=否 1=是 |
| recordDefinition | number | N | 录制清晰度 -1=跟随系统 0=标清 1=高清 2=超清 3=蓝光 |
| recordLimitType | number | N | 录制形式 -1=跟随系统 0=无限制 1=限时长(只录一段) 2=时长分段 |
| recordLimitValue | number \| null | N | 每段录制时长（分钟），仅 recordLimitType=1/2 时有意义 |
| isAutoUploadCloud | number | N | 自动上传云空间 0=否 1=是 |
| engSerViceType | string | N | 识别引擎，如 `"16k_zh"` |
| isBarrageMonitoring | number | N | 弹幕监控 0=否 1=是 |
| tradeId | string \| null | N | 行业 ID（Snowflake，**传 string 防精度丢失**） |

> 完整字段（录制时间模式、排班录制、业绩统计等）见 `AddOrUpdateAnchorBo.java`，本表聚焦话术质检相关。

**响应 data 结构**：`string`（直播间配置 ID，Snowflake）

> 新增场景返回新 ID；编辑场景返回已有 ID。

#### 错误码

| code | 含义 | 前端处理 |
|---|---|---|
| 70001 | Token（算力）不足 | 提示购买，禁止触发 |
| 70002 | 监控位授权数量不足 | 提示购买 |
| 70007 | 已开 AI 监控，不可改账号归属为竞品 | 提示先关闭 AI 监控 |
| 70014 | 功能未上线（还原度/巡检） | 隐藏对应入口 |

#### 示例请求（开启话术质检）

```http
POST /replay/anchorurl/addOrUpdateAnchor HTTP/1.1
Authorization: Bearer eyJhbGciOi...
Content-Type: application/json

{
  "secUid": "MS4wLjABAAAAexample",
  "anchorName": "李四",
  "platform": 0,
  "isScriptQualityInspection": 1,
  "isScriptFidelityMonitor": null,
  "isInteractionPatrol": null,
  "isAutoRecord": 1,
  "recordTime": "09:00:00-23:00:00"
}
```

#### 示例响应（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": "1234567890123456789"
}
```

#### 示例响应（监控位不足）

```json
{
  "code": 70002,
  "msg": "授权数量不足，请联系产品顾问购买",
  "data": null
}
```

---

### 2. 单能力 toggle 开关

**POST** `/replay/script-monitor/setMonitorEnabled`

**描述**：独立切换某一 AI 监控能力的开关（不影响其他字段）。幂等：当前状态与目标状态相同时直接返回 true，不写库。

**鉴权**：Header `Authorization: Bearer <jwt>` 必传

**防重复提交**：后端基于 `secUid + monitorType + enabled + token` 组合键防重。

**Content-Type**：无 Body，参数全部通过 Query String 传递。

**校验顺序（后端）**：参数合法性(70013) → 还原度/巡检暂禁(70014) → 监控位不足(70002) → 短路幂等 → 单字段写库

> 注意：setMonitorEnabled **不校验 Token（算力）**，与 addOrUpdateAnchor 不同。

#### 请求参数（Query）

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| secUid | string | Y | 非空 | 主播唯一标识 |
| monitorType | number | Y | 0 / 1 / 2 | 监控类型 0=话术质检 1=话术还原度 2=互动巡检 |
| enabled | number | Y | 0 / 1 | 目标状态 0=关闭 1=开启 |

#### 响应 data 结构

`boolean`：true 表示切换成功（含幂等情况）

#### 错误码

| code | 含义 | 前端处理 |
|---|---|---|
| 70002 | 监控位授权数量不足 | 开启前先查余量；不足时禁用开关 |
| 70013 | 参数非法（monitorType 或 enabled 超出范围） | 检查参数取值 |
| 70014 | 功能未上线（还原度 monitorType=1 / 巡检 monitorType=2） | 隐藏对应能力入口 |
| 30000 | 记录不存在（secUid 找不到对应直播间配置） | 提示"直播间配置不存在" |

#### 示例请求（开启话术质检）

```http
POST /replay/script-monitor/setMonitorEnabled?secUid=MS4wLjABAAAAexample&monitorType=0&enabled=1 HTTP/1.1
Authorization: Bearer eyJhbGciOi...
```

#### 示例响应（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": true
}
```

#### 示例响应（监控位不足）

```json
{
  "code": 70002,
  "msg": "授权数量不足，请联系产品顾问购买",
  "data": null
}
```

---

### 3. 拉直播间配置

**GET** `/replay/script-monitor/anchorBasicConfig`

**描述**：进入「直播间配置页」时调用，拉取指定直播间的完整基础信息及三个 AI 监控开关状态。

**鉴权**：Header `Authorization: Bearer <jwt>` 必传

**Content-Type**：无 Body（GET）

#### 请求参数（Query）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | Y | 主播唯一标识 |

#### 响应 data 结构：`AnchorUrlUserVo`

**AI 监控开关（本阶段重点）**

| 字段 | 类型 | 说明 |
|---|---|---|
| isScriptQualityInspection | number | 话术质检开关 0=关 1=开 |
| isScriptFidelityMonitor | number | 话术还原度开关 0=关 1=开（暂禁，开启返 70014） |
| isInteractionPatrol | number | 互动巡检开关 0=关 1=开（暂禁，开启返 70014） |
| standardScriptId | string（Snowflake）\| null | 当前已确认标准稿 ID，**B5 阶段恒为 null**。**字符串解析，防精度丢失** |

**标识字段**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | string（Snowflake）| 直播间配置主键（tb_anchor_url_user.id）。**必须用 string 接收** |
| userId | string（Snowflake）| 所属用户 ID。**必须用 string 接收** |
| tenantId | string（Snowflake）| 租户 ID。**必须用 string 接收** |
| anchorUrlSecUid | string | 主播唯一标识（回显） |
| anchorInfo | AnchorUrlInfoVo \| null | 主播基础信息（名称、头像、平台等），后端组装 |
| tradeId | string（Snowflake）\| null | 行业 ID。**非 null 时须用 string 接收** |

**录制配置**

| 字段 | 类型 | 说明 |
|---|---|---|
| isAutoRecord | number | 在线时自动录制 0=否 1=是 |
| isRemoveRecord | number | 是否从录制列表移除 0=否 1=是 2=已从恢复列表删除 |
| recordTime | string \| null | 录制时间段，格式 `"06:00:00-19:00:00"` |
| recordDefinition | number | 录制清晰度 -1=跟随系统 0=标清 1=高清 2=超清 3=蓝光 |
| recordLimitType | number | 录制形式 -1=跟随系统 0=无限制 1=限时长 2=时长分段 |
| recordLimitValue | number \| null | 每段录制时长（分钟） |
| recordTimeMode | number | 录制时间模式 0=按视频时长 1=按北京时间（默认） 2=按时间点分段 |
| segmentTimePoints | string \| null | 时间点分段列表，格式 `"09:30,10:40,11:20"` |
| isScheduleRecord | number | 是否按排班录制 0=否 1=是 |
| lastRecordTime | string \| null | 最后开始录制时间 |

**分析与诊断**

| 字段 | 类型 | 说明 |
|---|---|---|
| isAutoAnalysis | number | 自动分析视频 0=否 1=是 |
| isAutoDiagnosis | number | 自动诊断 0=否 1=是 |
| isDataDiagnosis | number | 开启数据诊断 0=否 1=是 |
| diagnosisGenerateNum | number \| null | 自动生成数据诊断剩余场次 |
| diagnosisParams | DiagnosisParams \| null | 诊断参数（modelId / cueWordsIds，均为 Snowflake string） |
| dataDiagnosisParams | DiagnosisParams \| null | 数据诊断参数，结构同 DiagnosisParams |

**监控与提醒**

| 字段 | 类型 | 说明 |
|---|---|---|
| isBarrageMonitoring | number | 弹幕监控 0=否 1=是 |
| smsTip | number | 上下播提醒 0=不提醒 1=上播 2=下播 3=上下播 |
| isDataViewing | number | 数据看板 0=否 1=是 |

**其他**

| 字段 | 类型 | 说明 |
|---|---|---|
| isAutoUploadCloud | number | 自动上传云空间 0=否 1=是 |
| isStatisticsPerformance | number | 统计业绩 0=否 1=是 |
| videoCount | number \| null | 已录制视频数 |
| isTop | number | 是否置顶 0=否 1=是 |
| addTopTime | string \| null | 置顶时间 |
| engSerViceType | string \| null | 识别引擎，如 `"16k_zh"` |
| pureRecordOnlineNum | number | 纯录制版是否获取在线人数 0=否 1=是 |
| remarksName | string \| null | 主播备注名称 |
| folderName | string \| null | 文件夹名称 |
| anchorSituation | string \| null | 主播账号情况描述 |
| accountType | number | 账号归属 0=自有，非0=竞品 |
| accountStage | number \| null | 账号阶段（字典 account_stage） |
| accountWaterLevel | number \| null | 账号水平（字典 account_water_level） |
| accountFlow | number \| null | 流量结构（字典 account_flow） |
| livingMode | number \| null | 直播间模式（字典 living_mode） |
| createDate | string \| null | 创建时间（ISO-8601，Asia/Shanghai） |
| updateDate | string \| null | 最后修改时间（ISO-8601） |
| deleteDate | string \| null | 从录制列表移除时间（ISO-8601） |

**授权状态（第三方平台）**

| 字段 | 类型 | 说明 |
|---|---|---|
| authJlbyStatus | number | 巨量百应授权状态 0=未授权 1=已授权 2=过期 3=失败 4=授权中 5=抖音号不匹配 |
| authJlbyStatusTime | string \| null | 巨量百应授权修改时间（ISO-8601） |
| authQcStatus | number | 千川授权状态，枚举同上 |
| authQcStatusTime | string \| null | 千川授权修改时间（ISO-8601） |
| authChannelStatus | number | 视频号授权 0=未授权 1=已授权 2=微信后台取消 3=用户取消 |
| authLifeStatus | number | 来客授权状态，枚举同巨量百应 |
| authLifeStatusTime | string \| null | 来客授权修改时间（ISO-8601） |

#### 错误码

| code | 含义 |
|---|---|
| 30000 | 数据不存在（secUid 对应记录不存在 / 跨租户访问） |
| 70011 | 无数据读取权限 |

#### 示例请求

```http
GET /replay/script-monitor/anchorBasicConfig?secUid=MS4wLjABAAAAexample HTTP/1.1
Authorization: Bearer eyJhbGciOi...
```

#### 示例响应（话术质检已开启）

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": "1234567890123456789",
    "userId": "1234500000000000001",
    "tenantId": "1000000000000000001",
    "anchorUrlSecUid": "MS4wLjABAAAAexample",
    "anchorInfo": null,
    "isScriptQualityInspection": 1,
    "isScriptFidelityMonitor": 0,
    "isInteractionPatrol": 0,
    "standardScriptId": null,
    "isAutoRecord": 1,
    "recordTime": "09:00:00-23:00:00",
    "recordDefinition": -1,
    "recordLimitType": -1,
    "recordLimitValue": null,
    "isAutoAnalysis": 1,
    "isAutoDiagnosis": 0,
    "isDataDiagnosis": 0,
    "isBarrageMonitoring": 0,
    "isAutoUploadCloud": 1,
    "smsTip": 0,
    "accountType": 0,
    "createDate": "2026-01-15T10:30:00",
    "updateDate": "2026-06-02T08:00:00"
  }
}
```

---

### 4. 查监控位余额

**GET** `/replay/userproperty/monitorPositionStatistics`

**描述**：查询当前登录用户（从 JWT 取 userId）的三类 AI 监控位授权量、使用量及剩余量。固定返回三项，顺序：话术质检 → 话术还原度 → 互动巡检。

**鉴权**：Header `Authorization: Bearer <jwt>` 必传

**Content-Type**：无 Body（GET）

**请求参数**：无

#### 响应 data 结构：`MonitorPositionAuthVo[]`

固定三个元素数组，顺序固定。

| 字段 | 类型 | 说明 |
|---|---|---|
| code | string | 监控位资产 code，见下方枚举 |
| name | string | 监控位中文名称 |
| totalQuantity | number | 套餐内授权总量；0 表示未购买该能力 |
| useQuantity | number | 已使用量（已开启该能力的直播间数） |
| remainingQuantity | number | 剩余可用量 = max(totalQuantity - useQuantity, 0) |

> 注：`hasAuth` / `hasSurplus` 字段后端标注了 `@JsonIgnore`，**不会出现在响应 JSON 中**，前端用 totalQuantity / remainingQuantity 自行判断。

**code 枚举**

| code | name | 对应功能 |
|---|---|---|
| `scriptQualityNum` | 话术质检监控位 | 话术质检 |
| `scriptFidelityNum` | 话术还原度监控位 | 话术还原度（暂禁） |
| `interactionPatrolNum` | 互动巡检监控位 | 互动巡检（暂禁） |

**前端判断逻辑**

| 状态 | 判断条件 | 前端处理 |
|---|---|---|
| 未购买 | `totalQuantity == 0` | 隐藏或禁用对应功能入口 |
| 已耗尽 | `totalQuantity > 0 && remainingQuantity == 0` | 开关灰显，提示"监控位已用完" |
| 可开启 | `remainingQuantity > 0` | 允许开启开关 |

> 话术质检阶段：前端展示 code=`scriptQualityNum` 的余量即可。其余两项接口契约保留，无需展示。

#### 示例请求

```http
GET /replay/userproperty/monitorPositionStatistics HTTP/1.1
Authorization: Bearer eyJhbGciOi...
```

#### 示例响应

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "code": "scriptQualityNum",
      "name": "话术质检监控位",
      "totalQuantity": 5,
      "useQuantity": 2,
      "remainingQuantity": 3
    },
    {
      "code": "scriptFidelityNum",
      "name": "话术还原度监控位",
      "totalQuantity": 0,
      "useQuantity": 0,
      "remainingQuantity": 0
    },
    {
      "code": "interactionPatrolNum",
      "name": "互动巡检监控位",
      "totalQuantity": 3,
      "useQuantity": 3,
      "remainingQuantity": 0
    }
  ]
}
```

---

### 5. 手动触发质检报告

**POST** `/replay/script-monitor/triggerReport`

**描述**：手动触发指定资源的某类监控报告生成，异步执行。生成成功后报告状态变为 status=1（生成中），前端轮询 batchReportStatus / reportStatus 查状态。

**鉴权**：Header `Authorization: Bearer <jwt>` 必传

**防重复提交**：后端基于 `sourceType + sceneType + sourceId + monitorType + token` 组合键防重，短时间内重复点击返回"请勿重复触发"。

**Content-Type**：`application/json`

> **话术质检阶段**：monitorType 传 **0**。monitorType=1（还原度）/ monitorType=2（巡检）当前返回 **70014**。

#### 请求参数（Body）

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| sourceType | number | Y | NotNull | 资源类型 0=录制视频 1=上传文件 |
| sceneType | number | Y | NotNull | 业务场景 0=复盘 1=视频分析 2=文案预审 |
| sourceId | string | Y | NotBlank | 资源 ID（非空） |
| monitorType | number | Y | NotNull | 监控类型 0=话术质检 1=还原度（暂禁） 2=巡检（暂禁） |

#### 响应 data 结构

`boolean`：true 表示触发成功（报告异步生成）

#### 错误码

| code | 含义 | 前端处理 |
|---|---|---|
| 70001 | Token（算力）不足（< 10万） | 提示购买，禁止触发 |
| 70014 | 功能未上线（monitorType=1/2） | 隐藏对应入口 |
| -9 | 参数校验失败（NotNull / NotBlank） | 检查入参 |

#### 示例请求（触发质检）

```http
POST /replay/script-monitor/triggerReport HTTP/1.1
Authorization: Bearer eyJhbGciOi...
Content-Type: application/json

{
  "sourceType": 0,
  "sceneType": 0,
  "sourceId": "video_abc123",
  "monitorType": 0
}
```

#### 示例响应（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": true
}
```

#### 示例响应（Token 不足）

```json
{
  "code": 70001,
  "msg": "您的算力数量不足，请联系产品顾问购买。",
  "data": null
}
```

---

### 6. 批量查报告状态

**POST** `/replay/script-monitor/batchReportStatus`

**描述**：批量查询多个资源的三类监控报告状态。跳过无权限或不存在的资源，不报错。上限 100 条。

**鉴权**：Header `Authorization: Bearer <jwt>` 必传

**Content-Type**：`application/json`

#### 请求参数（Body）

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| sources | SourceItem[] | Y | NotEmpty，最多 100 条 | 资源列表 |

`SourceItem` 对象：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceType | number | Y | 资源类型 0=录制视频 1=上传文件 |
| sceneType | number | Y | 业务场景 0=复盘 1=视频分析 2=文案预审 |
| sourceId | string | Y | 资源 ID（非空） |

#### 响应 data 结构：`ScriptMonitorReportStatusVo[]`

返回数组，每个元素对应一个有权限的资源。

`ScriptMonitorReportStatusVo` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| sourceType | number | 资源类型 |
| sceneType | number | 业务场景 |
| sourceId | string | 资源 ID |
| monitors | MonitorTypeStatusVo[] | 三类监控状态列表（固定 3 项：质检/还原度/巡检） |

`MonitorTypeStatusVo` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| monitorType | number | 监控类型 0=质检 1=还原度 2=巡检 |
| monitorTypeText | string | 类型文案，如"话术质检" |
| status | number | 报告状态，见下方枚举 |
| statusText | string | 状态文案，如"已生成" |
| reportId | string（Snowflake）\| null | 报告 ID。**字符串解析，防精度丢失**；未生成时为 null |
| summary | string \| null | 摘要文本；status=2 时有值 |
| isRead | number | 是否已读 0=未读 1=已读 |
| canConfirm | boolean | 当前账号是否可确认（仅录制人本人为 true） |
| unavailableReason | string \| null | 不可生成原因；status=4 时有值 |

**status 枚举**

| 值 | 含义 | 说明 |
|---|---|---|
| 0 | 未生成 | 可手动触发 |
| 1 | 生成中 | 禁止重复触发 |
| 2 | 已生成 | 可查看、可确认已读 |
| 3 | 生成失败 | 可重试 |
| 4 | 不可生成 | 展示 unavailableReason |

#### 错误码

| code | 含义 |
|---|---|
| -9 | sources 为空或超 100 条 |

#### 示例请求（查两个视频的话术质检状态）

```http
POST /replay/script-monitor/batchReportStatus HTTP/1.1
Authorization: Bearer eyJhbGciOi...
Content-Type: application/json

{
  "sources": [
    { "sourceType": 0, "sceneType": 0, "sourceId": "video_abc123" },
    { "sourceType": 0, "sceneType": 0, "sourceId": "video_xyz456" }
  ]
}
```

#### 示例响应

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "sourceType": 0,
      "sceneType": 0,
      "sourceId": "video_abc123",
      "monitors": [
        {
          "monitorType": 0,
          "monitorTypeText": "话术质检",
          "status": 2,
          "statusText": "已生成",
          "reportId": "1234567890123456789",
          "summary": "发现 3 条负面话术",
          "isRead": 0,
          "canConfirm": true,
          "unavailableReason": null
        },
        {
          "monitorType": 1,
          "monitorTypeText": "话术还原度",
          "status": 4,
          "statusText": "不可生成",
          "reportId": null,
          "summary": null,
          "isRead": 0,
          "canConfirm": false,
          "unavailableReason": "功能未上线"
        },
        {
          "monitorType": 2,
          "monitorTypeText": "互动巡检",
          "status": 4,
          "statusText": "不可生成",
          "reportId": null,
          "summary": null,
          "isRead": 0,
          "canConfirm": false,
          "unavailableReason": "功能未上线"
        }
      ]
    }
  ]
}
```

---

### 7. 单个查报告状态

**GET** `/replay/script-monitor/reportStatus`

**描述**：查询单个资源的三类监控报告状态。

**鉴权**：Header `Authorization: Bearer <jwt>` 必传

**Content-Type**：无 Body（GET）

#### 请求参数（Query）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceType | number | Y | 资源类型 0=录制视频 1=上传文件 |
| sceneType | number | Y | 业务场景 0=复盘 1=视频分析 2=文案预审 |
| sourceId | string | Y | 资源 ID |

#### 响应 data 结构：`ScriptMonitorReportStatusVo`

字段与 batchReportStatus 中单个元素完全一致，见接口 6。

#### 示例请求

```http
GET /replay/script-monitor/reportStatus?sourceType=0&sceneType=0&sourceId=video_abc123 HTTP/1.1
Authorization: Bearer eyJhbGciOi...
```

#### 示例响应（话术质检已生成）

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "sourceType": 0,
    "sceneType": 0,
    "sourceId": "video_abc123",
    "monitors": [
      {
        "monitorType": 0,
        "monitorTypeText": "话术质检",
        "status": 2,
        "statusText": "已生成",
        "reportId": "1234567890123456789",
        "summary": "发现 3 条负面话术",
        "isRead": 0,
        "canConfirm": true,
        "unavailableReason": null
      }
    ]
  }
}
```

---

### 8. 已读确认

**POST** `/replay/script-monitor/confirmRead`

**描述**：确认报告已读。重复调用幂等，返回 true。云空间分享只读场景不可调用。`confirmRole` 仅对话术质检报告（monitorType=0）生效，写入对应角色的「已知晓」勾选项。

**鉴权**：Header `Authorization: Bearer <jwt>` 必传

**Content-Type**：`application/json`

#### 请求参数（Body）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| reportId | string（Snowflake）| Y | 报告 ID。**必须以字符串传输**，19 位 Snowflake，JSON 传 number 会精度丢失 |
| confirmRole | number \| undefined | N | 确认角色。仅话术质检报告有意义：1=运营 2=主播 3=主管。还原度/巡检报告传此字段将被忽略 |

#### 响应 data 结构

`boolean`：true 表示成功

#### 规则

- 仅资源归属账号（录制人本人）可确认；他人调用返 70011
- 重复确认：幂等返回 true，不报错
- `confirmRole` 仅对 monitorType=0（质检）报告写入「已知晓」勾选

#### 错误码

| code | 含义 |
|---|---|
| 70011 | 无确认权限（非归属账号） |
| 70012 | 报告不存在 |

#### 示例请求（运营已知晓）

```http
POST /replay/script-monitor/confirmRead HTTP/1.1
Authorization: Bearer eyJhbGciOi...
Content-Type: application/json

{
  "reportId": "1234567890123456789",
  "confirmRole": 1
}
```

#### 示例响应（成功）

```json
{
  "code": 0,
  "msg": "success",
  "data": true
}
```

#### 示例响应（无权限）

```json
{
  "code": 70011,
  "msg": "无数据读取权限",
  "data": null
}
```

---

### 9. 质检报告详情

**GET** `/replay/script-monitor/qualityReportDetail`

**描述**：查询话术质检报告详情，含四类负面话术计数、报告正文（需 JSON.parse）、主播信息、已读状态及角色「已知晓」勾选列表（固定 3 项）。

**鉴权**：Header `Authorization: Bearer <jwt>` 必传

**Content-Type**：无 Body（GET）

#### 请求参数（Query）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| reportId | number 或 string | Y | 报告 ID（Snowflake）。URL Query String 中传数值通常精度安全，建议传字符串防万一 |

#### 响应 data 结构：`QualityReportDetailVo`

| 字段 | 类型 | 说明 |
|---|---|---|
| reportId | string（Snowflake）| 报告 ID。**字符串解析，防精度丢失** |
| sourceType | number | 资源类型 |
| sceneType | number | 业务场景 |
| sourceId | string | 资源 ID |
| status | number | 报告状态（枚举见接口 6） |
| crashCount | number \| null | 崩盘话术句数；status=2 时有值 |
| slackCount | number \| null | 摸鱼话术句数；status=2 时有值 |
| brandDamageCount | number \| null | 有损品牌话术句数；status=2 时有值 |
| afterSalesCount | number \| null | 增加售后风险话术句数；status=2 时有值 |
| reportContent | string \| null | **AI 合并报告 JSON 字符串**（来自 MongoDB）；status=2 时有值。**前端须 `JSON.parse()` 后使用** |
| anchorName | string | 主播名称 |
| liveTitle | string | 直播标题 |
| liveTime | string | 直播时间（ISO-8601，Asia/Shanghai） |
| isRead | number | 是否已读 0=未读 1=已读 |
| canConfirm | boolean | 当前用户是否可确认（仅录制人本人为 true） |
| confirmedRecords | ConfirmedRecordVo[] | 角色「已知晓」勾选列表，固定 3 项（运营/主播/主管） |
| createDate | string | 报告创建时间（ISO-8601，Asia/Shanghai） |

`ConfirmedRecordVo` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| confirmRole | number | 角色编码 1=运营 2=主播 3=主管 |
| confirmRoleName | string | 角色名称，如"运营" |
| confirmedBy | string \| null | 勾选者用户昵称；未勾选时为 null |
| confirmDate | string \| null | 勾选时间（ISO-8601）；未勾选时为 null |

**reportContent JSON 结构（`JSON.parse()` 后）**

```typescript
interface ReportContent {
  summary: {
    crashCount: number;              // 崩盘话术句数
    slackCount: number;              // 摸鱼话术句数
    brandDamageCount: number;        // 有损品牌话术句数
    afterSalesCount: number;         // 增加售后风险话术句数
    totalNegativeCount: number;      // 四类负面话术总句数（AI 合并后去重）
    totalSentencesAnalyzed: number;  // 本场分析话术总句数（分母）
    overallScore: number;            // 整体质量评分 0-100，越高越好
  };
  details: {
    crash: DetailItem[];
    slack: DetailItem[];
    brandDamage: DetailItem[];
    afterSales: DetailItem[];
  };
  summaryHtml: string;               // 整体评估 HTML 段落，可直接 innerHTML 渲染
  improvementSuggestions: string[];  // 3-5 条改进建议（纯文本）
}

interface DetailItem {
  timeRange: string;    // 命中时间段标识，如"段落 5"
  originalText: string; // 原文摘录
  issue: string;        // 问题描述
  severity: string;     // "high" | "medium" | "low"
}
```

#### 错误码

| code | 含义 |
|---|---|
| 70011 | 无查看权限 |
| 70012 | 报告不存在 |

#### 示例请求

```http
GET /replay/script-monitor/qualityReportDetail?reportId=1234567890123456789 HTTP/1.1
Authorization: Bearer eyJhbGciOi...
```

#### 示例响应

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "reportId": "1234567890123456789",
    "sourceType": 0,
    "sceneType": 0,
    "sourceId": "video_abc123",
    "status": 2,
    "crashCount": 2,
    "slackCount": 1,
    "brandDamageCount": 0,
    "afterSalesCount": 3,
    "reportContent": "{\"summary\":{\"crashCount\":2,\"slackCount\":1,\"brandDamageCount\":0,\"afterSalesCount\":3,\"totalNegativeCount\":6,\"totalSentencesAnalyzed\":150,\"overallScore\":78},\"details\":{\"crash\":[{\"timeRange\":\"段落 5\",\"originalText\":\"这个产品根本卖不出去\",\"issue\":\"负面定性话术，可能引发观众购买顾虑\",\"severity\":\"high\"}],\"slack\":[],\"brandDamage\":[],\"afterSales\":[{\"timeRange\":\"段落 12\",\"originalText\":\"有问题直接找我退\",\"issue\":\"承诺范围超出平台规则\",\"severity\":\"medium\"}]},\"summaryHtml\":\"<p>本场直播共检测到 6 条负面话术。</p>\",\"improvementSuggestions\":[\"避免使用绝对否定性话术\",\"售后承诺应符合平台规则\"]}",
    "anchorName": "李四",
    "liveTitle": "2026-06-01 直播间",
    "liveTime": "2026-06-01T19:00:00",
    "isRead": 0,
    "canConfirm": true,
    "confirmedRecords": [
      {
        "confirmRole": 1,
        "confirmRoleName": "运营",
        "confirmedBy": "张三",
        "confirmDate": "2026-06-01T20:00:00"
      },
      {
        "confirmRole": 2,
        "confirmRoleName": "主播",
        "confirmedBy": null,
        "confirmDate": null
      },
      {
        "confirmRole": 3,
        "confirmRoleName": "主管",
        "confirmedBy": null,
        "confirmDate": null
      }
    ],
    "createDate": "2026-06-01T19:30:00"
  }
}
```

---

## 错误码对照表

| code | 含义 | 后端文案 | 前端处理建议 |
|---|---|---|---|
| 0 | 成功 | success | — |
| -9 | 参数校验失败（NotNull / NotEmpty / 超上限） | 统一验证参数异常 | 检查必填字段 |
| 30000 | 记录不存在 | 数据不存在 | 提示"直播间配置不存在"或"报告不存在" |
| 70001 | Token（算力）不足 | 您的算力数量不足，请联系产品顾问购买。 | 提示购买，禁止触发相关操作 |
| 70002 | 监控位授权数量不足 | 授权数量不足，请联系产品顾问购买 | 提示购买，开关灰显 |
| 70007 | 已开 AI 监控，不可改账号归属为竞品 | 请先关闭AI话术监控功能后，再修改账号归属类型 | 提示先关闭 AI 监控 |
| 70011 | 无数据读取权限 | 无数据读取权限 | 只读展示，隐藏操作按钮 |
| 70012 | 报告不存在 | 报告不存在 | 提示"报告不存在"或刷新列表 |
| 70013 | 参数非法（monitorType / enabled 超出范围） | 参数校验失败 | 检查请求参数取值 |
| 70014 | 功能未上线（话术还原度 / 互动巡检暂禁） | 功能未上线 | 隐藏对应功能入口 |

---

## 字段名约定

| 约定 | 详细说明 |
|---|---|
| Snowflake Long → string | 后端 Long 类型 ID（19 位），超过 JS Number 安全范围（2^53-1），**必须以 string 接收或传输**。涉及字段：`reportId`、`id`（直播间配置）、`standardScriptId`、`userId`、`tenantId`、`tradeId` |
| 时间戳 | `ISO-8601 YYYY-MM-DDTHH:mm:ss`，无时区信息，按 Asia/Shanghai 解读 |
| 0/1 开关字段 | 类型为 number（Java Integer），非 boolean |
| null vs undefined | 接口可选字段不传时后端不写库（如 `isScriptQualityInspection=null` 表示不变更） |
| reportContent | 字符串型 JSON，需 `JSON.parse()` 后使用，结构见接口 9 |

---

## 业务流程串联示例（话术质检典型路径）

```
1. 进入直播间配置页
   → GET /replay/script-monitor/anchorBasicConfig?secUid=xxx
   → GET /replay/userproperty/monitorPositionStatistics
   （两个请求可并发）

2. 用户点开「话术质检」开关（状态 0→1）
   → POST /replay/script-monitor/setMonitorEnabled?secUid=xxx&monitorType=0&enabled=1
   → 成功：data=true，useQuantity +1
   → 失败 70002：监控位已耗尽，灰显开关并提示购买

3. 录制完成，后端自动触发（前端无感）
   → 后端 autoTriggerForVideo 走 MQ → AI 任务 → tb_script_monitor_report
   → 报告状态：PENDING → GENERATING → GENERATED

4. 进入复盘报告列表
   → POST /replay/script-monitor/batchReportStatus
     body: { sources: [{ sourceType:0, sceneType:0, sourceId:"video_abc" }] }
   → 看到 monitorType=0 的 status=2（已生成）

5. 点报告进详情
   → GET /replay/script-monitor/qualityReportDetail?reportId=1234567890123456789
   → 渲染：crashCount/slackCount/brandDamageCount/afterSalesCount
   → JSON.parse(reportContent) 获取明细列表和评分

6. 运营点击「已知晓」
   → POST /replay/script-monitor/confirmRead
     body: { reportId: "1234567890123456789", confirmRole: 1 }

7. 用户想手动重新触发质检
   → POST /replay/script-monitor/triggerReport
     body: { sourceType:0, sceneType:0, sourceId:"video_abc", monitorType:0 }
```
