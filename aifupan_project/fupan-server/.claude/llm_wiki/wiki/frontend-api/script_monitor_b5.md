# 话术智能监控 B5 API（直播间基础配置 + 监控位统计）

> 本模块所有响应均符合 [`R<T>` 统一响应封装](./_response_envelope.md)；本文档不重复说明 `code` / `msg` / `data`，**响应 data 结构**指 R\<T\> 内的 T。
>
> 错误码全集 → [./_error_codes.md](./_error_codes.md)
>
> B4 已发布的 5 个质检接口 → [./script_monitor.md](./script_monitor.md)（本文件不覆盖 B4 内容）
>
> 最后更新：2026-06-02 | 来源：`.claude/runs/Change__2026-06-02_b5-monitor-position/openspec.md`（forward mode）

---

## 接口列表

| 方法 | 路径 | 鉴权 | 简述 |
|---|---|---|---|
| GET | /replay/script-monitor/anchorBasicConfig | Token 必传 | 直播间基础配置（含 B5 三个 AI 监控开关） |
| GET | /replay/userproperty/monitorPositionStatistics | Token 必传 | 当前用户三类监控位授权统计 |

---

## GET /replay/script-monitor/anchorBasicConfig

**描述**：前端进入「直播间配置页」时调用，拉取指定直播间的完整基础信息及三个 AI 监控能力开关状态（话术质检 / 话术还原度 / 互动巡检），同时返回已绑定的标准稿 ID（B5 阶段恒为 null）。

**鉴权**：Header `Authorization: Bearer <jwt>` 必传

**Content-Type**：`application/json`（GET 请求，参数通过 Query String 传递）

### 请求参数

| 来源 | 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|---|
| Query | secUid | string | Y | 非空 | 主播唯一标识（`tb_anchor_url_user.anchor_url_sec_uid`），与项目其他模块（弹幕监控位 / addOrUpdateAnchor 等）参数风格一致 |

> **secUid 类型说明**：抖音 / 快手等平台主播的唯一字符串标识，长度 ≤ 64，由前端从主播列表 / 路由参数取得。

**示例请求**

```http
GET /replay/script-monitor/anchorBasicConfig?secUid=MS4wLjABAAAABYa2yd-9BXCD9agZ9SAjTJFfbSgLnd6_NC6SJ1adhEk HTTP/1.1
Authorization: Bearer eyJhbGciOi...
```

### 响应 data 结构：`AnchorUrlUserVo`

> `AnchorUrlUserVo` 继承 `BasicSettingsBaseDto`（账号阶段/流量结构等扩展字段），下表列出全部字段。

#### B5 新增字段（本批次重点）

| 字段 | 类型 | 说明 |
|---|---|---|
| isScriptQualityInspection | number | 话术质检开关。`0` 关闭，`1` 开启（Java: Integer） |
| isScriptFidelityMonitor | number | 话术还原度监控开关。`0` 关闭，`1` 开启（Java: Integer） |
| isInteractionPatrol | number | 互动巡检开关。`0` 关闭，`1` 开启（Java: Integer） |
| standardScriptId | string \| null | 当前已确认标准稿 ID（`tb_standard_script.id`，Snowflake）。**B5 阶段统一返回 `null`，B3 标准稿能力就绪后生效** |

#### 已有字段（B5 不变）

**基础标识**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | string | 主键（Snowflake，19 位，`tb_anchor_url_user.id`）。**必须用 string 接收** |
| userId | string | 所属用户 ID（Snowflake）。**必须用 string 接收** |
| tenantId | string | 租户 ID（Snowflake）。**必须用 string 接收** |
| anchorUrlSecUid | string | 主播唯一标识（secUid） |

**主播信息（嵌套对象）**

