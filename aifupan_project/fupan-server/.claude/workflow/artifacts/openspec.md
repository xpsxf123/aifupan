spec_mode: STANDARD

# <一句话描述 task>

> Template — copy into `<run_dir>/openspec.md` and fill in. Use Slim Spec (only sections 1, 2, 7) for LOW risk; full sections 1–7 for MEDIUM/HIGH.

## 1. Context

- **Business goal**: <one sentence>
- **Scope of change** (modules / packages / classes — objective checklist):
  - <module/class>
  - <module/class>
- **Dependencies** (wiki docs you read — relative links):
  - `[../wiki/domain/<module>_domain.md]`
  - `[../wiki/api/<module>_api.md]`

## 2. Domain Model

If no change, write "None".

- New / updated terms:
- State machine changes / enum updates:

## 3. API Contract (Handoff)

If no change, write "None". Otherwise highly structured.

- **Endpoint**: `<METHOD> <path>`
- **Header / Auth**: token required? special headers (`x-jiuyu-client-id`, `api-key`)?
- **Request**:
  - Field types, required/optional, validation rules
  - Standard JSON example
- **Response**:
  - Full JSON response schema (including error shape `R<T>` failure code)

## 4. Data Model

If no change, write "None".

- Table name, new fields, types, defaults, indexes (DDL block)
- Migration impact (online / offline / lock duration)

## 5. Business Logic

- Step-by-step behavior
- Error handling and fallback branches

## 6. Non-Functional Constraints (Hard Constraints)

If none, write "None".

- Security & permissions (tenant isolation, ownership check)
- Concurrency & idempotency (`@NoRepeatSubmit` / `@CustomRedissonLock`)
- Forbidden patterns (DO NOT do)
- Rollback steps for partial failures

## 7. Acceptance Criteria (Testing)

This section MUST use structured language suitable for tests.

- **Happy path** — Given / When / Then
- **Edge cases** — invalid params, concurrency, permission denied, etc.
- **Unit test requirements** — key branches and asserts

## Machine Section (for sub-agent dispatch)

```markdown
## Allowed Scope
- <file>

## Acceptance Criteria
- AC-001: Given ... when ... then ...

## Task Dependencies
- Depends on: <task> — Status: DONE | IN_PROGRESS | PENDING

## Hard Constraints
- <constraint>
```

## Human Section (for HIGH-risk Approval Gate — Chinese)

```markdown
## 做什么 / 为什么
**现状**: <…>
**需要**: <…>
**范围**: <…>

## 怎么做
<approach + rationale; for HIGH, include alternative comparison table>

## 需要你确认的
- [ ] <decision question>
```
