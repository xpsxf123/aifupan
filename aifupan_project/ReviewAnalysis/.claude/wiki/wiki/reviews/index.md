# Reviews Index

This domain stores review artifacts and summaries (design reviews, code review checklists, audit reports).

## Hard Rules (MUST)
- If you produce a reusable review report, it MUST be linked from this index.
- Keep entries short and evidence-based. Link to the source files and line ranges when applicable.

## Review Records

| Review Date | Target (PR/Design/Spec) | Reviewer | Key Findings / Status | Report Link |
|---|---|---|---|---|
| 2026-04-23 | 全项目代码质量审查 | Claude Code | 16 个问题 (3 Critical / 5 High / 8 Medium) | `[code_review_20260423.md]` |
| 2026-04-23 | 架构与可维护性审查 | SOLO | 9 个问题 (3 Critical / 3 High / 3 Medium) | `[architecture_maintainability_review_20260423.md]` |
| 2026-04-23 | 修复计划 (合并两份报告) | Claude Code | 24 个问题 / 5 阶段 / 126h | `[fix_plan.md]` |

---

## Archive Extraction SOP
During `Archive` (or after Phase 3: Review), append a new row for any significant review report generated.

### Append Template
```markdown
| {YYYY-MM-DD} | {Target} | {Reviewer} | {Status/Key Findings} | `[{report_doc_name}]` |
```
