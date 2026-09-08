# Explore Report — restoreDegree-slice-c

## Spec Inference

- **What**: 实装话术还原度 Slice C — 新建 `fidelityReportDetail` GET API（1:1 mirror QualityReportDetailVo 13 字段）+ 适配 `reportStatus` / `batchReportStatus` 装配 monitorType=1 还原度状态 + AI 复盘 / AI 切片 / 云空间三处入口展示；视频分析 + 文案预审 sourceType=1 跳过；triggerReport(monitorType=1) + sourceType=1 加 guard 拒绝。
- **Why**: 还原度功能至此完整闭环（Slice A 配置 + Slice B 生成 + Slice C 详情/列表/入口），前端可拿到 GENERATED 报告全文与摘要。
- **Scope shape**: MEDIUM 风险 + 主要 mirror 质检/巡检既有范式（qualityReportDetail / patrolReportDetail / pickMonitorEnabled / buildSummaryObject），跨 ≤2 模块（replay-ai 主 + replay-api Controller endpoint），无 DDL / 无 SPI 新增。

## Acceptance Criteria

### Happy Path

- **AC-001 (fidelityReportDetail 录制人首次查看)**: Given 已登录录制人，reportId 对应 monitorType=1 还原度报告且 status=GENERATED，when GET `/replay/script-monitor/fidelityReportDetail?reportId=<id>`，then 返 R<FidelityReportDetailVo> 含 13 字段（mirror QualityReportDetailVo），isRead 写库为 1，confirmedAt 写当前时间（独立事务 fail-safe，mirror 质检 markReadIfNeeded）。

- **AC-002 (fidelityReportDetail 非录制人查看)**: Given 已登录非录制人但同租户用户，when GET 详情，then 返 R<FidelityReportDetailVo>，**不触发 markReadIfNeeded**（isRead 不变）。

- **AC-003 (reportStatus 适配 monitorType=1)**: Given 同 sourceType+sceneType+sourceId 还原度报告 status=GENERATED，when 前端调 `reportStatus`，then 返回的 monitorType=1 三类报告状态对象中含 summary（markdown 文本透传 summaryJson）+ status + 红点 + 确认权限。

- **AC-004 (batchReportStatus 适配)**: Given 多资源批量查询，when 调 `batchReportStatus`，then 数组元素中 monitorType=1 还原度状态完整装配（mirror 质检 / 巡检）。

### Edge Case ACs

- **AC-005 (报告不存在)**: Given reportId 不存在 / 已删除 / 不属于当前租户（非云空间分享场景），when GET 详情，then 抛 BusinessException(70011 SCRIPT_MONITOR_NO_PERMISSION) 或 70012（report_not_exist），mirror 质检 detail。

- **AC-006 (triggerReport sourceType=1 + monitorType=1 拒绝)**: Given 前端调 triggerReport monitorType=1 + sourceType=1（上传文件），when ScriptMonitorBll.triggerReport 校验，then 抛 BusinessException(70014 SCRIPT_MONITOR_FEATURE_NOT_AVAILABLE，message="还原度暂不支持上传文件场景")，**不**进入 FidelityGenerateBll（避免无 secUid 抛 70013）。

- **AC-007 (markReadIfNeeded 异常容错)**: Given 录制人首次查看时 markReadIfNeeded 写库失败（DB 异常），when detail 接口返回，then 详情正常返（fail-safe），错误降级 warn log，mirror 质检既有范式。

- **AC-008 (回归 质检 monitorType=0 + 巡检 monitorType=2)**: Given Slice C 改动 reportStatus / batchReportStatus 适配 monitorType=1 分支后，when 调 reportStatus 含 monitorType=0/2 资源，then 质检 / 巡检状态装配不变（buildSummaryObject 既有逻辑保留）。

## Hidden Scope（grep callers / 受影响代码点）

