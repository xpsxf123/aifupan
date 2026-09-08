---
spec_mode: STANDARD
risk: MEDIUM
frontend-facing: false
triggers: []
---

# 三类监控报告 detail 接口放权到云空间分享场景

## Human Section（做什么 / 为什么 / 怎么做 / 待确认项）

- **做什么**：把 `qualityReportDetail` / `patrolReportDetail` / `fidelityReportDetail` 三个接口的 read 权限从「同租户」放宽到「同租户 **OR** 报告对应视频已上传云空间」。
- **为什么**：主播将录制视频上传到云空间即等同于公开分享，分享接收方可能是任何登录态用户（含跨租户），需要让其拿到 reportId 后可直接查看 detail 报告。当前接口对跨租户访问者一律抛 70011，导致分享链无法访问。
- **怎么做**：3 个方法内 video 加载顺序前移到权限校验之前；权限判定从「`report.tenantId == user.activeTenantId` 否则抛 70011」改为「同租户 **OR** `video.uploadStatus == 1` 否则抛 70011」。其余逻辑（写已读规则、报告装配、video.null 兜底 log）均不变。
- **待确认项**：无（与用户已对齐 sourceType=1 / video=null / 不改 reportStatus 三点边界）。

## 1. Context

- **Business goal (one sentence):** 让云空间分享接收方可以查看话术质检 / 互动巡检 / 话术还原度三类报告详情，扩大已分享报告的曝光面。
- **Scope of change:**
  - `replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorBll.java` — 3 个 detail 方法权限分支镜像改造
  - `replay-api/src/main/java/com/jiuyu/replay/api/controller/words/ScriptMonitorController.java` — 仅 Javadoc 同步描述新读权限规则
  - `replay-ai/src/test/java/com/jiuyu/replay/ai/bll/ScriptMonitorBllTest.java` — 单测覆盖新分支
- **Dependencies consulted:** 主上下文已读 `ScriptMonitorBll.java` / `ScriptMonitorController.java` / `AnchorVideoEntity.java` / `AnchorVideoInfoVo.java`；无 wiki drill 需要。
- **Explorer hand-off:** `<run_dir>/explore_report.md`

## 2. Domain Model
Not applicable — 不引入新业务术语 / 状态机 / 枚举；`upload_status` 已是既有字段（0 未上传 / 1 已上传），语义不变。

## 2.5 Business Architecture
Not applicable — 改动仅在 `replay-ai` 单模块内，未触达上下游系统或跨模块新增调用（`AnchorVideoFeign` 在 `loadSingleVideo` 已存在，仅顺序前移）。

## 3. API Contract
Not applicable — 三个 detail 端点 path / method / 请求参数 / 响应 schema / 错误码均不变；仅放宽内部权限判定，原本抛 70011 的部分场景改为正常返回 200。

## 4. Data Model
Not applicable — 无 DDL；不新增字段；不改持久化结构。

## 5. Business Logic

### 5.1 三个方法的镜像改造

**改造前**（伪代码，3 个方法结构一致）：

```
report = reportService.getById(reportId)
报告不存在/已删 → 抛 SCRIPT_MONITOR_REPORT_NOT_EXIST
if (report.tenantId != user.activeTenantId)
    throw 70011 "无数据读取权限"
video = loadSingleVideo(...)  // 在校验之后加载
video == null → log.warn 兜底
hasConfirmPermission(user, video) && isRead==0 → markReadIfNeeded
... 装配 VO 返回
```

**改造后**：

```
report = reportService.getById(reportId)
报告不存在/已删 → 抛 SCRIPT_MONITOR_REPORT_NOT_EXIST

// video 加载提到权限校验之前
video = loadSingleVideo(report.sourceType, report.sourceId)

sameTenant = (report.tenantId == user.activeTenantId)
publicShared = (video != null && video.uploadStatus == 1)
if (!sameTenant && !publicShared)
    throw 70011 "无数据读取权限"

video == null → log.warn 兜底（与原代码一致）
hasConfirmPermission(user, video) && isRead==0 → markReadIfNeeded（与原代码一致）
... 装配 VO 返回（与原代码一致）
```

### 5.2 三方法的差异点（保留原差异）

| 方法 | sourceType 处理差异 | 装配差异 |
|---|---|---|
| `qualityReportDetail` | sourceType=1（上传文件）走 `fillAnchorFieldsFromUploadFile` 装配主播信息 | 跨租户 + sourceType=1 必抛 70011（loadSingleVideo 对 sourceType=1 走 SensitiveWordsFeign，跨租户语义上 video=null 兜底，publicShared=false） |
| `patrolReportDetail` | sourceType 仅录制视频实际命中 | 镜像 quality |
| `fidelityReportDetail` | sourceType=1 直接 `log.warn`，video=null；sourceType=0 正常加载 | sourceType=1 异常分支天然 publicShared=false → 跨租户必抛 70011 |

### 5.3 不变项（明确锁定）

- `hasReadPermission` / `hasConfirmPermission` 两个工具方法**不动**——前者服务 `reportStatus` / `batchReportStatus` 列表场景（按 video.tenantId 判断，与本次 read scope 解耦）；后者保留「同租户 + 录制人本人」写已读触发条件。
- 跨租户分享接收方调用 detail **不写已读**（`hasConfirmPermission` 要求 `video.userId == user.id`，跨租户天然不满足）。
- `reportStatus` / `batchReportStatus` 跨租户语义不变——本次不放权。
- 错误码 70011 沿用；不引入新错误码。

## 5.5 Technical Architecture
Not applicable — 无 MQ / 分布式锁 / 缓存 / 定时任务变动；事务边界不跨方法；`loadSingleVideo` 已有的 Feign 调用顺序前移不构成新跨模块编排。

