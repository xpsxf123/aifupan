# 话术还原度 API（前端对接稿）

> 直播复盘 / 云空间场景的"话术还原度"功能完整接口集。
> 本模块所有响应均符合 [`R<T>` 统一响应封装](./_response_envelope.md)；本文档不重复说明 `code` / `msg` / `data`，**响应 data 结构**指 `R<T>` 内的 `T`。
> Snowflake ID（19 位）→ 前端必须用 `string` 接收，禁 `number`。后端 JacksonSerializerConfig 已全局序列化为 String。
>
> 错误码全集 → [./_error_codes.md](./_error_codes.md)
>
> 来源：`replay-api/.../ScriptMonitorController.java` + `StandardScriptController.java` + 各 BO/VO 反向扫描。
> 实现状态：✅ 全部接口已实装（Slice A 配置 + Slice B 触发/状态 + Slice C 详情）。

---

## 接口列表

| # | 方法 | 路径 | 鉴权 | 简述 |
|---|---|---|---|---|
| 1 | GET | `/replay/script-monitor/anchorBasicConfig` | 登录 | 查直播间基础配置（含还原度开关、标准稿状态） |
| 2 | POST | `/replay/script-monitor/setMonitorEnabled` | 登录 + 防重 | 切换还原度开关（monitorType=1） |
| 3 | POST | `/replay/script-monitor/generateStandardScript` | 登录 + 防重 | AI 生成标准稿时间轴（不落库） |
| 4 | POST | `/replay/script-monitor/confirmStandardScript` | 登录 + 防重 | 确认并落库标准稿 |
| 5 | GET | `/replay/script-monitor/standardScriptDetail` | 登录 | 查已确认标准稿 |
| 6 | POST | `/replay/script-monitor/triggerReport` | 登录 + 防重 | 手动触发还原度报告（monitorType=1） |
| 7 | POST | `/replay/script-monitor/reportStatus` | 登录 | 查单资源 3 类报告状态（含还原度 monitor） |
| 8 | POST | `/replay/script-monitor/batchReportStatus` | 登录 | 批量查报告状态（最多 100） |
| 9 | GET | `/replay/script-monitor/fidelityReportDetail` | 登录 | 查还原度报告详情 |

> **鉴权**：所有接口需 `Authorization: Bearer <jwt>`；`tenantId` / `userId` 从 JWT 取，前端**不传**。

---

## 枚举值

### sourceType（资源类型）

| 值 | 含义 | 还原度是否支持 |
|---|---|---|
| 0 | 录制视频 | ✅ |
| 1 | 上传文件分析 | ❌ 不支持，前端不展示入口；后端 `triggerReport` 触发会抛 70014 |

### sceneType（业务场景）

| 值 | 含义 | 还原度是否支持 |
|---|---|---|
| 0 | 复盘场景（录制视频）| ✅ |
| 1 | 视频分析（上传文件）| ❌ 不展示 |
| 2 | 文案预审（上传文件）| ❌ 不展示 |

### monitorType（监控类型）

| 值 | 含义 |
|---|---|
| 0 | 话术质检 |
| 1 | **话术还原度（本模块）** |
| 2 | 互动巡检 |

### status（报告状态）

| 值 | 含义 | 说明 |
|---|---|---|
| 0 | 未生成 | 可手动触发 |
| 1 | 生成中 | 禁止重复触发（防重提交拦截）|
| 2 | 已生成 | 可查详情 |
| 3 | 生成失败 | 可重试 |
| 4 | 不可生成 | `unavailableReason` 展示原因 |

### speechMode（话术模式）

| 值 | 含义 |
|---|---|
| 0 | 非循环话术（有时间轴顺序）|
| 1 | 循环话术（无固定顺序，按 `cycleDurationMinutes` 循环）|

---

## 1. GET `/replay/script-monitor/anchorBasicConfig`

**描述**：按当前登录用户 + 租户 + `secUid` 查 `tb_anchor_url_user`，返回直播间 3 类监控开关 + 标准稿状态。

**请求参数**

| 来源 | 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| Query | secUid | string | Y | 主播唯一标识 |

**响应 data 结构**（`AnchorUrlUserVo`，本模块只用以下还原度相关字段）

