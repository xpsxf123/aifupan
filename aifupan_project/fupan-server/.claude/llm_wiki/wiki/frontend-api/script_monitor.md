<!-- LIBRARIAN-NOTE 2026-06-08: 行数 972 > 500 阈值（linter WARN）。推迟拆分原因：B3 接口（generateStandardScript / confirmStandardScript / standardScriptDetail）和 B5 接口（fidelityReportDetail）均为 TBD，实装后文档将大幅重写；拆分后需立即重做，重复返工成本高于收益。建议 B3 + B5 全部实装并稳定后统一按 status-overview / quality-detail / patrol-detail / standard-script / fidelity-detail 五个子领域拆分，届时委派 @knowledge-architect。下次进入此文件的 Librarian 若 B3/B5 仍未落地，可延续此 NOTE。 -->

# 话术智能监控 API

> **ChangeLog**：2026-06-08 删除角色确认机制（`confirmRead` endpoint 下线）+ 已读语义迁移到 report 主表（Standard HIGH，B9 批次）。`QualityReportDetailVo` 删除 `confirmedRecords`，`isRead` 语义来源改为主表字段；`InteractionPatrolReportDetailVo` 新增 `isRead` 字段、`confirmedAt` 语义改为首次查看时间戳（来源主表）；`MonitorTypeStatusVo.isRead` 来源改为主表（前端无感知）。

> 本模块所有响应均符合 [`R<T>` 统一响应封装](./_response_envelope.md)；本文档不重复说明 `code` / `msg` / `data`，**响应 data 结构**指 R\<T\> 内的 T。
>
> 错误码全集 → [./_error_codes.md](./_error_codes.md)
>
> 最后更新：2026-06-08
> 来源：`.claude/runs/Change__2026-06-08_13-17-24/openspec.md`（forward 模式，B9 变更）；原始反向核查来源：`.claude/runs/Change__2026-05-22_script-monitor/openspec.md` + Controller
>
> **实现批次说明**：
> - **B4 已实现**：`reportStatus`、`batchReportStatus`、`triggerReport`、`qualityReportDetail`（共 4 个接口；`confirmRead` 已在 B9 下线）
> - **B9 变更（2026-06-08）**：删除 `confirmRead` endpoint；`qualityReportDetail` 删 `confirmedRecords`；`patrolReportDetail` 新增 `isRead` + 改 `confirmedAt` 来源；`batchReportStatus` / `reportStatus` 的 `monitors[].isRead` 来源改主表
> - **后续批次 TBD**：`anchorBasicConfig`（B4）、`generateStandardScript`（B3）、`confirmStandardScript`（B3）、`standardScriptDetail`（B3）、`fidelityReportDetail`（B5）、`addOrUpdateAnchor` 扩展字段（B3）、`monitorPositionStatistics`（B5）

---

## 接口列表

| # | 方法 | 路径 | 鉴权 | 简述 | 状态 |
|---|---|---|---|---|---|
| 3.1 | GET | /replay/script-monitor/anchorBasicConfig | 登录 | 查询直播间基础配置（含 AI 开关） | **TBD** |
| 3.2 | POST | /replay/script-monitor/batchReportStatus | 登录 | 批量查询报告状态（最多 100 条）；`monitors[].isRead` 来源改主表（B9）| B4 + 2026-06-02 扩字段 + B9 |
| 3.3 | POST | /replay/script-monitor/reportStatus | 登录 | 查询单个资源的三类监控报告状态；`monitors[].isRead` 来源改主表（B9）| B4 + 2026-06-02 改契约 + B9 |
| 3.4 | POST | /replay/script-monitor/triggerReport | 登录 + 防重复提交 | 手动触发报告生成 | B4 已实现 |
| ~~3.5~~ | ~~POST~~ | ~~`/replay/script-monitor/confirmRead`~~ | ~~登录~~ | ~~确认报告已读~~ | **B9 下线（HTTP 404）** |
| 3.6 | GET | /replay/script-monitor/qualityReportDetail | 登录 | 查询质检报告详情；B9 删 `confirmedRecords`，`isRead` 语义改为主表 | B4 + B9 |
| 3.7 | GET | /replay/script-monitor/patrolReportDetail | 登录 | 查询互动巡检报告详情；B9 新增 `isRead` + `confirmedAt` 语义改主表 | B9 |
| 3.8 | POST | /replay/script-monitor/generateStandardScript | 登录 | AI 生成标准稿时间轴（不落库） | **TBD** |
| 3.9 | POST | /replay/script-monitor/confirmStandardScript | 登录 + 防重复提交 | 确认并落库标准稿 | **TBD** |
| 3.10 | GET | /replay/script-monitor/standardScriptDetail | 登录 | 查询当前直播间已确认标准稿 | **TBD** |
| 3.11 | GET | /replay/script-monitor/fidelityReportDetail | 登录 | 查询话术还原度报告详情 | **TBD** |
| 3.12 | POST/PUT | /replay/words/anchorUrl/addOrUpdateAnchor | 登录 | 新增/编辑直播间（含 AI 监控开关扩展字段） | **TBD**（现有接口新增字段）|
| 3.13 | GET | /replay/userproperty/monitorPositionStatistics | 登录 | 查询三类监控位额度统计 | **TBD** |

