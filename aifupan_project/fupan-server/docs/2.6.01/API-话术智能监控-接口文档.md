# 话术智能监控 - 接口契约文档 v2.0

> **版本**: 2.60.5
> **生成日期**: 2026-06-08
> **基准代码**: feature/2.6.01 HEAD `a65052552`
> **覆盖范围**: 服务端 ↔ 客户端前端 / 管理后台前端
> **对应 Controller**: `replay-api/src/main/java/com/jiuyu/replay/api/controller/words/ScriptMonitorController.java`

---

## ChangeLog

### v2.0（2026-06-08）— 全面对齐当前实现

**重大变更**（对应 git 历史 `b7821975a` + `a2553e8e7`）：

1. **互动巡检合并报告：JSON → Markdown 标签**
   - `InteractionPatrolReportDetailVo.summaryJson` / `reportContent` 由 JSON 字符串改为 **Markdown 文本片段**，前端**直接渲染、不再 `JSON.parse`**
   - `MonitorTypeStatusVo.summary`（巡检维度）同样改 Markdown
   - 摘要来源：AI 合并报告内 `<aifupan-data-block>` 标签包裹的 Markdown 段

2. **已读语义迁移到 report 主表 + 删确认机制**
   - **删除**接口：`POST /confirmRead`（HTTP 404）
   - **删除**字段：所有 VO 中的 `canConfirm`；`QualityReportDetailVo.confirmedRecords`
   - **新增**字段：`InteractionPatrolReportDetailVo.isRead`
   - **来源变更**：
     - `qualityReportDetail` / `patrolReportDetail` / `batchReportStatus` / `reportStatus` 的 `isRead` 全部来自主表 `tb_script_monitor_report.is_read`
     - `patrolReportDetail.confirmedAt` 来自主表 `tb_script_monitor_report.confirmed_at`
   - **业务流程**：录制人首次打开 detail 接口 → 后端自动写已读（无需前端调用任何接口）
   - **数据库变更**：DROP `tb_script_monitor_read` + `tb_script_monitor_role_confirm`；`tb_script_monitor_report` 新增 `is_read TINYINT NOT NULL DEFAULT 0` + `confirmed_at DATETIME NULL`
   - **历史数据不 backfill**：上线后所有曾"已读"的报告回归"未读"（产品侧已知悉）

**结构变更**：

- 文档从 13 接口（含 6 个 TBD）精简为 **7 个已实现接口**，TBD 接口只在末尾"后续批次预告"列出 1 行
- 所有 JSON 示例对齐 VO 当前真实字段

### v1.5（2026-06-08）— B9 局部更新（已被 v2.0 覆盖）

### v1.4（2026-06-02）— summary_json 9 列合并 1 列

---

## 接口清单（当前已实现 7 个）

| # | 方法 | 路径 | 鉴权 | 简述 |
|---|---|---|---|---|
| 1 | POST | `/replay/script-monitor/reportStatus` | 登录 | 查询单个资源的三类监控状态 |
| 2 | POST | `/replay/script-monitor/batchReportStatus` | 登录 | 批量查询（最多 100 条） |
| 3 | POST | `/replay/script-monitor/triggerReport` | 登录 + 防重复 | 手动触发报告生成 |
| 4 | GET | `/replay/script-monitor/qualityReportDetail` | 登录 | 查询话术质检报告详情 |
| 5 | GET | `/replay/script-monitor/patrolReportDetail` | 登录 | 查询互动巡检报告详情 |
| 6 | GET | `/replay/script-monitor/anchorBasicConfig` | 登录 | 查询直播间基础配置（含三类 AI 监控开关） |
| 7 | POST | `/replay/script-monitor/setMonitorEnabled` | 登录 + 防重复 | 切换单个 AI 监控能力开关 |

> **后续批次预告（TBD，尚未实现）**：`fidelityReportDetail` (B5) / `generateStandardScript` / `confirmStandardScript` / `standardScriptDetail` (B3) / `monitorPositionStatistics` / `addOrUpdateAnchor` 扩展字段。前端可忽略，待后续批次发布单独接口文档。

