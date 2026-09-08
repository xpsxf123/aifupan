---
date: 2026-05-28
feature: workflow_refactor_java_harness
type: architecture
run_id: 20260528_122000_workflow_refactor_java_harness
related_specs:
  - .claude/runs/20260528_122000_workflow_refactor_java_harness/openspec.md
related_adr:
  - .claude/wiki/wiki/architecture/adr/ADR-001-workflow-refactor-java-harness.md
related_wal:
  - [[20260528_workflow_refactor_java_harness_rules]]
---

# WAL — 工作流重构（借鉴 java-harness-agent）

## Context

2026-05-28，借鉴 `/Users/hehui/dev/git/public/java-harness-agent` 仓库的设计理念，对当前项目（C#/.NET WinForms 主播分析工具）的 Agent 工作流做了 HIGH 风险元结构重构。完整 STANDARD 流程：Explorer → Propose → Review → Approval Gate → Implement（6 Wave）→ QA → Archive。用户显式批准了 "完全照搬 Java 结构" 方案。

## Facts

1. **顶层入口统一**：`CLAUDE.md` 合并自原 `AGENTS.md`，成为唯一权威入口（12 章节，395 行）。`AGENTS.md` 退化为 15 行引用文件。

   [Confidence: HIGH]
   [Evidence: CLAUDE.md:1-395 + AGENTS.md:1-15]

2. **`.claude/{rules,agents,commands}/` 三大 SSOT 目录创建**：
   - `rules/`: 7 个 SSOT 文件（lifecycle / policy / risk-profiles / skill-precedence / dispatch-template / tasklist-policy / wal-policy）+ 5 个 C# 规则 kebab-case mirror（项目原有）+ 1 个 README
   - `agents/`: 10 个角色文件（含新增 skill-graph-curator）
   - `commands/`: 5 个 phase 触发命令（h-explore / h-propose / h-implement / h-qa / h-archive）

   [Confidence: HIGH]
   [Evidence: .claude/rules/*.md, .claude/agents/*.md, .claude/commands/*.md]

3. **双入口符号链接**：`.claude/wiki/` → `.claude/wiki/`，`.claude/skills/` → `.claude/skills/`。物理文件唯一存放于 `.claude/`，新入口仅为统一 java-harness 风格 `.claude/` 目录结构。

   [Confidence: HIGH]
   [Evidence: ls -la .claude/wiki .claude/skills]

4. **资产保护通过**：16 WAL 碎片 + 6 个 C# 规则文件 + 10 个归档 openspec 内容**字节级未变**（shasum 验证 PASS）。

   [Confidence: HIGH]
   [Evidence: shasum diff /tmp/wal_before.sha /tmp/wal_final.sha = empty]

5. **Skill 体系按 Zone 重组**：26 个 skill（含 3 个新建：csharp-build-resolver / failure-memory-helper / skill-router）全部含 `zone:` frontmatter 字段（A/B/C/D/E/N/U/M 8 分组）。`intent-gateway` 退化为 stub 指向 `skill-router`。`trae-skill-index/SKILL.md` 重写为 Zone 矩阵 + 互斥规则。

   [Confidence: HIGH]
   [Evidence: .claude/skills/*/SKILL.md frontmatter; .claude/skills/trae-skill-index/SKILL.md]

6. **5 项 java-harness 高收益设计植入**：
   - **Triage Probe**：`.claude/scripts/triage_probe.py` 5 信号客观打分（blast_radius / failure_history / ambiguity / danger_keywords / intent_class）
   - **4 级 Risk**（TRIVIAL/LOW/MEDIUM/HIGH）替代原 3 级
   - **Slim Spec**（LOW 风险 5 字段模板）+ **ADR**（HIGH 风险强制）双 schema
   - **Wiki Anti-Bloat**（3000 行硬约束 + 报告类 10000 行例外）`.claude/scripts/wiki/anti_bloat_check.py`
   - **WAL Confidence + Evidence 标签**（每条事实强制）

   [Confidence: HIGH]
   [Evidence: .claude/scripts/triage_probe.py, .claude/scripts/wiki/anti_bloat_check.py, .claude/wiki/schema/{slim_spec,adr}_schema.md]

7. **中文规则数量净增 48 条**：重构前 baseline 74 条 [MUST]/[NEVER]/[SHOULD]，重构后 122 条。所有中文规则保留，未被英化覆盖。

   [Confidence: HIGH]
   [Evidence: grep '\[MUST\]|\[NEVER\]|\[SHOULD\]' CLAUDE.md AGENTS.md .claude/wiki/wiki/preferences/*.md .claude/rules/*.md]

8. **Hooks 升级**：`.claude/settings.json` 在 PostToolUse 加 anti_bloat hook（wiki 修改时自动跑）+ UserPromptSubmit 加 triage_probe hook（每次用户提示自动评分输出 `[Triage Probe]` 块）。原有 secrets_linter / dotnet format / commit gate hooks 全保留。

   [Confidence: HIGH]
   [Evidence: .claude/settings.json:hooks]

## Anti-Patterns

- **❌ 禁止物理拷贝 `.claude/wiki/` 内容到 `.claude/wiki/`**：双入口必须用符号链接，避免双源漂移。
- **❌ 禁止删除 `.claude/runs/{LIFECYCLE,ROLE_MATRIX,HOOKS}.md`**：保留作为历史与脚本入口。权威以 `.claude/rules/` 为准，旧文件可继续被 `.claude/scripts/` 引用。
- **❌ 禁止在 `.claude/rules/` 重新定义已有 C# 规则**：B 组 5 个文件是 `.claude/wiki/wiki/preferences/*.md` 的 kebab-case mirror，修改 C# 规则只改原文件，mirror 通过符号链接自动反映。
- **❌ 禁止重写历史 16 WAL 碎片**：仅可加新索引到 KNOWLEDGE_GRAPH.md。

## Follow-ups

- **Windows 端兼容性验证**（待）：本仓库在 Mac 重构，需在 Windows 端 `git config core.symlinks=true` + 重新 checkout 验证 `.claude/wiki/` 与 `.claude/skills/` 符号链接生效
- **scope_guard.py 增强**：支持 focus_card 的具体文件清单格式（当前仅支持 prefix）
- **`triage_probe.py` 优化**：识别更多中文 intent 关键字（如 "改成"、"调整"）
- **`failure_memory.py` 落地**：当前 failure-memory-helper skill 已建，但配套脚本 `.claude/scripts/failure_memory.py` 未实现，后续 PATCH 任务补
- **`subagent_return_gate.py`**：dispatch-template.md 提到的返回值校验脚本暂未实现，是 follow-up
