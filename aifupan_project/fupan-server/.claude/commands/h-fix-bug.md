---
description: Scenario DEBUG 全链路：收集 bug → 查相似历史 → 派 @debugger root-cause → 起 launch_spec → p1/p2 输出 incident 建议 → 用户确认 fix scope → PATCH inline 或 /h-brief。Root cause confirmed 前禁 fix code。
argument-hint: [<bug 描述 或 ticket-url>] [--source yunxiao|manual] [--production] [--severity p1|p2|p3]
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Agent
---

输入：`$ARGUMENTS`

按 `lifecycle.md` Scenario DEBUG 协议。**root cause confirmed 前禁 fix code。**

## 步骤

### 1. 解析参数 + 起 TaskList

按 [`tasklist-usage.md`](../rules/tasklist-usage.md) **Pattern C** 起 5-task pipeline（强制）。p1/p2 + `--production` 时在 task 4 后插 1 个 `Record incident` task。每进入下方对应 step 前 → 该 task in_progress；step 完 → completed。

- 位置参数 `[bug 描述 或 ticket-url]`：缺则 step 2 用 `AskUserQuestion` 收。
- `--source yunxiao|manual`（默认 `manual`）：MVP 只支持 `manual`（用户粘贴 ticket 全文）。`yunxiao` 留作未来接 OpenAPI。
- `--production`：标线上 bug，触发 step 6 incident 录入。p1/p2 隐含 `--production`。
- `--severity p1|p2|p3`（默认 `p3`）：
  - `p1` 服务不可用 / 数据丢失 → Emergency。
  - `p2` 功能降级 / 显著影响。
  - `p3` 测试 / QA 环境 bug。

`--production` + `p3` 矛盾 → `AskUserQuestion` 让用户先校正再继续。

### 2. 收集 bug 详情

**`--source manual`（默认）：** `AskUserQuestion` 问：

> 描述 bug：期望行为 / 实际行为 / 复现步骤 / 错误信息 / 堆栈。

把回答写入 `<run_dir>/bug_raw.md`（用 `python3 .claude/scripts/tools/new_run_dir.py --intent Change` 起 run_dir；Debug 性质由 launch_spec Phase + 文件名 bug_raw.md 标识，不靠 intent 名）。

slug 推断：从 bug 标题或前 5 个词转 kebab-case（如 `user-login-500-error`）。

### 3. 查 incidents/ 历史相似 bug

```bash
grep -rli "<关键词1>\|<关键词2>" .claude/llm_wiki/incidents/*.md 2>/dev/null
```

关键词从 Step 2 收集到的组件名 / 错误信息中提取（如 `AnchorVideoService` / `tenant_id` / `OOM`）。

命中 → 内联输出：

```
[Past Incidents Matching This Area]
- <file>: <一行摘要 from incident 标题>
```

无命中也输出 `[Past Incidents] none in incidents/`。

命中作为输入喂给 step 4 的 @debugger。

### 4. 派 @debugger 做 root-cause 分析

按 `dispatch-template.md` 派 `@debugger`。Allowed Scope = `none`（read-only，禁改业务代码）。Inputs 含：

- `<run_dir>/bug_raw.md`
- Step 3 命中的 incident 文件路径（若有）
- 严重度 / production 标记

@debugger 走 `.claude/skills/systematic-debugging/SKILL.md` 协议（假设 → bisect → 验证），产出：

```
[Root Cause]: <一行：被违反的不变量 / 触发条件>
[Evidence]: <file:line 或 log 摘录>
[Blast Radius]: <还能影响什么>
[Fix Shape]: <高层方法，非代码>
```

校验返回：`python3 .claude/scripts/gates/subagent_return_gate.py --task-kind audit --return-file <tmp>`。

**root cause 未找到** → 输出 `[Status]: ESCALATE` + `[Reason]: root cause not identified after <N> dispatch rounds`，**不写 incident**（incident 要求 Root cause 字段真实，未找到时落 incident 反而污染知识库；让用户补证据后再跑）。max 2 次重派。

incident 建议块仅在 root cause confirmed ∧ severity ≥ p2 时输出（step 6）。root cause 未找到一律 ESCALATE。

### 5. 创建 launch_spec 行

root cause confirmed。Risk 推断：

