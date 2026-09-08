---
adr: "0003"
title: 已读状态存储从独立子表（tb_script_monitor_read）迁移到报告主表列
status: accepted
date: 2026-06-08
source: archive/20260608_B9-read-semantics-to-report-column.md
---

<!-- Source: archive/20260608_B9-read-semantics-to-report-column.md (harvested 2026-06-08) -->

# ADR-0003: 已读状态存储从独立子表迁移到报告主表列

**Status:** accepted
**Date:** 2026-06-08
**Deciders:** 用户（业务决策人）/ beta（技术实现）

## Context

当前系统有独立表 `tb_script_monitor_read`（report_id + user_id + is_read + createDate），设计初衷是"多用户已读状态"，即每个查看用户都有独立已读记录。然而业务上真正需要的"已读"语义是"录制人本人是否已知晓"——该状态是 per-report 维度而非 per-user 维度（录制人唯一确定一个报告的已读状态）。现有方案每次批量查询都需要额外 JOIN/IN 查 `tb_script_monitor_read`，且管理两张子表（read + role_confirm）增加了代码复杂度。用户决策：将已读状态内置为报告主表的两个列（`is_read TINYINT`, `confirmed_at DATETIME`）。

## Decision

将 `is_read` 和 `confirmed_at` 作为列新增到 `tb_script_monitor_report`，删除 `tb_script_monitor_read` 表及其全套 Entity/DAO/Service 代码；已读写入改为主表 UPDATE，`buildMonitorStatus` 直接从 entity 字段取值。

## Alternatives Considered

**Alternative A: 保留 tb_script_monitor_read 表，仅简化 confirmRead 为"打开即自动调用"**

- Pros: 无 DDL 删表风险；代码改动量小；历史数据不受影响。
- Cons: 额外表仍然存在，每次批量查询仍需额外 IN 查询（N 个 reportId → `tb_script_monitor_read` IN 查询）；代码路径复杂度未降低；两张子表（read + roleConfirm）维护负担未减轻。
- Failure conditions: 当报告量增大时，批量状态查询的 `loadReadReportIds` IN 查询性能衰退。
- Estimated complexity: S
- Why not chosen: 未能解决"per-report 已读本质是主表列"的设计问题，长期维护成本更高；业务语义上"录制人已读"是报告维度属性，不是用户维度状态。

**Alternative B: 合并到主表（本次选择）**

- Pros: 消除 JOIN/IN 查询，`buildMonitorStatus` 直接 entity.isRead；代码路径简化；删除两张子表降低系统熵。
- Cons: 历史数据不迁移（is_read DEFAULT 0），存量已读报告重置为未读；DDL 不可回滚（DROP TABLE 后旧代码崩溃）。
- Failure conditions: DDL 执行期间与代码上线有窗口期（先 ADD COLUMN 后 DROP TABLE 的顺序缓解）；大表 ALTER TABLE 锁表风险（生产环境需评估行数，建议 gh-ost 或低峰执行）。
- Estimated complexity: M
- Why chosen: 与业务语义最一致；性能改善；代码简化幅度大于风险。

**Alternative C: 在 tb_script_monitor_report 增加 is_read 列，但保留 tb_script_monitor_read 作为历史查询**

- Pros: 存量已读数据不丢失；可做数据迁移后再 DROP。
- Cons: 两个已读来源并存，查询逻辑需要判断"新记录用主表，旧记录用子表"，实现复杂度不降反升；需要额外迁移脚本。
- Failure conditions: 迁移脚本出错导致已读状态不一致。
- Estimated complexity: L
- Why not chosen: 过渡期方案复杂度高，且用户已接受不 backfill 历史数据（决策 8），无需保留子表。

## Consequences

**Positive**
- `buildMonitorStatus` / `loadReportMap` 路径简化，消除额外 IN 查询，批量状态查询性能改善。
- 删除约 8 个类文件（Entity × 2 + DAO × 2 + Service × 4），代码库缩减。
- 已读状态与报告生命周期绑定（`triggerReport` 重置时一并清零）。

**Negative**
- 存量已读数据不 backfill：历史通过 `confirmRead` 已确认的报告，前端将重新显示为"未读"。
- `tb_script_monitor_read` DROP 后旧版代码无法兼容运行，回滚需同步重建表并恢复代码。

## Risks

- Risk: `ALTER TABLE` 大表锁表 → Mitigation: 生产前评估 `SELECT COUNT(*) FROM tb_script_monitor_report`，超 50 万行改用 gh-ost 在线 DDL；`tb_script_monitor_report` 写压力低（报告生成频率远低于视频录制频率），锁表时间可控。
- Risk: 代码上线窗口期（旧版代码 + 新 DDL）→ Mitigation: 先 ADD COLUMN（DEFAULT 0 无破坏性），旧代码继续查子表正常运行；随后上线新代码（停止查子表）；最后 DROP TABLE（或允许子表残留到下一个 release 清理）。