| 字段 | 类型 | 说明 |
|---|---|---|
| anchorInfo | AnchorUrlInfoVo \| null | 主播基础信息对象。字段结构见下方 [AnchorUrlInfoVo 说明](#anchorurlinfovo-字段说明)（由后端组装，非本表重点） |

**录制配置**

| 字段 | 类型 | 说明 |
|---|---|---|
| isAutoRecord | number | 在线时是否自动录制分析。`0` 否，`1` 是 |
| isRemoveRecord | number | 是否从录制列表移除。`0` 否，`1` 是，`2` 已从恢复列表删除 |
| recordTime | string \| null | 录制时间段，格式 `"06:00:00-19:00:00"` |
| recordDefinition | number | 录制清晰度。`-1` 跟随系统，`0` 标清，`1` 高清，`2` 超清，`3` 蓝光 |
| recordLimitType | number | 录制形式。`-1` 跟随系统，`0` 无限制，`1` 限制时长（只录一段），`2` 时长分段录制 |
| recordLimitValue | number \| null | 每段录制时长，单位分钟。仅 `recordLimitType=1/2` 时有意义 |
| recordTimeMode | number | 录制时间模式。`0` 按视频时长，`1` 按北京时间（默认），`2` 按时间点分段 |
| segmentTimePoints | string \| null | 时间点分段录制时间点列表，格式 `"09:30,10:40,11:20"`，多个逗号分隔。仅 `recordTimeMode=2` 时有意义 |
| isScheduleRecord | number | 是否按排班录制。`0` 否，`1` 是 |
| lastRecordTime | string \| null | 最后开始录制时间（格式由后端定，参考 string） |

**分析与诊断**

| 字段 | 类型 | 说明 |
|---|---|---|
| isAutoAnalysis | number | 是否自动分析视频。`0` 否，`1` 是 |
| isAutoDiagnosis | number | 是否自动诊断。`0` 否，`1` 是 |
| isDataDiagnosis | number | 是否开启数据诊断。`0` 否，`1` 是 |
| diagnosisGenerateNum | number \| null | 自动生成数据诊断剩余场次 |
| diagnosisParams | DiagnosisParams \| null | 诊断参数。结构见下方 [DiagnosisParams 说明](#diagnosisparams-字段说明) |
| dataDiagnosisParams | DiagnosisParams \| null | 数据诊断参数。结构同 DiagnosisParams |

**监控与提醒**

| 字段 | 类型 | 说明 |
|---|---|---|
| isBarrageMonitoring | number | 是否开启弹幕监控。`0` 否，`1` 是 |
| smsTip | number | 上下播短信提醒。`0` 不提醒，`1` 上播提醒，`2` 下播提醒，`3` 上下播提醒 |
| isDataViewing | number | 是否开启数据看板。`0` 否，`1` 是 |

**云存储与统计**

| 字段 | 类型 | 说明 |
|---|---|---|
| isAutoUploadCloud | number | 是否自动上传到云空间。`0` 否，`1` 是 |
| isStatisticsPerformance | number | 是否统计业绩。`0` 否，`1` 是 |
| videoCount | number \| null | 已录制的视频数量 |

**置顶**

| 字段 | 类型 | 说明 |
|---|---|---|
| isTop | number | 是否置顶。`0` 否，`1` 是 |
| addTopTime | string \| null | 加入置顶的时间（格式由后端定） |

**语音识别**

| 字段 | 类型 | 说明 |
|---|---|---|
| engSerViceType | string \| null | 识别引擎模型（默认识别语言），如 `"16k_zh"` |
| pureRecordOnlineNum | number | 纯录制版是否获取在线人数。`0` 否，`1` 是 |

**主播备注**

| 字段 | 类型 | 说明 |
|---|---|---|
| remarksName | string \| null | 主播备注名称 |
| folderName | string \| null | 文件夹名称（主播名去掉特殊符号；仅含特殊符号时为 UUID） |
| anchorSituation | string \| null | 主播账号情况描述 |

**时间戳**

| 字段 | 类型 | 说明 |
|---|---|---|
| createDate | string \| null | 创建时间，ISO-8601（`java.util.Date`，Asia/Shanghai） |
| updateDate | string \| null | 最后修改时间，ISO-8601 |
| deleteDate | string \| null | 从录制列表移除主播的时间，ISO-8601 |

**授权状态（第三方平台）**

| 字段 | 类型 | 说明 |
|---|---|---|
| authJlbyStatus | number | 授权巨量百应状态。`0` 未授权，`1` 已授权，`2` 授权过期，`3` 授权失败，`4` 授权中，`5` 抖音号不匹配 |
| authJlbyStatusTime | string \| null | 授权巨量百应状态修改时间，ISO-8601 |
| authQcStatus | number | 千川授权状态。同上枚举 |
| authQcStatusTime | string \| null | 千川授权状态修改时间，ISO-8601 |
| authChannelStatus | number | 微信视频号授权状态。`0` 未授权，`1` 已授权，`2` 微信后台取消授权，`3` 用户取消授权 |
| authLifeStatus | number | 来客授权状态。`0` 未授权，`1` 已授权，`2` 授权过期，`3` 授权失败，`4` 授权中，`5` 抖音号不匹配 |
| authLifeStatusTime | string \| null | 来客授权状态修改时间，ISO-8601 |

**账号属性（继承自 BasicSettingsBaseDto）**

| 字段 | 类型 | 说明 |
|---|---|---|
| accountType | number | 账号归属类型。`0` 自有账号，非 `0` 竞品账号（同行账号） |
| accountStage | number \| null | 账号阶段（字典 `account_stage`） |
| accountWaterLevel | number \| null | 账号水平（字典 `account_water_level`） |
| accountFlow | number \| null | 流量结构（字典 `account_flow`） |
| livingMode | number \| null | 直播间模式（字典 `living_mode`） |
| livingTarget | number \| null | 直播目标（字典 `living_target`） |
| livingModality | number \| null | 直播形式（字典 `living_modality`） |
| marketing | number \| null | 营销组件（字典 `marketing`） |
| optimizeDirection | string \| null | 优化方向（多选，字典 `optimize_direction`，逗号或 JSON 格式，前端按需解析） |
| learning | string \| null | 学习方向（多选，字典 `learning`） |
| tradeId | string \| null | 行业 ID（Snowflake，**若非 null 须用 string 接收**） |
| premiereDate | string \| null | 首播日期，ISO-8601 |

---

#### AnchorUrlInfoVo 字段说明

> `anchorInfo` 为后端组装的主播基础信息对象，字段来自 `AnchorUrlInfoVo`。B5 文档仅保证 anchorBasicConfig 接口的完整契约；AnchorUrlInfoVo 的完整结构可在 B4 文档 [script_monitor.md](./script_monitor.md) 中查阅，或联系后端确认字段列表。

---

#### DiagnosisParams 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| modelId | string \| null | 模型 ID（Snowflake，**若非 null 须用 string 接收**） |
| cueWordsIds | string[] \| null | 问题 ID 列表（Snowflake，**每项须用 string 接收**） |

---

### 错误码

| code | 含义 | 处理建议 |
|---|---|---|
| 30000 | 数据不存在（secUid 对应记录不存在 / 跨租户访问） | 提示"直播间配置不存在"，返回上一页 |
| 70011 | 无数据读取权限（当前用户无权访问该直播间） | 提示"无查看权限" |

> 完整错误码 → [_error_codes.md](./_error_codes.md)

### 示例响应

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
    "isInteractionPatrol": 1,
    "standardScriptId": null,
    "isAutoRecord": 1,
    "isRemoveRecord": 0,
    "isBarrageMonitoring": 0,
    "isAutoUploadCloud": 1,
    "isTop": 0,
    "isAutoDiagnosis": 0,
    "isDataDiagnosis": 0,
    "recordTime": "09:00:00-23:00:00",
    "smsTip": 0,
    "recordDefinition": -1,
    "recordLimitType": -1,
    "recordLimitValue": null,
    "recordTimeMode": 1,
    "segmentTimePoints": null,
    "isScheduleRecord": 0,
    "isAutoAnalysis": 1,
    "isStatisticsPerformance": 0,
    "engSerViceType": "16k_zh",
    "accountType": 0,
    "accountStage": null,
    "accountWaterLevel": null,
    "accountFlow": null,
    "tradeId": "1000000000000000002",
    "createDate": "2026-01-15T10:30:00",
    "updateDate": "2026-06-01T08:00:00"
  }
}
```

---

## GET /replay/userproperty/monitorPositionStatistics

**描述**：查询当前登录用户（取 JWT userId）的三类 AI 监控能力授权情况（总量 / 已用 / 剩余），固定返回三项，顺序为：话术质检 → 话术还原度 → 互动巡检。前端在直播间配置页「开启自动监控」前需调用此接口判断余量是否充足。

**鉴权**：Header `Authorization: Bearer <jwt>` 必传

**Content-Type**：无请求体（userId 从 JWT 上下文取）

### 请求参数

无（userId 及 tenantId 均从 JWT 上下文自动获取，前端无需传参）。

**示例请求**

```http
GET /replay/userproperty/monitorPositionStatistics HTTP/1.1
Authorization: Bearer eyJhbGciOi...
```

### 响应 data 结构：`List<MonitorPositionAuthVo>`

返回固定三个元素的数组，顺序：`scriptQualityNum` → `scriptFidelityNum` → `interactionPatrolNum`。

**`MonitorPositionAuthVo` 字段**

| 字段 | 类型 | 说明 |
|---|---|---|
| code | string | 监控位资产 code（枚举值见下表） |
| name | string | 监控位中文名称 |
| totalQuantity | number | 套餐内授权总量。`0` 表示未购买该能力（`hasAuth = false`） |
| useQuantity | number | 已使用数量（已开启该监控能力的直播间数） |
| remainingQuantity | number | 剩余可用数量 = `max(totalQuantity - useQuantity, 0)`。`0` 表示已耗尽 |

**code 枚举值**

| code | name | 含义 |
|---|---|---|
| `scriptQualityNum` | 话术质检监控位 | 话术质检能力授权量 |
| `scriptFidelityNum` | 话术还原度监控位 | 话术还原度监控能力授权量 |
| `interactionPatrolNum` | 互动巡检监控位 | 互动巡检能力授权量 |

**前端判断逻辑说明**

| 状态 | 条件 | 含义 | 前端用途 |
|---|---|---|---|
| `hasAuth` | `totalQuantity > 0` | 套餐内有该能力 | 判断功能入口是否展示 |
| `hasSurplus` | `remainingQuantity > 0` | 还有剩余可用量 | 开关开启（0→1）前必须满足此条件，否则后端抛 70002 |
| 已耗尽 | `totalQuantity > 0 && remainingQuantity == 0` | 有授权但已全部占用 | 开关灰显或提示"监控位已用完" |
| 未购买 | `totalQuantity == 0` | 套餐未包含该能力 | 隐藏或禁用对应功能入口 |

> **子账号说明**：三类监控位均为租户共享（`sub_account_have=0`），子账号调用时后端自动回退到主账号的资产数据，前端无需区分主子账号。

### 错误码

本接口无特定业务错误码，失败时返回通用错误（如 `401` 未登录）。完整错误码 → [_error_codes.md](./_error_codes.md)

### 示例响应

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

## 变更历史

- 2026-06-02：初次发布 B5 两个接口（forward mode，openspec: `.claude/runs/Change__2026-06-02_b5-monitor-position/openspec.md`）
