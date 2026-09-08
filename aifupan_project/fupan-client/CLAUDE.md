# Claude Code 根入口

本文件是 Claude Code 在仓库根目录读取的简短入口，不是单一来源。

## 单一来源

- 提示词工程单一来源目录：`agnet/`
- 若根目录 `CLAUDE.md`、`.claude/CLAUDE.md`、`.claude/rules/`、`.trae/` 与 `agnet/` 内容冲突，以 `agnet/` 为准

## Claude Code 必须遵循

- Claude Code 会同时读取根目录 `CLAUDE.md` 与 `.claude/CLAUDE.md`
- 读取本文件后，必须继续读取 `.claude/CLAUDE.md`
- 读取 `.claude/CLAUDE.md` 后，必须继续读取 `agnet/BOOTSTRAP.md`
- 不得把入口桥文件当作完整规则包

## 关键规则内联

- 实现、修复、重构、治理任务开始前，至少继续读取：
  - `agnet/AGENT.md`
  - `agnet/TOOLS.md`
  - `agnet/MEMORY.md`
  - `agnet/INDEX.md`
- 新增或修改规则、技能、索引结构、多 Agent 兼容层、桥接入口、hooks 时，必须更新 `agnet/CHANGE_LOG.md`
- 可复用知识优先写入对应域的 `wal/`，再按窗口合并到域 `index.md`
- `WAL` 只记录可复用知识、稳定决策、模板和约束，不记录普通业务发布细节
- `self_skills/INDEX.md -> skills/INDEX.md -> 目标 SKILLS.md` 是唯一允许的技能优先级
- `KNOWLEDGE_GRAPH -> 域 index -> 具体 wiki 页面` 是默认知识读取漏斗

## 详细配置入口

@.claude/CLAUDE.md
