# Core Governance

本规则文件始终生效，用于避免 Claude Code 只读取入口桥而不继续进入单一来源。

## MUST

- MUST 将 `agnet/` 视为提示词工程单一来源。
- MUST 在读取 `CLAUDE.md` 后继续读取 `agnet/BOOTSTRAP.md`。
- MUST 在执行实现类任务前继续读取：
  - `agnet/AGENT.md`
  - `agnet/TOOLS.md`
  - `agnet/MEMORY.md`
  - `agnet/INDEX.md`
- MUST 在规则/技能/索引结构发生变更时更新 `agnet/CHANGE_LOG.md`。
- MUST 在 `agnet/` 与桥接层发生冲突时，以 `agnet/` 为准。

## MUST NOT

- MUST NOT 把入口桥当作完整规则包。
- MUST NOT 在 `.claude/` 中维护独立于 `agnet/` 的长期业务规则副本。
