---
name: "local-code-intelligence"
description: "Cross-session failure-memory reflex. UserPromptSubmit hook scans recent archive/index.md + incidents/*.md for Fix/Hotfix entries (14-day window) and injects ≤3 reminders on stderr before the agent acts. Silent within 30-min cooldown or when no matches. Hook-only — no agent-callable surface."
---

# Local Code Intelligence

## Mechanism

Hook-mounted reflex. The agent does not invoke this skill.

| Aspect | Value |
|---|---|
| Script | `.claude/scripts/harness/failure_memory.py` |
| Mount | `.claude/scripts/harness/user_prompt_submit_hook.py` (UserPromptSubmit) |
| Entry point | `emit_if_due()` (Python import, not CLI) |
| Data source 1 | `.claude/llm_wiki/archive/index.md` — rows matching `Fix` / `Hotfix` / `修复` / `回炉` |
| Data source 2 | `.claude/llm_wiki/incidents/*.md` — files with `_TEMPLATE.md` frontmatter |
| Window | 14 days |
| Output cap | ≤3 lines to stderr |
| Cooldown | 30 min (state: `.claude/runs/.failure_memory_state.json`) |

## Agent obligations

None. Read injected reminders if they appear at the top of a user-prompt block; treat as advisory hints, verify before acting.

## Manual invocation (debugging only)

```bash
python3 .claude/scripts/harness/failure_memory.py
```

Equivalent to one `emit_if_due()` call. Bypasses cooldown only if state file is absent.

## Related skills (no shared scripts)

- [systematic-debugging](../systematic-debugging/SKILL.md) — root-cause hunt; uses `grep -rn` and `mvn test`
- [task-decomposition-guide](../task-decomposition-guide/SKILL.md) — blast-radius estimation via manual grep enumeration
- [security-review-checklist](../security-review-checklist/SKILL.md) — table-access paths via grep over Mapper.xml + repository interfaces
