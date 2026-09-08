# Focus Card — restoreDegree-slice-b

## Goal (one sentence)
解除 monitorType=1 三处限闸，新建 ScriptMonitorFidelityGenerateBll（串行 4 次 AI 调用）+ StandardScriptFeign 跨模块 SPI，打通话术还原度报告生成核心链路。

## Non-Goals (out of scope)
- fidelityReportDetail API（GET 报告详情）→ Slice C
- 多页面适配 → Slice C
- score/deviation_summary 字段填充 → Slice C
- DDL 变更（无）
- 提示词内容修改（无）

## Allowed Scope

### replay-ai
- `replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorBll.java`
- `replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorMqHandler.java`
- `replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorFidelityGenerateBll.java`（新建）
- `replay-ai/src/main/java/com/jiuyu/replay/ai/api/ScriptMonitorApi.java`

> 删除（2026-06-12 用户拍板收紧 — finishReport 签名不动 + Entity 不补字段）：
> - ~~ScriptMonitorReportWriteService.java~~（不动 — FidelityGenerateBll 直接调公共 finishReport）
> - ~~impl/ScriptMonitorReportWriteServiceImpl.java~~（不动 — 同上）
> - ~~entity/ScriptMonitorReportEntity.java~~（不动 — score/speechSpeed/deviationSummary 全永久不写）

### replay-words
- `replay-words/src/main/java/com/jiuyu/replay/words/api/StandardScriptApi.java`（新建）

### replay-generic
- `replay-generic/src/main/java/com/jiuyu/replay/generic/feign/words/StandardScriptFeign.java`（新建）
- `replay-generic/src/main/java/com/jiuyu/replay/generic/vo/words/StandardScriptInfoVo.java`（新建）

### Tests (test-engineer Slice B QA 扩展)
- `replay-ai/src/test/java/com/jiuyu/replay/ai/bll/ScriptMonitorBllTest.java`（扩 — 补 StandardScriptFeign 第 12 参 mock）
- `replay-ai/src/test/java/com/jiuyu/replay/ai/bll/ScriptMonitorBllAutoTriggerTest.java`（扩）
- `replay-ai/src/test/java/com/jiuyu/replay/ai/bll/ScriptMonitorBllReportStatusTest.java`（扩）
- `replay-ai/src/test/java/com/jiuyu/replay/ai/bll/ScriptMonitorBllSummaryTest.java`（扩）
- `replay-ai/src/test/java/com/jiuyu/replay/ai/bll/ScriptMonitorFidelityGenerateBllTest.java`（新建 — 还原度生成 8+ case）
- `replay-ai/src/test/java/com/jiuyu/replay/ai/bll/ScriptMonitorMqHandlerTest.java`（扩 — fidelity tag 分发）
- `replay-ai/src/test/java/com/jiuyu/replay/ai/api/ScriptMonitorApiTest.java`（扩 — autoTriggerForVideo monitorType=1）

## Stop Rules
- 编辑超出 Allowed Scope → 立即停止，发起 [Boundary Exception Request]
- 同一 phase 失败 ≥3 次 → 停止上报
- 涉及 DDL 变更 → [Boundary Exception Request]（本 Slice 无 DDL）
