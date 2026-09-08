# BOOTSTRAP（多 Agent 统一入口）

本文件是提示词工程的统一入口，供 Claude Code、Trae、Codex 及其他 Agent 作为“首个加载文件”使用。

## 目标

- 将 `agnet/` 作为提示词工程的单一来源目录。
- 避免每个 Agent 只看到“路径映射表”却不继续读取核心规则。
- 将“基础治理规则自动加载”与“按需深入读取 wiki/skills”分离。
- 保证多个 Agent 共享同一套记忆、规则、知识图谱与技能目录，而不是各自维护一份副本。

## 单一来源

- 单一来源目录：`agnet/`
- 规则发现层/适配层：
  - Claude Code：`CLAUDE.md`、`.claude/CLAUDE.md`、`.claude/rules/`、`.claude/settings.json`
  - Trae：`.trae/` 与 `.trae/rules/`
  - 其他 Agent：各自的入口桥接文件
- 原则：桥接文件只做“加载与映射”，不做二次发散维护。

## 强制加载规则

任何 Agent 在读取到本文件后，必须遵循以下规则：

1. 不得把本文件当作全部上下文。
2. 必须把本文件视为“统一入口 + 读取导航器”。
3. 必须根据任务类型继续读取下述必要文件，而不是停留在路径映射层。
4. 若桥接层与 `agnet/` 内容冲突，以 `agnet/` 为准。

## 必读顺序

### A. 基础治理层

以下文件构成“基础规则包”，适合在会话开始时加载：

- `agnet/AGENT.md`
- `agnet/TOOLS.md`
- `agnet/MEMORY.md`
- `agnet/POLICIES.md`
- `agnet/CHANGE_LOG.md`

### B. 导航层

以下文件用于决定“接下来去哪里读”：

- `agnet/INDEX.md`
- `agnet/wiki/README.md`
- `agnet/wiki/KNOWLEDGE_GRAPH.md`

### C. 技能层

实现/分析任务进入技能层时，必须按以下优先级：

1. `agnet/self_skills/INDEX.md`
2. `agnet/skills/INDEX.md`
3. 对应技能目录下的 `SKILLS.md`

### D. 领域知识层

进入具体知识时，必须遵循“图谱 -> 域索引 -> 具体页面”的正向漏斗：

1. `agnet/wiki/KNOWLEDGE_GRAPH.md`
2. 任务相关域的 `agnet/wiki/<domain>/index.md`
3. 仅在域索引不足以支持决策时，下沉读取具体页面

## 任务场景到读取链路

### 代码实现 / 缺陷修复

必读：

- `agnet/AGENT.md`
- `agnet/TOOLS.md`
- `agnet/MEMORY.md`
- `agnet/INDEX.md`

按需继续：

- `agnet/wiki/KNOWLEDGE_GRAPH.md`
- 相关域 `index.md`
- `self_skills/INDEX.md` -> `skills/INDEX.md`

### 规则 / 提示词工程治理

必读：

- `agnet/AGENT.md`
- `agnet/POLICIES.md`
- `agnet/CHANGE_LOG.md`
- `agnet/INDEX.md`

按需继续：

- `agnet/wiki/README.md`
- `agnet/wiki/通用-双层记忆与图谱工作流.md`
- `agnet/wiki/通用-上下文漏斗与写回.md`
- `agnet/wiki/通用-WAL与防膨胀规则.md`
- `agnet/wiki/通用-生命周期与门禁.md`

### 知识检索 / 历史决策复用

必读：

- `agnet/MEMORY.md`
- `agnet/INDEX.md`
- `agnet/wiki/KNOWLEDGE_GRAPH.md`

按需继续：

- 对应域 `index.md`
- 对应 wiki 页面

### 技能扩展 / 模板复用

必读：

- `agnet/TOOLS.md`
- `agnet/self_skills/INDEX.md`
- `agnet/skills/INDEX.md`

