<!--
/**
 * @description 智能体模型同步说明。
 * 用于在 `agent/`（单一来源）与 `.trae/rules/`（Trae 发现层）之间保持内容一致。
 */
-->

# 智能体模型同步（前端智能体）

## 为什么需要同步

Trae 更偏向从 `.trae/rules/` 读取“规则/约束/入口文档”。  
而本项目仍希望保留 `agent/` 作为单一来源目录（便于在 Trae 之外复用）。

因此采用“规则文件复制 + 手动同步”的方式：

- 单一来源：`agent/`
- Trae 规则层：`.trae/rules/`
- skills/wiki：不复制，仅在 `.trae/skills/INDEX.md`、`.trae/wiki/README.md` 提供映射入口

## 同步方式

执行脚本：`.trae/rules/agent_model_sync.ps1`

- 从单一来源同步到 Trae 规则层：
  - `powershell -ExecutionPolicy Bypass -File .trae/rules/agent_model_sync.ps1 -Direction root_to_rules`
- 从 Trae 规则层同步回单一来源：
  - `powershell -ExecutionPolicy Bypass -File .trae/rules/agent_model_sync.ps1 -Direction rules_to_root`

## 同步的文件范围

- `AGENT.md`
- `INDEX.md`
- `MEMORY.md`
- `POLICIES.md`
- `TOOLS.md`
- `CHANGE_LOG.md`
- `DREAMS.md`