---

## 通用约定

### 鉴权

所有接口需要登录态：Header `Authorization: Bearer <jwt>` + Header `Token: <用户身份 Token>`。

`tenantId` / `userId` 全部从登录态取，**前端不传**这两个字段。

### 响应封装

所有接口返回统一格式：

```json
{
  "code": 0,
  "msg": "ok",
  "data": <T>
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | number | 0 = 成功；非 0 = 业务错误码 |
| `msg` | string | 文案：成功时 "ok" 或描述 |
| `data` | T | 实际响应数据；失败时通常为 null |

下文"响应数据"指 `data` 中的 T。

### 错误码

| code | 含义 | 触发场景 |
|---|---|---|
| 0 | 成功 | — |
| 401 | 未登录 / Token 过期 | — |
| 70001 | 参数非法 | 必填字段缺失或类型错误 |
| 70006 | 报告不存在 | reportId 不存在或已软删除 |
| 70011 | 无数据读取权限 | 跨租户访问 |
| 70013 | 报告生成中，请勿重复触发 | triggerReport 状态冲突 |

完整错误码见 `StatusCode.java`（70001-70013 段）。

### 枚举值

#### sourceType（资源类型）

| 值 | 含义 |
|---|---|
| 0 | 录制视频 |
| 1 | 上传文件分析 |

#### sceneType（业务场景）

| 值 | 含义 | 适用 sourceType |
|---|---|---|
| 0 | 复盘场景 | 0 |
| 1 | 视频分析 | 1 |
| 2 | 文案预审 | 1 |

#### monitorType（监控类型）

| 值 | 含义 |
|---|---|
| 0 | 话术质检 |
| 1 | 话术还原度 |
| 2 | 互动巡检 |

#### status（报告状态）

| 值 | 含义 | 说明 |
|---|---|---|
| 0 | 未生成 | 可手动触发 |
| 1 | 生成中 | 禁止重复触发 |
| 2 | 已生成 | 可查看 |
| 3 | 生成失败 | 可重试 |
| 4 | 不可生成 | 展示 `unavailableReason` |

---

## 1. POST `/replay/script-monitor/reportStatus`

查询**单个资源**的三类监控（质检/还原度/巡检）状态、报告 ID、是否已读、监控开关、报告摘要。

### 请求

**Content-Type**: `application/json`

**Body**：`ReportStatusBo`

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `sourceType` | number | Y | 非 null | 资源类型；枚举见上 |
| `sceneType` | number | Y | 非 null | 业务场景；枚举见上 |
| `sourceId` | string | Y | 非空 | 资源 ID（videoId 或 uploadFileId）|
| `secUid` | string | N | — | 主播唯一标识；传入后 `monitors[].monitorEnabled` 按 (userId, tenantId, secUid) 查 `tb_anchor_url_user` 三开关；未传 / 上传文件场景 / 查无记录 → `monitorEnabled` 返 null |

### 响应

`data`：`ScriptMonitorReportStatusVo`

| 字段 | 类型 | 说明 |
|---|---|---|
| `sourceType` | number | 回传请求中的 sourceType |
| `sceneType` | number | 回传请求中的 sceneType |
| `sourceId` | string | 回传请求中的 sourceId |
| `monitors` | `MonitorTypeStatusVo[]` | 三类监控状态列表（固定 3 项：质检/还原度/巡检）|

`MonitorTypeStatusVo` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `monitorType` | number | 监控类型；0/1/2 |
| `monitorTypeText` | string | 文案，如 "话术质检" |
| `status` | number | 报告状态；0~4 |
| `statusText` | string | 状态文案，如 "已生成" |
| `reportId` | number\|null | 报告 ID（Snowflake，19 位长 number；JS 可能精度丢失，**前端务必按 string 处理**或用大整数库）；未生成时为 null |
| `summary` | string\|null | **报告摘要**：质检/还原度=**JSON 字符串**（前端 `JSON.parse` 后取 `crashCount`/`score` 等字段）；巡检=**Markdown 文本片段**（前端直接渲染，**不要 JSON.parse**，来自 AI 合并报告 `<aifupan-data-block>` 标签内文本）；未生成/失败/不可生成 → null；巡检空标签 → `""` |
| `isRead` | number | 是否已读 0=未读 1=已读。**B9 起**来源主表 `tb_script_monitor_report.is_read`；仅录制人本人首次打开 detail 接口后置 1 |
| `unavailableReason` | string\|null | 不可生成原因；status=4 时有值 |
| `monitorEnabled` | number\|null | 该 monitorType 开关：0=关 / 1=开 / null=不适用（未传 secUid / 上传文件场景 / `tb_anchor_url_user` 无记录）|

### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "sourceType": 0,
    "sceneType": 0,
    "sourceId": "v_12345",
    "monitors": [
      {
        "monitorType": 0,
        "monitorTypeText": "话术质检",
        "status": 2,
        "statusText": "已生成",
        "reportId": 1234567890123456789,
        "summary": "{\"crashCount\":0,\"slackCount\":2,\"brandDamageCount\":0,\"afterSalesCount\":1}",
        "isRead": 1,
        "unavailableReason": null,
        "monitorEnabled": 1
      },
      {
        "monitorType": 1,
        "monitorTypeText": "话术还原度",
        "status": 0,
        "statusText": "未生成",
        "reportId": null,
        "summary": null,
        "isRead": 0,
        "unavailableReason": null,
        "monitorEnabled": 0
      },
      {
        "monitorType": 2,
        "monitorTypeText": "互动巡检",
        "status": 2,
        "statusText": "已生成",
        "reportId": 1234567890123456791,
        "summary": "## 互动有效性\n- 总互动数：320\n- 有效互动：78%\n- 主要问题：催单回复不及时",
        "isRead": 0,
        "unavailableReason": null,
        "monitorEnabled": 1
      }
    ]
  }
}
```

