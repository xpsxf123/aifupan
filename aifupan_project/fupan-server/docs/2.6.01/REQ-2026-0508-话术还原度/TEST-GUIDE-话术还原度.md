# 话术还原度功能 — 测试说明

> 测试范围：Slice A 标准稿能力 + Slice B 报告生成 + Slice C 报告详情 + PATCH #15 提示词 JSON 改造。
> 接口契约 → `.claude/llm_wiki/wiki/frontend-api/fidelity.md`
> 后端归档：`Change__2026-06-11_12:33:48` / `Change__2026-06-11_18:57:48` / `Change__2026-06-12_23:27:59`

---

## 1. 测试环境准备

### 1.1 账号准备

| 账号类型 | 用途 | 说明 |
|---|---|---|
| 主账号 A（自有账号 accountType=0）| 主要测试账号 | 需开通话术还原度授权（`scriptFidelityMonitorNum` ≥ 1）|
| 主账号 B（不同租户）| 跨租户隔离测试 | 与 A 同等权限即可 |
| 子账号 / 同租户其他用户 | 已读语义测试 | A 租户下另一个用户 |
| 主账号 C（竞品账号 accountType≠0）| 非自有账号校验 | 不需开通 |

### 1.2 数据准备

- 主账号 A 名下至少 2 个录制视频（已 ASR 转写完成）
  - V1：3-5 分钟短视频（用于快速跑通流程）
  - V2：30+ 分钟长视频（用于 AI 调用耗时观察）
- 主账号 A 至少 1 段标准稿草稿（用于"先开开关 + 后建标准稿"路径测试）
- 主账号 B 名下 1 个录制视频 V3（用于跨租户访问测试）
- AI Token 余额：A 充足（≥ 100,000）；B 任意；A 测低额场景时手动调到 < 100,000

### 1.3 工具

- Postman / Apifox 集合：导入接口契约 fidelity.md
- 数据库直连：`tb_anchor_url_user` / `tb_standard_script` / `tb_script_monitor_report` / `tb_script_monitor_report_body`（MongoDB）
- 日志查看：`replay-ai` / `replay-words` / `replay-api` 三个服务日志

---

## 2. 测试用例总览

| # | 用例 | 涉及接口 | 优先级 | 类型 |
|---|---|---|---|---|
| TC-01 | 开还原度开关 + 标准稿全流程 | 2 / 3 / 4 / 5 / 1 | P0 | 主流程 |
| TC-02 | 手动触发还原度报告生成 | 6 / 7 / 9 | P0 | 主流程 |
| TC-03 | 自动触发还原度报告（场次结束）| 自动触发 + 7 / 9 | P1 | 主流程 |
| TC-04 | 还原度报告详情 + 已读语义 | 9 | P0 | 主流程 |
| TC-05 | 标准稿循环模式（speechMode=1）| 3 / 4 / 5 | P1 | 分支 |
| TC-06 | 标准稿覆盖（先确认 → 再确认）| 4 / 5 | P1 | 分支 |
| TC-07 | 开还原度但未确认标准稿 | 2 | P0 | 边界 |
| TC-08 | 触发还原度但无标准稿 | 6 | P0 | 边界 |
| TC-09 | 上传文件场景拒绝（sourceType=1）| 6 | P0 | 边界 |
| TC-10 | 跨租户访问 | 9 | P0 | 安全 |
| TC-11 | 非录制人查看（不写已读）| 9 | P1 | 边界 |
| TC-12 | 重新触发后 isRead/confirmedAt 重置 | 6 / 9 | P1 | 边界 |
| TC-13 | 非自有账号拒绝 | 6 / 2 | P1 | 边界 |
| TC-14 | AI Token 不足 | 3 / 6 | P1 | 边界 |
| TC-15 | 还原度授权不足 | 2 / 6 | P1 | 边界 |
| TC-16 | 报告不存在 / 已删除 | 9 | P1 | 边界 |
| TC-17 | 参数校验失败 | 3 / 4 / 6 | P2 | 校验 |
| TC-18 | 防重提交拦截 | 3 / 4 / 6 / 2 | P2 | 边界 |
| TC-19 | reportStatus / batchReportStatus 还原度项 | 7 / 8 | P0 | 状态查询 |
| TC-20 | 前端多页面适配（复盘/AI切片/云空间）| 7 / 9 | P0 | 集成 |

