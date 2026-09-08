# Dispatch Prompt Template

Canonical skeleton for every sub-agent prompt. The main agent fills in placeholders — it MUST NOT write dispatch prompts from scratch. The receiving sub-agent MUST validate this structure before doing any work; missing sections → return `[Status]: ESCALATE` with the missing section name.

**Why this exists:** sub-agents do NOT inherit `CLAUDE.md`, project rules, memory, or anti-loop limits. The dispatch prompt is the *only* contract they see. Free-form prompts lead to forgotten constraints and unparseable returns.

---

## Template (copy verbatim, fill the `<…>` placeholders)

```
# Dispatch: <agent-name>

## Task Contract
**Allowed Scope** (verbatim from openspec — file paths/prefixes, one per line):
- <path-or-prefix-1>
- <path-or-prefix-2>

**Acceptance Criteria** (Given/When/Then, copied from openspec; assign each an AC-id):
- AC-1: Given <…>, when <…>, then <…>.
- AC-2: Given <…>, when <…>, then <…>.

**Hard Constraints** (jiuyu invariants the sub-agent MUST NOT violate)

**适用判定（按 Allowed Scope 路径）：**

| Allowed Scope 含 | 该节处理 |
|---|---|
| 业务代码（`**/src/**/*.java`、`sql/**/*.sql`、Mapper `*.xml`） | **必跑脚本生成**（下方命令），禁手抄 |
| 仅 wiki / run_dir 文件（`<run_dir>/openspec.md`、`focus_card.md`、`decomposition_plan.md`、`.claude/llm_wiki/**`） | 跳过脚本，写 `none`（业务不变量不适用），仅列 sub-agent 自身约束 |
| `none`（read-only sub-agent，如 @ambiguity-gatekeeper / @debugger） | 写 `none` |

**业务代码脚本生成（强制）：**

```bash
python3 .claude/scripts/harness/generate_hard_constraints.py --files <Allowed Scope paths>
```

Paste the script output verbatim into this section. The script:
- pattern-matches each path to applicable categories (controller / service / entity / sql / ddl / cross_module / security / universal)
- emits only the bullets that apply (e.g., a pure Mapper.xml scope gets SQL rules only, not Controller rules)
- pulls every bullet from CLAUDE.md §5 + openspec_schema.md §4 (`--validate-ssot` is the drift guard)
- inserts a `<!-- categories fired -->` comment so the reviewer can see what triggered

Fallback (script unavailable): copy from [`CLAUDE.md §5`](../../CLAUDE.md), pick rows relevant to your scope, and surface as `[Issues Found]` that the script was bypassed.

Example output for `[Controller.java, ServiceImpl.java, tb_xx.sql]`:
- DI 用构造器注入，禁 `@Autowired` / `@Resource`（新代码强制；改 legacy 时镜像周围风格）
- Controller 必须返回 `R<T>`，禁裸返业务对象
- 写方法加 `@Transactional(rollbackFor = Exception.class)`
- ID 用 `SnowflakeManager.nextValue()`
- 列表查询必须带 `tenantId` 过滤
- SQL 占位用 `#{}` 禁 `${}`
- 新表必含 `tenant_id BIGINT NOT NULL`、`is_deleted TINYINT NOT NULL DEFAULT 0`、`create_date DATETIME NOT NULL`、`update_date DATETIME NOT NULL`
- (其余按 scope 弹出)


## Inputs
- Openspec: <.claude/runs/<run_dir>/openspec.md>
- Focus card: <.claude/runs/<run_dir>/focus_card.md>
- Files to inspect/modify: <comma-separated paths or "see Allowed Scope">
- Commit range / line numbers (if applicable): <…>
- Other inputs: <…>

## Hard Limits (apply to YOU, the sub-agent — your context does NOT inherit them)
- MAX 3 retries per gate/linter run.
- MAX 2 retries for compile failures **inside your Allowed Scope** (`mvn -pl <touched-modules> compile`, never `-am` unless you actually edited the shared dep). Errors in upstream modules outside your Allowed Scope do NOT count against retries — narrow the `-pl` flag, note as `[Pre-existing Issue]`, do not bail. See `.claude/agents/lead-engineer.md` "After Each Change" for the full triage protocol.
- After 2 same-root-cause failures **inside your Allowed Scope**: STOP, return `[Status]: ESCALATE`.
- DO NOT modify files outside Allowed Scope. If required, return `[Status]: BOUNDARY_EXCEPTION` with the file and reason — wait for main agent, do not edit.
- DO NOT bypass safety checks (`--no-verify`, `--no-gpg-sign`, `git commit --amend` on published commits, etc.).
- DO NOT invoke other sub-agents. Return to the main agent for orchestration.
- DO NOT use @Autowired or @Resource in NEW classes (constructor injection — see CLAUDE.md §5). When editing legacy classes already using @Resource / @Autowired throughout, **mirror the surrounding pattern** for added fields (外科手术原则 / minimal-blast-radius) and surface the inconsistency in `[Issues Found]` rather than refactoring 80 fields. DO NOT use @TableLogic (manual isDeleted). DO NOT skip @Transactional on multi-table write methods.

## Expected Output (structured — parseable by main agent)
Return ONLY this block, no preamble:

[Status]: PASS | PARTIAL | FAIL | ESCALATE | BOUNDARY_EXCEPTION
[Files Changed]: <list of relative paths with +N/-M line counts, or "none">
[Commands Run]: <each command + exit code, or "none">
[ACs Mapped]: <AC-id → test method or evidence → PASS/FAIL/SKIP>
[Issues Found]: <numbered list, or "none">
[Next Step]: <one sentence — what main agent should do next>

(If [Status] is ESCALATE or BOUNDARY_EXCEPTION, also include a [Reason]: line explaining why.)

## Template Source
This prompt was built from: .claude/rules/dispatch-template.md
```

---

## Validation rules (for the receiving sub-agent)

Before doing anything, check the incoming prompt has these sections (header lines):
- `## Task Contract` with all three subsections: Allowed Scope, Acceptance Criteria, Hard Constraints
- `## Inputs`
- `## Hard Limits`
- `## Expected Output`

If any are missing OR if Allowed Scope is empty:
```
[Status]: ESCALATE
[Reason]: Dispatch prompt missing required section(s): <list>
[Next Step]: Main agent must re-dispatch using .claude/rules/dispatch-template.md
```

Do not attempt to fill in missing sections from inference. The contract must be explicit.

---

## Return validation (for the main agent — RUN AFTER RECEIVING)

After the sub-agent returns, validate the structure before acting on it:

```bash
python3 .claude/scripts/gates/subagent_return_gate.py \
    --return-file <path> \
    --task-kind implement|review|extract|audit
```

Exit codes:
- **0 OK** — proceed
- **1 WARN** — return is structurally valid but has consistency warnings (e.g., `[Status]=PASS` with no files changed). Surface the WARN to the user.
- **2 FAIL** — return is structurally broken or contains contradiction (`[Status]=PASS` with an AC row reporting FAIL). Re-dispatch.

This gate catches a real failure mode: sub-agent claims `[Status]: PASS` while internal evidence ([Files Changed], [Commands Run], [ACs Mapped]) is empty or contradicts the claim.

---

## When the template is NOT required

For LEARN/MAINTENANCE-only sub-agent dispatches (e.g., librarian, knowledge-architect doing read-only wiki ops), the `Hard Constraints` and `ACs Mapped` may be empty (`none`) — but the headers must still be present. This keeps the parsing contract uniform.

---

## Examples

### Example 1 — Dispatching lead-engineer

```
# Dispatch: lead-engineer

## Task Contract
**Allowed Scope**:
- replay-words/src/main/java/com/jiuyu/replay/words/service/AnchorUrlService.java
- replay-words/src/main/java/com/jiuyu/replay/words/service/impl/AnchorUrlServiceImpl.java
- replay-words/src/main/java/com/jiuyu/replay/words/dao/AnchorUrlDao.java
- replay-api/src/main/java/com/jiuyu/replay/api/controller/words/AnchorUrlController.java

**Acceptance Criteria**:
- AC-1: Given 已登录用户, when POST /replay/words/anchorUrl/updateAnchorBaseInfo, then 主播基础信息更新成功且写入 tb_anchor_url, 返回 R<Boolean>(true).
- AC-2: Given 当前用户无该主播权限, when 调用接口, then 抛出 BusinessException(ResponseCode.AUTH_FAIL).

**Hard Constraints**:
- Service 写方法必须加 @Transactional(rollbackFor = Exception.class)
- ID 使用 SnowflakeManager.nextValue()
- Controller 必须返回 R<T>，禁止直接返回业务对象
- DI 使用构造器注入（禁 @Autowired 和 @Resource）

## Inputs
- Openspec: .claude/runs/Change__2026-05-19_20-30-00/openspec.md
- Focus card: .claude/runs/Change__2026-05-19_20-30-00/focus_card.md
- Files to inspect/modify: see Allowed Scope
- Other inputs: existing AnchorUrlService at replay-words/src/main/java/com/jiuyu/replay/words/service/AnchorUrlService.java

## Hard Limits
[…verbatim from template…]

## Expected Output
[…verbatim from template…]

## Template Source
This prompt was built from: .claude/rules/dispatch-template.md
```

### Examples 2-5 — Variations (从 Example 1 派生，只列差异)

通用字段（`## Inputs` 通用项、`## Hard Limits`、`## Expected Output` 结构、`## Template Source`）沿用本文件顶部 Template 与 Example 1。下表只列**与 Example 1 的差异**：

#### Example 2 — code-reviewer（review-only）

| 字段 | 差异 |
|---|---|
| Allowed Scope | `(review-only — no edits)` |
| AC | `Given diff <range>, when reviewed, then no CRITICAL findings remain.` |
| Hard Constraints | 3-layer / N+1 / 租户隔离 三项检查；**不 flag 既存 Bll-Producer-Rse wrappers**（除非新引入）；report-only |
| Inputs add | Commit range + Files changed 列表 |

#### Example 3 — ambiguity-gatekeeper（Explorer DoR check）

| 字段 | 差异 |
|---|---|
| Allowed Scope | `(read-only — no file edits)` |
| AC | `none`（gatekeeper 出 verdict，非 implementation）|
| Hard Constraints | 禁猜 missing DoR signals → BLOCK 问用户；≤1 wiki file（仅 `preferences/index.md`）；不起 AC（让 @requirement-engineer 起） |
| Inputs | raw intent text + run_dir（read-only）+ shortcut（@vibe/@standard/@patch/none） |
| Expected Output | `[Status]: PASS \| BLOCK \| ESCALATE`；`[Issues Found]` 含 numbered missing signals + clarifying questions；详格式见 `.claude/agents/ambiguity-gatekeeper.md` "Output Format" |

#### Example 4 — requirement-engineer（Explorer AC translation）

| 字段 | 差异 |
|---|---|
| Allowed Scope | `<run_dir>/explore_report.md`（唯一可写） |
| AC | `Given DoR-PASS, when @requirement-engineer 工作, then explore_report.md 含 ≥1 happy-path + ≥2 edge ACs / Spec Inference / Hidden Scope（grep callers）/ Recommended Allowed Scope` |
| Hard Constraints | 详尽内容**写文件不内联返回**；≤5 wiki files；vague 语 push back 进 `[Issues Found]`；源码 read-only |
| Inputs | raw intent + DoR-PASS 行 + run_dir + 可能模块 hint + `adversarial_round: A`（HIGH 二次派标） |
| Expected Output | `[ACs Mapped]: AC-001 → happy; AC-002 → <edge>; AC-003 → <edge>; ...`；详格式见 `.claude/agents/requirement-engineer.md` "Part 2" |

#### Example 5 — system-architect（Propose design + openspec）

| 字段 | 差异 |
|---|---|
| Allowed Scope | `<run_dir>/openspec.md` + `<run_dir>/focus_card.md` + `wiki/specs/index.md`（追加 1 行） |
| AC | AC-1: openspec 含全 REQUIRED + 触发 CONDITIONAL 段（见 `openspec_schema.md` Section trigger matrix）；AC-2: `risk=HIGH` 时 §9 ≥2 Nygard ADR（Context/Decision/Alternatives/Consequences/Risks）；AC-3: openspec Allowed Scope 与 focus_card 一致 |
| Hard Constraints | **Step 0 强制读 explore_report.md，禁 re-derive AC / re-crawl wiki**；禁写源码（contract only）；ADR 嵌 §9 不分文件（@architecture-curator 在 Archive 抽出）；不允许 skip 触发段（N/A 时含 header + 一行 reason）；jiuyu 不变量（`@Autowired/@Resource/@TableLogic` 禁、跨模块走 Feign） |
| Inputs | run_dir + explore_report path + focus_card path + risk + frontend-facing + module |
| Expected Output | 额外两行 `[Sections Activated]: §1, §2.5, ...`、`[ADR Count]: <K>`（MEDIUM=0，HIGH≥2）；`explore_report.md` 缺失或空 → `ESCALATE` + `[Reason]: Explorer phase did not produce a usable report.` |
