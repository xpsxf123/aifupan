# Skill Precedence & Composition Matrix

Single source of truth for "at trigger window X, which skills run, in what order, and what's mutually exclusive".

Background: many skills declare `MANDATORY` in their descriptions. When several MANDATORY skills target the same trigger window, the agent silently picks one — making behavior unpredictable. This file resolves those conflicts.

**Reading rule:** if a skill description says `MANDATORY`, also check this file for layering. The matrix below overrides any skill's standalone MANDATORY claim.

---

## Trigger Windows

### Zone A — Before writing Java code (Implement phase)

**SSOT for hard invariants:** project-specific invariants (DI 风格、`SnowflakeManager`、软删除、`@TableLogic` 禁用、`tenantId` 过滤、`#{}` vs `${}` 等)的真相源在 [`CLAUDE.md §5`](../../CLAUDE.md)。本 Zone 不重复条款，只列**何时该 pull 哪份 SKILL**。

**Pull-mode**（按需触发），不要在 Implement 开始时一次性 read 全部：

| 触发条件 | Pull |
|---|---|
| 写 Controller / DTO 校验 / `R<T>` | [java-backend-api-standard](../skills/java-backend-api-standard/SKILL.md) |
| 写 Service 写方法 / 业务异常 | [error-code-standard](../skills/error-code-standard/SKILL.md) |
| 写 Mapper / Entity / SQL | [mybatis-sql-standard](../skills/mybatis-sql-standard/SKILL.md) |
| 分层 / 命名 / POJO 子包拿不准 | [java-engineering-standards](../skills/java-engineering-standards/SKILL.md) |
| 防守编程 / PageUtils / in-memory 装配 | [java-backend-guidelines](../skills/java-backend-guidelines/SKILL.md) |
| BeanUtils / Hutool 边界 | [utils-usage-standard](../skills/utils-usage-standard/SKILL.md) |
| Javadoc / checkstyle 报错时 | [java-javadoc-standard](../skills/java-javadoc-standard/SKILL.md) + [checkstyle](../skills/checkstyle/SKILL.md) |
| ≥3 ordered steps 时 | [writing-plans](../skills/writing-plans/SKILL.md) |
| 测试基建存在的模块 | [test-driven-development](../skills/test-driven-development/SKILL.md) |
| Bug-fix / 未知根因 | [systematic-debugging](../skills/systematic-debugging/SKILL.md) |
| AI 重写完准备进 QA | [ai-slop-cleaner](../skills/ai-slop-cleaner/SKILL.md)（可选）|

具体的"哪个文件触发 reflex 检查哪几条"已在 [`lead-engineer.md`](../agents/lead-engineer.md) 内化为 4 张按文件类型的微 checklist。Zone A 不重复列。

**反模式（过去做法，已废弃）：** Implement 开始一次性 read 全部 7 个 SKILL.md → token 浪费、知识脱靶、与 [`KNOWLEDGE_GRAPH.md`](../llm_wiki/KNOWLEDGE_GRAPH.md) "lazy-load 1–2 indexes per step" 自相矛盾。

---

### Zone B — Code review (after writing code)

These **compete** for the same window. Pick exactly ONE primary reviewer.

| Profile | Primary reviewer | Additional |
|---|---|---|
| Vibe | [code-review-checklist](../skills/code-review-checklist/SKILL.md) (inline, self-correction loop) | — |
| Standard MEDIUM | [code-reviewer](../agents/code-reviewer.md) sub-agent (isolated context) | — |
| Standard HIGH | [code-reviewer](../agents/code-reviewer.md) sub-agent | HIGH-risk also runs `devops-review-and-refactor` matrix + [adversarial-review](../skills/adversarial-review/SKILL.md) (one isolated round) |

**Mutual exclusion:** `code-review-checklist` skill and `code-reviewer` sub-agent are **never both invoked** for the same change — duplicate effort with no error-rate gain.

Security-sensitive changes (auth, secrets, IDOR risk like 跨租户访问) additionally run `secrets_linter.py` gate on top of the primary reviewer.

---

### Zone C — QA (Phase 5)

| Condition | Skills (compose) |
|---|---|
| AC count ≤ 3 AND risk ≠ HIGH | [devops-testing-standard](../skills/devops-testing-standard/SKILL.md) — single-pass |
| AC count ≥ 4 OR risk = HIGH | [devops-testing-standard](../skills/devops-testing-standard/SKILL.md) + [ultraqa](../skills/ultraqa/SKILL.md) + Evidence Mapping Table (AC ↔ Test ↔ Result) |
| Always before Archive | [verify](../skills/verify/SKILL.md) — last-mile sanity check |
| HIGH risk touching auth / secrets / data exposure | + [security-review-checklist](../skills/security-review-checklist/SKILL.md) |

---

### Zone D — Archive phase

| Skill | When |
|---|---|
| [remember](../skills/remember/SKILL.md) | Discovered knowledge crosses sessions — classify into project memory |
| [api-documentation-rules](../skills/api-documentation-rules/SKILL.md) | Reference only — when `@knowledge-harvester` later sublimates archive, follow these conventions for `wiki/api/<module>_api.md` |
| [database-documentation-sync](../skills/database-documentation-sync/SKILL.md) | Reference only — when `@knowledge-harvester` later sublimates archive, follow these conventions for `wiki/data/<module>_data.md` |

