# Multi Agent Compatibility

本规则文件用于约束 Claude Code 在多 Agent 共仓治理中的行为。

## 核心原则

- 一个项目只允许存在一套长期维护的提示词工程单一来源。
- 不同 Agent 可以有不同入口桥，但不能各自维护独立知识库。
- Claude Code 侧的桥接资产只负责自动加载与继续导航，不负责承载完整业务知识。

## Claude Code 责任

- 自动加载 `CLAUDE.md` 与 `.claude/rules/`。
- 通过这些桥接资产继续回到 `agnet/`。
- 在需要继续深入时，从 `agnet/wiki/` 与 `agnet/skills/` 中读取，而不是在 `.claude/` 内重复造一份。

## 兼容边界

- `.claude/`：Claude Code 发现层
- `.trae/`：Trae 发现层
- `agnet/`：统一单一来源

任何桥接层只能做：

- 映射
- 引导
- 自动加载
- 重注入基础规则

不能做：

- 长期持有独立业务事实
- 独立维护一套与 `agnet/` 脱节的记忆或 wiki
