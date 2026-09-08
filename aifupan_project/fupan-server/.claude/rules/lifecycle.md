# Routing, Lifecycle & Hooks

Single source of truth for: how a request gets classified, what phases follow, which hooks fire when, and what to do on failure.

---

# Part 1 — Routing: Three Modes + Phase Decision Matrix

**Core principle:** 档位 (mode) 是粗分类；每个 phase 内部仍按"意图 / 复杂度 / 影响"动态决定 **skip / inline / dispatch**。不是"档定下来 phase 就固定"。

## Modes (粗分类)

| Mode | 触发判定 | 写代码？ | openspec？ | Archive？ |
|---|---|---|---|---|
| **Vibe** | LEARN / 解释代码 / 单行 / typo / 文案改 / 配置改一行 | 最多一行 | 否 | 否 |
| **PATCH** (中间档) | 写代码但范围可控：≤3 文件、≤50 行、单 jiuyu 模块、不动公共 API / DB / auth / 错误码 | 是 | 否 | 1 行 changelog |
| **Standard** | 动公共 API / DB schema / auth / 错误码 / 核心业务主路径 / 跨 ≥3 模块 | 是 | 是 | 完整 openspec 归档 |

**Default:** 主 agent 按上表"触发判定"自动路由，**第一条输出**必须是一行路由说明：

```
[Route] Mode=<Vibe|PATCH|Standard> | Risk=<MEDIUM|HIGH> (Standard only) | Phases={<active phase list>} | Reason: <一句话理由>
```

例：
- `[Route] Mode=Vibe | Phases={Implement(inline)} | Reason: 只是解释 SnowflakeManager 的用法`
- `[Route] Mode=PATCH | Phases={Implement+gates, Archive(1-line)} | Reason: 改 2 个 Service 方法、未动公共 API`
- `[Route] Mode=Standard | Risk=MEDIUM | Phases={Explorer(dispatch), Propose(dispatch), Implement, QA(verify), Archive} | Reason: 新增 Controller endpoint`
- `[Route] Mode=Standard | Risk=HIGH | Phases={all + Approval + adversarial + §9 ADR} | Reason: ALTER TABLE + 跨 3 模块`

## Phase Decision Matrix

每个 phase 都有 3 档：**skip / inline / dispatch**。下表锁定每个 (Mode, Phase) 的默认动作。Mid-flight 升降级允许（见 Boundary rules）。

| Phase            | Vibe              | PATCH                            | Standard MEDIUM                       | Standard HIGH                              |
|------------------|-------------------|----------------------------------|---------------------------------------|--------------------------------------------|
| **Explorer**     | skip              | skip<br>(or inline DoR if 意图模糊) | dispatch `@ambiguity` + `@requirement-engineer` | dispatch + adversarial-A 第二轮            |
| **Propose**      | skip              | skip (no openspec)               | dispatch `@system-architect`<br>(schema trigger 裁剪 §1-§8) | dispatch + §9 ADR (≥2 Nygard 强制)         |
| **Review**       | skip              | **dispatch `@code-reviewer`**（强制；不允许 inline）| **dispatch `@code-reviewer`**（强制；不允许 inline）| dispatch `@code-reviewer` + adversarial-B  |
| **Approval Gate**| —                 | —                                | FYI only (no block)                   | required (block until human approves)     |
| **Implement**    | inline            | inline<br>+ `scope_guard` hook<br>+ `secrets_linter` | + L1-L7 java standards<br>+ scope_guard<br>+ secrets_linter | + 监控埋点要求<br>+ 上述全部                  |
| **QA**           | skip              | **dispatch `@test-engineer`**（强制写并跑单测） | **dispatch `@test-engineer`** + `verify` (AC≤3) OR `ultraqa` (AC≥4)| **dispatch `@test-engineer`** + `ultraqa` + Evidence Mapping Table |
| **Archive**      | skip              | 1-line changelog<br>(no openspec move) | full openspec move<br>`@documentation-curator` | + `@architecture-curator` ADR extract<br>+ `@harvest` recommendation |