Per-change Archive is mechanical: `@documentation-curator` moves the openspec, that's it. Knowledge sublimation into per-module wiki is threshold-triggered (`@harvest <module>`, see lifecycle.md Part 5) — NOT a per-change Archive step. Vibe mode: skip archive zone entirely.

---

### Zone E — Debugging (Scenario DEBUG)

| Skill | When |
|---|---|
| [systematic-debugging](../skills/systematic-debugging/SKILL.md) | Always — disciplined root-cause method (hypothesis → bisect → verify) |
| [devops-bug-fix](../skills/devops-bug-fix/SKILL.md) | After root cause identified — structured diagnose → reproduce → fix → verify workflow |

Composition order: systematic-debugging finds the root cause; devops-bug-fix structures the fix workflow. Neither is skipped.

---

### Zone F — Explorer phase (Standard only)

**Critical:** Explorer phase Mounted Roles dispatch obligation is **Tier-2 conditional** (see [policy.md](policy.md#tier-2--conditional) + [lifecycle.md](lifecycle.md) Phase 1 Inline Path / Dispatch Path). When Tier-2 triggers fire, the skills below run **inside the dispatched sub-agent's context**, not in the main agent's. The "Executed in" column tells you whose context owns the skill's token cost. When Tier-2 inline-eligible, the main agent runs them in its own context (slim explore_report path).

| Skill | When | Executed in |
|---|---|---|
| [requirement-intake](../skills/requirement-intake/SKILL.md) | Front door — first triage for any non-trivial input (PRD / bug / signal) | `@requirement-engineer` sub-agent (NOT main) |
| [devops-requirements-analysis](../skills/devops-requirements-analysis/SKILL.md) | Always — convert request into ACs | `@requirement-engineer` sub-agent |
| [local-code-intelligence](../skills/local-code-intelligence/SKILL.md) | Before reading source files — BM25 wiki + Java symbol index + failure memory | `@requirement-engineer` sub-agent |
| [brainstorming](../skills/brainstorming/SKILL.md) | Multiple plausible solutions exist — generate option space | `@system-architect` sub-agent at Propose (Zone G), NOT Explorer |
| [cognitive-bias-checklist](../skills/cognitive-bias-checklist/SKILL.md) | Before committing to interpretation — check anchoring / availability / confirmation bias | `@requirement-engineer` sub-agent (Cognitive Checks section in its SKILL/agent) |
| [spec-quality-checklist](../skills/spec-quality-checklist/SKILL.md) | Before exiting Explorer — focus_card / openspec quality self-check | Main agent (post-orchestration, light-weight verification) |
| [product-manager-expert](../skills/product-manager-expert/SKILL.md) | Raw input lacks structured intent+scope+AC (e.g., PRD ingestion) | `@requirement-engineer` sub-agent — PRD ingestion is exactly the workload that MUST stay out of main context |
| [prd-task-splitter](../skills/prd-task-splitter/SKILL.md) | EPIC / multi-module scope | `@system-architect` sub-agent at Propose (acting as Foreman per Scenario EPIC); the splitting itself is the delegated unit of work |

**Composition:** these chain, they don't compete. **But they chain inside the sub-agent, not the main agent.** The main agent's job in Explorer is orchestration: dispatch → wait → read structured return → dispatch next. It does not invoke these skills directly.

---

### Zone G — Propose / Review phase (Standard only)

| Skill | When |
|---|---|
| [devops-system-design](../skills/devops-system-design/SKILL.md) | Always for Standard Propose |
| [devops-task-planning](../skills/devops-task-planning/SKILL.md) | Always for Standard Propose |
| [task-decomposition-guide](../skills/task-decomposition-guide/SKILL.md) | Breaking work into ≥3 ordered steps when not PRD-driven |
| [writing-plans](../skills/writing-plans/SKILL.md) | Always — produce the implementation plan that openspec references |
| [architecture-decision-records](../skills/architecture-decision-records/SKILL.md) | HIGH risk — produces the ≥2 ADR alternatives required by lifecycle.md |
| [adversarial-review](../skills/adversarial-review/SKILL.md) | HIGH risk Review — one isolated round of critique (Category A: solving the right problem; Category B: using the right approach) |
| [devops-review-and-refactor](../skills/devops-review-and-refactor/SKILL.md) | Always for Standard Review |
| [global-backend-standards](../skills/global-backend-standards/SKILL.md) | Always for Standard Review (master index) |

---

## Conflict Resolution Rules

When this matrix and a skill's individual description disagree:

1. **This file wins** for trigger-window selection (which skills run).
2. **The skill's own file wins** for content rules (how the chosen skill executes).
3. If you find a new conflict not covered here, add a row rather than silently picking one skill.

## Index maintenance

After adding/removing/renaming a skill that participates in any zone above, update this file in the same commit. The [skill-graph-manager](../skills/skill-graph-manager/SKILL.md) skill verifies this in its lint pass.
