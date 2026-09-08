# Explore Report — <task title>

Run ID: `<YYYYMMDD_HHMMSS>_<slug>`
Phase: `Explorer` | Mounted Role: `@Ambiguity Gatekeeper + @Requirement Engineer + @Focus Guard`

## 1. User Intent
<one sentence restating what the user wants>

## 2. Ambiguities Resolved
- <question> → <user answer or assumption>

## 3. Read Inventory
| File | Why | Bytes / lines read |
|---|---|---|
| `<path>` | <reason> | <n> |

Budget consumed: wiki <X>/3, code <Y>/8.

## 4. Findings
<numbered, evidence-cited, ≤ 5 items>

1. <claim> — evidence: `file.cs:line`
2. <...>

## 5. Core Context Anchors (MUST)
Required by HOOKS.md Explorer post-hook. Used by later phases to skip re-navigation.

- **Domain anchors**: <wiki links>
- **API anchors**: <wiki links / Controller files>
- **Data anchors**: <table names / schema files>
- **Architecture anchors**: <module boundaries>
- **Preferences / red lines**: <forbidden patterns, idempotency, rollback constraint>
- **Invariants**: <business rules, enum values, state notes>

## 6. Recommendation
<which Profile (@patch / @standard), proposed focus_card scope, proposed risk level>
