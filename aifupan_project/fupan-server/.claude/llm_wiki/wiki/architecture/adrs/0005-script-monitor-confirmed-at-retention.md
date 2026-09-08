---
adr: "0005"
title: confirmed_at 字段精度保留 vs 仅返回 isRead 布尔值
status: accepted
date: 2026-06-08
source: archive/20260608_B9-read-semantics-to-report-column.md
---

<!-- Source: archive/20260608_B9-read-semantics-to-report-column.md (harvested 2026-06-08) -->

# ADR-0005: confirmed_at 字段精度保留 vs 仅返回 isRead 布尔值

**Status:** accepted
**Date:** 2026-06-08
**Deciders:** 用户（业务决策人）/ beta（技术实现）

## Context

`patrolReportDetail` 响应中有 `confirmedAt` 字段（当前来自 `tb_script_monitor_read.createDate`）。迁移后需要决定：是否在 `tb_script_monitor_report` 上保留精确的 `confirmed_at` 时间戳字段，还是仅保留 `is_read TINYINT` 并在响应中删除 `confirmedAt`。若保留 `confirmed_at`，需要在 ADD COLUMN DDL 中额外增加 `DATETIME NULL` 列；若删除，则前端需要移除对 `confirmedAt` 的依赖。用户决策（决策 10）明确需要 `confirmed_at` 字段。

## Decision

在 `tb_script_monitor_report` 上新增 `confirmed_at DATETIME NULL`（用户决策 10），detail 接口写已读时同步 `confirmed_at=NOW()`（仅首次 NULL→非NULL），`InteractionPatrolReportDetailVo.confirmedAt` 保留且来源改为 `report.confirmedAt`。

## Alternatives Considered

**Alternative A: 仅 is_read TINYINT，删除 confirmedAt 字段**

- Pros: DDL 简洁（仅 1 个 ADD COLUMN）；前端不依赖时间精度，只需 0/1；无时间精度污染问题（update_date 被多处写入不可作为 confirmedAt 替代）。
- Cons: `patrolReportDetail` 需删除 `confirmedAt` 字段（BREAKING）；前端已有"确认时间"显示逻辑需调整；丢失精确首次查看时间（运营可能需要审计）。
- Failure conditions: 前端依赖 `confirmedAt` 显示已读时间，删除导致 UI 信息丢失。
- Estimated complexity: S
- Why not chosen: 用户明确需要保留 `confirmedAt` 精度（决策 10）；前端已有时间显示 UI 需对齐。

**Alternative B: 保留 confirmed_at DATETIME NULL（本次选择）**

- Pros: `patrolReportDetail.confirmedAt` 语义准确（首次查看时间）；前端无需删除已有 UI 字段；运营可审计。
- Cons: 额外 1 个 ADD COLUMN；需 `@TableField(updateStrategy = FieldStrategy.ALWAYS)` 注解确保 null 可写；`triggerReport` 重置时需额外清空 `confirmed_at=NULL`。
- Failure conditions: 忘记 `updateStrategy=ALWAYS` 导致 `confirmed_at=NULL` 无法通过 MyBatis-Plus `updateById` 写入 DB（null 字段默认 ignore）。
- Estimated complexity: S
- Why chosen: 用户决策明确；保留时间精度不增加显著复杂度。

**Alternative C: 使用 update_date 代替 confirmed_at（不新增列）**

- Pros: 无 DDL 变更（不需要 ADD COLUMN confirmed_at）。
- Cons: `update_date` 被 `finishReport` / `restoreOrFail` / `markNotApplicable` / `handleExistingAndReturn` 等多处写入，不能精确代表"首次查看时间"；会产生错误的 `confirmedAt` 值（报告重新生成后 update_date 更新，但语义上 confirmedAt 应为 NULL）。
- Failure conditions: 任何非查看行为触发 update_date 更新，导致 confirmedAt 显示错误时间。
- Estimated complexity: S（无 DDL）
- Why not chosen: 语义不正确，前端显示数据会误导用户。

## Consequences

**Positive**
- `confirmedAt` 精度准确，仅在录制人首次查看时写入，不受其他操作污染。
- `triggerReport` 重置 `confirmed_at=NULL` 语义清晰（报告重新生成，已读状态清零）。

**Negative**
- 需要额外 `@TableField(updateStrategy = FieldStrategy.ALWAYS)` 注解（易遗漏）。
- `triggerReport` 路径需额外处理 `confirmed_at=NULL` 重置。

## Risks

- Risk: `@TableField(updateStrategy = FieldStrategy.ALWAYS)` 遗漏 → `confirmed_at=NULL` 重置无法写入 DB → Mitigation: code-review checklist 明确检查 `is_read` 和 `confirmed_at` 两字段的 annotation；单测 AC-013 验证重置行为。
