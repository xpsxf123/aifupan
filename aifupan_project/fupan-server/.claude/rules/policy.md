# Project Policy: Commit, Write-back, Dispatch

Anti-loop / Scope Guard / Approval Gate / Yield Discipline → see [lifecycle.md](lifecycle.md).

---

## Part 1 — Hard Constraints

| Constraint | Rule |
|---|---|
| **Artifact Paths** | Runtime artifacts live in `.claude/runs/<intent>__<yyyy-MM-dd_HH:mm:ss>[__NN]/`. Never in repo root. Archive moves `openspec.md` to `.claude/llm_wiki/archive/YYYYMMDD_<slug>.md`. |
| **State Files** | Multi-task queue: `.claude/runs/launch_spec_<YYYYMMDD_HHMMSS>.md`. Per-task contract: `<run_dir>/openspec.md` (+ `focus_card.md`, `explore_report.md`, `current_task.md`). PATCH mode: `<run_dir>/focus_card.md` only. |
| **Safety Bypass** | Never use `--no-verify`, `--no-gpg-sign`, or other safety-bypass flags unless the user explicitly asks. |

### Debug-only Bypass Switches

| Env Var | Effect | When to use |
|---|---|---|
| `CLAUDE_SCOPE_GUARD_BYPASS=1` | PreToolUse hook returns 0, skips `scope_guard.py` | One-shot prefix: `CLAUDE_SCOPE_GUARD_BYPASS=1 <cmd>`. Never persist to `settings.json` / shell profile / CI. Commit message MUST state reason. |
| `CLAUDE_POST_HOOK_BLOCK=1` | PostToolUse hook upgrades `secrets_linter` FAIL to exit 2 (blocks Edit) | Strict-mode debugging only. Default is soft warning. |

---

## Part 2 — Commit Policy

**Never commit:**
- `.claude/runs/` (active run_dirs, launch_specs, cache)
- Python caches: `__pycache__/`, `*.pyc`
- Build/IDE artifacts: `target/`, `build/`, `.idea/`, `.vscode/`, `.DS_Store`
- `HELP.md`, `*.log`, `.qoder`, `.qoder.zip`, `.mvn` (per project `.gitignore`)
- Configuration files containing secrets (`.env`, `credentials.*`, plaintext `application-*.yml` with API keys)

**Only commit:**
- Source code (`src/`, `pom.xml`)
- SQL migrations (`sql/`)
- Archived openspecs (`.claude/llm_wiki/archive/`)
- Wiki content (`.claude/llm_wiki/wiki/`, `.claude/llm_wiki/KNOWLEDGE_GRAPH.md`)
- Skills, agents, rules (`.claude/skills/`, `.claude/agents/`, `.claude/rules/`)
- Workflow templates (`.claude/workflow/`)
- Scripts (`.claude/scripts/`)
- `CLAUDE.md`

**Never auto-commit.** Only commit when the user explicitly asks. Project commit style: Chinese subject prefix `feat/fix/refactor/docs(domain):`, body in Chinese when context is Chinese.

---

## Part 3 — Write-back: Archive & Wiki

### Language Rule

- **Machine-facing** (code, schemas, paths, script names): English
- **Human-facing** (rationale, context, summaries): Chinese

### Per-change Archive (Archive phase, every STANDARD/PATCH task)

1. Move `<run_dir>/openspec.md` → `.claude/llm_wiki/archive/YYYYMMDD_<slug>.md`
2. Append one row to `.claude/llm_wiki/archive/index.md`:
   ```
   | YYYY-MM-DD | Profile | Summary | [link](./YYYYMMDD_<slug>.md) |
   ```
3. Per-change Archive does NOT touch per-module wiki files — those are written only by `@harvest`.

### Knowledge Harvest (threshold-triggered)

