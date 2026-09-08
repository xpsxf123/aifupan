---
description: 派 @system-architect 把 openspec.md 骨架填到 ready-to-implement；HIGH risk 强制 ≥2 ADR 写入 §9。
argument-hint: [<run_dir 或 slice_id>]
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Agent
---

前置：`/h-brief` 产出的 `<run_dir>/openspec.md` 骨架已过 `schema_checker.py`。

本命令派 `@system-architect`（Dispatch Path，见 [lifecycle.md](../rules/lifecycle.md) Phase 2、[policy.md](../rules/policy.md#tier-2--conditional) Tier-2）。敲入 `/h-design` 即放弃 Inline Path，禁 inline 设计。

> Tier-2 Inline-eligible（risk=MEDIUM ∧ 简单 delta ∧ 无 ADR）→ 跳过本命令，主 agent 直接写 slim openspec。

## 步骤

### 1. 定位 run_dir

`$ARGUMENTS` 形态判别：含 `/` → 路径；匹配 `^S-\d+$` → slice_id；其他 → 视为路径再校验。

- 路径 → 用之。
- slice_id → 从最新 `launch_spec_*.md` 查 Artifact 列，取父目录。
- 缺省 → `python3 .claude/scripts/harness/find_active_focus_card.py` 取 focus_card 父目录。

校验 `<run_dir>/openspec.md` 存在。

**LOW risk guard：** 读 openspec frontmatter，若 `risk=LOW` 且 `spec_mode=SLIM` → 输出 `[Design Skipped] LOW risk + SLIM spec — 不需要 /h-design 派 @system-architect。直接在 openspec 4 段（Change Summary / Scope / Risk & Rollback / Verification & Evidence）补完后进 Implement，/h-resume 给具体 Next。` 停手。**不派 architect，不更新 launch_spec。**

### 2. 校验 Explorer 产物

`@system-architect` Step 0 要读 `<run_dir>/explore_report.md`。

| 状态 | 处理 |
|---|---|
| 存在 | 直接 step 3 |
| 缺失 + openspec frontmatter 有 `slice_id` | 用 `<顶层 run_dir>/decomposition_plan.md` 作等价物，在 dispatch Inputs 注明 `equivalent_to: decomposition_plan.md slice <S-XXX>` |
| 缺失 + 无 slice_id + Tier-2 触发条件**任一**满足（risk=HIGH ∨ >2 wiki 文件要 drill ∨ ≥2 contested-edge ACs ∨ >150 行已有代码要读） | 按 `dispatch-template.md` Example 4 派 `@requirement-engineer` 补 explore_report，回到 step 3 |
| 缺失 + 无 slice_id + Tier-2 **全不触发** | 主 agent inline 写 slim `<run_dir>/explore_report.md`（Spec Inference 3 行 + 3-5 ACs bullets + Allowed Scope 推荐）后 step 3 |

衔接：`/h-from-ticket` 落 Phase=`1_Explorer`。`/h-design` 前先补 Explorer 产物（inline 或 dispatch 按上表），launch_spec Phase 翻 `2_Propose` 后再跑本命令。

### 3. 构建 Source Documents

dispatch `## Inputs` 段必须包含：

```
### Source Documents (READ FIRST, no substitutions)
- openspec: <run_dir>/openspec.md            ← 原地补全
- explore_report: <run_dir>/explore_report.md  ← Phase 1 真相源，禁止再 grep wiki 推 ACs
- focus_card: <run_dir>/focus_card.md        ← 本阶段 finalize
- schema: .claude/llm_wiki/schema/openspec_schema.md
- standards (pull on demand only):
  - .claude/skills/java-engineering-standards/SKILL.md
  - .claude/skills/java-backend-api-standard/SKILL.md
  - .claude/skills/mybatis-sql-standard/SKILL.md

### Routing Decision (from /h-brief, do not re-derive)
- risk: <LOW|MEDIUM|HIGH>
- frontend-facing: <true|false>
- module: <…>

### Hard Switches
- risk=HIGH → §9 必须 ≥2 个 Nygard-format ADR（schema_checker 强制）。
- frontend-facing=true → §3 + §8 必须完整。
- DB schema 变更 → §4 必须含 DDL + Index Reasoning + Anti-JOIN 策略。
```

### 4. 派 `@system-architect`

按 `dispatch-template.md` Example 5。Allowed Scope：

- `<run_dir>/openspec.md`
- `<run_dir>/focus_card.md`
- `.claude/llm_wiki/wiki/specs/index.md`

Hard Constraints 从 `CLAUDE.md §5` 摘与 Allowed Scope 相关的行（按 dispatch-template Authoring rule，不要全摘）。

Expected Output 含 `[Sections Activated]` + `[ADR Count]`。

### 5. 校验返回

```bash
python3 .claude/scripts/gates/subagent_return_gate.py --task-kind implement --return-file <tmp>
```

FAIL → 把 `[Issues Found]` 回显给 architect 重派（≤3 次）。3 次仍 FAIL → 停。

### 6. 结构体检

```bash
python3 .claude/scripts/wiki/schema_checker.py <run_dir>/openspec.md
```

FAIL → 把缺失行透传 architect 重派 1 次。

### 7. HIGH risk ADR 内容自检

risk=HIGH 时扫 §9：

- ≥2 个 `### ADR-N`。
- 每个含 Context / Decision / Alternatives Considered (≥2) / Consequences (Positive + Negative + Risks)。
- 两个 ADR 是真不同 trade-off。

不达标 → emit `[ADR Quality Warning] <具体哪条>`，问用户是否重派。

### 8. 更新 launch_spec

`Edit` 当前行 Phase：

- MEDIUM → `3_Review`
- HIGH → `3.5_Approval`，Status 改 `WAITING_APPROVAL`

### 9. 输出

```
[Design Done] sections=<§…> | ADR=<K> | next_phase=<3_Review|3.5_Approval>
Read: <run_dir>/openspec.md  (Human Section 中文，HIGH 审批用)
```

不回贴 openspec 全文。

## 硬约束

- 禁止 inline 设计：一旦自己开始读 `wiki/architecture/*.md`、画 mermaid、列 alternatives → 停手 dispatch。
- ADR 必须嵌入 §9，不分文件（分文件由 `@architecture-curator` 在 Archive 抽出）。
- focus_card.md 本命令 finalize，之后 PreToolUse hook 靠它阻拦越界编辑。
- 禁止跳过 `subagent_return_gate.py` 直接信 `[Status]: PASS`。