| 字段 | 类型 | 说明 |
|---|---|---|
| anchorUrlUserId | string（Snowflake）| 直播间配置 ID |
| secUid | string | 主播唯一标识 |
| anchorName | string | 主播名称 |
| isScriptQualityInspection | number | 话术质检开关 0/1 |
| **isScriptFidelityMonitor** | number | **话术还原度开关 0/1** |
| isInteractionPatrol | number | 互动巡检开关 0/1 |
| standardScriptId | string（Snowflake）\| null | 当前已确认标准稿 ID；无稿为 null |
| ... | ... | 其他直播间字段同 `anchorUrlUserVo` 主体 |

**示例响应**

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "anchorUrlUserId": "1234567890123456789",
    "secUid": "MS4wLjABAAAA...",
    "anchorName": "李四",
    "isScriptQualityInspection": 1,
    "isScriptFidelityMonitor": 1,
    "isInteractionPatrol": 0,
    "standardScriptId": "1234567890123456790"
  }
}
```

---

## 2. POST `/replay/script-monitor/setMonitorEnabled`

**描述**：按 `(secUid, monitorType, enabled)` 切单能力开关。还原度时 `monitorType=1`。当前状态==目标状态时幂等不写库。

**防重**：`secUid_monitorType_enabled_token`。

**请求参数（Query / Form）**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| secUid | string | Y | 非空 | 主播唯一标识 |
| monitorType | number | Y | 0/1/2 | 还原度传 `1` |
| enabled | number | Y | 0/1 | 目标状态 |

**响应 data 结构**：`boolean`（`true` = 切换成功，含幂等）

**示例**

```http
POST /replay/script-monitor/setMonitorEnabled?secUid=MS4wLjABAAAA...&monitorType=1&enabled=1
Authorization: Bearer <jwt>
```

```json
{ "code": 0, "msg": "success", "data": true }
```

**错误码**

| code | 含义 |
|---|---|
| 70001 | 算力（Token）不足，无法开启 |
| 70002 | 授权数量不足 |
| 70005 | 开启还原度前未确认标准稿 |
| 70011 | 主播 secUid 与当前用户不匹配 |

---

## 3. POST `/replay/script-monitor/generateStandardScript`

**描述**：调 AI 同步生成标准稿时间轴（60s 超时）。**不落库**，前端暂存后由用户编辑确认再调 [接口 4](#4-post-replayscript-monitorconfirmstandardscript) 入库。

**防重**：`generateStandardScript` 全局 key。

**请求参数（Body / JSON）**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| speechMode | number | Y | 0/1 | 0=非循环 / 1=循环 |
| speechSpeed | number | Y | 100-500 | 语速（字/分钟）|
| cycleDurationMinutes | number | 条件 | speechMode=1 时必填 | 循环话术预估时长（分钟）|
| referenceScript | string | Y | 非空 | 参考直播脚本原文 |

**响应 data 结构**：`StandardScriptVo`

| 字段 | 类型 | 说明 |
|---|---|---|
| speechMode | number | 话术模式（回显）|
| speechSpeed | number | 语速（回显）|
| cycleDurationMinutes | number \| null | 循环时长；speechMode=0 时为 null |
| referenceScript | string | 参考脚本原文（回显）|
| timeAxisScript | TimeAxisItemVo[] | AI 生成的时间轴列表 |

`TimeAxisItemVo`：

| 字段 | 类型 | 说明 |
|---|---|---|
| timeRange | string | 时间段，如 `"00:00-05:00"` |
| title | string | 段落标题 |
| content | string | 话术内容 |

> **注意**：响应**不含** `standardScriptId`，本接口不落库。

**示例请求**

```http
POST /replay/script-monitor/generateStandardScript HTTP/1.1
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "speechMode": 0,
  "speechSpeed": 280,
  "cycleDurationMinutes": null,
  "referenceScript": "大家好，欢迎来到直播间，我们今天给大家带来..."
}
```

**示例响应**

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "speechMode": 0,
    "speechSpeed": 280,
    "cycleDurationMinutes": null,
    "referenceScript": "大家好，欢迎来到直播间，我们今天给大家带来...",
    "timeAxisScript": [
      {
        "timeRange": "00:00-03:00",
        "title": "开场介绍",
        "content": "大家好，欢迎进入直播间，今天我们准备了..."
      },
      {
        "timeRange": "03:00-08:00",
        "title": "产品讲解 1 — 核心利益点",
        "content": "首先介绍我们的明星产品..."
      },
      {
        "timeRange": "08:00-12:00",
        "title": "互动答疑",
        "content": "看到很多家人在问..."
      }
    ]
  }
}
```