- `ScriptMonitorBll.qualityReportDetail` / `patrolReportDetail` — mirror 范式参考，**不动**
- `ScriptMonitorBll.assembleReportStatus` / `buildSummaryObject` / `pickMonitorEnabled` — Slice C 加 monitorType=1 分支，**不破坏质检/巡检既有逻辑**
- `ScriptMonitorBll.markReadIfNeeded` — mirror 复用，**不动**（fail-safe 独立事务范式）
- `ScriptMonitorReportEntity` — **不动**（Slice B 已拍板永久不补 score/speechSpeed/deviationSummary 字段）
- `ScriptMonitorReportWriteService.finishReport` — **不动**（Slice B 三参签名稳定）
- `ScriptMonitorController` — 新增 `fidelityReportDetail` endpoint
- `ScriptMonitorBll.triggerReport` 入口 — 加 sourceType=1 + monitorType=1 guard（AC-006）
- `ScriptMonitorApi.autoTriggerForVideo` — **不动**（Slice B 已仅按 sourceType=0 触发还原度）
- frontend-api wiki `.claude/llm_wiki/wiki/frontend-api/script_monitor.md` — 同步 fidelityReportDetail 完整契约（从 TBD 改为 GA）

## Recommended Allowed Scope（Implement 阶段 focus_card 候选）

### replay-ai (主)
- `replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorBll.java`（新增 fidelityReportDetail 方法 + 适配 reportStatus 装配 + triggerReport guard）
- `replay-ai/src/main/java/com/jiuyu/replay/ai/vo/FidelityReportDetailVo.java`（**新建**，1:1 mirror QualityReportDetailVo 13 字段）
- `replay-ai/src/main/java/com/jiuyu/replay/ai/vo/MonitorTypeStatusVo.java`（如有 monitorType=1 装配字段调整 — TBD 看现有代码）

### replay-api
- `replay-api/src/main/java/com/jiuyu/replay/api/controller/words/ScriptMonitorController.java`（新增 GET `/fidelityReportDetail` endpoint）

### Tests (test-engineer 阶段)
- `replay-ai/src/test/java/com/jiuyu/replay/ai/bll/ScriptMonitorBllTest.java`（扩 fidelityReportDetail 5+ case + reportStatus monitorType=1 装配 case + triggerReport sourceType=1 guard case）

## Wiki Sources Consulted

- PRD `docs/2.6.01/REQ-2026-0508-话术还原度/REQ-2026-0508-话术还原度.md` v1.3（R-P0-007 输出展示 / R-P0-008 报告覆盖页面 / R-P0-009 重新生成数据处理 / R-P0-011 灰度隐藏）
- 任务行 `docs/2.6.01/TASK-BREAKDOWN-话术智能监控.md` T28 + T29
- Slice A/B 归档 (cueType/标准稿/B3/B8/触发链/报告生成 全就位)
- `.claude/llm_wiki/wiki/frontend-api/script_monitor.md`（fidelityReportDetail 当前 TBD B5 标记）
- 质检详情范式 `replay-ai/.../ScriptMonitorBll.java` qualityReportDetail + QualityReportDetailVo
- 巡检详情范式 patrolReportDetail + InteractionPatrolReportDetailVo（markdown 语义改造对齐）
- 用户 2026-06-12 拍板：B 出参 1:1 mirror QualityReportDetailVo 13 字段（不加 3 还原度专属字段）+ D 范围缩减（视频分析 + 文案预审 跳过）

## Open Questions（5 项议题 A-E）

- **A 入参契约**：reportId 单参（mirror 质检 `qualityReportDetail(Long reportId)`），跨租户分享除已分享到云空间外抛 70011。**推荐：mirror 质检完全一致**。
- **B 出参字段**：1:1 mirror QualityReportDetailVo 13 字段（**用户 2026-06-12 已拍板**）。
- **C reportStatus 装配**：mirror `buildSummaryObject` 按 monitorType 分支，monitorType=1 还原度返 summaryJson markdown 字符串透传（与质检/巡检 monitorType=0/2 装配范式一致）。**推荐：mirror 现有路由**。
- **D 多页面适配范围**：用户已拍板 — AI 复盘 / AI 切片 / 云空间 ✓；视频分析 / 文案预审 ❌。**前端契约约定 + 后端 reportStatus 不返 sourceType=1 的 monitorType=1 数据（或返 status=4 NOT_APPLICABLE）**。架构师拍板：前端约定 vs 后端 status=4 vs 后端不返该字段。
- **E triggerReport sourceType=1 + monitorType=1 拒绝**：架构师拍板：(a) 用 70014 复用 + message="还原度暂不支持上传文件场景" vs (b) 引入新错误码 vs (c) 静默 status=4 NOT_APPLICABLE 不拒绝。**推荐 (a) 复用 70014**（不扩 StatusCode，与 Slice A B3/B8 70014 同范式）。
