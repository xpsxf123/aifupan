---
spec_mode: STANDARD
risk: MEDIUM
frontend-facing: true
module: script-monitor
triggers: [api, domain]
slug: restoreDegree-slice-c
launch_spec: ../launch_spec_20260612_232826.md
---

# 话术还原度 Slice C — 报告详情 + 多页面适配

## 1. Context

- **Business goal (one sentence):** 实装话术还原度 Slice C — 新建 `fidelityReportDetail` GET API（1:1 mirror `QualityReportDetailVo` 13 字段）+ 适配 `reportStatus`/`batchReportStatus` 装配 monitorType=1 还原度状态 + `triggerReport` sourceType=1 + monitorType=1 guard，使还原度功能完整闭环于 sourceType=0 录制视频专属场景。
- **Scope of change:**
  - `replay-ai`：新建 `FidelityReportDetailVo`（mirror `QualityReportDetailVo` 13 字段）；`ScriptMonitorBll` 新增 `fidelityReportDetail` 方法 + `triggerReport` sourceType=1 guard；`buildSummaryJson` 已可复用（无需修改）
  - `replay-api`：`ScriptMonitorController` 新增 `GET /fidelityReportDetail` endpoint
- **Dependencies consulted:**
  - `explore_report.md` — AC list、Hidden Scope、范式参考确认
  - `wiki/frontend-api/script_monitor.md` — 3.11 fidelityReportDetail 当前 TBD
  - `QualityReportDetailVo.java` — 13 字段精确核实
  - `ScriptMonitorBll.java` — qualityReportDetail/patrolReportDetail mirror 范式 + triggerReport 插入点
- **Explorer hand-off:** `.claude/runs/Change__2026-06-12_23-27-59/explore_report.md`

## 2. Domain Model

Not applicable — 无新业务术语、无新状态机、无新枚举。`FidelityReportDetailVo` 为已有域概念的 VO 实现，monitorType=1 枚举值 Slice B 已就位。

## 2.5 Business Architecture

Not applicable — Slice C 不新增跨模块 Feign/MQ 通路；所有跨模块 SPI（`AnchorVideoFeign`、`StandardScriptFeign` 等）Slice A/B 已就位，Slice C mirror 既有调用路径。

## 3. API Contract

### 3.1 GET /replay/script-monitor/fidelityReportDetail

**鉴权：** Header `Authorization: Bearer <jwt>` 必填；`tenantId`/`userId` 从 JWT 取，前端不传。

**请求参数（Query）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| reportId | Long | Y | 非 null，> 0 | 报告 ID（Snowflake） |

**响应 data 结构：** `FidelityReportDetailVo`

| 字段 | 类型 | 说明 |
|---|---|---|
| reportId | string（Long序列化） | 报告 ID |
| sourceType | number | 资源类型（还原度固定 0=录制视频）|
| sceneType | number | 业务场景（固定 0=复盘）|
| sourceId | string | 资源 ID |
| status | number | 报告状态（枚举见 script_monitor.md）|
| summaryJson | string \| null | 摘要 markdown 文本（字段名为历史命名，实际存 markdown；mirror 巡检 patrol-summary-md-cast 范式；未生成时 null）|
| reportContent | string \| null | 报告正文 markdown 全文（来自 MongoDB）|
| anchorName | string \| null | 主播名称 |
| liveTitle | string \| null | 直播标题 |
| liveTime | string（Date 序列化）\| null | 直播时间（录制开始时间）|
| isRead | number | 是否已读 0=未读 1=已读 |
| confirmedAt | string（Date 序列化）\| null | 录制人首次查看时间；未读时 null |
| createDate | string（Date 序列化）| 报告创建时间 |

**错误码：**

| code | 含义 |
|---|---|
| 70011 | 无数据读取权限（跨租户非云空间分享）|
| 70012 | 报告不存在或已删除 |
| 70013 | 参数校验失败（reportId 为 null 或非法）|