---

## 3. 详细用例

### TC-01 — 开还原度开关 + 标准稿全流程 [P0]

**目标**：验证从无标准稿状态走通"开开关 → 生成标准稿 → 编辑 → 确认 → 查详情"完整链路。

**前置**：账号 A 名下 secUid_A 直播间未确认标准稿，还原度开关关闭。

**步骤**

| 步 | 操作 | 接口 | 期望 |
|---|---|---|---|
| 1 | 查直播间配置 | `GET /anchorBasicConfig?secUid=secUid_A` | `isScriptFidelityMonitor=0`，`standardScriptId=null` |
| 2 | 尝试开还原度开关 | `POST /setMonitorEnabled?secUid=secUid_A&monitorType=1&enabled=1` | **抛 70005**（请先确认标准直播稿）|
| 3 | AI 生成标准稿 | `POST /generateStandardScript` body `{speechMode:0, speechSpeed:280, referenceScript:"<300+ 字脚本>"}` | 60s 内返回 `timeAxisScript[]` 列表非空，**响应不含 standardScriptId** |
| 4 | 用户编辑（修改某条 content） | 前端本地编辑 timeAxisScript | 无后端调用 |
| 5 | 确认落库 | `POST /confirmStandardScript` body 含编辑后的 timeAxisScript + secUid | 返回 `standardScriptId`（19 位 string）|
| 6 | 查标准稿详情 | `GET /standardScriptDetail?secUid=secUid_A` | `hasScript=true`，字段全对，timeAxisScript 含编辑后内容 |
| 7 | 再次开还原度开关 | `POST /setMonitorEnabled?secUid=secUid_A&monitorType=1&enabled=1` | 返回 `true` |
| 8 | 复查直播间配置 | `GET /anchorBasicConfig?secUid=secUid_A` | `isScriptFidelityMonitor=1`，`standardScriptId=<步骤 5 的 ID>` |

**DB 验证**
- `tb_standard_script`：1 行 `is_deleted=0`，`secUid=secUid_A`，`user_id=A`，`tenant_id=A 租户`
- `tb_anchor_url_user`：`is_script_fidelity_monitor=1`

---

### TC-02 — 手动触发还原度报告生成 [P0]

**目标**：TC-01 完成后，验证手动触发还原度报告，AI 串行 4 次生成 GENERATED 状态报告。

**前置**：TC-01 完成；A 名下 V1 录制视频已 ASR 转写。

**步骤**

| 步 | 操作 | 接口 | 期望 |
|---|---|---|---|
| 1 | 查 V1 报告状态 | `POST /reportStatus` body `{sourceType:0, sceneType:0, sourceId:V1, secUid:secUid_A}` | `monitors[1].status=0`（未生成）/ `monitorEnabled=1` |
| 2 | 手动触发 | `POST /triggerReport` body `{sourceType:0, sceneType:0, sourceId:V1, monitorType:1}` | 返回 `true` |
| 3 | 立即查状态 | 同步骤 1 | `monitors[1].status=1`（生成中），`reportId` 非空 |
| 4 | 等待 60-120s（长视频更久），轮询 | 同步骤 1 每 5s 一次 | 最终 `monitors[1].status=2`（已生成），`summary` 为 Markdown 文本片段 |
| 5 | 验证 summary 内容 | 取响应 `monitors[1].summary` | 非空 Markdown 字符串；前端可直接 Markdown 渲染（含 `**加粗**` / `- 列表` 等）|

**DB 验证**
- `tb_script_monitor_report`：1 行 `monitor_type=1`，`status=2`，`report_body_id` 非空
- MongoDB `script_monitor_report_body`：1 文档 `_id=<report_body_id>`，`content` 含 `<aifupan-data-block>` 标签
- AI 调用日志：4 次 cueType 调用 cueType=18/19/25/26（或 27），最后一次为合并校验

---

### TC-03 — 自动触发还原度报告（场次结束）[P1]

**目标**：录制人开了还原度 + 已配标准稿，场次结束后自动触发。

