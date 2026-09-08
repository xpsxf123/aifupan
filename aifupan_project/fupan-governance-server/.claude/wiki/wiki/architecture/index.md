# Architecture Index (Baselines & ADRs)

Architecture baselines and ADRs (Architecture Decision Records). Consulted for cross-cutting technical choices (module boundaries, middleware, global patterns).

## Baselines & Guards
- Security baseline: [../preferences/security_rules.md](../preferences/security_rules.md)

## ADR List

ADR files live in the [adr/ directory](adr/README.md). Append a row here when adding a new one. Numbering: scan adr/NNNN-*.md, take max + 1.

| ADR # | Title | Status | Decision Summary | Date | Doc Link |
|---|---|---|---|---|---|
| (Example) ADR-001 | Use JWT for stateless auth | Accepted | Reduce Redis dependency; validate at the gateway | 2026-04-14 | `[adr_001_jwt.md]` |

> Append rule: WAL fragment → `wal/YYYYMMDD_<slug>_architecture.md`. Format enforced by `wal_template_gate.py`.

## Reference Documents
- [企业后台管理账户体系设计文档](企业后台管理账户体系设计文档.md) — 完整账户体系架构，包含双系统联动、AOP 拦截器链、数据权限模型

## WAL Fragments