> **鉴权说明**：所有接口均需登录态（Header `Authorization: Bearer <jwt>`）。`tenantId` / `userId` 全部从登录态获取，前端**不传**这两个字段。

---

## 枚举值说明

### sourceType（资源类型）

| 值 | 含义 |
|---|---|
| 0 | 录制视频 |
| 1 | 上传文件分析 |

### sceneType（业务场景）

| 值 | 含义 | 适用 sourceType |
|---|---|---|
| 0 | 复盘场景 | 0 |
| 1 | 视频分析 | 1 |
| 2 | 文案预审 | 1 |

### monitorType（监控类型）

| 值 | 含义 |
|---|---|
| 0 | 话术质检 |
| 1 | 话术还原度 |
| 2 | 互动巡检 |

### status（报告状态）

| 值 | 含义 | 说明 |
|---|---|---|
| 0 | 未生成 | 可手动触发 |
| 1 | 生成中 | 禁止重复触发 |
| 2 | 已生成 | 可查看、可确认已读 |
| 3 | 生成失败 | 可重试 |
| 4 | 不可生成 | 展示 unavailableReason |

---

## GET /replay/script-monitor/anchorBasicConfig

> **状态：TBD（后续批次实现）**

**描述**：查询直播间基础配置，含三类 AI 监控开关及当前已确认标准稿 ID。

**鉴权**：Header `Authorization: Bearer <jwt>` 必填

**Content-Type**：无请求体（GET）

**请求参数**

| 来源 | 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| Query | anchorUrlUserId | number | Y | 直播间配置 ID（Snowflake，≤ 2^53 安全范围内可用 number；但建议后端统一返回 string，待实现时确认） |

**响应 data 结构**

| 字段 | 类型 | 说明 |
|---|---|---|
| anchorUrlUserId | number | 直播间配置 ID |
| anchorId | number | 主播/直播间 ID |
| secUid | string | 主播唯一标识 |
| anchorName | string | 主播名称 |
| tradeId | number | 行业 ID |
| tradeName | string | 行业名称 |
| accountType | number | 账号归属 0=自有 非0=竞品 |
| livingMode | number | 直播间模式 |
| accountWaterLevel | number | 账号水平 |
| accountFlow | number | 流量结构 |
| recordTime | string | 录制时间 |
| smsTip | number | 上下播提醒 0/1 |
| engSerViceType | string | 默认识别语言 |
| recordDefinition | number | 录制清晰度 |
| recordLimitType | number | 录制形式 |
| recordLimitValue | number | 每段录制时长（分钟）|
| isAutoUploadCloud | number | 自动上传云空间 0/1 |
| isAutoAnalysis | number | 自动分析视频 0/1 |
| isAutoDiagnosis | number | 自动诊断 0/1 |
| isDataDiagnosis | number | 自动数据诊断 0/1 |
| isScriptQualityInspection | number | 话术质检开关 0=关 1=开 |
| isScriptFidelityMonitor | number | 话术还原度开关 0=关 1=开 |
| isInteractionPatrol | number | 互动巡检开关 0=关 1=开 |
| standardScriptId | number \| null | 当前已确认标准稿 ID；无已确认稿时为 null |

---

## POST /replay/script-monitor/batchReportStatus

**描述**：批量查询多个资源的三类监控报告状态。跳过无权限或不存在的资源，不报错。

**鉴权**：Header `Authorization: Bearer <jwt>` 必填

**Content-Type**：`application/json`

**请求参数（Body）**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| sources | SourceItem[] | Y | 非空，最多 100 条 | 资源列表 |

`SourceItem` 对象：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceType | number | Y | 资源类型；枚举见上方 |
| sceneType | number | Y | 业务场景；枚举见上方 |
| sourceId | string | Y | 资源 ID（非空字符串）|
| secUid | string | N | 主播唯一标识；传入后该 source 的 `monitors[].monitorEnabled` 会按 (当前用户, secUid) 查 `tb_anchor_url_user` 三开关；未传 / 上传文件场景 / 查无记录 → `monitorEnabled` 返 null |

**响应 data 结构**：`ScriptMonitorReportStatusVo[]`

返回数组，每个元素对应一个有权限的资源（跳过无权限/不存在的项）。

`ScriptMonitorReportStatusVo` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| sourceType | number | 资源类型 |
| sceneType | number | 业务场景 |
| sourceId | string | 资源 ID |
| monitors | MonitorTypeStatusVo[] | 三类监控状态列表（固定 3 项：质检/还原度/巡检）|