| Severity | Risk | Mode |
|---|---|---|
| p3 | LOW | PATCH |
| p2 | MEDIUM | Standard MEDIUM |
| p1 | HIGH (强制) | Standard HIGH + Scenario A Emergency Hotfix |

定位最新 `.claude/runs/launch_spec_*.md`（无则按 `/h-brief` step 5 表头格式新建）。追加一行（**严格 5 列** `Intent | Status | Phase | Artifact/Log | Failed_Reason`，risk 写 openspec frontmatter 不入表）：

```
| fix-<slug> | PENDING | 4_Implement | <run_dir>/bug_raw.md | - |
```

severity → openspec frontmatter `risk` 字段（p3→LOW、p2→MEDIUM、p1→HIGH）。
p1 额外加注（**写在 launch_spec 文件末尾 `## Notes` 段**，禁追加到 Artifact 列破坏列分隔）：`fix-<slug>: Scenario A Emergency Hotfix — 无 Propose/Review，secrets_linter 必过`。
p2 同上：`fix-<slug>: Standard MEDIUM Slim Spec required before Implement`。

### 6. 生产 bug 建议起 incident（`--production` 或 p1/p2）

**p3 跳过 step 6。**

p1/p2 时输出建议块（不代调 `/h-incident`）：

```
[Bug → Incident Candidate]
severity=<P0|P1|P2>  source=<prod-alert|customer-report|qa-found|post-mortem>  slug=<slug>
Root cause: <step 4 [Root Cause]>
Blast radius: <step 4 [Blast Radius]>
Fix shape: <step 4 [Fix Shape]>

建议起 incident 记录（与本次 fix 并行进行，不阻塞后续 step）。
Run: /h-incident "<bug 一句话症状 + root cause + blast radius>"
```

step 7-9 不阻塞于 incident 建议。

### 7. 用户确认 fix scope

`AskUserQuestion` 展示：

```
Root Cause: <from step 4>
Fix Shape: <from step 4>
Blast Radius: <from step 4>
Estimated Risk: <LOW|MEDIUM|HIGH>
Files likely in scope: <from blast radius>

How do you want to proceed?
```

选项：

| 选项 | 含义 |
|---|---|
| Fix it now (inline PATCH) | LOW 默认；直接 Implement，无 openspec |
| Write a Slim Spec first | MEDIUM 推荐；`/h-brief fix-<slug> --risk medium --slim` |
| Full STANDARD task_brief | HIGH 必须；`/h-brief fix-<slug> --risk high` |
| Stop here — I'll fix it manually | 停，仅 root-cause 报告，不动 launch_spec status |

**用户选择前禁写 fix 代码。**

### 8. 转 Implement

按 step 7 用户选择：

**"Fix it now"**：
- Edit launch_spec 行 Status `PENDING` → `IN_PROGRESS`
- 起 run_dir（`new_run_dir.py --intent Change`）+ focus_card（Allowed Scope = blast radius 文件）；PATCH 性质由 launch_spec Phase=`4_Implement` 直跳 + 无 openspec 标识
- 进 Implement，**TDD 优先**：先写复现 root cause 的失败测试，再写 fix（若测试基建存在）

**"Slim Spec"** / **"Full STANDARD"**：
- Edit launch_spec 行 Phase → `2_Propose`
- 提示用户跑 `/h-brief fix-<slug> --risk <…>`，本命令到此结束

**"Stop here"**：
- launch_spec 行保留 PENDING
- 输出仅 root-cause 报告

### 9. 输出

```
[Fix-Bug] slug=fix-<slug> | severity=<p…> | production=<yes|no>
  Root Cause:   <step 4>
  Blast Radius: <step 4>
  Risk:         <LOW|MEDIUM|HIGH>
  Mode:         <PATCH inline | Slim Spec pending | STANDARD pending | stopped>
  Incident:     <path 或 "n/a (p3)">
  launch_spec:  fix-<slug> → <PENDING|IN_PROGRESS>
Next: <一句具体动作>
```

## 硬约束

- Root cause confirmed 前禁 fix code。
- p3 不输出 incident 建议块。
- p1 强制 HIGH。
- `--production` + `p3` 矛盾必 AskUserQuestion 校正。
- Step 1-7 禁源码改。
- 不代调 `/h-incident`，p1/p2 输出建议块。
- @debugger 派 ≤2 次，同根因 2 次失败 → ESCALATE。