**前置**：A 直播间开了还原度且配了标准稿；正在直播中（或模拟场次结束事件）。

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | 直播间录制视频 V_auto 生成 | 触发录制结束事件 |
| 2 | 等待 5-10s | 后端 `autoTrigger` 触发还原度生成 |
| 3 | 查 `reportStatus` for V_auto | `monitors[1].status=1` 或 `2` |
| 4 | 等待 60-120s | 最终 `monitors[1].status=2` |
| 5 | 检查报告归属 | DB `tb_script_monitor_report.create_user_id` = A，`tenant_id` = A 租户 |

**关键校验**：autoTrigger 不应触发 sourceType=1 还原度（仅录制视频自动触发）。

---

### TC-04 — 还原度报告详情 + 已读语义 [P0]

**目标**：录制人首次查看自动写已读 + 同租户非录制人查看不写。

**前置**：TC-02 完成，V1 还原度报告 status=2。

**步骤**

| 步 | 操作 | 接口 | 期望 |
|---|---|---|---|
| 1 | DB 检查 | `SELECT is_read, confirmed_at FROM tb_script_monitor_report WHERE id=<reportId>` | `is_read=0`，`confirmed_at=NULL` |
| 2 | A（录制人）首次查详情 | `GET /fidelityReportDetail?reportId=<reportId>` | 13 字段齐全，`isRead=1`，`confirmedAt` 非空（ISO-8601）；`summaryJson` 为 Markdown 文本；`reportContent` 为 Markdown 全文 |
| 3 | DB 复查 | 同步骤 1 | `is_read=1`，`confirmed_at` ≈ 当前时间 |
| 4 | A 再次查详情 | 同步骤 2 | `isRead=1`，`confirmedAt` **不变**（不是覆盖写）|
| 5 | A 租户下子账号 A2 查详情 | 同步骤 2 | 字段返回正常但 `isRead=0`，DB 字段**不写**（非录制人不写）|

**字段断言（详情响应 13 字段）**

| 字段 | 期望 |
|---|---|
| reportId | string，19 位 Snowflake |
| sourceType | 0 |
| sceneType | 0 |
| sourceId | string，等于 V1 |
| status | 2 |
| summaryJson | non-null Markdown 文本片段 |
| reportContent | non-null Markdown 全文（含表格 / 段落 / `<aifupan-data-block>` 标签）|
| anchorName | string |
| liveTitle | string |
| liveTime | ISO-8601 |
| isRead | 0 → 1（首次后）|
| confirmedAt | null → ISO-8601（首次后）|
| createDate | ISO-8601 |

---

### TC-05 — 标准稿循环模式（speechMode=1）[P1]

**目标**：循环话术模式必须传 `cycleDurationMinutes`。

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | `POST /generateStandardScript` body `{speechMode:1, speechSpeed:280, referenceScript:"..."}`（**不传** cycleDurationMinutes）| 抛 70013 参数校验失败 |
| 2 | 同上但传 `cycleDurationMinutes:15` | 返回成功，`timeAxisScript[]` 应为循环段落（约 15 分钟内容）|
| 3 | `POST /confirmStandardScript` body 含 speechMode=1 + cycleDurationMinutes=15 + timeAxisScript | 返回 standardScriptId |
| 4 | `GET /standardScriptDetail` | `speechMode=1`，`cycleDurationMinutes=15` |

---

### TC-06 — 标准稿覆盖（先确认 → 再确认）[P1]

**目标**：每次 confirmStandardScript 旧稿应被软删 + INSERT 新稿。

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | 第一次 confirm（脚本 v1）| 返回 standardScriptId_1 |
| 2 | DB 查 | 1 行 `is_deleted=0` |
| 3 | 第二次 confirm（脚本 v2，referenceScript 改了一些）| 返回 standardScriptId_2，且 ≠ standardScriptId_1 |
| 4 | DB 查 | 第 1 行 `is_deleted=1`，第 2 行 `is_deleted=0`（新稿）|
| 5 | `GET /standardScriptDetail` | 返回 v2 内容；`standardScriptId=standardScriptId_2` |
| 6 | `GET /anchorBasicConfig` | `standardScriptId=standardScriptId_2` |

---

### TC-07 — 开还原度但未确认标准稿 [P0]