`MonitorTypeStatusVo` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| monitorType | number | 监控类型；枚举见上方 |
| monitorTypeText | string | 监控类型文案，如"话术质检" |
| status | number | 报告状态；枚举见上方 |
| statusText | string | 状态文案，如"已生成" |
| reportId | string（Snowflake）\| null | 报告 ID；未生成时为 null。**字符串解析**，19 位 Snowflake |
| summary | **string \| null** | 报告摘要 **Markdown 文本片段**（**2026-06-07 改造**：所有 monitorType 统一为从 AI 合并报告 `<aifupan-data-block>` 标签内提取的 Markdown 文本，**前端直接渲染、不再 `JSON.parse`**；按 monitorType 走不同文本形态见下方"summary 子结构"）；未生成 / 生成中 / 失败 / 不可生成 / AI 无标签 → null；空标签 → `""` |
| isRead | number | 是否已读 0=未读 1=已读。**B9 起**：来源从 `tb_script_monitor_read`（独立子表）改为 `tb_script_monitor_report.is_read`（主表字段）；语义不变（0/1），**前端无需改动字段读取**。`canConfirm` 字段 **已删除**（B9 之后前端不再需要"是否可确认"语义） |
| unavailableReason | string \| null | 不可生成原因；status=4 时有值 |
| monitorEnabled | number \| null | 该 monitorType 在该直播间的开关状态：0=关 / 1=开 / null=不适用（请求未带 secUid / 上传文件场景 / anchor_url_user 无记录）|

**summary 子结构**（status≠2 时 summary 字符串本身=null；status=2 时按 monitorType 走不同文本形态）：

```js
// 2026-06-07 起：summary 是 Markdown 文本片段，直接渲染，**不再** JSON.parse
// 推荐用 Markdown 渲染器（如 marked / react-markdown）组件展示
const summaryText = monitor.summary;
// 若值为 ""（空标签场景）则展示空内容；null 表示状态不到 2 / 无标签
```

| monitorType | 内容形态 | 说明 |
|---|---|---|
| 0 质检 | Markdown 文本 | 4 行汇总（崩盘 / 摸鱼 / 品牌伤害 / 售后风险话术句数），含 `<font color="red">数字</font>` 富文本格式 |
| 1 还原度 | TBD | B3 上线后定型 |
| 2 巡检 | Markdown 文本 | 概要总结段 3-5 行：互动有效回复率（X.X%）、总单元数、需回复弹幕数、有效 / 无效回复数、无效回复明细列表 |

> **注意**：`summary` 是 Markdown 文本片段（**不是 JSON**）。前端**直接渲染**即可；后端按 `<aifupan-data-block>` 标签提取后 `trim()`，找不到标签 / 内容 null → 字段为 null，空标签 → `""`。所有 monitorType 现已统一此范式（2026-06-07 起）。

**示例请求**

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

**示例响应**

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
          "summary": "{\"crashCount\":0,\"slackCount\":2,\"brandDamageCount\":0,\"afterSalesCount\":1}",
          "isRead": 0,
          "unavailableReason": null,
          "monitorEnabled": 1
        },
        {
          "monitorType": 1,
          "monitorTypeText": "话术还原度",
          "status": 2,
          "statusText": "已生成",
          "reportId": "1234567890123456790",
          "summary": "{\"score\":86,\"speechSpeed\":280,\"deviationSummary\":\"整体节奏接近标准稿，产品利益点讲解略有遗漏\"}",
          "isRead": 0,
          "unavailableReason": null,
          "monitorEnabled": 1
        },
        {
          "monitorType": 2,
          "monitorTypeText": "互动巡检",
          "status": 4,
          "statusText": "不可生成",
          "reportId": null,
          "summary": null,
          "isRead": 0,
          "unavailableReason": "本场无弹幕数据",
          "monitorEnabled": 0
        }
      ]
    }
  ]
}
```

**错误码**

| code | 含义 |
|---|---|
| -9 | sources 为空或超过 100 条 |

---

## POST /replay/script-monitor/reportStatus

**描述**：查询单个资源的三类监控报告状态（质检 / 还原度 / 巡检）。

> **2026-06-02 契约变更**：原 `GET` 改为 `POST + JSON Body`，跟 `batchReportStatus` 风格对齐；新增可选字段 `secUid`，传入后响应每个 monitor 含 `monitorEnabled` 开关状态。

**鉴权**：Header `Authorization: Bearer <jwt>` 必填

**Content-Type**：`application/json`

**请求参数（Body）**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceType | number | Y | 资源类型；枚举见上方 |
| sceneType | number | Y | 业务场景；枚举见上方 |
| sourceId | string | Y | 资源 ID（非空字符串）|
| secUid | string | N | 主播唯一标识；传入后响应 `monitors[].monitorEnabled` 按 (当前用户, secUid) 查 `tb_anchor_url_user` 三开关；未传 / 查无记录 → `monitorEnabled` 返 null |

**响应 data 结构**：`ScriptMonitorReportStatusVo`

字段与 `batchReportStatus` 中的单个元素完全一致，见上方 `ScriptMonitorReportStatusVo` / `MonitorTypeStatusVo` 字段表（含 `monitorEnabled`）。

**示例请求**

```http
POST /replay/script-monitor/reportStatus HTTP/1.1
Authorization: Bearer eyJhbGciOi...
Content-Type: application/json

{
  "sourceType": 0,
  "sceneType": 0,
  "sourceId": "video_abc123",
  "secUid": "MS4wLjABAAAABYa2yd-9BXCD9agZ9SAjTJFfbSgLnd6_NC6SJ1adhEk"
}
```

**示例响应**

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
        "summary": "{\"crashCount\":0,\"slackCount\":2,\"brandDamageCount\":0,\"afterSalesCount\":1}",
        "isRead": 1,
        "unavailableReason": null,
        "monitorEnabled": 1
      }
    ]
  }
}
```