**错误码**

| code | 含义 |
|---|---|
| 70001 | AI Token 不足（生成消耗 Token）|
| 70008 | AI 生成失败（超时 / 模型异常），可重试 |
| 70013 | 参数校验失败（speechMode=1 但 cycleDurationMinutes 缺）|

---

## 4. POST `/replay/script-monitor/confirmStandardScript`

**描述**：把完整标准稿落库为"已确认"状态。按 `(tenantId+userId+secUid)` 联合定位：先软删旧稿（`is_deleted=1`），再 INSERT 新稿。

**防重**：`confirmStandardScript` 全局 key。

**请求参数（Body / JSON）**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| secUid | string | Y | 非空 | 主播唯一标识（落库主定位字段）|
| speechMode | number | Y | 0/1 | 话术模式 |
| speechSpeed | number | Y | 100-500 | 语速 |
| cycleDurationMinutes | number | 条件 | speechMode=1 时必填 | 循环时长 |
| referenceScript | string | Y | 非空 | 参考脚本原文 |
| timeAxisScript | TimeAxisItemBo[] | Y | 非空 | 时间轴列表（来自接口 3 响应，用户可编辑后提交）|

`TimeAxisItemBo`（同 `TimeAxisItemVo` 结构，全部 `@NotBlank`）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| timeRange | string | Y | 时间段 |
| title | string | Y | 段落标题 |
| content | string | Y | 话术内容 |

**响应 data 结构**：`StandardScriptConfirmVo`

| 字段 | 类型 | 说明 |
|---|---|---|
| standardScriptId | string（Snowflake）| 新落库标准稿 ID |

**示例响应**

```json
{
  "code": 0,
  "msg": "success",
  "data": { "standardScriptId": "1234567890123456790" }
}
```

**错误码**

| code | 含义 |
|---|---|
| 70013 | 参数校验失败 |

---

## 5. GET `/replay/script-monitor/standardScriptDetail`

**描述**：按 `(tenantId+userId+secUid)` 查当前直播间已确认标准稿。无稿时 `hasScript=false`，`speechSpeed` 返回默认值 `280`，其余字段为 `null`。

**请求参数**

| 来源 | 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| Query | secUid | string | Y | 主播唯一标识 |

**响应 data 结构**：`StandardScriptDetailVo`

| 字段 | 类型 | 说明 |
|---|---|---|
| hasScript | boolean | 是否有已确认稿 |
| standardScriptId | string（Snowflake）\| null | 标准稿 ID；无稿时 null |
| secUid | string \| null | 主播唯一标识（回显）|
| speechMode | number \| null | 话术模式 |
| speechSpeed | number | 语速；**无稿时返回默认值 280** |
| cycleDurationMinutes | number \| null | 循环时长 |
| referenceScript | string \| null | 参考脚本原文 |
| timeAxisScript | TimeAxisItemVo[] \| null | 时间轴列表 |
| createDate | string \| null | 标准稿确认时间（ISO-8601）|

**示例响应（有稿）**

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "hasScript": true,
    "standardScriptId": "1234567890123456790",
    "secUid": "MS4wLjABAAAA...",
    "speechMode": 0,
    "speechSpeed": 280,
    "cycleDurationMinutes": null,
    "referenceScript": "大家好，欢迎来到直播间...",
    "timeAxisScript": [
      { "timeRange": "00:00-03:00", "title": "开场介绍", "content": "大家好..." },
      { "timeRange": "03:00-08:00", "title": "产品讲解", "content": "首先..." }
    ],
    "createDate": "2026-06-11T15:30:00"
  }
}
```

**示例响应（无稿）**

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "hasScript": false,
    "standardScriptId": null,
    "secUid": null,
    "speechMode": null,
    "speechSpeed": 280,
    "cycleDurationMinutes": null,
    "referenceScript": null,
    "timeAxisScript": null,
    "createDate": null
  }
}
```

---

## 6. POST `/replay/script-monitor/triggerReport`

**描述**：手动触发指定资源的还原度报告生成。`monitorType=1` + `sourceType=0`（仅录制视频）。