**目标**：守约 70005 错误码。

**前置**：secUid_A_new 是新直播间，未确认标准稿。

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | `POST /setMonitorEnabled?secUid=secUid_A_new&monitorType=1&enabled=1` | 抛 **70005**（请先确认标准直播稿）|
| 2 | DB | `tb_anchor_url_user.is_script_fidelity_monitor` 不变（保持 0）|

---

### TC-08 — 触发还原度但无标准稿 [P0]

**目标**：守约 70005 在 triggerReport 处也生效。

**前置**：手动清掉 secUid_A 的标准稿（`UPDATE tb_standard_script SET is_deleted=1 WHERE sec_uid=...`）；保持 `is_script_fidelity_monitor=1`（数据不一致场景）。

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | `POST /triggerReport` body monitorType=1 | 抛 **70005** |
| 2 | DB | `tb_script_monitor_report` 不应有新记录 |

---

### TC-09 — 上传文件场景拒绝（sourceType=1）[P0]

**目标**：还原度功能明确不支持上传文件分析 / 文案预审。

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | `POST /triggerReport` body `{sourceType:1, sceneType:1, sourceId:<uploadFileId>, monitorType:1}` | 抛 **70014**（功能未上线 / 还原度暂不支持上传文件场景）|
| 2 | `POST /triggerReport` body `{sourceType:1, sceneType:2, sourceId:..., monitorType:1}` | 同上 |
| 3 | 前端视频分析页 / 文案预审页 | 不展示还原度入口（前端契约约定）|
| 4 | `POST /reportStatus` body `{sourceType:1, sceneType:1, sourceId:...}` | `monitors[1].status` 可能 = 0（永远不会变成 2）|

---

### TC-10 — 跨租户访问 [P0]

**目标**：A 不能查 B 的还原度报告。

**前置**：B 名下 V3 有还原度报告 reportId_B（status=2）。

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | A 登录调 `GET /fidelityReportDetail?reportId=<reportId_B>` | 抛 **70011**（无数据读取权限）|
| 2 | A 登录调 `POST /reportStatus` body `{sourceType:0, sceneType:0, sourceId:V3}` | 跳过或返回空（不报错），不暴露报告 ID |
| 3 | DB 验证 | A 操作不应影响 B 数据 |

> **云空间分享场景特殊**：如果 V3 已分享到云空间且 A 是分享 receiver，应允许查（按 `qualityReportDetail` / `patrolReportDetail` 同范式）。需 PRD 6.5.1 确认是否也覆盖还原度，开发时如未覆盖则记为 TECH-DEBT。

---

### TC-11 — 非录制人查看（不写已读）[P1]

**目标**：同租户但非录制人查详情，不写已读。

**前置**：V1 录制人为 A；A2 是 A 租户下另一用户。

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | DB 检查 | `is_read=0`，`confirmed_at=NULL` |
| 2 | A2 查详情 | 返回正常 13 字段；`isRead=0` |
| 3 | DB 复查 | 字段不变 |
| 4 | A 查详情 | 返回 `isRead=1`，`confirmedAt` 非空 |
| 5 | DB 复查 | 字段已写 |

---

### TC-12 — 重新触发后 isRead/confirmedAt 重置 [P1]

**目标**：旧报告读过后，重新触发生成时 is_read/confirmed_at 应重置。

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | V1 报告 reportId_old，A 已查过 → `is_read=1`、`confirmed_at` 非空 | 准备状态 |
| 2 | 删除旧报告或触发新生成（同一 V1 重新 trigger）| 后端会软删旧报告 + 新生成 reportId_new |
| 3 | A 查 `fidelityReportDetail?reportId=<reportId_new>` | `isRead=0`，`confirmedAt=null`（新报告初始态）|
| 4 | A 再查 | `isRead=1`，`confirmedAt` 写入 |

> 已知语义：历史已读的报告**不 backfill**，旧 reportId 仍可查到 isRead=1 状态。

---

### TC-13 — 非自有账号拒绝 [P1]

**前置**：C 是竞品账号（accountType ≠ 0）。

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | C 调 `POST /setMonitorEnabled?monitorType=1&enabled=1` | 抛 **70004**（仅自有账号支持）|
| 2 | C 调 `POST /triggerReport monitorType=1` | 同上 |