---

## POST /replay/script-monitor/triggerReport

**描述**：手动触发指定资源的某类监控报告生成。生成中状态（status=1）时调用会被防重复提交机制拦截。

**鉴权**：Header `Authorization: Bearer <jwt>` 必填

**防重复提交**：后端基于 `sourceType_sceneType_sourceId_monitorType` 组合键做防重复，短时间内重复点击将返回"请勿重复触发"。

**Content-Type**：`application/json`

**请求参数（Body）**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceType | number | Y | 资源类型；枚举见上方 |
| sceneType | number | Y | 业务场景；枚举见上方 |
| sourceId | string | Y | 资源 ID（非空）|
| monitorType | number | Y | 监控类型；枚举见上方 |

**响应 data 结构**：`boolean`（true 表示触发成功，异步生成）

**触发校验规则**（后端，供前端预判禁用入口）：

| 条件 | 错误码 |
|---|---|
| 非自有账号（accountType≠0）| 70004 |
| AI Token < 100,000 | 70001 |
| 话术还原度（monitorType=1）且无已确认标准稿 | TBD（B3 批次校验）|
| 互动巡检（monitorType=2）且本场无弹幕 | 70010 |
| 报告生成中（status=1）| 防重复提交机制拦截 |

**示例请求**

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

**示例响应**

```json
{
  "code": 0,
  "msg": "success",
  "data": true
}
```

**错误码**

| code | 含义 |
|---|---|
| 70001 | 算力（Token）不足 |
| 70004 | 仅自有账号支持 |
| 70010 | 本场无弹幕（互动巡检）|
| 70014 | 功能未上线 |

---

## ~~POST /replay/script-monitor/confirmRead~~ （B9 下线）

> **此接口已在 B9 批次（2026-06-08）整体删除，调用将返回 HTTP 404。**
>
> 前端必须停止调用此接口，并移除"确认已读"按钮的主动触发逻辑。已读状态现由后端在录制人首次打开 `qualityReportDetail` / `patrolReportDetail` 接口时自动写入。
>
> 同步移除的功能：三角色（运营/主播/主管）已知晓勾选 UI、`confirmRole` 参数。

---

## GET /replay/script-monitor/qualityReportDetail

**描述**：查询话术质检报告详情，含四类负面话术统计、报告正文 JSON、主播信息、已读状态。**B9 起**：录制人首次调用时后端自动将 `is_read` 置 1（无需前端主动调用 `confirmRead`）；三角色已知晓字段（`confirmedRecords`）已删除。

**鉴权**：Header `Authorization: Bearer <jwt>` 必填

**Content-Type**：无请求体（GET）

**请求参数**

| 来源 | 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| Query | reportId | number | Y | 报告 ID（Snowflake，URL Query String 传 number 时精度安全，后端 Long 接收；如有精度顾虑可传字符串）|

**响应 data 结构**：`QualityReportDetailVo`

| 字段 | 类型 | 说明 |
|---|---|---|
| reportId | string（Snowflake）| 报告 ID。**字符串解析**，19 位 Snowflake |
| sourceType | number | 资源类型 |
| sceneType | number | 业务场景 |
| sourceId | string | 资源 ID |
| status | number | 报告状态；枚举见上方 |
| summaryJson | string \| null | **摘要 JSON 字符串**（2026-06-02 改造：原 4 个顶层 count 字段合并到此；前端 `JSON.parse(summaryJson)` 后取 `crashCount/slackCount/brandDamageCount/afterSalesCount` 及可选 `totalNegativeCount/overallScore` 等字段。`reportContent` 已含同样数据可双取一）。status≠2 或 AI 非法 JSON → null |
| reportContent | string \| null | AI 合并报告完整 JSON 字符串（status=2 时有值）；**前端需 JSON.parse** 后使用，结构见下方 |
| anchorName | string | 主播名称 |
| liveTitle | string | 直播标题 |
| liveTime | string | 直播时间（ISO-8601，Asia/Shanghai）|
| isRead | number | 是否已读 0=未读 1=已读。**B9 起**：来源从 `tb_script_monitor_read`（独立子表）改为 `tb_script_monitor_report.is_read`（主表字段）；语义为"录制人首次查看后自动置 1"，**非录制人查看不写**。写失败（DB 抖动）时响应仍为 0，下次打开自动重试 |
| ~~canConfirm~~ | ~~boolean~~ | **B9 已删除**。原"是否可确认"字段；B9 后端在 detail 接口内自动写已读，前端不再需要此字段，对应"已知晓"按钮 UI 一并下线 |
| ~~confirmedRecords~~ | ~~ConfirmedRecordVo[]~~ | **B9 已删除**。原三角色「已知晓」勾选列表不再返回，前端应停止读取此字段并移除对应 UI |
| createDate | string | 报告创建时间（ISO-8601，Asia/Shanghai）|

### reportContent JSON 结构（JSON.parse 后）

`reportContent` 是一个 JSON 字符串，前端必须先 `JSON.parse()` 再使用。解析后结构：

