---
description: 起单个 openspec.md 骨架（按 schema），并落 launch_spec IN_PROGRESS 行。
argument-hint: <一句话任务描述> [--slice S-XXX] [--launch-spec <path>] [--risk low|medium|high] [--slim]
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Agent
---

输入：`$ARGUMENTS`

产出**一个** `<run_dir>/openspec.md` 骨架，通过 `schema_checker.py`，并在 launch_spec 落 IN_PROGRESS 行。

## 步骤

### 0. Research 分流自检（**必跑**）

`/h-brief` 是 Change 入口，会产 openspec + focus_card + launch_spec + 默认派 `@system-architect`。如果意图实际是**调研 / 可行性研究**（输出报告而非代码），跑完整套要浪费 ~6× token。

**判定（信号 score 投票）：**

| 信号类型 | 关键词 |
|---|---|
| **R-verb** | 分析 / 调研 / 评估 / 探索 / 可行性 / 现状 / 能给到什么程度 / feasibility / research / investigate / assess |
| **R-outcome** | 报告 / 清单 / Gap 分析 / 数据现状 / 能不能做 / 可不可行 / 成本估算 |
| **C-verb（反信号，抵消 1 个 R 信号）** | 实现 / 修复 / 优化 / 新增 / 重构 / 上线 / 改造 / fix / implement / refactor |
| **C-outcome（反信号，抵消 1 个 R 信号）** | 代码 / endpoint / SQL / migration / fix it |

**Score = R信号数 − C信号数**：

| Score | 处理 |
|---|---|
| ≤0 | 继续 step 1（Change 意图）|
| = 1 | emit `[Research Diversion]` catalog marker + **AskUserQuestion** 让用户二选一（不要硬停文本）|
| ≥ 2 | 同上（信号强，仍 Ask 不静默切换）|

**AskUserQuestion 题（仅 Score ≥1 时发）：**

| header | options |
|---|---|
| `Intent: research or change?` | `Research — 切到 /h-research（产报告，不写代码）` / `Change — 继续 /h-brief（写代码）` / `取消（让我重新表述意图）` |

- 选 `Research` → 输出 `Switch with: /h-research <主题>`，终止本命令。
- 选 `Change` → 跳 step 1。
- 选 `取消` → 输出空、不动状态，等用户重发。

**短路条件（不发 Ask 直接 step 1）：** 已有 `--slice` 参数（slice 来自 /h-decompose，必是 Change）。

### 1. Ambiguity 自检

有 `--slice` → 跳过（已经过 `/h-decompose` 校验）。

无 `--slice` → 按 [policy.md Tier-2](../rules/policy.md#tier-2--conditional) 条件判定（**禁止用字数代理**）：

| 触发条件 | 处理 |
|---|---|
| `$ARGUMENTS` 缺 action verb（做什么） / target（动谁） / measurable outcome（怎样算成）任一，且对话上下文中用户没有预先澄清 | 派 `@ambiguity-gatekeeper`（按 `dispatch-template.md` Example 3） |
| 用户敲入 `@standard` shortcut 触发本命令 | 派 `@ambiguity-gatekeeper`（policy.md Tier-2 (b)） |
| 三要素齐全且无 `@standard` | inline 自检；emit `[DoR PASS] Action: <verb> \| Target: <object> \| Outcome: <measurable>`（catalog marker），跳到 step 2 |

派 `@ambiguity-gatekeeper` 后：
- `[Status]: BLOCK` → 把澄清问题转给用户，停。
- `[Status]: PASS` → 继续。
- 校验返回：`python3 .claude/scripts/gates/subagent_return_gate.py --task-kind audit --return-file <tmp>`

### 2. Risk 路由

优先级（先匹配先用，禁静默 default）：

1. `--risk low|medium|high` arg 显式给 → 用之
2. `--slim` → LOW
3. 按 `lifecycle.md` Part 1 Trigger-based 升级阈值自动评级

第一行输出 catalog 中的 canonical `[Route]`（见 `lifecycle.md` Part 3.5）：

```
[Route] Mode=Standard | Risk=<LOW|MEDIUM|HIGH> | Phases={2_Propose, ...} | Reason: <一句>
```

LOW → Slim Spec（4 段）；MEDIUM/HIGH → Standard Spec。

### 3. run_dir

- 无 `--slice`：`python3 .claude/scripts/tools/new_run_dir.py --intent Change`。
- 有 `--slice`：从 launch_spec 表查该行 Artifact 列，取父目录。

### 4. 写 openspec.md

路径：`<run_dir>/openspec.md`。Schema 按 `.claude/llm_wiki/schema/openspec_schema.md`。

Frontmatter：

```yaml
spec_mode: STANDARD | SLIM
risk: LOW | MEDIUM | HIGH
frontend-facing: true | false
module: <module-name>       # frontend-facing=true 时必填
triggers: []                # /h-design 阶段填
slice_id: S-XXX             # 有 --slice 时填
launch_spec: <相对路径>
```

Body：

- SLIM → 完整 4 段（Change Summary / Scope / Risk & Rollback / Verification & Evidence）。
- STANDARD → §1 写一句 Goal；§5 / §6 / §7 占位 `TBD by /h-design`；其余条件段不写。

同步写 `<run_dir>/focus_card.md`，Allowed Scope 用 `<TBD by /h-design>` 占位。

### 5. 绑定 launch_spec

**无 `--slice`** → 在 `.claude/runs/` 写 `launch_spec_<YYYYMMDD_HHMMSS>.md`：

```markdown
# Launch Spec — <slug> — <timestamp>

## State Machine
| Intent | Status | Phase | Artifact/Log | Failed_Reason |
|---|---|---|---|---|
| <slug> | IN_PROGRESS | 2_Propose | <run_dir>/openspec.md | - |

## Resume
- /h-resume 查活跃；/h-design 进 Propose；/h-archive 完结。
- HIGH risk 在 /h-design 后 Status=WAITING_APPROVAL。
```

把 launch_spec 相对路径回填到 openspec.md frontmatter `launch_spec:`。

**有 `--slice`** → 用 `Edit` 把该 slice 行的 `Status` 改 `IN_PROGRESS`、`Phase` 改 `2_Propose`。

### 6. 结构体检

```bash
python3 .claude/scripts/wiki/schema_checker.py <run_dir>/openspec.md
```

FAIL → 把缺失行透传给用户，不自动补。

### 7. 输出

```
[Brief Done] run_dir=<…> | risk=<…> | spec_mode=<…> | launch_spec=<…>
Next: /h-design to fill via @system-architect.
```

## 硬约束

- 一次只产**一个** openspec.md；多任务用 `/h-decompose`。
- 路径严格 `<run_dir>/openspec.md`。
- focus_card.md 必须落盘（即便占位）。
- State Machine 表 5 列、列顺序固定。
- 骨架阶段不调 `@system-architect`、不写 ACs、不做设计。
