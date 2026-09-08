# CHANGE_LOG（变更日志）

## 记录原则

- 每次新增/修改规则、技能、索引结构都必须记录
- 涉及安全红线/禁止行为/权限边界的修改必须用户显式同意后才可生效

## 条目模板

- 变更摘要：
- 影响范围：
- 风险：
- 回滚：
- 审核点：

## 2026-04-25

- 变更摘要：新增 wiki_graph_builder / memory_compressor / legacy_wiki_bootstrapper 三个通用技能，用于 wiki 知识图谱写回、长期记忆压缩与老项目逆向 bootstrap。
- 影响范围：`skills/INDEX.md` 与 `skills/<skill_name>/SKILLS.md`（新增目录与条目注册）。
- 风险：若不加甄别地写回可能造成 INDEX/wifi/MEMORY 信息冗余；需避免将敏感信息写入记忆文件。
- 回滚：删除 `skills/wiki_graph_builder`、`skills/memory_compressor`、`skills/legacy_wiki_bootstrapper` 并移除 `skills/INDEX.md` 对应条目。
- 审核点：确认技能命名与目录结构符合约定；确认写回边界（涉及安全边界变更需显式确认）。

## 2026-05-18

- 变更摘要：新增“需求版本管理”体系入口，建立 `docs/需求版本库.md` 并登记 2.60.3（话术质检）相关文档入口；补充对应 wiki 与 INDEX 关系。
- 影响范围：`agent/AGENT.md`、`agent/INDEX.md`、`agent/MEMORY.md`、`agent/wiki/README.md`、`agent/wiki/项目-复盘客户端-需求版本管理.md`、`docs/README.md`、`docs/需求版本库.md`。
- 风险：如版本库未及时更新可能导致入口过期；需在新增/变更需求时同步维护版本库条目。
- 回滚：删除版本库与 wiki 页面，并回退 AGENT/INDEX/MEMORY 对应条目。
- 审核点：确认版本库入口与主PRD一致；确认冲突点以主PRD为准并可追溯。

## 2026-05-18（补充）

- 变更摘要：将需求版本库升级为“目录版”，入口调整为 `docs/需求版本库/README.md`，并为 2.60.3 建立版本目录（含 meta 与 figma 节点数据）。
- 影响范围：`docs/需求版本库/README.md`、`docs/需求版本库/2.60.3/*`、`agent/AGENT.md`、`agent/MEMORY.md`、`agent/wiki/项目-复盘客户端-需求版本管理.md`、`.trae/rules/frontend_agent__AGENT.md`、`.trae/rules/frontend_agent__MEMORY.md`。
- 风险：旧入口文档/历史链接可能仍指向 `docs/需求版本库.md`；需要保持旧入口兼容跳转。
- 回滚：删除 `docs/需求版本库/` 并把入口引用切回 `docs/需求版本库.md`。
- 审核点：确认目录版入口可直达目标版本目录；确认 meta.json 中登记的路径与实际文件一致。

## 2026-05-29

- 变更摘要：从 java-harness-agent 的通用设计中抽取“知识图谱入口/上下文漏斗/WAL 写回/生命周期门禁/治理类 skills”，补全 agent 的治理能力。
- 影响范围：`wiki/KNOWLEDGE_GRAPH.md`、`wiki/*/index.md`、`wiki/*/wal/README.md`、`wiki/通用-*.md`、`skills/*/SKILLS.md`、`skills/INDEX.md`、`templates/*`、`scripts/wiki/*`。
- 风险：新增域目录与 WAL 规则需要团队习惯迁移；若不做合并窗口管理可能导致 wal/ 长期堆积。
- 回滚：删除本次新增的 wiki/skills/templates/scripts 文件，并回退 `skills/INDEX.md`、`AGENT.md`、`INDEX.md` 的增量内容。
- 审核点：确认未引入 Java 后端专属规则；确认 WAL 不包含敏感信息；确认新增技能已登记在 skills/INDEX.md。

## 2026-06-03

- 变更摘要：强化“注释规范”底层规则，新增 commenting_guard 技能用于生成/修改代码时补齐文件头说明 + JSDoc + 复杂逻辑单行注释。
- 影响范围：`.trae/rules/project_rules.md`、`.trae/skills/commenting_guard/SKILL.md`。
- 风险：过度注释可能造成文件膨胀与合并冲突；需按“结构化注释 + 复杂逻辑单行注释”执行，避免机械逐行注释。
- 回滚：删除 `.trae/skills/commenting_guard/`；回退 `.trae/rules/project_rules.md` 的注释条款增量。
- 审核点：确认 *.vue 也覆盖注释规范；确认技能触发条件明确且可复用；确认未引入敏感信息到注释中。

## 2026-07-20

- 变更摘要：新增多 Agent 统一入口 `agnet/BOOTSTRAP.md`，并为 Claude Code 建立 `CLAUDE.md + .claude/rules/ + SessionStart hook` 兼容桥接层，解决只读取映射入口、不继续进入单一来源的问题。
- 影响范围：`agnet/BOOTSTRAP.md`、`agnet/POLICIES.md`、`CLAUDE.md`、`.claude/settings.json`、`.claude/hooks/emit-bootstrap.js`、`.claude/rules/*`、`docs/提示词设计/*`。
- 风险：若桥接层后续独立演化，可能与 `agnet/` 单一来源脱节；若 hooks 输出过长，会引入上下文膨胀与噪声。
- 回滚：删除 `CLAUDE.md`、`.claude/rules/`、`.claude/hooks/emit-bootstrap.js` 对应配置与新增文档；删除 `agnet/BOOTSTRAP.md`，并回退 `agnet/POLICIES.md` 的增量条目。
- 审核点：确认所有 Agent 仍以 `agnet/` 为单一来源；确认 `.claude/` 只承载桥接与自动加载逻辑；确认 hooks 仅注入基础治理摘要而不复制整套业务知识。

## 2026-07-20（补充）

- 变更摘要：根据 Claude Code“仓库根 `CLAUDE.md` 与 `.claude/CLAUDE.md` 合并加载”的机制，升级 Claude 兼容层为“双入口桥接 + 关键规则内联 + PostToolUse 文档维护检查”组合方案。
- 影响范围：`CLAUDE.md`、`.claude/CLAUDE.md`、`.claude/settings.json`、`.claude/hooks/check-doc-maintenance.js`、`agnet/BOOTSTRAP.md`、`agnet/POLICIES.md`、`docs/提示词设计-多种agent工具提示词工程兼容/*`。
- 风险：若入口桥内联规则过多，可能导致桥接层膨胀；若 PostToolUse 规则过宽，可能带来维护提醒噪声。
- 回滚：删除 `.claude/CLAUDE.md` 与 `check-doc-maintenance.js`，回退 `CLAUDE.md`、`.claude/settings.json`、`agnet/BOOTSTRAP.md`、`agnet/POLICIES.md` 与相关兼容文档到补充前版本。
- 审核点：确认根入口只保留简短摘要与关键规则；确认 `.claude/CLAUDE.md` 负责详细配置；确认 PostToolUse 只做维护检查提醒，不复制业务知识或阻断正常编码流程。