**读法：**
- "skip" = 该 phase 在该 mode 下不执行。
- "inline" = 主 agent 在当前上下文里完成该 phase 的轻量动作。
- "dispatch" = 必须 dispatch 对应 sub-agent（见 [policy.md](policy.md#tier-1--always-mandatory) Tier 1 — Always MANDATORY；Tier-2 是否升级为 dispatch 见 [policy.md](policy.md#tier-2--conditional) 触发条件）。
- **Review + QA 不允许 inline**（PATCH / Standard 一律强制 dispatch `@code-reviewer` + `@test-engineer`）。理由：B7/B8 多次出现 inline self-check 漏掉 CRITICAL（B8 switch NPE、LambdaUpdateWrapper 缺 tenantId），e2e 验不出边界（B8 monitorType=99）。规则来源：memory `feedback-review-test-mandatory`。

## Trigger-based 升级阈值（mode 自动判定的硬规则）

主 agent 路由时按下列阈值**从下往上**匹配 — 任一上层条件满足即升档：

| 触发条件 (任一满足) | 目标档 |
|---|---|
| 仅 LEARN / 解释 / 一行修 | **Vibe** |
| 写代码 ∧ ≤5 文件 ∧ ≤80 行 ∧ 单模块 ∧ 不动公共 API/auth/错误码 ∧ (无 DDL ∨ **isolated additive DDL** — 仅新表 / 新索引 / 新表上的 ADD COLUMN) | **PATCH** |
| 写代码 ∧ (公共 Controller endpoint 改 / 公共 method signature 改 / 核心业务主路径 / 改前端契约 / **exposed additive DDL** — 既有表 ADD COLUMN 且字段被新代码读出到 API/MQ/Feign) | **Standard MEDIUM** |
| (**destructive DDL**) ∨ (改 auth/权限策略) ∨ (改错误码体系) ∨ (跨 ≥3 jiuyu 模块) ∨ (修改 `replay-common`/`replay-generic`) ∨ (blast radius 不明) | **Standard HIGH** |

**DDL 三档定义（关键 — 决定走哪条流水线）：**

| 档 | 形态 | 本质 | 回滚 | 路由 |
|---|---|---|---|---|
| **Isolated additive** | 全新 `CREATE TABLE`（无外键回指既有表）+ `CREATE INDEX` + 新表上 `ADD COLUMN` | 没有既有代码读这张表，系统侧边新长一块 | `DROP TABLE` | PATCH |
| **Exposed additive** | 既有表 `ADD COLUMN`（可空 / 带 default），新字段被 Controller/Feign/MQ payload 暴露 | DDL 安全但对外契约扩了，需 FE 协调 / 下游兼容 | 先收 API 再回滚字段 | MEDIUM |
| **Destructive** | `ALTER TABLE` 改既有列（类型 / 长度 / 名字 / nullability）/ `DROP COLUMN` / `DROP INDEX`（在用）/ `DROP TABLE` / 数据迁移 / 加 `NOT NULL` 无 default 到非空表 | 动既有行 / 破坏既有读写契约 | 有代价或不可回滚 | HIGH |

## Boundary rules

- **Mid-flight 升 / 降级允许**：发现意图复杂度跳档（如 PATCH 写到一半要改 Controller endpoint；或 Standard 读完 explore_report 发现就一行）→ 立即 emit:
  ```
  [Mode Change] <from> → <to> (reason: <what was discovered>)
  ```
  升级时**补**所需 phase；降级时**保留**已 dispatch 的 sub-agent 输出到 `<run_dir>/`。**已写代码任何方向都不丢弃**。
- **Never** force Standard on a Vibe-eligible change just because 用户说"重要"或"线上"。用上表触发条件，不是 vibes。
- **Vibe override:** 用户带 `@vibe` shortcut 时强制 Vibe，即使触发判定建议升档 — 但主 agent 必须输出 `[Vibe Override] Detected change touches X — proceeding due to @vibe; risk acknowledged.`

---

# Part 2 — Special Scenarios

These override default classification. Match against the scenario list BEFORE applying standard routing.

### Scenario DEBUG — Deep Troubleshooting
**Trigger:** Bug/error with unknown root cause, or `@debug` shortcut.
**Routing:** Vibe mode. Main agent dispatches `@debugger` sub-agent for the root-cause hunt (isolated context, read-only on business code). ALLOWED to run terminal commands (`mvn test`, log reading, ≤5 retries). FORBIDDEN from modifying ANY file (business code, SQL, config, `.claude/`). Once root cause found → `@debugger` produces RCA report and YIELDS. User decides whether to launch a Change task (Vibe or Standard) to apply the fix.
**Agent:** `.claude/agents/debugger.md`
**Skills:** `.claude/skills/systematic-debugging/SKILL.md` (hypothesis protocol), `.claude/skills/devops-bug-fix/SKILL.md` (diagnose/reproduce — fix portion deferred to follow-up Change task)

### Scenario EPIC — Cross-Module / Framework Refactor
**Trigger:** Feature spanning ≥3 jiuyu modules, framework migration (e.g., Spring Boot 2 → 3, jakarta migration), or massive refactor.
**Routing:** Standard, risk HIGH (forced). Main agent acts as **Foreman**:
- MUST slice work into micro-tasks (one task_brief per slice)
- MUST delegate to sub-agents via [dispatch-template.md](dispatch-template.md)
- MUST NOT write code directly
**Skills:** `.claude/skills/prd-task-splitter/SKILL.md`

### Scenario A — Emergency Hotfix
**Trigger:** 线上事故 / 客户阻断 / 必须立刻发版。
**Routing:** Vibe mode. No Propose/Review. Required:
- `[Intent Check] emergency=true`
- Slim spec with `## Emergency Justification` section
- `secrets_linter.py` MUST pass before commit
**Skills:** `.claude/skills/devops-bug-fix/SKILL.md`

### Scenario B — Database Migration
**Trigger:** **Destructive DDL only** — `ALTER TABLE` changing existing column type / length / name / nullability, `DROP COLUMN`, `DROP INDEX` (in use), `DROP TABLE`, scripted data migration (`UPDATE/INSERT ... SELECT` over existing rows), or adding `NOT NULL` without default to a non-empty table. Additive DDL (`CREATE TABLE` new, `CREATE INDEX` new, `ADD COLUMN` nullable / defaulted) does NOT trigger this scenario — it routes to Standard MEDIUM per the Part 1 trigger matrix.
**Routing:** Standard, risk HIGH (forced). Approval Gate required.
**Gate:** `python3 .claude/scripts/gates/migration_gate.py --sql-dir <path>`
**Constraint:** Schema change MUST be documented in archived openspec under `.claude/llm_wiki/wiki/data/<module>_data.md` at next milestone. New SQL files go under repo-root `sql/` directory (existing convention).

### Scenario C — Breaking API Change
**Trigger:** Removing/renaming public endpoint; backward-incompatible request/response schema change; auth/permission strategy change on existing endpoint.
**Routing:** Standard, risk HIGH (forced).
**Gate:** `python3 .claude/scripts/gates/api_breaking_gate.py --openspec <run_dir>/openspec.md`
**Constraint:** Must document migration guide in openspec (前后端协同 / 老版本兼容窗口).

### Scenario D — Performance Tuning
**Trigger:** Slow query, high latency, memory/CPU optimization (e.g., 大量 `tb_anchor_video` 扫描).
**Routing:** LEARN first — gather baseline evidence (bottleneck + metric + proposed fix). Then re-route as Change with audit as anchor.

### Scenario E — Dependency Upgrade
**Trigger:** Changes to `pom.xml`.
**Routing:** Vibe for patch-version bumps; Standard for major/minor version or new dependency.
**Gate:** `python3 .claude/scripts/gates/dependency_gate.py --pom <pom.xml>`

### Scenario F — Research / Feasibility
**Trigger:** `@research` shortcut, or `/h-research`, or intent verb ∈ {分析 / 调研 / 评估 / 探索 / 可行性 / feasibility / research} AND output is a report (not code / not a contract). User phrasing like "能给到什么程度" / "现状如何" / "调研一下" / "what data can we provide".
**Routing:** No openspec, no focus_card, no launch_spec, no run_dir. Output: `.claude/llm_wiki/wiki/research/YYYYMMDD__<slug>.md`. Single inline OR single `@requirement-engineer` dispatch (research mode — grep + write report, NO AC generation, NO Recommended Allowed Scope). Terminal — no downstream phases. If user decides to act → `/h-brief <slug>` consumes the research as Spec Inference seed.
**Command:** `/h-research <topic>`
**Boundary vs Scenario D (Performance Tuning):** D is "LEARN before fixing perf issue, then re-route to Change"; F is "deliver a report, may never become Change". Overlap when perf investigation is purely diagnostic → use F.
**Anti-pattern:** Do NOT run `/h-brief` → `/h-design` when intent is feasibility study — wastes ~6× tokens dispatching `@system-architect` for a contract that may never be implemented.

### Scenario MAINTENANCE — Wiki / Knowledge Operations
**Trigger:** `@gc`, `@wiki-update`, `@librarian`, "整理 wiki", "提取知识", "拆分文档", "扫描项目".
**Routing:** No code phases. Role-specific flow (see Maintenance Operations below).

---

# Part 3 — Shortcuts

| Shortcut | Forces Mode | Effect |
|---|---|---|
| `@read` / `@learn` | Vibe (read-only) | Read-only; never write code, never run gates |
| `@vibe` | Vibe | LEARN / 一行修 / typo / 解释。**写代码超过一行即触发自动升档** unless `--strict-vibe` (see below) |
| `@patch` / `@quickfix` | **PATCH** (NOT a Vibe alias anymore) | 写代码 + scope_guard + secrets_linter + 1 行 changelog。不写 openspec、不 dispatch。**发现碰到公共面 → emit `[Mode Change] PATCH → Standard`** |
| `@standard` | Standard | Force full lifecycle (Explorer dispatch + Propose dispatch + ...). Risk MEDIUM/HIGH 仍按 Risk Classification 自动分支 |
| `@debug` | Vibe (Scenario DEBUG) | Dispatch `@debugger` sub-agent; read-only root-cause hunt → RCA report → yield to user. **If source ≠ dev-found, debugger emits `[Incident Draft]` and main agent prompts user before writing to `incidents/`.** |
| `@incident` | Vibe (Maintenance-ish) | User pastes an exception/log/symptom; main agent inline extracts → `area / severity / source / status / title / Symptom / Root cause / Fix / Reflex` per `_TEMPLATE.md` → presents draft → on user confirmation, writes `.claude/llm_wiki/incidents/YYYY-MM-DD__<area>__<slug>.md`. No code phases. Detailed flow in [`incidents/README.md`](../llm_wiki/incidents/README.md). |
| `@research` | Vibe (Scenario F) | Force `/h-research` flow: produce `wiki/research/YYYYMMDD__<slug>.md` only. Zero contract, zero launch_spec, zero downstream phase. Use when output is a report (feasibility / 现状 / 能给到什么程度), not code. |
| `@gc` / `@librarian` | Maintenance | Wiki compaction flow |
| `@wiki-update` / `@milestone` | Maintenance | Knowledge extractor flow |
| `@harvest <module>` | Maintenance | Dispatch `@knowledge-harvester` for threshold-triggered consolidation |

Flags:
- `--strict-vibe` (combine with `@vibe`): suppress auto-upgrade — user accepts the risk of acting outside the mode's normal envelope. Use sparingly.
- `--risk medium|high`: override Standard mode's auto-classification.
- `--launch`: only with `@standard`. Forces `launch_spec_*.md` creation.
- `--test "<cmd>"`: override default test command for QA phase.
- `--yes`: auto-confirm low-risk prompts (does NOT bypass Approval Gate for HIGH).

**No shortcut** → main agent runs the Phase Decision Matrix auto-routing per Part 1. First output line MUST be the `[Route]` block.

---

# Part 3.5 — Bracket Marker Catalog (单点查表，防漏发)

All bracket markers the main agent may emit. **One place to check** before deciding which to use. Sub-agent return-block markers (`[Status]`, `[Files Changed]`, etc.) live in [dispatch-template.md](dispatch-template.md), NOT here.

| Marker | Trigger | Required payload | Where emitted |
|---|---|---|---|
| `[Route] Mode=<…> \| Risk=<…> \| Phases={…} \| Reason: <…>` | **Always** — first line of response when starting any task | Mode + (Risk if Standard) + active Phases + one-line reason | First line of first response |
| `[Mode Change] <from> → <to> (reason: <…>)` | Mid-flight when complexity jump discovered (升 or 降，both directions) | from-mode, to-mode, what triggered the change | Inline before the next phase action |
| `[Vibe Override] Detected change touches <…> — proceeding due to @vibe; risk acknowledged.` | User typed `@vibe` but matrix would otherwise force upgrade | What the touched surface is | First line after Route, before code |
| `[Intent Check] emergency=true` | Scenario A (Emergency Hotfix) | `emergency=true` literal | First line after Route |
| `[DoR PASS] Action: <verb> \| Target: <object> \| Outcome: <measurable>` | Inline ambiguity-gate path (Tier-2 inline-eligible) | verb / target / outcome triplet | End of Explorer when ambiguity-gatekeeper inlined |
| `[Explorer Done] AC count=N \| Open Qs=M \| Recommended scope rows=K \| Report: <run_dir>/explore_report.md` | After Explorer sub-agent dispatch returns successfully | AC count, open question count, scope row count, report path | End of Explorer phase |
| `[Propose Done] spec_mode=<STANDARD\|SLIM> \| sections=N \| ADR alts=K \| Allowed Scope=M files` | After Propose phase produces openspec.md | spec_mode, sections active, ADR count, allowed-scope file count | End of Propose phase |
| `[Boundary Exception Request] file=<…>, reason=<…>` | Implement needs to edit a file outside Allowed Scope | the file path + business reason | When out-of-scope edit becomes necessary; **block and wait for approval** |
| `[Plan Invalidation] Discovery: <…> \| Invalidated Assumption: <…> \| Impact: <…> \| Proposed Action: ROLLBACK_TO_<PHASE>` | Implement uncovers structural contradiction with openspec assumption | discovery + invalidated assumption + impact + proposed rollback | Inline at moment of discovery; **stop, await human** |
| `[Incident Draft] ...` | `@incident` shortcut OR `@debugger` finds RCA suggesting incident write | full incident draft per `incidents/_TEMPLATE.md` | At end of incident-flow; user must confirm before file write |
| `[Research Done] report=<path> \| path=<inline\|dispatch> \| gaps=<N> \| recommend=<P0\|P1\|P2 路径>` | End of `/h-research` after report written | report path, execution path, gap count, recommended action tier | Last line of `/h-research` run |
| `[Research Diversion] Intent looks like research (verb=<…>, outcome=<报告\|数据\|能力>) — recommend /h-research over /h-brief.` | `/h-brief` / `@standard` / inline routing detects research-pattern input | detected verb + outcome class | First line; halt /h-brief, wait for user confirm to switch or override |

**Don't invent new markers.** If a situation doesn't fit a row above, narrate plainly without brackets — bracket markers are reserved for the structured signals listed here.

---

# Part 4 — Lifecycle: Phases & State Machine

## Canonical Phase Flow

```
Explorer → Propose → Review → [Approval Gate if HIGH] → Implement → QA → Archive
```

The canonical phase sequence above applies to **Standard mode**. **Vibe** skips Explorer/Propose/Review/QA/Archive entirely; **PATCH** skips Explorer/Propose/Review (and runs only the gate-equipped tail: Implement+gates → Archive 1-line).

Each phase is independently classified as **skip / inline / dispatch** per the Phase Decision Matrix (Part 1). Even within Standard, individual phases may degrade (e.g., Review = inline self-check for MEDIUM, dispatch for HIGH).

## Active Run Directory (Standard mode only)

Standard tasks create an isolated run directory. PATCH and Vibe do NOT — they leave no `.claude/runs/` artifact.

- Pattern: `.claude/runs/<intent>__<yyyy-MM-dd_HH:mm:ss>[__NN]/`
- All artifacts MUST be written under `<run_dir>/`:
  - `<run_dir>/openspec.md` (the task contract)
  - `<run_dir>/focus_card.md` (scope lock)
  - `<run_dir>/explore_report.md` (Phase 1 hand-off — written by `@requirement-engineer`)
  - `<run_dir>/current_task.md` (checklist with final `[ ] Archive openspec + changelog` item)

## PATCH Mode — Lightweight Artifacts (run_dir)

PATCH tasks create a lightweight run_dir under `.claude/runs/PATCH__<yyyy-MM-dd_HH:mm:ss>/`. Unlike Standard mode, the run_dir only contains `focus_card.md` — no openspec, no explore_report, no current_task.

- **Pre-Implement:** main agent creates `<run_dir>/` and writes a 1-shot `focus_card.md`. Contents = file whitelist only:
  ```markdown
  # Focus Card (PATCH ad-hoc)
  ## Allowed Scope
  - <file path 1>
  - <file path 2>
  ```
  ≤ 10 lines. No openspec, no AC list, no architecture decisions.
- **Implement:** `scope_guard` hook + `secrets_linter` hook fire normally (they auto-discover the run_dir via `find_active_focus_card.py`).
- **Archive:** append a single line to `.claude/llm_wiki/archive/index.md`:
  ```
  | YYYY-MM-DD | PATCH | <one-line summary> | (no spec — PATCH mode) |
  ```

If during PATCH the change grows beyond the trigger threshold (extra file, public API touch discovered, etc.):
1. Emit `[Mode Change] PATCH → Standard MEDIUM/HIGH (reason: <what was discovered>)`.
2. The existing run_dir stays; add `openspec.md` + `explore_report.md` to it, dispatch the now-required Explorer/Propose phases.
3. Already-written code stays — it's accounted for in the new openspec's Allowed Scope.

## Discipline Pillars

| Pillar | 规则 |
|---|---|
| **PDD** Plan-Driven | 写代码前先声明依赖；≥3 task 画依赖图，并行 soft limit=3 |
| **BDD** AC Format | 每条 AC 必须 `Given <前置>, when <动作>, then <可观测可量化结果>`；"处理一下" / "更好用" 这类含糊语 BLOCK，强制要量化 |
| **SDD** Contract-First | `openspec.md` 是普适契约；spec 未完不写代码。格式见 [openspec_schema.md](../llm_wiki/schema/openspec_schema.md) |
| **TDD** Red→Green→Refactor | RED 起失败测（来自 AC）→ GREEN 最小通过 → REFACTOR。**测试基建未覆盖的模块** → 用 manual verification 进 QA evidence |

## Phase Details

### Phase 1: Explorer

**Mounted Roles** (dispatch obligation is Tier-2 conditional — see [policy.md](policy.md#tier-2--conditional-dispatch-only-when-complexity-threshold-is-met) + [dispatch-template.md](dispatch-template.md)):

| Role | Mode | Rationale |
|---|---|---|
| `@ambiguity-gatekeeper` | **Conditional** — dispatch if intent has no verb/target/outcome AND user did NOT pre-clarify in conversation, OR `@standard` shortcut explicit. Otherwise inline `[DoR PASS]` line. | DoR check + runaway-trigger. Independence only matters when intent is genuinely vague. Clear intent → main agent emits the DoR line itself. |
| `@requirement-engineer` | **Conditional** — dispatch if risk=HIGH, OR >2 wiki files to drill, OR ≥2 contested-edge ACs, OR >150 lines of code to read. Otherwise inline (slim explore_report or 5-bullet AC list). | Wiki drilling + AC drafting can pollute context — but only when the drilling is real. For pre-clarified design with known target files, inline is cheaper. |
| `@focus-guard` | **MAY remain inline** | Lightweight `focus_card.md` format check, no project-context payload. Setup-time only; runtime per-edit enforcement is the PreToolUse hook in Phase 4. |

> Path 选择：dispatch 保持主上下文干净（避免 wiki drill + AC 草稿污染），但有固定成本（模板校验 + sub-agent 启动 + 返回解析）。Tier-2 触发条件见 [policy.md](policy.md#tier-2--conditional)。

**Orchestration (main agent — pick path per Tier-2 evaluation):**

0. **Decision step:** evaluate Tier-2 triggers per [policy.md](policy.md#tier-2--conditional-dispatch-only-when-complexity-threshold-is-met). If NONE fire → take the **Inline Path** (step 1' below). If ANY fire → take the **Dispatch Path** (steps 1–4).

**Inline Path (Tier-2 not triggered):**

1'. Main agent emits a one-line `[DoR PASS] Action: … | Target: … | Outcome: …` in transcript (skip `@ambiguity-gatekeeper`).
2'. Main agent writes a slim `<run_dir>/explore_report.md` (Spec Inference 3 lines + 3-5 ACs as bullets + Allowed Scope) OR skips the file and emits a 5-bullet AC summary directly in transcript when the change is ≤2 files.
3'. Main agent writes `<run_dir>/focus_card.md` (scope lock).

**Dispatch Path (any Tier-2 trigger fires):**

1. **Dispatch `@ambiguity-gatekeeper`** per [dispatch-template.md](dispatch-template.md) Example 3. `[Status]: BLOCK` → 转用户澄清，**halt Explorer**；`PASS` → step 2。验证返回 `subagent_return_gate.py --task-kind audit`。
2. **Dispatch `@requirement-engineer`** per Example 4。产 `<run_dir>/explore_report.md`（详尽 Spec Inference + AC + Hidden Scope + Recommended Allowed Scope）；仅返回 `[Status]/[ACs Mapped]/[Issues Found]` block。验证 `--task-kind extract`。
3. **HIGH only:** 二次派 `@requirement-engineer` 加 `adversarial_round: A`（Category A — 是否解决对题）。一轮即止，**禁循环**。
4. **Inline (main agent):** 产 `<run_dir>/focus_card.md` 草稿（Phase 2 architect 最终定稿；本阶段是 PreToolUse hook 的占位）。

**Output (Explorer phase deliverables — what the main agent has after orchestration):**
- `<run_dir>/explore_report.md` — written by `@requirement-engineer` sub-agent. Contains Spec Inference, AC list, Hidden Scope, Recommended Allowed Scope, Open Questions.
- `<run_dir>/focus_card.md` — scope lock draft written by main agent.
- A **one-line summary** in the main transcript: `[Explorer Done] AC count=N | Open Qs=M | Recommended scope rows=K | Report: <run_dir>/explore_report.md`
- **Forbidden in main transcript:** the full AC list, the wiki excerpts, or any inline `[Explore]` block. These belong in `explore_report.md`. The main agent reads `explore_report.md` on demand during Propose — not by absorbing it into its context here.

> Anti-regression：Tier-2 触发 dispatch 时，主上下文读 `preferences/` `domain/` 或起 AC 即违反；Inline-eligible 时反之可以。判定见 [policy.md](policy.md#tier-2--conditional)。

### Phase 2: Propose
**Mounted Roles:** `@system-architect` — **dispatch when Tier-2 triggers fire** (see [policy.md](policy.md#tier-2--conditional-dispatch-only-when-complexity-threshold-is-met))

> Path 选择：dispatch 用于 long-write + 多 wiki anchor 的设计（HIGH 风险 / ≥2 contested 设计 / 跨模块 choreography / 预期 openspec >150 行）；inline 仅限 MEDIUM + 直接 delta（无 ADR、无跨模块）。完整触发见 [policy.md](policy.md#tier-2--conditional)。

**Orchestration (main agent — pick path per Tier-2 evaluation):**

**Inline Path (Tier-2 not triggered — Standard MEDIUM + straightforward delta):**

1'. Main agent reads the Explorer hand-off (`explore_report.md` if Dispatch Path was used in Phase 1, or its own slim version from the Inline Path) + the user-confirmed design choices.
2'. Main agent writes `<run_dir>/openspec.md` directly — slim shape: §1 Context + §4 Data Model (if DDL) + §5 Business Logic + §6 Non-Functional Constraints + §7 ACs. Skip §2/§2.5/§3/§5.5/§6.5/§8/§9 unless their trigger explicitly fires per [openspec_schema.md](../llm_wiki/schema/openspec_schema.md). Target length ~80 lines.
3'. Main agent finalizes `<run_dir>/focus_card.md` (Allowed Scope exhaustive).
4'. Run `python3 .claude/scripts/wiki/schema_checker.py <run_dir>/openspec.md` — structural gate.

**Dispatch Path (any Tier-2 trigger fires — HIGH risk, ≥2 contested designs, cross-module choreography, or expected >150-line openspec):**

1. **Dispatch `@system-architect`** per [dispatch-template.md](dispatch-template.md) Example 5。子代理内部协议见 [`.claude/agents/system-architect.md`](../agents/system-architect.md) Process（Step 0 强制读 `explore_report.md`，不再 re-crawl）。Risk → 段覆盖：LOW 4 段 Slim；MEDIUM 核心 §1/§5/§6/§7 + 触发条件段；HIGH 全段 + §9 ≥2 ADR。
2. 验证返回 `subagent_return_gate.py --task-kind implement`（architect 输出是 written contract）。
3. `schema_checker.py <run_dir>/openspec.md` 结构 gate。FAIL → echo `[Issues Found]` 重派。

**Output (Propose phase deliverables):**
- `<run_dir>/openspec.md` — written by `@system-architect` sub-agent (the contract for all downstream phases)
- `<run_dir>/focus_card.md` — finalized scope lock (written by sub-agent during Step 3, supersedes Phase 1 draft)
- One-line summary in main transcript: `[Propose Done] spec_mode=STANDARD|SLIM | sections=N | ADR alts=K | Allowed Scope=M files`
- **Forbidden in main transcript:** the full openspec content, the design alternative trees, the wiki excerpts. Main agent reads `openspec.md` on demand at Approval Gate / Implement entry.

### Phase 3: Review
**Mounted Roles:** `@system-architect`

**Activities:**
- MEDIUM: `.claude/skills/code-review-checklist/SKILL.md` + `.claude/skills/java-engineering-standards/SKILL.md`
- HIGH: above + adversarial review category B (是否用了正确方式解决) — ONE round
- Plan Review Checklist (≥3 tasks): Completeness, Consistency, Feasibility, Risk Coverage, Dependency Soundness
- Review fails → roll back to Propose

### Approval Gate (HIGH only)
Present `<run_dir>/openspec.md` Human Section (Chinese — 做什么/为什么 + 怎么做 + 待确认项) to user. Set launch_spec row to `WAITING_APPROVAL`. Wait for explicit approval. Responses:
- Full approval → enter Implement
- Partial → record approved sections, roll back rejected only
- Full rejection → roll back to Propose

### Phase 4: Implement
**Mounted Roles:** `@lead-engineer`, `@focus-guard`

> `@focus-guard` at Implement: run-time check — fires on every file edit (also wired as the PreToolUse hook calling `scope_guard.py`). Blocks edits whose target file is outside `focus_card.md` Allowed Scope. Bypass: `CLAUDE_SCOPE_GUARD_BYPASS=1` (one-shot env var).

**Actions:**
1. Read openspec Machine Section — Allowed Scope + ACs + Hard Constraints
2. **TaskList init**：openspec §7 AC ≥3 时，按 [tasklist-usage.md](tasklist-usage.md) Pattern B 把 ACs 映射为 TaskList（强制）。AC <3 跳过。
3. Implement strictly within Allowed Scope. Out-of-scope edits → `[Boundary Exception Request]` + wait for approval.
4. After each change: `mvn compile -q 2>&1 | tail -20`. MAX 2 retries on compile failure.
5. **YIELD:** Once code is written + compile is clean, STOP and ask human for permission to enter QA. Do not auto-continue into heavy test runs.

**[Plan Invalidation]:** openspec 核心假设结构性矛盾（非 missing dep）时 emit catalog marker（格式见 Part 3.5 Bracket Marker Catalog）。**禁扩 scope 救火，等用户决定。**

### Phase 5: QA
**Mounted Roles:** `@code-reviewer`, `@documentation-curator`

**Actions:**
1. Run `mvn clean compile` (full) if not run since last edit
2. Run targeted tests if they exist; otherwise document manual verification
3. ACs ≥ 4 or risk = HIGH: produce Evidence Mapping Table — each Given/When/Then → test method (or manual step) → expected → actual → status
4. Code review per `.claude/skills/code-review-checklist/SKILL.md`
5. MAX 2 retries on failure → 3rd failure: STOP, ask human.

### Phase 6: Archive

**Mounted Roles execute in this strict order** (later roles read what curator just moved):

| # | Role | Triggered for | Purpose |
|---|---|---|---|
| 1 | `@documentation-curator` | All intents/profiles | Moves `<run_dir>/openspec.md` → `archive/YYYYMMDD_<slug>.md`; appends changelog. **Must run first** — downstream roles read the archived file. |
| 2 | `@architecture-curator` | Workflow intent | Drafts notes into `wiki/architecture/` (agent/skill/hook/lifecycle/scripts topology). Skip if no system-level impact. |
| 3 | `@skill-graph-curator` | All, if skills added/removed/renamed | Syncs `trae-skill-index/SKILL.md` with `.claude/skills/<*>/SKILL.md`. |
| 4 | `@librarian` | STANDARD Change Archive, or `@gc` invoked | Lints wiki health, splits oversize files via `@knowledge-architect`. |

Per-change Archive does **NOT** extract knowledge into per-module wiki files. Knowledge sublimation is a separate threshold-triggered op (`@harvest`) — see Part 5.

(Source of truth for mount: `.claude/workflow/role_matrix.json`. The roles array order MUST match the above.)

**Steps (executed by `@documentation-curator`):**
1. Move `<run_dir>/openspec.md` → `.claude/llm_wiki/archive/YYYYMMDD_<slug>.md`
2. Append one-line changelog to `.claude/llm_wiki/archive/index.md`

**Steps (executed by `@architecture-curator`, Workflow intent only):**
3. Read the archived openspec, write note into the relevant `wiki/architecture/<topic>.md`.

**Steps (after all mounted roles complete):**
4. (Optional) Ask human for 1–10 rating. Rating ≥ 8 → extract praised practice to `preferences/index.md`. Rating ≤ 5 → extract anti-pattern.
5. Recommend `python3 .claude/scripts/wiki/harvest_threshold.py --all` if any module's archive accumulation may be ripe for `@harvest`.

---

# Part 5 — Maintenance Operations

When user requests pure knowledge ops, no code phases:

| Trigger | Role | Flow |
|---|---|---|
| `@gc` / `@librarian` / "整理 wiki" | `@librarian` | Lint → split oversize → close orphans |
| `@harvest <module>` / threshold report flags a module | `@knowledge-harvester` | Threshold check → classify archives (PRIMARY/SUPERSEDED/REDUNDANT/AMBIGUOUS) → ≤4 human Q's → CONFIRM gate → merge to wiki + delete archives + tombstone rows |
| index > 500 lines / "拆分文档" | `@knowledge-architect` | Check → Deduplicate → Split → Rewrite index |
| "扫描项目" / "审计代码库" | Explorer (inline) | Scan → Report (no writes) |

---

# Part 6 — Hooks (settings.json wired)

Real Claude Code hooks live in `.claude/settings.json`. The harness scripts they invoke:

| Hook | Trigger | Script |
|---|---|---|
| **PreToolUse** | Before every Edit/Write | `pre_tool_use_hook.py` → `scope_guard.py` (blocks out-of-scope edits when an active openspec exists; silent skip otherwise) |
| **PostToolUse** | After every Edit/Write | `post_tool_use_hook.py` → `secrets_linter.py` on changed file |
| **UserPromptSubmit** | Before every user prompt | `user_prompt_submit_hook.py` → injects recurring failure patterns into context (silent if none) |

These run automatically. The agent does not invoke them manually.

**scope_guard semantics:** `find_active_focus_card.py` 按 mtime 取 latest `.claude/runs/<intent>__*/focus_card.md` → 读 `## Allowed Scope` 段非占位条目 → 比对待 Edit 文件。无 ACTIVE focus_card（不存在 / Allowed Scope 全占位 `<...>` / 不含 `## Allowed Scope` 段）→ silent skip（Vibe 不限制）；命中 → pass；越界 → exit 2 阻拦 Edit。`.claude/*` 路径（runs / scripts / rules / agents / skills / commands / workflow / llm_wiki / settings.json）在 `HARNESS_PREFIXES` 白名单内，免约束。Bypass 见 [policy.md](policy.md#debug-only-bypass-switches) `CLAUDE_SCOPE_GUARD_BYPASS=1`。

**Phase 2_Propose 占位窗口期保护语义：** `/h-brief` & `/h-from-ticket` 写占位 focus_card（`<TBD by /h-design>`）→ find_active_focus_card 视为非 ACTIVE → 业务代码 Edit 不受 scope_guard 拦截（等同 Vibe）。设计期不该写业务代码，主 agent 应在 `/h-design` finalize focus_card 后再进 Implement；如需提前写业务代码，必须先 finalize Allowed Scope。

## Agent-Executed Phase Gates

| Phase | Gate | Command |
|---|---|---|
| Explorer → Propose | Impact check | (manual; use Grep/Bash to enumerate callers) |
| Propose → Implement | openspec structure | `python3 .claude/scripts/wiki/schema_checker.py <run_dir>/openspec.md` |
| Propose → Implement | HIGH risk: Approval Gate | Human approval — block until granted |
| Implement → QA | Compile | `mvn compile -q` (MAX 2 retries) |
| QA → Archive | Secrets scan | `python3 .claude/scripts/gates/secrets_linter.py --paths "<changed files>"` |

## Role-Matrix Gate Driver (`run.py`)

In addition to the standalone gates above, every Standard phase MAY invoke the
role-aware aggregator `python3 .claude/scripts/gates/run.py`. It reads
`.claude/workflow/role_matrix.json`, resolves the roles mounted at
`(intent, profile, phase)`, evaluates any `condition` strings against the
active openspec frontmatter, and runs each role's gates.

**Canonical invocation:**

```bash
python3 .claude/scripts/gates/run.py \
    --intent Change --profile STANDARD --phase <Explorer|Propose|Implement|QA|Archive> \
    --topic <feature_slug> --date YYYYMMDD
# --run-dir is OPTIONAL: when omitted, run.py auto-discovers the active run_dir
# by mtime under .claude/runs/<intent>__*/openspec.md.
# --scenario DEBUG adds Scenario-mounted roles (e.g., debugger).
```

**Auto-discovery semantics for `--run-dir`:**
1. Explicit `--run-dir <path>` wins (CI / batch drivers).
2. Else `CLAUDE_ACTIVE_RUN_DIR` env var (set by orchestration shells / harness).
3. Else newest `.claude/runs/<intent>__*/` that contains `openspec.md`.
4. None of the above → conditional mounts fail-safe-skip (Vibe work or no active task is correctly left unrestricted).

**Conditional mount behavior** — mounts in `role_matrix.json` may carry a
`condition` string of the form `openspec.<key> (==|!=) <value>`. Hyphens in
openspec frontmatter keys are normalized to underscores
(`frontend-facing: true` → `openspec.frontend_facing == true`). When the
expression can't be parsed, the mount is fail-safe-skipped and noted in the
gate report's `Mount Condition Evaluation` section.

## Scenario-Specific Gates

| Scenario | Gate |
|---|---|
| B (DB Migration) | `python3 .claude/scripts/gates/migration_gate.py --sql-dir <path>` |
| C (Breaking API) | `python3 .claude/scripts/gates/api_breaking_gate.py --openspec <path>` |
| E (Dependency) | `python3 .claude/scripts/gates/dependency_gate.py --pom <pom.xml>` |
| Any Java change | `python3 .claude/scripts/gates/comment_linter_java.py --path <dir>` (advisory) |

## Failure Protocol

On any gate failure:
1. Fix the underlying issue (do NOT add bypass justifications without DBA / lead sign-off)
2. Re-run. MAX 2 retries per phase
3. Same phase fails 3 times → STOP and ask human

## Compound Failure Decision Matrix

| Scenario | Action |
|---|---|
| QA → Implement, scope unchanged | Normal rollback within existing Allowed Scope |
| QA → Implement, scope needs expansion | STOP. `[Boundary Exception Request]`. Wait for approval. |
| Same phase fails twice, same root cause | STOP. Escalate with evidence. |
| Same phase fails twice, different root causes | STOP. Roll back to Propose for contract amendment. |
| Implement compile fails (shift_left) | Fix, max 2 retries. Both fail → downgrade to Propose. |
