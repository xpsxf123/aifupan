# Domain Index (Vocabulary & State)

Business vocabulary, state machines, and invariants. Drilled into during Explorer/Propose to avoid term drift.

## Core Concepts & State Machines

| Concept | Definition | Related Concepts | Details |
|---|---|---|---|
| Employee Lifecycle | 员工生命周期：入职→正常→停用→删除的状态流转 | Employee, Role, Tenant, Onboarding | [employee_lifecycle](wal/20260525_employee_lifecycle.md) |

> Append rule: WAL fragment → `wal/YYYYMMDD_<slug>_domain.md`. Format enforced by `wal_template_gate.py`. Split into per-line dictionaries when > 30 concepts.

## WAL Fragments

| Date | Fragment | Summary |
|---|---|---|
| 2026-05-25 | [employee_lifecycle](wal/20260525_employee_lifecycle.md) | Employee state definitions, onboarding/offboarding rules, external account binding |
| 2026-05-25 | [legacy_requirements_index](wal/20260525_legacy_requirements_index.md) | Pointers to legacy PRD/requirements docs in `docs/` for background reference |