```typescript
interface ReportContent {
  summary: ReportSummary;
  details: ReportDetails;
  summaryHtml: string;              // 整体评估文字摘要（HTML 段落，可直接渲染）
  improvementSuggestions: string[]; // 改进建议列表（3-5 条）
}

interface ReportSummary {
  crashCount: number;           // 崩盘话术句数
  slackCount: number;           // 摸鱼话术句数
  brandDamageCount: number;     // 有损品牌话术句数
  afterSalesCount: number;      // 增加售后风险话术句数
  totalNegativeCount: number;   // 四类负面话术总句数
  totalSentencesAnalyzed: number; // 本场分析的话术总句数
  overallScore: number;         // 整体话术质量评分 (0-100，越高越好)
}

interface ReportDetails {
  crash: DetailItem[];       // 崩盘话术明细列表
  slack: DetailItem[];       // 摸鱼话术明细列表
  brandDamage: DetailItem[]; // 有损品牌话术明细列表
  afterSales: DetailItem[];  // 增加售后风险话术明细列表
}

interface DetailItem {
  timeRange: string;    // 命中时间段标识（如"段落 5"）
  originalText: string; // 原文摘录
  issue: string;        // 问题描述
  severity: string;     // 严重程度："high" | "medium" | "low"
}
```

**字段说明**：

| 字段路径 | 类型 | 说明 |
|---|---|---|
| summary.crashCount | number | 崩盘话术句数（与外层 crashCount 一致）|
| summary.slackCount | number | 摸鱼话术句数（与外层 slackCount 一致）|
| summary.brandDamageCount | number | 有损品牌话术句数（与外层 brandDamageCount 一致）|
| summary.afterSalesCount | number | 增加售后风险话术句数（与外层 afterSalesCount 一致）|
| summary.totalNegativeCount | number | 四类负面话术总句数（≥ 各类之和，AI 合并可能去重）|
| summary.totalSentencesAnalyzed | number | 本场分析话术总句数（分母，用于计算占比）|
| summary.overallScore | number | 整体质量评分 0-100，100 分最优 |
| details.crash | DetailItem[] | 崩盘话术明细，可为空数组 |
| details.slack | DetailItem[] | 摸鱼话术明细，可为空数组 |
| details.brandDamage | DetailItem[] | 有损品牌话术明细，可为空数组 |
| details.afterSales | DetailItem[] | 增加售后风险话术明细，可为空数组 |
| details.\*.timeRange | string | 命中时间段标识，如"段落 5"（非精确时间戳）|
| details.\*.severity | string | "high" \| "medium" \| "low" |
| summaryHtml | string | AI 生成的整体评估 HTML 段落，可直接 innerHTML 渲染 |
| improvementSuggestions | string[] | 3-5 条改进建议，纯文本 |

**示例请求**

```http
GET /replay/script-monitor/qualityReportDetail?reportId=1234567890123456789 HTTP/1.1
Authorization: Bearer eyJhbGciOi...
```