**示例请求：**
```http
GET /replay/script-monitor/fidelityReportDetail?reportId=1234567890123456789 HTTP/1.1
Authorization: Bearer eyJhbGciOi...
```

**示例响应：**
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
    "summaryJson": "**还原度摘要**\n\n整体节奏接近标准稿，产品利益点讲解略有遗漏。",
    "reportContent": "# 话术还原度报告\n\n...",
    "anchorName": "张三",
    "liveTitle": "618大促直播",
    "liveTime": "2026-06-01 20:00:00",
    "isRead": 1,
    "confirmedAt": "2026-06-12 23:30:00",
    "createDate": "2026-06-12 22:00:00"
  }
}
```

### 3.2 reportStatus / batchReportStatus — monitorType=1 装配

现有 `reportStatus` / `batchReportStatus` 接口不新增字段，仅确保 `monitors[]` 中 monitorType=1 条目的 `summary` 字段已正确装配（markdown 文本透传 `summaryJson`）。`buildSummaryJson` 方法现已直接返回 `report.getSummaryJson()`，monitorType 无关，无需分支改造。**此处无接口契约变更，仅确认装配路径通畅。**

## 4. Data Model

Not applicable — 无 DDL，无新 MongoDB collection，无新 Redis 结构。所有字段 Slice A/B 已就位于 `tb_script_monitor_report`。

## 5. Business Logic

### 5.1 fidelityReportDetail 快乐路径

1. Controller 接收 `@RequestParam Long reportId`，调 `ScriptMonitorBll.fidelityReportDetail(reportId)`。
2. `reportId` 为 null → 抛 `BusinessException(SCRIPT_MONITOR_PARAM_INVALID, "reportId 不能为空")`。
3. `currentUser()` 取当前用户，`reportService.getById(reportId)` 查主表。
4. report 为 null 或 `isDeleted=1` → 抛 `BusinessException(SCRIPT_MONITOR_REPORT_NOT_EXIST, 70012)`。
5. `report.getTenantId()` != `user.getActiveTenantId()` → 抛 `BusinessException(SCRIPT_MONITOR_NO_PERMISSION, 70011)`（非云空间分享场景；mirror 质检/巡检权限规则）。
6. `loadSingleVideo(report.getSourceType(), report.getSourceId())` 加载视频信息（可为 null，不抛，log.warn）。
7. `hasConfirmPermission(user, video)` 判断是否为录制人。
8. 录制人 且 `report.isRead == 0` → `try { writeService.markReadIfNeeded(reportId, user.getActiveTenantId()); report.setIsRead(1); report.setConfirmedAt(new Date()); } catch (Exception e) { log.warn(...) }` — fail-safe 独立事务，mirror 质检/巡检范式。
9. `reportBodyRepository.findByReportId(reportId)` 查 MongoDB 正文（Optional → null 兜底）。
10. 装配 `FidelityReportDetailVo`（13 字段，mirror 质检 VO 装配逻辑）：
    - `sourceType=0` → 从 `video` 填充 `anchorName`/`liveTitle`/`liveTime`
    - `sourceType=1` → **不会发生**（还原度 sourceType 固定 0；但为防御性编程，可复用 `fillAnchorFieldsFromUploadFile` 空实现或 log.warn 兜底）
11. 返回 `FidelityReportDetailVo`，Controller 包装 `R.ok(vo)` 返回。

### 5.2 triggerReport sourceType=1 + monitorType=1 guard (AC-006)

**插入位置：** `ScriptMonitorBll.triggerReport` 方法中，在现有 `sourceType-sceneType 非法组合` 校验之后、`loadSingleVideo` 之前（第一个 if 块后紧跟新增 guard）。

```
// Slice C：还原度不支持上传文件场景
if (Objects.equals(monitorType, MonitorTypeEnum.FIDELITY_MONITOR.getCode())
        && Objects.equals(sourceType, MonitorSourceTypeEnum.UPLOAD_FILE.getCode())) {
    throw new BusinessException(StatusCode.SCRIPT_MONITOR_FEATURE_NOT_AVAILABLE.getCode(),
            "还原度暂不支持上传文件场景");
}
```

> **BusinessException 构造范式（mirror Slice B 既有用法）：** 项目使用 `(int code, String msg)` 重载，不是 `(StatusCode enum, String msg)`。所有抛点（70011 / 70012 / 70013 / 70014）均显式调 `.getCode()` 取整型 code，避免编译错。

**议题 E 决策（inline）：** 复用 70014 `SCRIPT_MONITOR_FEATURE_NOT_AVAILABLE`（含 message 覆盖），**不引入新错误码**。理由：与 Slice A B3/B8 70014 同范式，StatusCode 不扩展，错误码体系稳定；"上传文件场景不可用" 语义与 70014 功能未上线完全对齐；message 字符串可覆盖区分来源。

**议题 D 决策（用户已拍板 + inline 确认）：** 视频分析/文案预审入口通过前端契约约定（前端 reportStatus 请求中 sourceType=1 时不传 monitorType=1）+ 后端 reportStatus 中 monitorType=1 报告自然不存在（DB 无 sourceType=1 的还原度报告行）→ status=0 NOT_GENERATED，**无需后端主动返 status=4**。`batchReportStatus` 行为一致。此方案最简，无新代码分支，符合 YAGNI。

### 5.3 Idempotency / replay safety

- `fidelityReportDetail` 为 GET 只读（含 markReadIfNeeded 写已读），fail-safe 独立事务，写失败详情正常返——无重复提交风险，无需 `@NoRepeatSubmit`。
- `triggerReport` guard 为前置拒绝，无状态写入，无幂等性问题。

## 5.5 Technical Architecture

Not applicable — Slice C 无新跨模块 Feign 通路、无新 MQ topic、无 @Scheduled job、无新 Redis cache、无分布式锁。`markReadIfNeeded` 独立事务范式 Slice B/B9 已就位，Slice C 复用。

## 6. Non-Functional Constraints

- **DI：** 构造器注入，禁 `@Autowired`，禁 `@Resource`（新类）。
- **Controller 返回值：** 统一 `R<T>`（`com.jiuyu.replay.generic.vo.common.R`），禁裸返业务对象。
- **ID 序列化：** 全局 `JacksonSerializerConfig` Long → String，`FidelityReportDetailVo.reportId` 声明 `Long` 即可，前端收 string。
- **跨模块调用：** 禁直接 import replay-words / replay-power 内部 Service/Dao；只走 replay-generic Feign SPI。
- **代码标识符：** 绝对禁止中文标识符（字段名/方法名/类名/变量名/常量名）。
- **租户隔离：** detail 接口必须校验 `report.tenantId == user.activeTenantId`（mirror 质检/巡检）。
- **软删除：** 查报告时必须检查 `isDeleted == 1`（手动，禁 `@TableLogic`）。
- **时间戳：** `confirmedAt` 内存 in-place 写 `new Date()`（mirror 质检 B9 范式），不在此方法加 `@Transactional`（markReadIfNeeded 已有独立事务）。
- **Java 标识符命名：** `FidelityReportDetailVo` / `fidelityReportDetail` 与 PRD/任务行保持一致。
- **`summaryJson` 字段：** Schema description 写"摘要 markdown 文本（字段名为历史命名，实际存 markdown）"，保留历史字段名（mirror 质检 VO 命名）。
- **IN 子句：** fidelityReportDetail 为单报告查询，无 IN 风险；如后续扩批量需按 500 分批。

## 6.5 Design Patterns

Not applicable — Slice C 为纯 mirror 实现，不引入新设计模式，不重构既有模式。

## 7. Acceptance Criteria

（直接 mirror explore_report.md ACs，未重新推导。）

- **AC-001 (fidelityReportDetail 录制人首次查看):** Given 已登录录制人，reportId 对应 monitorType=1 报告且 status=GENERATED，when GET `/replay/script-monitor/fidelityReportDetail?reportId=<id>`，then **VO 返回**含 13 字段、`isRead=1`、`confirmedAt` 非 null（mirror 质检 B9 范式：报告对象 in-memory 改字段，DB 由独立事务异步持久化；DB 写失败时 fail-safe 走 AC-007）。
- **AC-002 (fidelityReportDetail 非录制人查看):** Given 已登录非录制人但同租户用户，when GET 详情，then 返 `R<FidelityReportDetailVo>`，不触发 `markReadIfNeeded`（`isRead` 不变）。
- **AC-003 (reportStatus 适配 monitorType=1):** Given 同 sourceType+sceneType+sourceId 还原度报告 status=GENERATED，when 调 `reportStatus`，then 返回 monitorType=1 条目含 `summary`（markdown 文本）+ `status=2` + `isRead` + `reportId`。
- **AC-004 (batchReportStatus 适配):** Given 多资源批量查询，when 调 `batchReportStatus`，then 数组元素中 monitorType=1 还原度状态完整装配（summary/status/isRead 字段均正确）。
- **AC-005 (报告不存在):** Given reportId 不存在 / 已删除 / 不属于当前租户（非云空间分享），when GET 详情，then 抛 `BusinessException(70012)` 或 `BusinessException(70011)`（mirror 质检）。
- **AC-006 (triggerReport sourceType=1 + monitorType=1 拒绝):** Given 调 `triggerReport` 且 `monitorType=1` + `sourceType=1`，when 校验，then 抛 `BusinessException(70014, "还原度暂不支持上传文件场景")`，不进入 `FidelityGenerateBll`。
- **AC-007 (markReadIfNeeded 异常容错):** Given 录制人首次查看时 `markReadIfNeeded` 写库失败，when detail 接口返回，then 详情正常返（fail-safe），错误降级 warn log。
- **AC-008 (回归 质检 monitorType=0 + 巡检 monitorType=2):** Given Slice C 改动后，when 调 `reportStatus` 含 monitorType=0/2 资源，then 质检/巡检状态装配不变（`buildSummaryJson` 既有逻辑保留）。

**Unit test 要点（@test-engineer 参考）：**

| AC-id | 被测方法 | 关键断言 |
|---|---|---|
| AC-001 | `ScriptMonitorBll#fidelityReportDetail` | isRead=1，confirmedAt 非 null，reportContent 非 null |
| AC-002 | `ScriptMonitorBll#fidelityReportDetail` | markReadIfNeeded 不被调用（verify(writeService, never())）|
| AC-005 | `ScriptMonitorBll#fidelityReportDetail` | 抛 BusinessException(70012) / 70011 |
| AC-006 | `ScriptMonitorBll#triggerReport` | 抛 BusinessException(70014，message 含"还原度暂不支持上传文件场景"）|
| AC-007 | `ScriptMonitorBll#fidelityReportDetail` | markReadIfNeeded 抛异常时 vo 仍正常返回 |
| AC-008 | `ScriptMonitorBll#buildMonitorStatus` | monitorType=0/2 summary 装配路径不变 |

## 8. Frontend Contract Publishing

- `frontend-facing: true`
- `module: script-monitor`

Phase 4 **before** Implement 完成后，dispatch `@frontend-api-doc-writer`（mode: forward）。

Agent 读取本 openspec §3 + §7，将 `script_monitor.md` 中 3.11 `fidelityReportDetail` 从 TBD 更新为完整契约（13 字段表 + 请求参数 + 错误码 + 示例）；同时更新 `batchReportStatus` / `reportStatus` 的 monitorType=1 `summary` 字段说明（从 `TBD` 更新为"Markdown 文本，与 monitorType=2 巡检同范式"）。

Phase 6 Archive：`@frontend-api-doc-writer`（mode: reverse）执行字段漂移核查。

## 9. Architecture Decision Records

Not applicable — Slice C risk=MEDIUM，不强制 Nygard ADR。议题 E（70014 复用 vs 新增错误码）已在 §5.2 inline 决策。

---

## Allowed Scope

- `replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorBll.java`
- `replay-ai/src/main/java/com/jiuyu/replay/ai/vo/FidelityReportDetailVo.java`（新建）
- `replay-api/src/main/java/com/jiuyu/replay/api/controller/words/ScriptMonitorController.java`

## 做什么 / 为什么

**现状：** 话术还原度 Slice A（配置）+ Slice B（报告生成）已完成，但前端无法查看还原度报告详情，`reportStatus` 接口中 monitorType=1 的 `summary` 字段有值但 `fidelityReportDetail` 接口尚未实现（TBD），triggerReport 对上传文件场景也未做防御性拒绝。

**需要：** 实装 `fidelityReportDetail` GET 接口（1:1 mirror 质检报告详情 13 字段），确认 `reportStatus`/`batchReportStatus` monitorType=1 装配路径通畅，并在 `triggerReport` 加 sourceType=1 + monitorType=1 guard 防止误触发。视频分析/文案预审入口通过前端契约约定跳过，后端无额外逻辑。

**范围：** replay-ai 2 文件（ScriptMonitorBll 方法扩展 + FidelityReportDetailVo 新建）+ replay-api 1 文件（Controller 新增 endpoint），共 3 文件。

---

## Plan Deviation Reflection

2026-06-13

### 1. Architect openspec 3 处技术细节需要 lead-engineer 实现时校准（用户审 openspec 前置发现）

**Deviation：** architect 写 openspec 时引用了不存在的方法 / 用错构造签名 / 错误码表缺一行：
- §5.1 Step 10 提到"复用 `fillAnchorFieldsFromUploadFile` 空实现" — 此方法**不存在**于现网代码（架构师假定可复用质检空兜底方法但未 grep 验证）。lead-engineer 实现时改为 log.warn 兜底。
- §5.2 BusinessException 构造范式写成 `(StatusCode enum, String msg)` — 项目实际使用 `(int code, String msg)` 重载（Slice B FidelityGenerateBll 既有用法）。lead-engineer 实现时按实际签名 `.getCode()` 调。
- §3.1 错误码表只列 70011 / 70012，但 §5.1 Step 2 提到 reportId null → 70013 — 错误码表缺一行。
- §7 AC-001 写"isRead 写库为 1"与 fail-safe 异步独立事务范式矛盾（应说"VO 返回 isRead=1，DB 持久化由 markReadIfNeeded 处理"）。

**Reason：** Architect Step 0 强制读 explore_report 后写 openspec，未 grep 现网代码验证细节假设（fillAnchorFieldsFromUploadFile / BusinessException 重载签名）。

**Cost：** 用户审 openspec 前置发现 4 项需修，3 处直接 Edit 修复（70013 补行 / AC-001 fail-safe 校准 / BusinessException 加 `.getCode()` + blockquote 说明）+ 1 项留 lead-engineer 实现时按实际兜底（fillAnchorFieldsFromUploadFile 不强行 mirror 不存在的方法）。**用户审是这次有效拦截，避免 lead-engineer dispatch 时按错误 spec 写代码再返工**。

### 2. focus_card 缺 test 路径 — 重复 Slice B Plan Deviation #5 未汲取教训

**Deviation：** test-engineer dispatch 时 scope_guard 拦了 — focus_card.md 只列 3 个 main code 文件，未含 `ScriptMonitorBllTest.java` 测试路径。test-engineer 第一次返回 BOUNDARY_EXCEPTION（设计好 7 case 清单但无法落盘）。主 agent 扩 focus_card 加测试路径后 re-dispatch 才通过。

**Reason：** **Slice B Plan Deviation #5 已记录同问题** — "建议未来 dispatch 模板默认含 `replay-ai/src/test/java/**` 等通用测试 Allowed Scope，避免每次扩 focus_card"。本 Slice C architect 写 focus_card 时仍只列 main code 3 文件，主 agent dispatch test-engineer 时也未预先检查。**这是会话学习未沉淀的反复踩坑**。

**Cost：** 一次 BOUNDARY_EXCEPTION（test-engineer 浪费 ~110s + 17 tool calls 重新设计 7 case 但无法落盘）+ 一次 focus_card Edit 扩 test 路径 + 一次 re-dispatch（test-engineer 重新跑 ~201s + 25 tool calls）。**累计成本 ~5min + ~40 tool calls** — 完全可通过 architect 写 focus_card 时预含 test 路径避免。

**Action item（写进 architecture-curator 范围）：** 下次 architect dispatch 模板应预含约定的 test 路径（`replay-ai/src/test/java/**` 等同模块测试目录），不依赖 lead-engineer 后手扩。

### 3. Test-engineer 自检修 3 处 import 类型错（写测试时发现实现细节）

**Deviation：** test-engineer 设计 mock 时假定的类型与实现实际类型不符：
- 假定 MongoDB body 类型为 `ScriptMonitorReportBodyDocument` → 实际是 `ScriptMonitorReportBodyEntity`（`@Field("content")` MongoDB 注解）
- 假定主播 nested 类型为 `AnchorInfoVo` → 实际是 `AnchorUrlInfoVo`（`com.jiuyu.replay.generic.vo.words` 包）
- 缺 `FidelityReportDetailVo` import — 加

**Reason：** test-engineer 写测试时通过 ScriptMonitorBll 调用链反向推断 mock 类型，初版猜错；compile error 后修正。这是测试写作的正常过程（写测试时发现实现细节），不算偏离开发流程，但说明 lead-engineer dispatch prompt 中可以预先列出"测试 mock 用的关键 Entity/Vo 类型"清单，让 test-engineer 不踩坑。

**Cost：** test-engineer 自检 + 自修 3 处类型，编译通过；零 main code 改动。

### 4. Code-reviewer Round 1 抓 MINOR #1 严格 mirror（小 fix）

**Deviation：** Code-reviewer Round 1 抓 fidelityReportDetail NOT_EXIST 抛 BusinessException 用 `(int, String)` 双参带 message 覆盖，而 mirror 基准 qualityReportDetail 用 `(StatusCode enum)` 单参。功能等价（errorCode 70012 相同），但严格 mirror 风格更一致。

**Reason：** lead-engineer 实现时 mirror 质检但加了 "或已删除" 额外 message，未严格 1:1。MINOR 级别可选 fix。

**Cost：** 1 行 Edit 修复（去掉双参带 message，改单参 enum 默认 "报告不存在"，errorCode 不变 + 7 case 测试全通过证明等价）。Round 2 CONFIRMED FIXED。

### 5. MINOR #2 linter 误报明确判断 + 文档化（不修原因记录）

**Deviation：** comment_linter_java 误判 `FidelityReportDetailVo.java:21` / `ScriptMonitorController.java:46,116` "public type/method missing Javadoc"。实际 Javadoc 完整，linter 只检查 `public` 前一行是否 `/**`，遇 `@Data`/`@Operation` 等注解夹层即误判。

**Reason：** linter 工具自身缺陷，**pre-existing 全模块全域同问题**（既有 QualityReportDetailVo / InteractionPatrolReportDetailVo / 整个 controller 层都触发同样误报），非本 Slice 新引入。

**Cost：** 不修。Round 2 code-reviewer 也确认 linter 误报 + 文档化。建议未来独立任务修 linter 脚本（lookback 多行 + 跳过 `@注解`），但**不在 Slice C 范围**。