---

## 2. POST `/replay/script-monitor/batchReportStatus`

批量查询**多个资源**的三类监控报告状态。跳过无权限/不存在的资源，不报错。

### 请求

**Content-Type**: `application/json`

**Body**：`BatchReportStatusBo`

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `sources` | `SourceItem[]` | Y | 非空，最多 100 条 | 资源列表 |

`SourceItem` 对象（与 `ReportStatusBo` 同结构）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sourceType` | number | Y | 资源类型 |
| `sceneType` | number | Y | 业务场景 |
| `sourceId` | string | Y | 资源 ID |
| `secUid` | string | N | 主播唯一标识 |

### 响应

`data`：`ScriptMonitorReportStatusVo[]`

返回数组，每个元素对应一个有权限的资源（跳过无权限/不存在的项）。每项结构同 §1 `ScriptMonitorReportStatusVo`。

### 响应示例（成功）

```json
{
  "code": 0,
  "msg": "ok",
  "data": [
    {
      "sourceType": 0,
      "sceneType": 0,
      "sourceId": "v_12345",
      "monitors": [
        { "monitorType": 0, "monitorTypeText": "话术质检", "status": 2, "statusText": "已生成", "reportId": 1234567890123456789, "summary": "{\"crashCount\":0}", "isRead": 1, "unavailableReason": null, "monitorEnabled": 1 },
        { "monitorType": 1, "monitorTypeText": "话术还原度", "status": 0, "statusText": "未生成", "reportId": null, "summary": null, "isRead": 0, "unavailableReason": null, "monitorEnabled": 0 },
        { "monitorType": 2, "monitorTypeText": "互动巡检", "status": 2, "statusText": "已生成", "reportId": 1234567890123456791, "summary": "## 互动有效性\n- 总互动数：320", "isRead": 0, "unavailableReason": null, "monitorEnabled": 1 }
      ]
    }
  ]
}
```

---

## 3. POST `/replay/script-monitor/triggerReport`

手动触发**单个监控类型**的报告生成。带 `@NoRepeatSubmit` 防重复，5 秒内同一组合（sourceType + sceneType + sourceId + monitorType + token）只允许一次。

### 请求

**Content-Type**: `application/json`

**Headers**:
- `Token`: 用户身份 Token（用于防重复 key 区分；可选，由请求拦截器自动注入）

**Body**：`TriggerReportBo`

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `sourceType` | number | Y | 非 null | 资源类型 |
| `sceneType` | number | Y | 非 null | 业务场景 |
| `sourceId` | string | Y | 非空 | 资源 ID |
| `monitorType` | number | Y | 非 null | 监控类型 0=质检 1=还原度 2=巡检 |

### 响应

`data`：`Boolean` — 触发成功返 `true`；不返回 reportId（前端轮询 `reportStatus` 等状态变 `GENERATED` 后再调 detail）

### 行为说明

- 已是 `GENERATING` 状态时：抛 70013 错误码（生成中请勿重复）
- 已是 `GENERATED` 状态时：再次触发会**覆盖**旧报告，**重置** `is_read=0` / `confirmed_at=null`
- 不可生成（如巡检场景下视频无弹幕）：status 直接置为 4 不可生成
- 防重复：5 秒窗口内同组合调用直接返回 "请勿重复触发，请稍后再试"

### 响应示例

```json
{ "code": 0, "msg": "ok", "data": true }
```

---

## 4. GET `/replay/script-monitor/qualityReportDetail`

查询**话术质检报告**详情。

### 请求

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `reportId` | number | Y | 报告 ID（来自 `reportStatus.monitors[质检].reportId`）|

### 响应

`data`：`QualityReportDetailVo`

| 字段 | 类型 | 说明 |
|---|---|---|
| `reportId` | number | 报告 ID（Snowflake 长 number，前端按字符串处理）|
| `sourceType` | number | 资源类型 |
| `sceneType` | number | 业务场景 |
| `sourceId` | string | 资源 ID |
| `status` | number | 报告状态 |
| `summaryJson` | string\|null | 摘要 **JSON 字符串**（前端 `JSON.parse` 后取 `crashCount/slackCount/brandDamageCount/afterSalesCount` 等字段）；未生成 / AI 返回非法 JSON → null |
| `reportContent` | string\|null | 报告正文 **Markdown 全文**（来自 MongoDB；状态非 `GENERATED` 时为 null）。**注**：旧文档曾标注为 HTML，实际为 Markdown，前端直接渲染 |
| `anchorName` | string | 主播名称 |
| `liveTitle` | string | 直播标题 |
| `liveTime` | string | 直播时间（ISO-8601）|
| `isRead` | number | 是否已读 0/1（**B9 起来源主表**；录制人本人首次打开本接口时后端自动置 1）|
| `createDate` | string | 报告创建时间（ISO-8601）|

### 行为说明

- **录制人本人**首次调用 → 后端自动 UPDATE `is_read=1, confirmed_at=NOW()`（独立事务 + fail-safe，写失败不影响详情返回）
- 同租户**非录制人**调用 → 不写已读，正常返回
- **跨租户**调用 → 抛 70011

### 响应示例

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "reportId": 1234567890123456789,
    "sourceType": 0,
    "sceneType": 0,
    "sourceId": "v_12345",
    "status": 2,
    "summaryJson": "{\"crashCount\":0,\"slackCount\":2,\"brandDamageCount\":0,\"afterSalesCount\":1}",
    "reportContent": "# 话术质检报告\n## 摘要\n本场直播话术整体表现良好...",
    "anchorName": "张主播",
    "liveTitle": "618 大促",
    "liveTime": "2026-06-08T10:00:00",
    "isRead": 1,
    "createDate": "2026-06-08T14:30:00"
  }
}
```

