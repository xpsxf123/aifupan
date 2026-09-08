# Claude Code 项目详细配置

本文件是 Claude Code 在 `.claude/` 目录下读取的项目详细配置，与仓库根目录 `CLAUDE.md` 合并生效。

## 读取顺序与优先级

- Claude Code 会读取：
  - 根目录 `CLAUDE.md`
  - `.claude/CLAUDE.md`
  - `.claude/rules/`
- 以上内容是合并加载，不是互相覆盖
- `.claude/CLAUDE.md` 位于更后位置，适合放详细规则与执行约束
- 但任何桥接层都不是单一来源，单一来源始终是 `agnet/`

## 单一来源约束

- `agnet/` 是唯一长期维护的提示词工程主目录
- `.claude/` 只负责：
  - 入口桥接
  - 详细配置
  - 自动加载
  - hooks 注入与检查
- `.claude/` 不得维护独立于 `agnet/` 的业务知识副本

## 强制继续读取

- 读取本文件后，必须继续读取 `agnet/BOOTSTRAP.md`
- 然后按任务场景继续读取：
  - 实现类任务：`agnet/AGENT.md`、`agnet/TOOLS.md`、`agnet/MEMORY.md`、`agnet/INDEX.md`
  - 治理类任务：`agnet/POLICIES.md`、`agnet/CHANGE_LOG.md`、`agnet/wiki/README.md`
  - 技能类任务：`agnet/self_skills/INDEX.md` -> `agnet/skills/INDEX.md` -> 目标 `SKILLS.md`
  - 知识类任务：`agnet/wiki/KNOWLEDGE_GRAPH.md` -> 域 `index.md` -> 具体 wiki 页面

## 文档维护规则内联

- 只要修改以下内容，就必须检查是否同步维护文档与治理记录：
  - `agnet/`
  - `.claude/`
  - `CLAUDE.md`
  - `.trae/`
  - `docs/提示词设计-多种agent工具提示词工程兼容/`
- 若修改的是规则、技能、索引结构、多 Agent 兼容层、桥接机制、hooks，则必须更新 `agnet/CHANGE_LOG.md`
- 若形成稳定可复用策略，应更新 `agnet/POLICIES.md`
- 若形成可复用知识，应写入对应域 `wal/`，而不是只留在对话中

## 自动加载机制

- `.claude/rules/` 负责始终生效的高优先级规则
- `SessionStart hook` 负责在启动、恢复、清理、压缩时重注入基础治理摘要
- `PostToolUse hook` 负责在文件修改后检查是否需要补维护 `CHANGE_LOG`、`POLICIES` 或兼容文档

## 目标

本文件的目的不是替代 `agnet/`，而是确保 Claude Code 不会停在“看见路径映射”这一层，而会沿着统一的入口链真正进入单一来源。

@agnet/BOOTSTRAP.md