**示例响应**

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
    "summaryJson": "{\"crashCount\":2,\"slackCount\":1,\"brandDamageCount\":0,\"afterSalesCount\":3,\"totalNegativeCount\":6,\"overallScore\":78}",
    "reportContent": "{\"summary\":{\"crashCount\":2,\"slackCount\":1,\"brandDamageCount\":0,\"afterSalesCount\":3,\"totalNegativeCount\":6,\"totalSentencesAnalyzed\":150,\"overallScore\":78},\"details\":{\"crash\":[{\"timeRange\":\"段落 5\",\"originalText\":\"这个产品根本卖不出去\",\"issue\":\"负面定性话术，可能引发观众购买顾虑\",\"severity\":\"high\"}],\"slack\":[],\"brandDamage\":[],\"afterSales\":[{\"timeRange\":\"段落 12\",\"originalText\":\"有问题直接找我退\",\"issue\":\"承诺范围超出平台规则\",\"severity\":\"medium\"}]},\"summaryHtml\":\"<p>本场直播共检测到 6 条负面话术...</p>\",\"improvementSuggestions\":[\"避免使用绝对否定性话术\",\"售后承诺应符合平台规则\"]}",
    "anchorName": "李四",
    "liveTitle": "2026-05-30 直播间",
    "liveTime": "2026-05-30T19:00:00",
    "isRead": 1,
    "createDate": "2026-05-30T19:30:00"
  }
}
```

**错误码**

| code | 含义 |
|---|---|
| 70011 | 无查看权限（跨租户访问）|
| 70006 | 报告不存在或已删除 |

---

## GET /replay/script-monitor/patrolReportDetail

**描述**：查询互动巡检报告详情，含弹幕回复有效性明细、已读状态。**B9 起**：录制人首次调用时后端自动将 `is_read` 置 1；新增 `isRead` 字段；`confirmedAt` 语义来源改为主表 `tb_script_monitor_report.confirmed_at`（首次查看时写入 `NOW()`，重新生成时重置为 null）。

**鉴权**：Header `Authorization: Bearer <jwt>` 必填

**Content-Type**：无请求体（GET）

**请求参数**

| 来源 | 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| Query | reportId | number | Y | 报告 ID（Snowflake，URL Query String 传 number 时精度安全，后端 Long 接收；如有精度顾虑可传字符串）|

**响应 data 结构**：`InteractionPatrolReportDetailVo`

| 字段 | 类型 | 说明 |
|---|---|---|
| reportId | string（Snowflake）| 报告 ID。**字符串解析**，19 位 Snowflake |
| sourceType | number | 资源类型；枚举见上方 |
| sceneType | number | 业务场景；枚举见上方 |
| sourceId | string | 资源 ID |
| status | number | 报告状态；枚举见上方 |
| summaryJson | string \| null | 摘要 Markdown 文本片段（**2026-06-07 改造**：来自 AI 合并报告 `<aifupan-data-block>` 标签内文本 trim 后；前端**直接渲染**，不再需要 `JSON.parse`；字段名历史遗留，实际存 Markdown 文本；未生成 / 无标签时 null；空标签返回 `""`）|
| reportContent | string \| null | 合并报告 Markdown 全文（来自 MongoDB，含弹幕明细表格 + `<aifupan-data-block>` 标签包裹的摘要段；前端直接渲染，不再需要 `JSON.parse`）|
| anchorName | string | 主播名称 |
| liveTitle | string | 直播标题 |
| liveTime | string | 直播时间（ISO-8601，Asia/Shanghai）|
| isRead | number | **B9 新增**。是否已读 0=未读 1=已读。来源：`tb_script_monitor_report.is_read`；仅录制人首次调用 detail 接口时后端自动置 1 |
| ~~canConfirm~~ | ~~boolean~~ | **B9 已删除**。原"是否可确认"字段已下线，B9 后端在 detail 接口内自动写已读 |
| confirmedAt | string（ISO-8601）\| null | **B9 语义改变（BREAKING）**。录制人首次查看时间戳，格式 `YYYY-MM-DDTHH:mm:ss`（Asia/Shanghai）。旧语义：来自 `tb_script_monitor_read.createDate`（独立子表）；**新语义**：来自 `tb_script_monitor_report.confirmed_at`（主表字段），录制人首次打开 detail 接口时写入 `NOW()`，重新触发报告生成时重置为 `null`。**历史已读数据不迁移**：存量已通过旧 `confirmRead` 确认的报告，此字段将重置为 null（不 backfill）|
| createDate | string | 报告创建时间（ISO-8601，Asia/Shanghai）|

> **confirmedAt 历史数据注意**：B9 DDL 上线后，`tb_script_monitor_report.confirmed_at` 初始值均为 `NULL`（`DEFAULT NULL`），历史已通过 `confirmRead` 确认的报告不迁移。前端侧这些报告的 `confirmedAt` 将重新显示为 `null`（未查看），这是已知且已接受的语义后果（产品侧已知悉）。

**示例请求**

```http
GET /replay/script-monitor/patrolReportDetail?reportId=1234567890123456789 HTTP/1.1
Authorization: Bearer eyJhbGciOi...
```

**示例响应**（录制人首次查看，后端自动写已读）

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
    "summaryJson": "互动有效回复率 **78.3%**，总单元数 12，需回复弹幕 36 条，有效回复 28 条，无效回复 8 条。\n\n**无效回复明细**：...",
    "reportContent": "## 互动巡检报告\n\n...",
    "anchorName": "李四",
    "liveTitle": "2026-05-30 直播间",
    "liveTime": "2026-05-30T19:00:00",
    "isRead": 1,
    "confirmedAt": "2026-06-08T10:30:00",
    "createDate": "2026-05-30T19:30:00"
  }
}
```

**错误码**

| code | 含义 |
|---|---|
| 70011 | 无查看权限（跨租户访问）|
| 70006 | 报告不存在或已删除 |

---

## POST /replay/script-monitor/generateStandardScript

> **状态：TBD（B3 批次实现）**

**描述**：调用 AI 生成标准稿时间轴内容。**不落库**，前端暂存后由用户确认再调 `confirmStandardScript`。

**鉴权**：Header `Authorization: Bearer <jwt>` 必填

**Content-Type**：`application/json`

**请求参数（Body）**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| anchorUrlUserId | number \| undefined | 条件 | — | 直播间配置 ID；修改直播间/手动分析场景必填，新增直播间时不传 |
| speechMode | number | Y | 0 或 1 | 话术模式 0=非循环 1=循环 |
| speechSpeed | number | Y | 100-500 | 语速（字/分钟）|
| cycleDurationMinutes | number \| undefined | 条件 | — | 循环话术预估时长（分钟）；speechMode=1 时必填 |
| referenceScript | string | Y | 非空 | 参考直播脚本原文 |

**响应 data 结构**

| 字段 | 类型 | 说明 |
|---|---|---|
| speechMode | number | 话术模式 |
| speechSpeed | number | 语速 |
| cycleDurationMinutes | number \| null | 循环时长 |
| referenceScript | string | 参考脚本原文（回显）|
| timeAxisScript | TimeAxisItem[] | AI 生成的时间轴内容 |

`TimeAxisItem` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| timeRange | string | 时间段，如"00:00-05:00" |
| title | string | 段落标题 |
| content | string | 话术内容 |

> **注意**：响应中**不含** standardScriptId，此接口不落库。

**错误码**

| code | 含义 |
|---|---|
| 70001 | AI Token 不足（生成消耗 Token）|
| 70008 | 标准稿生成失败，请重试 |

