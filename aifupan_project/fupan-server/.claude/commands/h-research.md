---
description: 起调研 / 可行性报告（Scenario F），输出 wiki/research/YYYYMMDD__<slug>.md。零代码、零契约、零 launch_spec。
argument-hint: <调研问题陈述> [--force]
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Agent
---

输入：`$ARGUMENTS`

产出**一份** `.claude/llm_wiki/wiki/research/YYYYMMDD__<slug>.md` 调研报告。**禁止** 产 openspec / focus_card / launch_spec / ADR / Allowed Scope。

## 何时该用本命令

| 用户意图动词 | 走哪 |
|---|---|
| 分析 / 调研 / 评估 / 探索 / 可行性 / 现状 / 能给到什么程度 / feasibility / research | **本命令** |
| 实现 / 修复 / 优化 / 新增 / 重构 / 上线 / 改造 | `/h-brief` |
| 起 PRD / EPIC 拆 N 任务 | `/h-decompose` |
| 复盘 bug / 找根因 | `/h-fix-bug` |

灰区（实现方案前的方案对比类调研）→ 仍用 `/h-brief`（属于 Change 的 Propose 阶段），不用本命令。本命令专治**输出是结论性报告、不一定动手**的场景。

## 步骤

### 1. DoR + 意图自检

`$ARGUMENTS` 必须含：
- 调研主题（一句话能复述的问题）
- 关注边界（看 X 模块 / X 功能 / X 数据流）

缺失 → 反问用户一句话补，**不要派 @requirement-engineer**（research 阶段 grep 即可，不需要 AC 工程师）。

### 2. slug + 落盘路径

```bash
slug=<kebab-case 5-8 词，反映调研主题>
date=$(date +%Y%m%d)
report_path=.claude/llm_wiki/wiki/research/${date}__${slug}.md
```

`mkdir -p .claude/llm_wiki/wiki/research`（首次使用时）。

**冲突检查：** `[ -e "$report_path" ]` → 停 `Existing research report at <path>. Rename slug or rerun with --force to overwrite.`。`--force` 用户必须显式传，禁静默覆盖。

### 3. 输出路由说明

```
[Route] Mode=Research (Scenario F) | Phases={Investigate(inline|dispatch), Report} | Reason: <一句>
```

### 4. 选择执行路径

按规模二选一：

| 触发 | 路径 |
|---|---|
| 已知主题边界清晰 ∧ ≤3 wiki 文件 ∧ ≤5 grep 关键词 | **Inline** — 主 agent 直接 Read / Grep / Write |
| 跨 ≥3 wiki anchor ∨ 需扫 >10 代码文件 ∨ 涉及未知模块拓扑 | **Dispatch** — 派 `@requirement-engineer`（research mode）|

**Dispatch 时的提示词关键差异**（vs `/h-brief` Example 4）：
- Allowed Scope = `<report_path>`（唯一可写）
- AC 内容："产报告，禁起 AC / 禁起 Allowed Scope / 禁起 Hidden Scope 推荐。只汇报 grep 出的事实 + 工作量估算 + 推荐路径 + 待澄清"
- Hard Constraints 沿用 dispatch-template.md，加一行 "本任务不进入 Implement，不要 derive 可实现性之外的设计选项"

### 5. 报告骨架（不强制 schema，建议章节）

```markdown
# Research — <主题>

**Date:** YYYY-MM-DD
**Question source:** <用户 / ticket / 内部讨论>
**Status:** <调研完成 / 待补 / 已转 Change>

## 1. 问题陈述（用户原文 verbatim）
## 2. 现状基线（wiki + 代码已有什么）
## 3. Gap 分析（每个数据点：现状 / 缺什么 / 工作量 S/M/L）
## 4. 推荐路径（do / partial / drop + 优先级排序）
## 5. 待澄清（≤5 个高杠杆问题给用户决策）

## Source Material
- Wiki: <清单>
- 代码 grep 验证关键路径：<清单>
```

**章节不全没关系**：调研问题千差万别，不强制 schema_checker。但 §1 / §2 / §3 / §5 是建议下限。

### 6. 不创建 launch_spec / 不进 archive

Research 是 fire-and-forget：报告写完就结束。
- 不写 `.claude/runs/`
- 不写 `launch_spec_*.md`
- 不进 `.claude/llm_wiki/archive/`
- 报告永久驻留在 `.claude/llm_wiki/wiki/research/`（归 wiki，git 跟踪）

如果用户要转 Change → 用 `/h-brief <slug>`，引用本报告路径作为 Spec Inference 种子。

### 7. 输出

```
[Research Done] report=<path> | path=<inline|dispatch> | gaps=<S/M/L 个数> | recommend=<P0/P1/P2 路径>
Next: 用户决定 — (a) /h-brief 转 Change | (b) 给相关团队评审 | (c) 存档观望
```

## 硬约束

- **不产**：openspec / focus_card / launch_spec / ADR / explore_report / Allowed Scope
- **不派**：`@system-architect`（research 不是 design）
- **不强制 schema_checker**：报告章节按需，质量靠主 agent + （可选）`@requirement-engineer` 自律
- **不进 Implement / QA / Archive**：本命令终点是报告落盘，无下游 phase
- **盘符固定**：`.claude/llm_wiki/wiki/research/`（不写 `.claude/runs/`，不写 `archive/`）
- **报告 ≤ 500 行**：超长拆专题文档（按 [policy.md Anti-bloat](../rules/policy.md#part-3--write-back-archive--wiki) 规则）
- **token 预算**：inline 路径 ≤30k；dispatch 路径 ≤80k（含子代理 50k）；超出停手反问