按需继续：

- 目标技能 `SKILLS.md`
- 目标模板目录 `templates/`

## 写回与治理规则

- 新知识优先写入对应域 `wal/`，再按窗口合并到域 `index.md`。
- `WAL` 记录的是“可复用知识与稳定决策”，不是普通发布日志。
- 新增/修改规则、技能、索引结构时，必须更新 `agnet/CHANGE_LOG.md`。
- 高频稳定结论才允许进入 `agnet/MEMORY.md`。
- 策略型经验优先进入 `agnet/POLICIES.md`。

## 多 Agent 兼容约束

- 不要求不同 Agent 直接读取同名入口文件。
- 允许为不同 Agent 提供不同桥接文件名，例如：
  - Claude Code 使用 `CLAUDE.md`
  - Trae 使用 `.trae/rules/*`
  - 其他工具使用各自约定入口
- 但所有桥接层都必须回到 `agnet/BOOTSTRAP.md` 与 `agnet/` 主目录，而不是复制出一套新的知识库。

## 推荐桥接策略

### 轻桥接

适用于只支持单一入口文件的 Agent：

- 提供一个极薄入口文件
- 入口文件只负责：
  - 声明 `agnet/` 是单一来源
  - 导入或摘要化 `BOOTSTRAP.md`
  - 告诉 Agent 必须继续沿导航链读取

### 双入口桥接

适用于像 Claude Code 这样会同时读取“仓库根入口 + 工具目录入口”的 Agent：

- 根目录入口文件负责“简短摘要 + 关键规则内联”
- 工具目录入口文件负责“详细配置 + 继续读取要求”
- 两者是合并加载，不是覆盖关系
- 但二者都不是单一来源，最终仍必须回到 `agnet/BOOTSTRAP.md`

### 强桥接

适用于支持规则目录、hooks、项目级设置的 Agent：

- 启动时自动加载核心规则
- 在会话开始/压缩/恢复时用 hook 重新注入基础治理摘要
- 在文件修改成功后用 `PostToolUse` hook 做文档维护检查
- 通过规则目录承载“始终生效”的高优先级规则
- 通过主入口文件承载任务分流与继续读取要求

## 失败模式与修正方式

### 失败模式 1：只读入口，不继续读取

修正：

- 在桥接入口中加入 `MUST continue reading` 约束
- 在 hook 中注入“必须继续读取的文件清单”
- 对支持双入口的工具，使用“根目录摘要 + 工具目录详细配置”的组合

### 失败模式 2：把业务知识和基础规则全塞进单文件

修正：

- 将基础规则保持在 `BOOTSTRAP + rules`
- 将业务知识继续留在 `wiki/` 与 `skills/`
- 将关键治理规则以内联摘要形式放入入口桥，而不是只放路径映射

### 失败模式 3：不同 Agent 维护不同版本的规则

修正：

- 所有 Agent 桥接层只允许引用 `agnet/`
- 不允许在 Agent 专属目录中长期维护独立业务规则副本

## 迁移要求

将本机制迁移到其他项目时，至少复制以下资产：

- `agnet/BOOTSTRAP.md`
- `agnet/AGENT.md`
- `agnet/TOOLS.md`
- `agnet/INDEX.md`
- `agnet/MEMORY.md`
- `agnet/POLICIES.md`
- `agnet/CHANGE_LOG.md`
- `agnet/wiki/`
- `agnet/skills/`
- `agnet/self_skills/`

然后再为目标 Agent 分别补充：

- 入口桥接文件
- 详细配置入口文件（若工具支持双入口）
- 规则目录桥接
- 启动 hook / 重新注入 hook
- `PostToolUse` 文档维护检查 hook

## 结论

本文件的职责不是替代所有规则文件，而是确保任何 Agent 进入项目时，都不会停在“映射关系”层，而会继续沿着统一的治理链路进入真正的提示词工程单一来源。