---

## 5. GET `/replay/script-monitor/patrolReportDetail`

查询**互动巡检报告**详情。

### 请求

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `reportId` | number | Y | 报告 ID（来自 `reportStatus.monitors[巡检].reportId`）|

### 响应

`data`：`InteractionPatrolReportDetailVo`

| 字段 | 类型 | 说明 |
|---|---|---|
| `reportId` | number | 报告 ID |
| `sourceType` | number | 资源类型 |
| `sceneType` | number | 业务场景 |
| `sourceId` | string | 资源 ID |
| `status` | number | 报告状态 |
| `summaryJson` | string\|null | 摘要 **Markdown 文本片段**（**B7 改造**：原 JSON 改为 Markdown；字段名沿用历史。前端**直接渲染、不要 JSON.parse**；来自 AI 合并报告 `<aifupan-data-block>` 标签内 trim 后）；未生成 / 无标签 → null；空标签 → `""` |
| `reportContent` | string\|null | 合并报告 **Markdown 全文**（来自 MongoDB，含弹幕明细表格 + `<aifupan-data-block>` 标签包裹的摘要段）；前端直接渲染 |
| `anchorName` | string | 主播名称 |
| `liveTitle` | string | 直播标题 |
| `liveTime` | string | 直播时间（ISO-8601）|
| `isRead` | number | 是否已读 0/1（**B9 新增**；录制人首次打开本接口时后端自动置 1）|
| `confirmedAt` | string\|null | 录制人首次查看时间戳（**B9** 来源主表 `tb_script_monitor_report.confirmed_at`）；未读时为 null；`triggerReport` 重新生成后重置为 null |
| `createDate` | string | 报告创建时间（ISO-8601）|