---

## POST /replay/script-monitor/confirmStandardScript

> **状态：TBD（B3 批次实现）**

**描述**：将完整标准稿内容落库为「已确认」状态。新增直播间时 anchorUrlUserId 不传；编辑时传入。每次确认会替换当前直播间旧稿（软删除）。

**鉴权**：Header `Authorization: Bearer <jwt>` 必填

**Content-Type**：`application/json`

**请求参数（Body）**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| anchorUrlUserId | number \| undefined | 条件 | — | 直播间配置 ID；修改/手动分析场景必填，新增直播间不传 |
| speechMode | number | Y | 0 或 1 | 话术模式 |
| speechSpeed | number | Y | 100-500 | 语速（字/分钟）|
| cycleDurationMinutes | number \| undefined | 条件 | — | speechMode=1 时必填 |
| referenceScript | string | Y | 非空 | 参考直播脚本原文 |
| timeAxisScript | TimeAxisItem[] | Y | 非空 | 时间轴内容（来自 generateStandardScript 响应，用户可编辑后提交）|

**响应 data 结构**

| 字段 | 类型 | 说明 |
|---|---|---|
| standardScriptId | string（Snowflake）| 已确认标准稿 ID。**字符串解析**，19 位 Snowflake |

**错误码**

| code | 含义 |
|---|---|
| 70001 | AI Token 不足 |
| 70013 | 参数校验失败（speechSpeed 超出 100-500；cycleDurationMinutes 未传但 speechMode=1）|

---

## GET /replay/script-monitor/standardScriptDetail

> **状态：TBD（B3 批次实现）**

**描述**：查询当前直播间已确认标准稿详情。无标准稿时 hasScript=false，speechSpeed 返回默认值 280。草稿不落库，本接口不返回草稿。

**鉴权**：Header `Authorization: Bearer <jwt>` 必填

**Content-Type**：无请求体（GET）

**请求参数**

| 来源 | 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| Query | anchorUrlUserId | number | Y | 直播间配置 ID |

**响应 data 结构**

| 字段 | 类型 | 说明 |
|---|---|---|
| hasScript | boolean | 是否有已确认标准稿 |
| standardScriptId | string（Snowflake）\| null | 标准稿 ID；hasScript=false 时为 null |
| anchorUrlUserId | number \| null | 绑定的直播间 ID；hasScript=false 时为 null |
| speechMode | number \| null | 话术模式 |
| speechSpeed | number | 语速；无标准稿时返回默认值 280 |
| cycleDurationMinutes | number \| null | 循环时长；非循环模式时为 null |
| referenceScript | string \| null | 参考脚本原文 |
| timeAxisScript | TimeAxisItem[] \| null | 时间轴内容 |
| createDate | string \| null | 标准稿确认时间（ISO-8601）|

---

## GET /replay/script-monitor/fidelityReportDetail

> **状态：TBD（B5 批次实现）**

**描述**：查询话术还原度报告详情，含评分、语速、偏差摘要。还原度报告无「已知晓」勾选项，不返回 confirmedRecords。

**鉴权**：Header `Authorization: Bearer <jwt>` 必填

**Content-Type**：无请求体（GET）

**请求参数**

| 来源 | 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| Query | reportId | number | Y | 报告 ID |

**响应 data 结构**

| 字段 | 类型 | 说明 |
|---|---|---|
| reportId | string（Snowflake）| 报告 ID |
| sourceType | number | 资源类型 |
| sceneType | number | 业务场景 |
| sourceId | string | 资源 ID |
| status | number | 报告状态 |
| score | number \| null | 还原度评分 0-100；status=2 时有值 |
| speechSpeed | number \| null | 实际语速（字/分钟）；status=2 时有值 |
| deviationSummary | DeviationSummary \| null | 偏差摘要；status=2 时有值 |
| reportContent | string \| null | 报告正文（结构 TBD，B5 实现时补充）|
| anchorName | string | 主播名称 |
| liveTitle | string | 直播标题 |
| liveTime | string | 直播时间（ISO-8601）|
| isRead | number | 是否已读 |
| ~~canConfirm~~ | ~~boolean~~ | **B9 已删除**（前端不再需要） |
| createDate | string | 报告创建时间（ISO-8601）|

`DeviationSummary` 字段（B5 实现时完整定义）：

| 字段 | 类型 | 说明 |
|---|---|---|
| missedSegments | number | 遗漏段落数 |
| outOfOrderSegments | number | 顺序错乱段落数 |
| deviatedSegments | number | 偏差段落数 |
| freePlaySegments | number | 自由发挥段落数 |

---

## POST/PUT /replay/words/anchorUrl/addOrUpdateAnchor（扩展字段）

> **状态：TBD（B3 批次新增字段）**

**描述**：现有接口新增 AI 监控相关请求字段。完整接口定义见直播间管理模块文档；本节仅说明新增字段。

**新增请求字段**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| isScriptQualityInspection | number \| undefined | N | 0 或 1 | 话术质检开关 |
| isScriptFidelityMonitor | number \| undefined | N | 0 或 1 | 话术还原度开关 |
| isInteractionPatrol | number \| undefined | N | 0 或 1 | 互动巡检开关 |
| standardScriptId | string（Snowflake）\| undefined | 条件 | — | isScriptFidelityMonitor=1 时必填，且必须为已确认标准稿 |

