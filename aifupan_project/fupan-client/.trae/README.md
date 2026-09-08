<!--
/**
 * @description Trae 智能体接入说明（入口文件）。
 * 说明：本项目的智能体模型主目录在 `agent/`；本目录仅提供 Trae 编辑器所需的兼容入口与索引。
 */
-->

# Trae 智能体接入（fupan-client）

## 约定

- 智能体模型主目录：`agent/`
- Trae 规则目录：`.trae/rules/`
- Trae 技能索引：`.trae/skills/INDEX.md`（映射到 `agent/skills/INDEX.md`）
- Trae 项目技能索引：`.trae/self_skills/INDEX.md`（映射到 `agent/self_skills/INDEX.md`）
- Trae 知识库索引：`.trae/wiki/README.md`（映射到 `agent/wiki/README.md`）

## 编辑建议（保证同步）

- 在 Trae 编辑器内修改规则：请优先改 `.trae/rules/` 下对应文件；需要同步回 `agent/` 时，按 `.trae/rules/agent_model_sync.md` 执行同步。
- 在普通编辑器中修改模型：请优先改 `agent/` 下对应文件；需要同步到 `.trae/rules/` 时，同样按同步说明执行。

