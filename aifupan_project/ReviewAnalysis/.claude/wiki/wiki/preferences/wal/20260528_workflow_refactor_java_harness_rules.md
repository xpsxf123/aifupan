---
date: 2026-05-28
feature: workflow_refactor_java_harness
type: rules
run_id: 20260528_122000_workflow_refactor_java_harness
related_specs:
  - .claude/runs/20260528_122000_workflow_refactor_java_harness/openspec.md
related_wal:
  - [[20260528_workflow_refactor_java_harness_architecture]]
---

# WAL — 工作流重构新规则（借鉴 java-harness-agent）

## Context

本次工作流重构同步引入了一组 **新工程规则**，所有 Agent 在未来任务中必须遵守。这些规则源自 java-harness-agent 设计，已在本项目落地为 `.claude/rules/` 7 个 SSOT 文件。

## Rules（新）

### R1. [Triage] [SHOULD] 用户提示后自动跑 Triage Probe

每次 UserPromptSubmit hook 自动调用 `.claude/scripts/triage_probe.py` 输出 5 信号客观打分。Agent 应优先采用其 `suggested_profile`，除非用户显式覆盖。

[Confidence: HIGH]
[Evidence: .claude/settings.json:hooks.UserPromptSubmit + .claude/scripts/triage_probe.py]

### R2. [Risk] [MUST] 使用 4 级风险分类（TRIVIAL/LOW/MEDIUM/HIGH）

- TRIVIAL (<3 行 + 无 danger 关键词) → 无 spec, inline 解释即可
- LOW → Slim Spec（5 字段）
- MEDIUM → 完整 openspec
- HIGH → openspec + ADR + 强制 Approval Gate

详 [.claude/rules/risk-profiles.md](../../../../rules/risk-profiles.md)。

[Confidence: HIGH]
[Evidence: .claude/rules/risk-profiles.md]

### R3. [Skill] [MUST] 按 Zone 触发，同 Zone 互斥

26 个 skill 按 Zone A/B/C/D/E/N/U/M 分组。Zone B/C 同时只能挂 1 个主 skill。详 [.claude/rules/skill-precedence.md](../../../../rules/skill-precedence.md)。

[Confidence: HIGH]
[Evidence: .claude/rules/skill-precedence.md + .claude/skills/trae-skill-index/SKILL.md]

### R4. [Dispatch] [MUST] Sub-agent 调用必含 5-section 契约

Inputs / Source Documents / Memory Snapshot / Hard Limits / Expected Output。详 [.claude/rules/dispatch-template.md](../../../../rules/dispatch-template.md)。

[Confidence: HIGH]
[Evidence: .claude/rules/dispatch-template.md]

### R5. [WAL] [MUST] 每条事实必含 Confidence + Evidence 标签

```markdown
[Confidence: HIGH|MEDIUM|LOW]
[Evidence: <file>:<line> | <commit-hash>]
```

详 [.claude/rules/wal-policy.md](../../../../rules/wal-policy.md)。

[Confidence: HIGH]
[Evidence: .claude/rules/wal-policy.md + 本 WAL 文件示范]

### R6. [WAL] [MAY] PATCH 任务可跳过 WAL，STANDARD 必写

- PATCH (TRIVIAL): 否
- PATCH (LOW): 可选（用户选 1 维度）
- STANDARD (MEDIUM): 强制（多选维度）
- STANDARD (HIGH): 强制（必含 Architecture + Rules + ADR）

[Confidence: HIGH]
[Evidence: .claude/rules/wal-policy.md §1]

### R7. [Wiki] [MUST] 文档 ≤ 3000 行（报告类 10000）

超限触发 `anti_bloat_check.py` FAIL → @knowledge-architect 拆分。

[Confidence: HIGH]
[Evidence: .claude/rules/policy.md §6 + .claude/scripts/wiki/anti_bloat_check.py]

### R8. [Csproj] [MUST] 新 .cs 文件必须同步注册到 ReviewAnalysis.csproj

旧式 csproj（.NET Framework 4.7.2）不自动包含。Mac 落地后 Windows 编译会报"找不到类型"。

[Confidence: HIGH]
[Evidence: 用户 memory 反复教训 + .claude/agents/lead-engineer.md]

## Anti-Patterns（新增反模式）

### AP1. [NEVER] 跳过 Triage Probe 输出

UserPromptSubmit hook 已自动注入 `[Triage Probe]` 块，agent 不能视而不见——必须在 Cognitive_Brake 中确认 suggested_profile。

### AP2. [NEVER] Sub-agent dispatch 用短促命令式 prompt

❌ `Agent(prompt="找一下 X")` ✅ `Agent(prompt="按 dispatch-template.md 5 section ...")`

### AP3. [NEVER] WAL 写在非 wal/ 子目录

`wal/` 子目录是约定，写在 `wiki/<domain>/` 根目录会成为孤儿。

### AP4. [NEVER] 跳过 Confidence/Evidence 标签

知识无来源 = 不可追溯 = 无效知识。

### AP5. [NEVER] 修改 16 个历史 WAL 内容

仅可加新索引到 KNOWLEDGE_GRAPH.md。

## Follow-ups

- 培训 / 推广：让协作者熟悉 .claude/rules/ 新结构
- Windows 端 git config core.symlinks=true 验证（部分协作者可能需手动 checkout）