## 6. Non-Functional Constraints

- **性能**：每个 detail 调用新增 0 次 DB / Feign 调用——`loadSingleVideo` 原本就执行，仅顺序前移到权限分支之前。
- **租户隔离**：放宽 read scope 是业务意图（主播主动上传到云空间即同意公开分享），不视为隔离漏洞；写入路径（`markReadIfNeeded`、`triggerReport` 等）租户隔离不变。
- **可观测性**：`video == null` 既有 log.warn 保留；不新增日志埋点。
- **回滚**：单文件改动，回滚 = 还原 3 个方法的权限分支；无 DDL / 数据迁移；秒级回滚。
- **安全**：
  - 不引入新外部入参（不收 `userId`、不收 `tenantId`，从 JWT 取 `currentUser()` 与原一致）。
  - SQL 占位不变（无 SQL 改动）。
  - 跨租户读路径在响应 VO 中不暴露其他租户的敏感字段（VO 已有字段均为业务展示字段，未含 phone/email 等 PII）。

## 6.5 Design Patterns
Not applicable — 不引入或改变命名模式。

## 7. Acceptance Criteria

- **AC-1（同租户基准）**: Given 用户 A 的 `activeTenantId == report.tenantId`, when 调用 `qualityReportDetail` / `patrolReportDetail` / `fidelityReportDetail` 任一接口, then HTTP 200 + 完整 VO；若 A 是录制人本人（`video.userId == A.id`）且 `report.isRead==0` → 触发 `markReadIfNeeded` 写入 `is_read=1, confirmed_at=NOW()`。
- **AC-2（跨租户已分享 → 放行）**: Given 用户 B 的 `activeTenantId != report.tenantId` 且 `video != null && video.uploadStatus == 1`, when 调用三个 detail 任一, then HTTP 200 + 完整 VO；`tb_script_monitor_report.is_read` 保持原值（跨租户必然不触发 `hasConfirmPermission`）。
- **AC-3（跨租户未上传 → 拒绝）**: Given 用户 B 跨租户 且 `video != null && video.uploadStatus == 0`, when 调用三个 detail 任一, then 抛 `BusinessException(SCRIPT_MONITOR_NO_PERMISSION, "无数据读取权限")` (HTTP 200 + R.code=70011)。
- **AC-4（跨租户 + video 已删/不存在 → 拒绝）**: Given 用户 B 跨租户 且 `loadSingleVideo` 返回 null, when 调用三个 detail 任一, then 抛 70011（无法证明已分享 → 按未分享处理）。
- **AC-5（跨租户 + sourceType=1 → 拒绝）**: Given 用户 B 跨租户 且 `report.sourceType == 1`, when 调用 `qualityReportDetail`（patrol / fidelity 同), then 抛 70011（sourceType=1 路径加载的 video 不参与云空间分享语义，publicShared 必为 false）。
- **AC-6（写已读规则不退化）**: Given 同租户但非录制人本人 OR 跨租户分享接收方, when 调用任一 detail, then `is_read` 字段保持原值；不触发 `markReadIfNeeded`。
- **AC-7（报告不存在/已删 → 优先于权限）**: Given `reportService.getById(reportId)` 返回 null 或 `isDeleted==1`, when 调用任一 detail, then 抛 `SCRIPT_MONITOR_REPORT_NOT_EXIST`（不暴露权限差异，保留原优先顺序）。

## 8. Frontend Contract
Not applicable — `frontend-facing: false`；请求 / 响应 schema 不变；前端无需配合改动；行为变化仅是「原本跨租户拿到的 70011 错误码在云空间分享场景下变成 200」，属向后兼容放宽。

## 9. ADRs
Not applicable — MEDIUM risk；设计无真正分歧（用户已定 upload_status 为唯一判定来源）。

## 10. Review & QA Outcome

### Code Review (`@code-reviewer`, 2 files, 4 findings)

- **CRITICAL / MAJOR (FIX)**:
  - MAJOR #1 — `ScriptMonitorController.java:83` `qualityReportDetail` 的 `@return` 残留 B9 已删的"角色已知晓列表"+ 缺权限说明段。**已修**：与 patrol/fidelity 风格对齐，加跨租户云空间分享说明。
- **MINOR (不修，决策与理由)**:
  - MINOR #2 — `ScriptMonitorBll.java:473` fidelity sourceType=1 的 `log.warn` 发生在权限校验之前，理论上跨租户探测者可枚举 reportId 是否存在。**决定不修**：① reportId 是 19 位 Snowflake Long，不可枚举；② log 仅服务端可见、不返用户；③ quality/patrol 的 `video not found` 兜底 log 同样在校验之前，单点修 fidelity 反造成不一致；④ 该 log 本是给 SRE 看的数据异常诊断，校验后才打反而漏报。
  - MINOR #4 — comment_linter 报 "public type missing Javadoc" 为预存在误报（注解与 `public class` 间格式问题），非本次引入。**决定不修**：跨整目录预存，独立 cleanup 任务。

### QA (`@test-engineer`)

- 新建 `replay-ai/src/test/java/com/jiuyu/replay/ai/bll/ScriptMonitorBllReadPermissionTest.java` (+466/-0)
- **21 / 21 全绿**（3 detail 方法 × 7 AC），JDK 21 `mvn -pl replay-ai test -Dtest=ScriptMonitorBllReadPermissionTest` 0.723s。
- AC-5(fidelity) 加 `verify(sensitiveWordsFeign, never()).getUploadFileInfo(any())` 关键回归。
- 与既有 `ScriptMonitorBllTest`（盯同租户已读语义）互补、零重叠。

