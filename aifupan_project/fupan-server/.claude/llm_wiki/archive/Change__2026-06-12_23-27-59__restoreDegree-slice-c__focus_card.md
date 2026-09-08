# Focus Card — restoreDegree-slice-c

## Goal (one sentence)
实装话术还原度 fidelityReportDetail GET 接口 + reportStatus monitorType=1 装配确认 + triggerReport sourceType=1 guard。

## Non-Goals (out of scope)
- 视频分析（sourceType=1, sceneType=1）入口适配
- 文案预审（sourceType=1, sceneType=2）入口适配
- score / speechSpeed / deviationSummary 字段填充
- 新增任何 DDL / Entity 字段
- reportStatus / batchReportStatus 接口契约字段变更

## Allowed Scope

### Main code
- replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorBll.java
- replay-ai/src/main/java/com/jiuyu/replay/ai/vo/FidelityReportDetailVo.java
- replay-api/src/main/java/com/jiuyu/replay/api/controller/words/ScriptMonitorController.java

### Tests (test-engineer Slice C QA 扩展)
- replay-ai/src/test/java/com/jiuyu/replay/ai/bll/ScriptMonitorBllTest.java

## Stop Rules

- 编辑超出 Allowed Scope → 立即停止，发起 [Boundary Exception Request]
- 同一 phase 失败 ≥3 次 → 停止上报
