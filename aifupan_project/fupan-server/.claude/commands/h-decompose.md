---
description: 把 PRD / EPIC 切成 N 个 vertical slice，每个 slice 一个 run_dir + openspec.md 骨架，串成 DAG launch_spec。
argument-hint: <PRD 路径 或 EPIC 描述>
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Agent
---

输入：`$ARGUMENTS`

## 适用判定

仅满足以下任一条件继续：跨 ≥3 jiuyu 模块 / 框架迁移 / 大规模重构 / 输入是 PRD 文档。否则输出 `[Decompose Skipped] reason: <…>; use /h-brief instead.` 停。

## 步骤

### 1. Ambiguity 预校验

按 `.claude/rules/dispatch-template.md` Example 3 派 `@ambiguity-gatekeeper`，Allowed Scope = `none`。

- `[Status]: BLOCK` → 把澄清问题转给用户，停。
- `[Status]: PASS` → 继续。

校验返回：

```bash
python3 .claude/scripts/gates/subagent_return_gate.py --task-kind audit --return-file <tmp>
```

### 2. 派 `@requirement-engineer` 拆解

按 `dispatch-template.md` Example 4 派。`<顶层 run_dir>` 用 `python3 .claude/scripts/tools/new_run_dir.py --intent Change` 生成。

Inputs 段追加：

```
task: decomposition mode — split into vertical slices
required_skills:
  - .claude/skills/prd-task-splitter/SKILL.md       (if PRD)
  - .claude/skills/task-decomposition-guide/SKILL.md (otherwise)
output_target: <顶层 run_dir>/decomposition_plan.md
```

Allowed Scope 仅 `<顶层 run_dir>/decomposition_plan.md`。Hard Constraints：禁动源码；每个 slice 必须通过 INVEST。

### 3. 为每个 slice 起骨架

读 `decomposition_plan.md`。对每个 slice：

1. `python3 .claude/scripts/tools/new_run_dir.py --intent Change` → 独立子 run_dir。
2. 写 `<slice_run_dir>/openspec.md`，schema 严格按 `.claude/llm_wiki/schema/openspec_schema.md`，frontmatter：

```yaml
spec_mode: STANDARD
risk: TBD
frontend-facing: TBD
module: <推断>
triggers: []
slice_id: S-001
depends_on: [S-002]
parent_decomposition: <顶层 run_dir>/decomposition_plan.md
launch_spec: <launch_spec 相对路径>
```

Body 只写 §1 Context 一句 Goal + INVEST 评估。其余条件段写占位 `TBD by /h-design`。

3. 写 `<slice_run_dir>/focus_card.md`，Allowed Scope 留 `<TBD by /h-design>` 占位。

### 4. 写 DAG launch_spec

在 `.claude/runs/launch_spec_<YYYYMMDD_HHMMSS>.md`：

```markdown
# Launch Spec — <topic> — <timestamp>

## DAG
\`\`\`mermaid
graph LR
  S001 --> S003
  S002 --> S003
\`\`\`

## State Machine
| Intent | Status | Phase | Artifact/Log | Failed_Reason |
|---|---|---|---|---|
| S-001 | PENDING | - | .claude/runs/Change__<ts1>/openspec.md | - |
| S-002 | PENDING | - | .claude/runs/Change__<ts2>/openspec.md | - |
| S-003 | PENDING | - | .claude/runs/Change__<ts3>/openspec.md | - |

## Resume
- 入度=0 的 slice 可并发：每个跑 `/h-design` → Implement → `/h-gates` → `/h-archive`。
- 入度>0 等所有 depends_on 翻 DONE。
```

`Intent` 列填 slice_id，与 openspec.md frontmatter `slice_id` 双向对齐。State Machine 表必须 5 列。

### 5. 输出

```
[Decompose Done] slices=N | launch_spec=<path>
DAG (parallel slices comma-separated, → = depends-on):
  S-001, S-002 → S-003
Next: /h-design on each 入度=0 slice (S-001, S-002 above).
```

不回贴 slice 细节到主上下文。

## 硬约束

- INVEST 评估、grep wiki、起 ACs 全部由 `@requirement-engineer` 做；inline 做 → 违反 `lifecycle.md` Phase 1。
- launch_spec 文件名严格 `launch_spec_<YYYYMMDD_HHMMSS>.md`。
- DAG 不能有环。
- 每 slice 一个独立 run_dir，禁止复用。