**防重**：`sourceType_sceneType_sourceId_monitorType_token` 组合 key，短时间内重复点击返回"请勿重复触发"。

**请求参数（Body / JSON）**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceType | number | Y | 还原度传 `0`（录制视频）；传 `1` 抛 70014 |
| sceneType | number | Y | 传 `0`（复盘）|
| sourceId | string | Y | 视频 ID |
| monitorType | number | Y | 还原度传 `1` |

**响应 data 结构**：`boolean`（`true` = 异步触发成功）

**触发前置校验**（按顺序）：

| 条件 | 错误码 |
|---|---|
| 资源不存在或无权限 | 70011 |
| 非自有账号（accountType ≠ 0）| 70004 |
| AI Token 不足 | 70001 |
| 还原度且无已确认标准稿 | 70005 |
| 还原度授权数量不足 | 70002 |
| 还原度且 `sourceType=1`（上传文件）| 70014 |
| 报告生成中（status=1）| 防重提交拦截 |

**示例请求**

```http
POST /replay/script-monitor/triggerReport HTTP/1.1
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "sourceType": 0,
  "sceneType": 0,
  "sourceId": "video_abc123",
  "monitorType": 1
}
```

**示例响应**

```json
{ "code": 0, "msg": "success", "data": true }
```

---

## 7. POST `/replay/script-monitor/reportStatus`

**描述**：查单资源的 3 类监控状态。**对还原度功能：响应数组里 `monitorType=1` 的那一项即还原度状态**。

**请求参数（Body / JSON）**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceType | number | Y | 资源类型 |
| sceneType | number | Y | 业务场景 |
| sourceId | string | Y | 资源 ID |
| secUid | string | N | 主播唯一标识；传入后 `monitors[].monitorEnabled` 按 `(userId, tenantId, secUid)` 查 `tb_anchor_url_user` 三开关 |

**响应 data 结构**：`ScriptMonitorReportStatusVo`

| 字段 | 类型 | 说明 |
|---|---|---|
| sourceType | number | 回显 |
| sceneType | number | 回显 |
| sourceId | string | 回显 |
| monitors | MonitorTypeStatusVo[] | 3 类监控状态（固定 3 项：质检/还原度/巡检）|

`MonitorTypeStatusVo`（还原度的那一项）：

| 字段 | 类型 | 说明 |
|---|---|---|
| monitorType | number | `1`（还原度）|
| monitorTypeText | string | `"话术还原度"` |
| status | number | 报告状态 0/1/2/3/4 |
| statusText | string | 状态文案 |
| reportId | string（Snowflake）\| null | 报告 ID；未生成时 null |
| summary | string \| null | 摘要 **Markdown 文本片段**（从 AI 合并报告 `<aifupan-data-block>` 标签内提取 + `trim()`）；status≠2 或 AI 无标签 → null；空标签 → `""` |
| isRead | number | 是否已读 0/1（来源 `tb_script_monitor_report.is_read`）|
| unavailableReason | string \| null | 不可生成原因；status=4 时有值 |
| monitorEnabled | number \| null | 开关状态 0/1/null（请求未带 secUid / 上传文件场景 / 无记录 → null）|

> **summary 渲染说明**：还原度 `summary` 是 Markdown 文本片段（**不是 JSON**），前端**直接用 Markdown 渲染器渲染**，禁 `JSON.parse`。该范式与互动巡检一致。

**示例响应**（只展示 monitors 中的还原度项）

```json
{
  "monitorType": 1,
  "monitorTypeText": "话术还原度",
  "status": 2,
  "statusText": "已生成",
  "reportId": "1234567890123456791",
  "summary": "本场还原度评估：整体节奏接近标准稿，**产品利益点讲解略有遗漏**。\n\n- 命中段落 8\n- 遗漏段落 2\n- 自由发挥 1",
  "isRead": 0,
  "unavailableReason": null,
  "monitorEnabled": 1
}
```

---

## 8. POST `/replay/script-monitor/batchReportStatus`

**描述**：批量查询，最多 100 条；跳过无权限 / 不存在的资源不报错。

**请求参数（Body / JSON）**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| sources | SourceItem[] | Y | 非空，最多 100 | 资源列表 |

