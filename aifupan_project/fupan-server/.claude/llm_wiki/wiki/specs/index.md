# Active Specs (OpenSpec)

This index lists `openspec.md` documents that are currently in progress or recently finished and still frequently referenced.

## Hard Rules (MUST)
- When a spec reaches `Archive` and its stable knowledge has been extracted, you MUST move it out of the active list.
- After extraction, long-term knowledge lives in `api/`, `data/`, and `domain/` indexes. The spec remains only for traceability.

## In Progress / Recent

| 功能 | 当前阶段 | 文件 |
|---|---|---|
| 云空间视频批量删除回收站 | `Phase 3: Approval Gate (HIGH)` | `.claude/runs/Change__2026-05-21_18-36-11__voice_recycle_bin/openspec.md` |
| 销售智能体行为信号聚合端点 | `Phase 3: Review` | `.claude/runs/Change__2026-05-23_00-41-30/openspec.md` |
| 互动巡检后端能力 T31-T36 | `Phase 3: Implement` | `.claude/runs/Change__2026-06-04_21-26-14/openspec.md` |
| 巡检合并报告 JSON → Markdown 契约改造 | `Phase 2: Propose (Approval Gate 待确认)` | `.claude/runs/Change__2026-06-07_19-01-01/openspec.md` |
| B9 已读语义简化（confirmRead 删除 + detail 内置写已读 + is_read 迁主表） | `Phase 2: Propose (Approval Gate 待确认)` | `.claude/runs/Change__2026-06-08_13-17-24/openspec.md` |
| 话术还原度 Slice A — 标准稿能力 + B3/B8 限闸解除（T21/T23/T24 三 API + AiEnums 枚举重命名扩充 + 5 套提示词 INSERT） | `Phase 3: Approval Gate (HIGH)` | `.claude/runs/Change__2026-06-11_12-33-48/openspec.md` |
| 话术还原度 Slice B — 报告生成核心（T25/T26/T27）— 限闸解除 + FidelityGenerateBll + StandardScriptFeign SPI + autoTriggerReport(monitorType=1) | `Phase 2: Propose (Approval Gate 待确认)` | `.claude/runs/Change__2026-06-11_18-57-48/openspec.md` |
| 话术还原度 Slice C — fidelityReportDetail GET 接口 + reportStatus monitorType=1 装配 + triggerReport sourceType=1 guard（T28/T29）| `Phase 3: Review` | `.claude/runs/Change__2026-06-12_23-27-59/openspec.md` |

---

## Lifecycle SOP
- After `Propose`: add a new row with status `Phase 3: Review`.
- After `Archive`: remove it from the active list, and move it to "Recently Archived" (or fully transfer to `archive/index.md`).

### Append Template
```markdown
| {feature name} | `{current phase}` | `[{file_name.md}]` |
```

## Recently Archived
（Knowledge extracted into api/data/domain. This section is read-only traceability.）

| 归档日期 | 功能 | 摘要 | 链接 |
|---|---|---|---|
| 2026-05-19 | CRM Spec-A 查询 API | 批量聚合查询 + 手机号订单查询 | [2026-05-14-crm-spec-a-query-apis.md](./2026-05-14-crm-spec-a-query-apis.md) |
| 2026-05-19 | CRM Spec-B 画像写回 | `tb_user_details` 字段同步 + `tb_crm_ai_profile` 新增 | [2026-05-14-crm-spec-b-profile-writeback.md](./2026-05-14-crm-spec-b-profile-writeback.md) |
| 2026-05-19 | CRM Spec-C 阶段事件 | `tb_crm_stage_event` 入库 + 受控快照刷新 | [2026-05-14-crm-spec-c-stage-event.md](./2026-05-14-crm-spec-c-stage-event.md) |
| 2026-05-19 | CRM Spec-D 订单事件出站 | Spring 事件 + 监听器聚合推送智能体 | [2026-05-14-crm-spec-d-order-event.md](./2026-05-14-crm-spec-d-order-event.md) |
| 2026-05-19 | CRM 重构设计文档 | Controller/Service/Mapper 三层收敛设计 | [2026-05-15-crm-controller-service-mapper-refactor-design.md](./2026-05-15-crm-controller-service-mapper-refactor-design.md) |
| 2026-05-19 | CRM Controller/Service/Mapper 重构 | Spec A-D 内部实现收敛为三层结构 | [2026-05-15-crm-controller-service-mapper-refactor.md](./2026-05-15-crm-controller-service-mapper-refactor.md) |