---

### TC-14 — AI Token 不足 [P1]

**前置**：A 的 AI Token 余额调到 < 100,000。

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | `POST /generateStandardScript` | 抛 **70001** |
| 2 | `POST /triggerReport monitorType=1`（已开还原度 + 有标准稿）| 抛 **70001** |
| 3 | `POST /setMonitorEnabled monitorType=1 enabled=1` | 抛 **70001** |
| 4 | 充值后重试 | 通过 |

---

### TC-15 — 还原度授权不足 [P1]

**前置**：A 租户 `scriptFidelityMonitorNum.useQuantity == totalQuantity`（无剩余位）。

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | `POST /setMonitorEnabled monitorType=1 enabled=1`（新直播间）| 抛 **70002** |
| 2 | `POST /triggerReport monitorType=1`（已开开关的直播间，本次触发占新位）| 抛 **70002**（如触发也占位）|

---

### TC-16 — 报告不存在 / 已删除 [P1]

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | `GET /fidelityReportDetail?reportId=99999999999999999`（不存在 ID）| 抛 **70012** |
| 2 | `UPDATE tb_script_monitor_report SET is_deleted=1 WHERE id=<reportId>` 后再查 | 抛 **70012** |

---

### TC-17 — 参数校验失败 [P2]

按字段逐个校验。

| 接口 | 参数 | 期望 |
|---|---|---|
| generateStandardScript | speechSpeed=99 / 501 | 70013（语速 100-500）|
| generateStandardScript | speechMode=null | 70013 |
| generateStandardScript | referenceScript="" | 70013 |
| generateStandardScript | speechMode=1, cycleDurationMinutes=null | 70013 |
| confirmStandardScript | secUid="" | 70013 |
| confirmStandardScript | timeAxisScript=[] | 70013 |
| confirmStandardScript | timeAxisScript[0].timeRange="" | 70013 |
| triggerReport | sourceType=null | 70013 |
| triggerReport | sourceId="" | 70013 |
| setMonitorEnabled | enabled=2 | 70013 或具体校验码 |

---

### TC-18 — 防重提交拦截 [P2]

**步骤**

| 接口 | 操作 | 期望 |
|---|---|---|
| generateStandardScript | 5s 内连发 2 次 | 第 2 次返回"请勿重复提交" |
| confirmStandardScript | 5s 内连发 2 次 | 同上 |
| triggerReport | 同 V1 + monitorType=1 在 5s 内连发 2 次 | 第 2 次返回"请勿重复触发，请稍后再试" |
| setMonitorEnabled | 同 secUid + monitorType=1 + enabled=1 连发 2 次 | 第 2 次返回"请勿重复提交，请稍后再试" |

防重 key 见 fidelity.md 各接口"防重"段。

---

### TC-19 — reportStatus / batchReportStatus 还原度项 [P0]

**目标**：状态查询接口正确返回 monitorType=1 那一项。

**步骤**

| 步 | 操作 | 期望 |
|---|---|---|
| 1 | `POST /reportStatus` body `{sourceType:0, sceneType:0, sourceId:V1, secUid:secUid_A}` | `monitors[]` 有 3 项 0/1/2；`monitors[1]` 是还原度 |
| 2 | 验证 `monitors[1].monitorType=1`，`monitorTypeText="话术还原度"` | |
| 3 | 验证 `monitors[1].monitorEnabled=1`（开关已开）| |
| 4 | 报告 status=2 时 `monitors[1].summary` 为 Markdown 文本片段（非 JSON）| 前端 `JSON.parse` 应失败，需 Markdown 渲染 |
| 5 | `POST /batchReportStatus` body `{sources:[{V1},{V2},{not_exist_id}]}` | 返回 ≤2 项（不存在的跳过不报错）|
| 6 | 不传 secUid 时，`monitors[1].monitorEnabled=null` | |

---

### TC-20 — 前端多页面适配 [P0]

**适配范围（已确认）**