### 行为说明

同 §4 `qualityReportDetail`（录制人首次查看时自动写已读 + fail-safe）。

### 响应示例

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "reportId": 1234567890123456791,
    "sourceType": 0,
    "sceneType": 0,
    "sourceId": "v_12345",
    "status": 2,
    "summaryJson": "## 互动有效性\n- 总互动数：320\n- 有效互动：78%\n- 主要问题：催单回复不及时",
    "reportContent": "# 互动巡检报告\n\n## 弹幕明细\n| 时间 | 用户 | 内容 |\n|---|---|---|\n| 10:05 | u1 | 多少钱 |\n\n## 摘要分析\n<aifupan-data-block>\n## 互动有效性\n...\n</aifupan-data-block>",
    "anchorName": "张主播",
    "liveTitle": "618 大促",
    "liveTime": "2026-06-08T10:00:00",
    "isRead": 1,
    "confirmedAt": "2026-06-08T15:23:00",
    "createDate": "2026-06-08T14:30:00"
  }
}
```

---

## 6. GET `/replay/script-monitor/anchorBasicConfig`

查询**直播间基础配置**（含三类 AI 监控开关、标准稿 ID 等）。

### 请求

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `secUid` | string | Y | 主播唯一标识 |

### 响应

`data`：`AnchorUrlUserVo`（**仅列与话术智能监控相关的关键字段**；完整结构含 50+ 个字段，覆盖录制配置/授权状态等，前端按需取用）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | number | 配置记录 ID |
| `userId` | number | 用户 ID |
| `tenantId` | number | 租户 ID |
| `anchorUrlSecUid` | string | 主播唯一标识（即请求中的 secUid）|
| `anchorInfo` | object\|null | 主播信息（含 anchorName、平台、URL 等）|
| **`isScriptQualityInspection`** | number | 话术质检开关 0=关 1=开 |
| **`isScriptFidelityMonitor`** | number | 话术还原度开关 0=关 1=开 |
| **`isInteractionPatrol`** | number | 互动巡检开关 0=关 1=开 |
| **`standardScriptId`** | number\|null | 当前已确认标准稿 ID（B3 阶段就绪，当前**恒为 null**）|
| `isAutoRecord` | number | 在线时自动录制 0/1 |
| `isAutoAnalysis` | number | 自动分析视频 0/1 |
| `isAutoDiagnosis` | number | 自动诊断 0/1 |
| `isDataDiagnosis` | number | 数据诊断开关 0/1 |
| ... | ... | 其他字段（录制清晰度、授权状态、诊断参数等）见 `AnchorUrlUserVo.java` |

### 响应示例（节选关键字段）

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "id": 9876543210987654321,
    "userId": 1001,
    "tenantId": 100,
    "anchorUrlSecUid": "MS4wLjABAAAA_xxxx",
    "anchorInfo": {
      "anchorName": "张主播",
      "anchorUrl": "https://live.douyin.com/xxx",
      "tradeId": 12,
      "tradeName": "美妆护肤"
    },
    "isScriptQualityInspection": 1,
    "isScriptFidelityMonitor": 0,
    "isInteractionPatrol": 1,
    "standardScriptId": null,
    "isAutoRecord": 1,
    "isAutoAnalysis": 1,
    "isAutoDiagnosis": 1
  }
}
```