```bash
# Check which modules have accumulated enough archives
python3 .claude/scripts/wiki/harvest_threshold.py --all
# exit 1 + module list → ripe; exit 0 → nothing to do.

# Dispatch @knowledge-harvester via dispatch-template.md. The harvester:
#   - classifies each archive: PRIMARY / SUPERSEDED / REDUNDANT / AMBIGUOUS
#   - asks ≤4 questions (AMBIGUOUS only)
#   - presents deletion plan; user must reply literal CONFIRM
#   - merges PRIMARY content into wiki/<area>/<module>_<topic>.md
#   - deletes PRIMARY+SUPERSEDED+REDUNDANT via `git rm`
#   - replaces deleted archive/index.md rows with strikethrough tombstones
#   - updates .claude/llm_wiki/wiki/.last_harvest.json
```

**Traceability after harvest:**
- `archive/index.md` row → `~~| date | summary | slug |~~ → harvested YYYY-MM-DD → <wiki_path>`
- `wiki/<area>/<module>_<topic>.md` sections carry `<!-- Sources: archive/<file1>, archive/<file2> (harvested YYYY-MM-DD) -->`
- Recover deleted spec: `git log --diff-filter=D --follow -- .claude/llm_wiki/archive/<file>.md`

**Hard rule:** Only `@knowledge-harvester` may delete files under `.claude/llm_wiki/archive/`. Manual deletion forbidden.

### Anti-bloat: 500-line Hard Limit

When any wiki file exceeds 500 lines:
1. Split into focused sub-documents per topic.
2. Rewrite original `index.md` as a lean routing index (links + 1–2 line summaries).
3. If top-level structure changes, update `KNOWLEDGE_GRAPH.md`.

Gate: `python3 .claude/scripts/wiki/wiki_linter.py` — FAIL on dead links or any file > 500 lines.

---

## Part 4 — Sub-agent Dispatch

| Mechanism | When |
|---|---|
| **Inline role adoption** — main agent reads the role's `.md` and follows it in current context | Tier-2 inline-eligible (see table) |
| **Sub-agent dispatch** (`Agent` tool) — fresh agent, isolated context, role-scoped `tools:` allowlist | Tier-1 always; Tier-2 when trigger fires |

Roles: [.claude/agents/](../agents/). Dispatch prompt MUST be built from [dispatch-template.md](dispatch-template.md).

### Tier 1 — Always MANDATORY

- `@code-reviewer`
- `@knowledge-harvester`
- `@security-sentinel`
- `@debugger`
- `@architecture-curator`
- `@frontend-api-doc-writer`

### Tier 2 — Conditional

| Role | Dispatch REQUIRED when (any fires) | OK inline when (all true) |
|---|---|---|
| `@ambiguity-gatekeeper` | (a) intent missing action verb / target / measurable outcome AND user did not pre-clarify; OR (b) user used `@standard` shortcut | Intent is clear "verb + target + outcome"; user named files / methods / fields |
| `@requirement-engineer` | (a) risk = HIGH; OR (b) > 2 wiki files to drill; OR (c) ≥ 2 ACs with contested edges; OR (d) > 150 lines of existing code to read | Risk ≤ MEDIUM AND user pre-clarified choices AND target code/tables already identified by path |
| `@system-architect` | (a) risk = HIGH; OR (b) ≥ 2 contested design alternatives; OR (c) cross-module Feign/MQ design; OR (d) openspec would exceed ~150 lines | Risk = MEDIUM AND design is a straightforward delta (new isolated table, single-method change, no cross-module choreography) AND no genuine alternative tree |

**Inline alternatives when Tier-2 conditions allow:**
- Skip `@ambiguity-gatekeeper` → emit `[DoR PASS] Action: … | Target: … | Outcome: …`.
- Skip `@requirement-engineer` → write 30-line `explore_report.md` directly (or skip the file when ACs fit in 5 bullets in main transcript).
- Skip `@system-architect` → write slim `openspec.md` (trigger-conditional sections only; for additive DDL + small delta typically §1 + §4 + §5 + §6 + §7, ~80 lines).

### Return validation

After a sub-agent returns:

```bash
python3 .claude/scripts/gates/subagent_return_gate.py --return-file <path> --task-kind implement|review|extract|audit
```

Exit codes: `0` OK; `1` WARN (surface to user); `2` FAIL (re-dispatch).

EPIC / Foreman pattern → [lifecycle.md](lifecycle.md) Part 2 Scenario EPIC.
