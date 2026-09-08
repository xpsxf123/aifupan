---
date: 2026-05-29
feature: wiki_linter_fix
type: slim
---

# Slim WAL — wiki_linter pre-existing FAIL 修复（2026-05-29 独立 PATCH）

死链 + 孤岛修复 PATCH（LOW 风险，单文件 KG.md，+45/-1）。删除 `KNOWLEDGE_GRAPH.md` 第 15 行已过时的 `../skills/trae-skill-index/SKILL.md` 引用（trae-skill-index skill 已删除，按 skill-precedence.md "无 gateway skill" 哲学换为 schema + templates 引用）；新增 §7 "完整活跃知识索引"分 6 个子节列出 25 个原孤岛文件（specs / api / data / domain / reviews / templates 各目录）。wiki_linter 结果从 ❌ FAIL（1 死链 + 26 孤岛）→ ⚠️ WARN（仅剩 8 个 pre-existing 超长文件预警，不在本任务 scope，另开独立 PATCH）。方法学：孤岛文件应在 KG 顶层 sub-index 间接引用或在 KG §7 直接索引，避免"未被任何 .md 引用"。

[Confidence: HIGH]
[Evidence: .claude/wiki/KNOWLEDGE_GRAPH.md commit (待 commit)；wiki_linter.py 输出从 FAIL → WARN 经 Bash 命令验证]
