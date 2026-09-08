# ADR Index

项目级架构决策记录（Architecture Decision Records）。每个 ADR 对应一次已确认的技术选型，status=accepted 表示已随代码发版落地。

| ADR | Title | Status | Date | Source |
|---|---|---|---|---|
| ADR-0001 | 合并报告输出格式选型 — Pure Markdown vs JSON + wrapper vs 双层 | accepted | 2026-06-07 | archive/20260608_patrol-summary-md-cast.md |
| ADR-0002 | 现网提示词更新 sql 策略 — UPDATE(replay-32.sql) vs 仅改 INSERT(replay-31.sql) | accepted | 2026-06-07 | archive/20260608_patrol-summary-md-cast.md |
| ADR-0003 | 已读状态存储从独立子表（tb_script_monitor_read）迁移到报告主表列 | accepted | 2026-06-08 | archive/20260608_B9-read-semantics-to-report-column.md |
| ADR-0004 | detail 接口"读+自动写已读"的事务边界设计 | accepted | 2026-06-08 | archive/20260608_B9-read-semantics-to-report-column.md |
| ADR-0005 | confirmed_at 字段精度保留 vs 仅返回 isRead 布尔值 | accepted | 2026-06-08 | archive/20260608_B9-read-semantics-to-report-column.md |