**相关错误码**

| code | 含义 |
|---|---|
| 70001 | AI Token 不足（开启任一监控时检查）|
| 70002 | 授权数量不足（开启对应功能时检查）|
| 70003 | 当前套餐不支持 |
| 70005 | 标准直播稿未确认（开启还原度时）|
| 70006 | 标准直播稿无效 |
| 70007 | 已开启 AI 监控，不能将自有账号改为竞品账号 |

---

## GET /replay/userproperty/monitorPositionStatistics

> **状态：TBD（B5 批次实现）**

**描述**：查询三类 AI 监控位额度统计（租户维度，从登录态获取 userId 和 tenantId，前端不传）。

**鉴权**：Header `Authorization: Bearer <jwt>` 必填

**Content-Type**：无请求体（GET）

**请求参数**：无（后端从登录态获取）

**响应 data 结构**

| 字段 | 类型 | 说明 |
|---|---|---|
| userId | string（Snowflake）| 当前用户 ID |
| tenantId | string（Snowflake）| 租户 ID |
| monitorPositions | MonitorPositionVo[] | 三类监控位额度列表（固定 3 项）|

`MonitorPositionVo` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| code | string | 资产 code：`scriptQualityInspectionNum` / `scriptFidelityMonitorNum` / `interactionPatrolNum` |
| name | string | 资产名称，如"话术质检监控位" |
| scope | string | 共享范围：`TENANT_SHARED`（租户共享）|
| scopeName | string | 共享范围文案，如"租户共享" |
| totalQuantity | number | 总授权量 |
| useQuantity | number | 已使用量 |
| remainingQuantity | number | 剩余量（= max(totalQuantity - useQuantity, 0)）|

**示例响应**

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "userId": "1000000000000000001",
    "tenantId": "1000000000000000002",
    "monitorPositions": [
      {
        "code": "scriptQualityInspectionNum",
        "name": "话术质检监控位",
        "scope": "TENANT_SHARED",
        "scopeName": "租户共享",
        "totalQuantity": 10,
        "useQuantity": 3,
        "remainingQuantity": 7
      },
      {
        "code": "scriptFidelityMonitorNum",
        "name": "话术还原度监控位",
        "scope": "TENANT_SHARED",
        "scopeName": "租户共享",
        "totalQuantity": 5,
        "useQuantity": 2,
        "remainingQuantity": 3
      },
      {
        "code": "interactionPatrolNum",
        "name": "互动巡检监控位",
        "scope": "TENANT_SHARED",
        "scopeName": "租户共享",
        "totalQuantity": 5,
        "useQuantity": 0,
        "remainingQuantity": 5
      }
    ]
  }
}
```

---

## 模块错误码汇总

| code | 含义 | 前端处理建议 |
|---|---|---|
| 70001 | 算力（AI Token）不足 | 提示购买，禁止触发操作 |
| 70002 | 授权数量不足 | 提示购买 |
| 70003 | 套餐不支持 | 提示升级 |
| 70004 | 仅自有账号支持 | 前端隐藏入口；后端兜底 |
| 70005 | 标准直播稿未确认 | 引导用户先确认标准稿 |
| 70006 | 报告不存在或已删除（detail 接口）/ 标准直播稿无效（监控开关接口）| 按接口上下文提示 |
| 70007 | 已开启 AI 监控，不可切换账号归属 | 提示先关闭 AI 监控 |
| 70008 | 标准稿 AI 生成失败 | 提示重试 |
| 70010 | 本场无弹幕数据 | 不展示互动巡检入口 |
| 70011 | 无权限（跨租户访问 / 非录制人）| 只读展示，隐藏操作按钮 |
| 70013 | 参数校验失败 | 检查请求参数 |
| 70014 | 功能未上线 | 隐藏对应入口 |

完整错误码说明 → [./_error_codes.md](./_error_codes.md)

---

## 变更历史

- 2026-06-08：B9 批次 — 删除角色确认机制 + 已读语义迁移到 report 主表（Standard HIGH）
  - `POST /confirmRead` endpoint 下线（HTTP 404），前端停止调用
  - `qualityReportDetail` 响应删除 `confirmedRecords` 字段；`isRead` 语义改为"录制人首次查看自动置 1"（来源改主表）
  - `patrolReportDetail` 新增 `isRead` 字段；`confirmedAt` 语义改为首次查看时间戳（来源改主表 `confirmed_at`）；历史已读数据不 backfill
  - `batchReportStatus` / `reportStatus` 响应 `monitors[].isRead` 来源改为主表，前端无感知
  - 删除 `confirmRole` 枚举说明（`ConfirmRoleEnum` 整体下线）
  来源：`.claude/runs/Change__2026-06-08_13-17-24/openspec.md`（forward 模式）
- 2026-05-30：初次发布（reverse 模式，B4 已实现接口 5 个；其余 8 个标 TBD）
  来源：`.claude/runs/Change__2026-05-22_script-monitor/openspec.md` + `ScriptMonitorController.java` + BO/VO 字段核查