---

## 7. POST `/replay/script-monitor/setMonitorEnabled`

切换**单个 AI 监控能力**开关（话术质检 / 话术还原度 / 互动巡检三选一）。带 `@NoRepeatSubmit` 防重复。

### 请求

**Content-Type**: `application/x-www-form-urlencoded` 或 Query 参数

**Headers**:
- `Token`: 用户身份 Token（防重复 key 区分；可选）

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `secUid` | string | Y | 主播唯一标识 |
| `monitorType` | number | Y | 监控类型 0=质检 1=还原度 2=巡检 |
| `enabled` | number | Y | 目标状态 0=关闭 1=开启 |

### 响应

`data`：`Boolean` — 切换成功返 `true`

### 行为说明

- 短路幂等：当前状态与目标状态相同时直接返回 `true`，不写库
- 话术还原度（monitorType=1）暂禁开启：B5 阶段恒返回业务错误
- 授权量校验：开启前会检查 token 余额 / 监控位授权
- 单字段写库 + 占用记录更新

### 响应示例

```json
{ "code": 0, "msg": "ok", "data": true }
```

---

## 完整 VO/BO Java 源参考

| 文档字段表 | Java 源文件 |
|---|---|
| `ReportStatusBo` | `replay-ai/src/main/java/com/jiuyu/replay/ai/bo/ReportStatusBo.java` |
| `BatchReportStatusBo` / `SourceItem` | `replay-ai/src/main/java/com/jiuyu/replay/ai/bo/BatchReportStatusBo.java` |
| `TriggerReportBo` | `replay-ai/src/main/java/com/jiuyu/replay/ai/bo/TriggerReportBo.java` |
| `ScriptMonitorReportStatusVo` | `replay-ai/src/main/java/com/jiuyu/replay/ai/vo/ScriptMonitorReportStatusVo.java` |
| `MonitorTypeStatusVo` | `replay-ai/src/main/java/com/jiuyu/replay/ai/vo/MonitorTypeStatusVo.java` |
| `QualityReportDetailVo` | `replay-ai/src/main/java/com/jiuyu/replay/ai/vo/QualityReportDetailVo.java` |
| `InteractionPatrolReportDetailVo` | `replay-ai/src/main/java/com/jiuyu/replay/ai/vo/InteractionPatrolReportDetailVo.java` |
| `AnchorUrlUserVo` | `replay-generic/src/main/java/com/jiuyu/replay/generic/vo/words/AnchorUrlUserVo.java` |

---

## 后续批次预告（TBD）

以下接口在后续批次实现，本文档不展开。前端联调时单独沟通：

| 接口 | 批次 | 用途 |
|---|---|---|
| `GET /fidelityReportDetail` | B5 | 话术还原度报告详情 |
| `POST /generateStandardScript` | B3 | AI 生成标准稿（不落库） |
| `POST /confirmStandardScript` | B3 | 确认并落库标准稿 |
| `GET /standardScriptDetail` | B3 | 查询已确认标准稿 |
| `GET /monitorPositionStatistics` | B5 | 监控位额度统计 |
| `POST /addOrUpdateAnchor` 扩展字段 | B3 | 新增/编辑直播间（含 AI 监控开关扩展）|

---

## 联系人

- 后端 owner：beta
- 接口契约权威源（最详细字段表 + 完整 JSON 示例）：`.claude/llm_wiki/wiki/frontend-api/script_monitor.md`
- B9 改动归档：`.claude/llm_wiki/archive/20260608_B9-read-semantics-to-report-column.md`
- B7 改动归档：`.claude/llm_wiki/archive/20260608_patrol-summary-md-cast.md`
- ADR：`.claude/llm_wiki/wiki/architecture/adrs/0001-0005-*.md`
