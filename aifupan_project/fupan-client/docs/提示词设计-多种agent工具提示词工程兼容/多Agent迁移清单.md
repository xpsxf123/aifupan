# 多 Agent 迁移清单

## 1. 适用场景

本清单用于把“单一来源 + 入口桥 + 规则目录 + hooks 注入”的多 Agent 兼容机制迁移到其他项目。

适合以下场景：

- 一个项目需要同时使用 Claude Code、Trae、Codex 或其他 Agent
- 希望所有 Agent 共享同一套 wiki / memory / skills / policies
- 不希望不同 Agent 各自维护一份提示词工程

## 2. 最小复制清单

迁移到新项目时，至少需要复制以下目录与文件：

### 2.1 单一来源层

- `agnet/AGENT.md`
- `agnet/TOOLS.md`
- `agnet/INDEX.md`
- `agnet/MEMORY.md`
- `agnet/POLICIES.md`
- `agnet/CHANGE_LOG.md`
- `agnet/BOOTSTRAP.md`
- `agnet/wiki/`
- `agnet/skills/`
- `agnet/self_skills/`

### 2.2 Claude Code 兼容层

- `CLAUDE.md`
- `.claude/CLAUDE.md`
- `.claude/settings.json`
- `.claude/rules/00-core-governance.md`
- `.claude/rules/10-context-funnel.md`
- `.claude/rules/20-writeback-and-change-log.md`
- `.claude/rules/30-multi-agent-compatibility.md`
- `.claude/hooks/emit-bootstrap.js`
- `.claude/hooks/check-doc-maintenance.js`

### 2.3 Trae 兼容层

- `.trae/rules/`
- `.trae/AGENT.md`
- `.trae/INDEX.md`
- `.trae/MEMORY.md`
- `.trae/POLICIES.md`
- `.trae/TOOLS.md`

## 3. 迁移步骤

### 第一步：确定单一来源目录名

推荐三选一：

- `.agent/`
- `agent/`
- `agnet/`

要求：

- 项目中只保留一个单一来源主目录
- 其他工具目录只能桥接到它

### 第二步：建立统一入口文件

在单一来源目录中新增：

- `BOOTSTRAP.md`

要求：

- 明确单一来源是谁
- 明确不同任务场景继续读取哪些文件
- 明确写回和 CHANGE_LOG 边界
- 明确桥接层不允许承载独立业务知识

### 第三步：为各 Agent 提供桥接入口

示例：

- Claude Code：`CLAUDE.md`
- Claude Code 详细配置：`.claude/CLAUDE.md`
- Trae：`.trae/rules/*`
- Codex：如果有固定入口文件，也创建一个极薄桥接文件

桥接文件必须满足：

- 指向统一 `BOOTSTRAP.md`
- 说明自己不是单一来源
- 明确要求继续读取
- 若工具支持多入口，采用“根入口简短摘要 + 工具目录详细配置”的组合

### 第四步：补规则目录

如果目标 Agent 支持规则目录，至少拆四类规则：

- 核心治理
- 上下文漏斗
- 写回与 CHANGE_LOG
- 多 Agent 兼容边界

### 第五步：补 SessionStart hook

如果目标 Agent 支持 hooks，至少增加一个启动阶段 hook：

- 启动时注入基础治理摘要
- 恢复会话时再次注入
- 压缩上下文时再次注入

### 第六步：补 PostToolUse 文档维护检查

如果目标 Agent 支持 hooks，再增加一个文件修改后的检查 hook：

- 在 `Write/Edit/MultiEdit` 成功后触发
- 检测是否修改了规则、桥接层、hooks、兼容文档
- 若命中治理文件，则提醒维护 `CHANGE_LOG`、`POLICIES` 与兼容文档

### 第七步：校验桥接是否真的回到单一来源

需要人工验证：

- Agent 是否只看到了入口文件
- 还是已经沿引导链继续进入 `BOOTSTRAP`、`INDEX`、`wiki`、`skills`

## 4. 迁移后的验收问题

迁移到新项目后，可以直接让 Agent 自检以下问题：

1. 单一来源目录在哪里？
2. 当前 Agent 的根入口文件是什么？
3. 当前 Agent 是否还有工具目录级详细入口文件？
4. 读取入口桥后，必须继续读取哪个统一入口文件？
5. 实现类任务必须继续读取哪几份核心文件？
6. skills 的优先级顺序是什么？
7. wiki 的读取顺序是什么？
8. 变更规则/技能/索引后应该更新哪里？
9. 修改治理文件后由哪个 hook 做文档维护检查？

如果 Agent 无法稳定回答这些问题，就说明桥接层还不够强。

## 5. 推荐对外说明模板

当你把这套能力交给其他项目或其他 Agent 使用时，可以直接使用下面这段说明：

> 本项目采用“单一来源 + 多 Agent 桥接”的提示词工程管理方式。  
> 真实知识、记忆、技能、规则只维护在单一来源目录中。  
> Claude Code、Trae、Codex 等工具只通过各自入口桥接回统一 `BOOTSTRAP` 文件，再按索引继续读取。  
> 对支持多入口的工具，采用“根入口简短摘要 + 工具目录详细配置”的组合。  
> 基础治理规则通过规则目录与 hooks 自动加载，深层知识通过 wiki/skills 按需查询，文件修改后通过 `PostToolUse` 自动做文档维护检查。  
> 禁止为不同 Agent 分别维护一套独立的提示词工程副本。

## 6. 常见错误

### 错误 1：桥接文件写得太厚

问题：

- 把大部分业务知识都塞进 `CLAUDE.md`

后果：

- 上下文变重
- 维护两份规则
- 与单一来源逐渐漂移

正确方式：

- 让桥接文件只负责指向 `BOOTSTRAP`

### 错误 2：没有 hooks 兜底

问题：

- 只写入口文件，不加自动注入

后果：

- 会话压缩/恢复后丢失基础治理规则

正确方式：

- 用 `SessionStart` 类 hooks 重注入简短治理摘要

### 错误 3：只做启动注入，不做执行后检查

问题：

- 启动时加载了规则
- 但改完治理文件后没有自动提醒维护文档

后果：

- `CHANGE_LOG`、`POLICIES`、兼容文档容易漏更

正确方式：

- 再补一个 `PostToolUse` 文档维护检查 hook

### 错误 4：没有统一 BOOTSTRAP

问题：

- 每个 Agent 都自己维护一套“主入口说明”

后果：

- 维护成本高
- 多入口内容容易互相漂移

正确方式：

- 统一让所有 Agent 指向一个 `BOOTSTRAP.md`

## 7. 推荐后续扩展

- 增加桥接清单 JSON，供脚本和 Agent 自动读取
- 增加 CHANGE_LOG 门禁脚本，检测规则变更后是否忘记登记
- 增加 wiki/skills 访问统计，用于评估哪些入口最常用
- 增加迁移脚本，一键为新项目生成桥接层
- 将兼容补丁抽象为 skill，要求目标 Agent 先自检入口读取规则再落地适配器（参考 `/.trae/skills/multi_agent_prompt_bridge/SKILL.md`）

## 8. 最终原则

无论迁移到哪个项目，都要坚持一句话：

> 提示词工程只能有一套单一来源，多 Agent 只能做桥接，不能各自养一套知识库。
