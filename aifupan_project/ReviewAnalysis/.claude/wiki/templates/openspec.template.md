# OpenSpec — <task title>

Run ID: `<YYYYMMDD_HHMMSS>_<slug>`
Status: `DRAFT | WAITING_APPROVAL | APPROVED | IMPLEMENTED | ARCHIVED`
Intent: `<Learn|Change|DocQA|Audit>` | Profile: `@<learn|patch|standard>` | Risk: `<LOW|MEDIUM|HIGH>`

## 1. Context & Motivation
<why this change is needed — keep to 3-5 bullets>

## 2. Scope
- In scope: <files / modules / contracts changed>
- Out of scope: <explicit non-goals>

## 3. Design
<diagram, data model, control flow, or 3-5 paragraphs>

## 4. Public contract changes
| Symbol / API / IPC msg | Before | After | Compatibility |
|---|---|---|---|
| `<name>` | `<sig>` | `<sig>` | `additive / breaking / no-change` |

## 5. Acceptance Criteria
- AC1 <observable, testable>
- AC2 <...>

## 6. Risk & Rollback
- Risks: <ranked>
- Rollback path: <single git revert / staged migration / etc.>

## 7. Validation Plan
- Build: <command>
- Tests: <command>
- Linters / gates: <commands>

## 8. Open Questions / Approval Gate
<questions for human approver — empty if none>

---

Failure log (appended by fail_hook):
- <YYYY-MM-DD HH:MM> <phase> <reason>