`SourceItem`：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceType | number | Y | 资源类型 |
| sceneType | number | Y | 业务场景 |
| sourceId | string | Y | 资源 ID |
| secUid | string | N | 同接口 7 |

**响应 data 结构**：`ScriptMonitorReportStatusVo[]`，每个元素同接口 7。

**示例请求**

```json
{
  "sources": [
    { "sourceType": 0, "sceneType": 0, "sourceId": "video_abc", "secUid": "MS4wLjABAAAA..." },
    { "sourceType": 0, "sceneType": 0, "sourceId": "video_xyz" }
  ]
}
```

---

## 9. GET `/replay/script-monitor/fidelityReportDetail`

**描述**：查还原度报告详情。**仅支持 `sourceType=0` 录制视频**。录制人首次查看时后端 fail-safe 独立事务自动写已读（`is_read=1` + `confirmed_at=NOW()`），非录制人查看不写。跨租户访问抛 70011；同租户可读。

**请求参数**

| 来源 | 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| Query | reportId | number | Y | 报告 ID（Snowflake，URL 上传 number 精度安全；如有顾虑可改 string）|

**响应 data 结构**：`FidelityReportDetailVo`（13 字段 1:1 mirror `QualityReportDetailVo`）

| 字段 | 类型 | 说明 |
|---|---|---|
| reportId | string（Snowflake）| 报告 ID |
| sourceType | number | 资源类型（固定 0）|
| sceneType | number | 业务场景（固定 0）|
| sourceId | string | 视频 ID |
| status | number | 报告状态 0/1/2/3/4 |
| summaryJson | string \| null | **摘要 Markdown 文本**（字段名为历史命名，实际存 Markdown，与互动巡检同范式）；从 AI 合并报告 `<aifupan-data-block>` 标签提取 + `trim()`；status≠2 / 无标签 → null；空标签 → `""` |
| reportContent | string \| null | 报告正文 Markdown 全文（来自 MongoDB）；status=2 时有值；前端直接渲染 |
| anchorName | string | 主播名称 |
| liveTitle | string | 直播标题 |
| liveTime | string（ISO-8601）| 直播录制开始时间 |
| isRead | number | 是否已读 0/1（来源 `tb_script_monitor_report.is_read`，仅录制人首次查看 detail 时后端自动置 1）|
| confirmedAt | string（ISO-8601）\| null | 录制人首次查看时间戳（来源主表 `confirmed_at`；未读时 null；重新触发生成会重置为 null）|
| createDate | string（ISO-8601）| 报告创建时间 |

> **重要：还原度报告无 `score` / `speechSpeed` / `deviationSummary` 等专属数值字段**。设计上"还原度评分 / 偏差摘要 / 实际语速"等信息已包含在 `summaryJson`（摘要段）+ `reportContent`（正文 Markdown 表格 + 段落明细）里。前端按 Markdown 渲染即可，不需要单独取数。

**示例请求**

```http
GET /replay/script-monitor/fidelityReportDetail?reportId=1234567890123456791 HTTP/1.1
Authorization: Bearer <jwt>
```

**示例响应（status=2 已生成）**

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "reportId": "1234567890123456791",
    "sourceType": 0,
    "sceneType": 0,
    "sourceId": "video_abc123",
    "status": 2,
    "summaryJson": "本场还原度评估：整体节奏接近标准稿，**产品利益点讲解略有遗漏**。\n\n- 命中段落 8\n- 遗漏段落 2\n- 自由发挥 1\n- 实际语速 285 字/分钟（标准 280）",
    "reportContent": "## 话术还原度报告\n\n### 一、整体评估\n\n本场直播总体还原度 **86 分**，节奏接近标准稿...\n\n### 二、段落对照\n\n| 段落 | 标准稿 | 实际话术 | 还原度 |\n|---|---|---|---|\n| 开场介绍 | 大家好，欢迎... | 大家好，欢迎进入... | 95% 高度还原 |\n| 产品讲解 1 | 首先介绍我们的... | 这款产品的核心是... | 78% 略有遗漏 |\n| 互动答疑 | （未涉及）| 看到很多家人在问... | 自由发挥 |\n\n### 三、改进建议\n\n1. 产品利益点 \"3年质保\" 未提及，建议加入...\n2. 优惠活动顺序略有调整，影响对比度...\n\n<aifupan-data-block>\n本场还原度评估：整体节奏接近标准稿...\n</aifupan-data-block>",
    "anchorName": "李四",
    "liveTitle": "2026-06-13 直播间",
    "liveTime": "2026-06-13T19:00:00",
    "isRead": 1,
    "confirmedAt": "2026-06-13T20:15:00",
    "createDate": "2026-06-13T19:35:00"
  }
}
```

**示例响应（status=1 生成中 / status=0 未生成）**

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "reportId": "1234567890123456791",
    "sourceType": 0,
    "sceneType": 0,
    "sourceId": "video_abc123",
    "status": 1,
    "summaryJson": null,
    "reportContent": null,
    "anchorName": "李四",
    "liveTitle": "2026-06-13 直播间",
    "liveTime": "2026-06-13T19:00:00",
    "isRead": 0,
    "confirmedAt": null,
    "createDate": "2026-06-13T19:30:00"
  }
}
```

