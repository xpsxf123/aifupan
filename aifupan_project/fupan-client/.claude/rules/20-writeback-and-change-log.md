# Writeback And Change Log

本规则文件用于约束 Claude Code 的知识写回与治理记录行为。

## 写回规则

- 新知识优先写入对应域的 `wal/`，再按窗口合并到域 `index.md`。
- `WAL` 只记录可复用知识、稳定决策、约束与模板，不记录普通聊天过程。
- 高频稳定结论才进入 `agnet/MEMORY.md`。
- 策略类经验优先进入 `agnet/POLICIES.md`。

## 变更记录

以下变更必须记录到 `agnet/CHANGE_LOG.md`：

- 新增或修改规则
- 新增或修改技能
- 新增或修改索引结构
- 调整多 Agent 兼容层与桥接机制

## 禁止项

- 不得把普通业务代码变更机械写入 `WAL`。
- 不得把敏感信息写入 `WAL`、`MEMORY`、`POLICIES`、`CHANGE_LOG`。