| 页面 | 是否适配 | 关键校验 |
|---|---|---|
| AI 复盘列表 | ✅ | 列表项展示还原度入口 / 状态 / 红点 |
| AI 复盘详情 | ✅ | 点击进入还原度详情页 |
| AI 切片复盘 | ✅ | sourceType=0 切片场景 |
| 云空间详情 | ✅ | sceneType=0 录制视频分享场景 |
| 视频分析（sourceType=1）| ❌ 不展示 | 前端契约约定不渲染还原度入口 |
| 文案预审（sourceType=1）| ❌ 不展示 | 同上 |

**测试方法**：用 TC-19 的 reportStatus + 各页面切换，确认 UI 入口正确展示 / 隐藏。

---

## 4. 性能 / 稳定性

| 项 | 期望 |
|---|---|
| `generateStandardScript` 响应时间 | 60s 内（超时抛 70008）|
| `triggerReport` 响应时间 | < 1s（异步触发）|
| 报告生成总时长 | 60-120s 内（4 次 AI 串行调用）|
| `fidelityReportDetail` 响应时间 | < 500ms（含 MongoDB 查正文）|
| `reportStatus` 响应时间 | < 200ms |
| `batchReportStatus` 100 条响应时间 | < 1s |

---

## 5. 回归项

主流程跑完后，须回归以下不变性：

| 模块 | 验证 |
|---|---|
| 话术质检（monitorType=0）| `triggerReport` / `qualityReportDetail` / `reportStatus monitors[0]` 全不变 |
| 互动巡检（monitorType=2）| `triggerReport` / `patrolReportDetail` / `reportStatus monitors[2]` 全不变 |
| 三类报告同一视频并存 | 触发 V1 同时跑 0/1/2，互不影响，DB 3 行 monitor_type 各不同 |
| 已读语义统一 | 质检 / 巡检 / 还原度 isRead 写入逻辑一致（仅录制人首次查）|

---

## 6. 已知限制 / 不测试项

| 项 | 说明 |
|---|---|
| `score` / `speechSpeed` / `deviationSummary` | 字段不存在于 VO（产品决策永久不写）|
| 视频分析 / 文案预审入口 | 不在范围内 |
| 历史 confirmRead 数据 backfill | B9 决策不迁移，旧已读报告 `confirmedAt` 重置 null |
| 70013 报告生成失败 → 新错误码 | Slice B 已 push back，沿用现有 70013 兜底 |

---

## 7. 测试通过判定

P0 用例（TC-01/02/04/07/08/09/10/19/20）全部通过 + P1 用例 ≥ 80% 通过 + 无 P0 bug + 性能指标全部达标 → **通过**。

---

## 附录 A：常用 SQL 查询

```sql
-- 查标准稿
SELECT id, user_id, tenant_id, sec_uid, speech_mode, speech_speed, is_deleted, create_date
FROM tb_standard_script
WHERE user_id = ? AND sec_uid = ? AND is_deleted = 0;

-- 查直播间开关
SELECT id, sec_uid, anchor_name, is_script_quality_inspection, is_script_fidelity_monitor, is_interaction_patrol
FROM tb_anchor_url_user
WHERE user_id = ? AND tenant_id = ? AND sec_uid = ?;

-- 查还原度报告
SELECT id, source_type, scene_type, source_id, monitor_type, status, is_read, confirmed_at, report_body_id, create_date
FROM tb_script_monitor_report
WHERE source_id = ? AND monitor_type = 1 AND is_deleted = 0
ORDER BY create_date DESC;

-- 查 MongoDB 报告正文
db.script_monitor_report_body.findOne({ _id: NumberLong(<report_body_id>) })
```

## 附录 B：典型错误码场景速查

| 错误码 | 触发场景 | 影响接口 |
|---|---|---|
| 70001 | AI Token < 阈值 | 3 / 6 / 2 |
| 70002 | 还原度授权位用完 | 2 / 6 |
| 70004 | 竞品账号操作 | 6 / 2 |
| 70005 | 未确认标准稿 + 开关 / 触发 | 2 / 6 |
| 70008 | AI 生成超时 / 失败 | 3 |
| 70011 | 跨租户访问 | 9 |
| 70012 | 报告不存在 / 删除 | 9 |
| 70013 | 参数校验失败 | 3 / 4 / 6 |
| 70014 | 上传文件场景 + 还原度 | 6 |