**示例响应（status=3 生成失败）**

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "reportId": "1234567890123456791",
    "sourceType": 0,
    "sceneType": 0,
    "sourceId": "video_abc123",
    "status": 3,
    "summaryJson": null,
    "reportContent": null,
    "anchorName": "李四",
    "liveTitle": "2026-06-13 直播间",
    "liveTime": "2026-06-13T19:00:00",
    "isRead": 0,
    "confirmedAt": null,
    "createDate": "2026-06-13T19:30:00"
  }
}
```

**错误码**

| code | 含义 |
|---|---|
| 70011 | 无查看权限（跨租户）|
| 70012 | 报告不存在 / 已删除 |

---

## 还原度功能完整使用流程（前端流程图）

```
直播间编辑                 标准稿管理                  视频复盘
─────────────             ─────────────              ─────────────
开还原度开关               1. generateStandardScript   1. 自动 / 手动触发
  ↓                          (AI 同步, 不落库)            triggerReport(monitorType=1)
查 anchorBasicConfig       2. 用户编辑 timeAxisScript    ↓
  ↓                       3. confirmStandardScript     report.status = 1 生成中
没有标准稿 → 70005           ↓                          ↓
  ↓                       4. standardScriptDetail      （AI 串行 4 次，约 60s）
跳标准稿管理 →                查到 standardScriptId      ↓
                                                       report.status = 2 已生成
                                                        ↓
                                                       前端轮询 reportStatus
                                                       拿到 monitorType=1 的 summary（Markdown）
                                                        ↓
                                                       点开详情
                                                       fidelityReportDetail(reportId)
                                                       渲染 reportContent Markdown 全文
```

---

## 还原度专属错误码速查

| code | 含义 | 前端处理 |
|---|---|---|
| 70001 | AI Token 不足 | 提示"算力不足请购买"，禁触发 |
| 70002 | 还原度授权数量不足 | 提示"授权不足请购买" |
| 70004 | 非自有账号 | 隐藏入口；后端兜底 |
| 70005 | 未确认标准稿 | 跳标准稿管理页 |
| 70011 | 无权限（跨租户）| 只读展示，隐藏操作按钮 |
| 70012 | 报告不存在 / 已删除 | 提示"报告已删除，请刷新列表" |
| 70013 | 参数校验失败 | 检查请求参数 |
| 70014 | 功能未上线（含上传文件场景）| 隐藏入口 |

完整错误码 → [./_error_codes.md](./_error_codes.md)

---

## 变更历史

- 2026-06-13：还原度功能首次发布（Slice A 配置 + Slice B 触发/状态 + Slice C 详情 + PATCH #15 提示词 JSON 改造 + PATCH #16 红线落地）。
  - 实装接口：`generateStandardScript` / `confirmStandardScript` / `standardScriptDetail` / `triggerReport(monitorType=1)` / `fidelityReportDetail` / `setMonitorEnabled`
  - 复用接口：`reportStatus` / `batchReportStatus` 已含还原度（`monitors[1]`）
  - 已确认范围缩减：仅支持 `sourceType=0` 录制视频；视频分析 / 文案预审入口前端不展示
  - 来源：`.claude/llm_wiki/archive/Change__2026-06-11_12-33-48__restoreDegree-slice-a__openspec.md` + `Change__2026-06-11_18-57-48__restoreDegree-slice-b__openspec.md` + `Change__2026-06-12_23-27-59__restoreDegree-slice-c__openspec.md`
