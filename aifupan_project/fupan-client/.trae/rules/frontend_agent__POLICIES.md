# POLICIES（策略库）

本文件记录可复用的行为策略，并基于反馈进行迭代。

## 策略条目模板

- 策略：一句话描述
- 适用场景：何时使用
- 风险：可能的副作用
- 证据：来自哪些交互/产出

## 多 Agent 入口桥接与自动加载

- 策略：提示词工程只维护一套单一来源目录；对支持多入口的 Agent，采用“根入口简短摘要 + 工具目录详细配置 + rules + SessionStart/PostToolUse hooks”的组合回到单一来源，不允许各自维护独立知识副本。
- 适用场景：同一仓库需要同时兼容 Claude Code、Trae、Codex 或其他 Agent 时。
- 风险：若桥接层写得过厚，会造成桥接层与单一来源双向漂移；若只有 SessionStart 没有 PostToolUse，会在改完治理文件后再次退化为“规则读到了但文档没维护”。
- 证据：Claude Code 会合并读取根目录 `CLAUDE.md` 与 `.claude/CLAUDE.md`，但若入口只有路径映射而没有关键规则内联，仍可能停留在索引层；引入统一 `BOOTSTRAP.md`、`.claude/rules/`、SessionStart 注入与 PostToolUse 文档维护检查后，可将基础治理规则自动带入会话，并在文件修改后补强维护提醒。

