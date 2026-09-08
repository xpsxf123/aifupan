# Research / Feasibility Reports

调研与可行性报告的归口。每份文件回答**一个调研问题**：现状如何 / 能给到什么程度 / 这事可不可行。

## 与其他知识层的关系

| 出处 | 状态 |
|---|---|
| `/h-research <主题>`（Scenario F，[lifecycle.md Part 2](../../../rules/lifecycle.md#scenario-f--research--feasibility)） | 唯一产出本目录文件的命令 |
| `/h-brief` | **不**写本目录；若用户输入是 research-pattern，`/h-brief` step 0 会反问并提示切到 `/h-research` |
| `archive/` | 完全不同 —— archive 是已交付的 Change openspec；research 可能永远不变成 Change |
| `specs/` | 完全不同 —— specs 是 in-flight 的 openspec proposals |
| `incidents/` | 完全不同 —— incidents 是异常 / 故障的事后档案 |

## 文件约定

- 命名：`YYYYMMDD__<kebab-case-slug>.md`
- 单文件 ≤500 行（与 wiki 通用约束一致）；超长拆专题
- 不进 archive、不归口 `@knowledge-harvester`、不强制 schema_checker
- 状态字段：`<调研完成 / 待补 / 已转 Change>`
- 如调研后转 Change → 用 `/h-brief <slug>`，并在本文件 Status 改为 "已转 Change，run_dir: \<path\>"

## 现有调研档案

| 日期 | 主题 | Status | 链接 |
|---|---|---|---|
| 2026-05-23 | 销售智能体行为信号数据可行性（内部 working notes，含 Errata） | 调研完成 | [20260523__sales-agent-data-signals.md](./20260523__sales-agent-data-signals.md) |
| 2026-05-23 | 销售智能体客户行为信号数据接口（**对外交付版 v0.1**） | 提案待评审 | [20260523__sales-agent-data-spec-handoff.md](./20260523__sales-agent-data-spec-handoff.md) |
| 2026-05-23 | 密码相关接口盘点 | 调研完成 | [20260523__password-api-inventory.md](./20260523__password-api-inventory.md) |
| 2026-05-24 | AI 问答 · 话术助手业务与处理流程（含 wiki 与代码偏差修订建议 G4） | 调研完成 | [20260524__ai-script-assistant-business-flow.md](./20260524__ai-script-assistant-business-flow.md) |
| 2026-05-24 | 话术助手 cue_words 实际数据样本（G3 伴生文档） | 调研完成 | [20260524__ai-script-assistant-cue-words-sample.md](./20260524__ai-script-assistant-cue-words-sample.md) |
| 2026-05-24 | 16 种 askType 提示词全图鉴 & 存储分布（1314 行 prompt + 字段语义推断 + 缺失 askType 存储定位） | 调研完成 | [20260524__cue-words-askType-inventory.md](./20260524__cue-words-askType-inventory.md) |
| 2026-05-24 | **AI 问答从简单 LLM API 升级为 Agent 架构（主报告 / 总览）** — MVP=话术助手 askType=6；含决策矩阵 12 项 + 5 周实施路径 + 5 个伴生文档导航 | **占位 / 待补** | `20260524__ai-agent-uplift-master.md`（计划列出但尚未撰写） |
| 2026-05-24 | Agent 架构设计（伴生 1）— loop / 5 阶段压缩 / 状态机 / 主循环伪代码 / 与现有 ask() 兼容 | **占位 / 待补** | `20260524__agent-architecture.md`（计划列出但尚未撰写） |
| 2026-05-24 | Agent 技术选型（伴生 2）— Spring AI / pgvector / Langfuse / DeepEval / 11 维评分矩阵 | **占位 / 待补** | `20260524__agent-tech-stack.md`（计划列出但尚未撰写） |
| 2026-05-24 | Agent Skills 与子智能体调度（伴生 3）— 12 内容 + 5 数据 + 3 元 Skill / 3 子 Agent / Planner prompt / 动态模型选择 | **占位 / 待补** | `20260524__agent-skills-and-dispatch.md`（计划列出但尚未撰写） |
| 2026-05-24 | Agent 知识库管理与提示词治理（伴生 4）— 1314 prompt 萃取 / 清洗 5 规则 / 结构化 YAML / RAG 索引 / 灰度发布 | 调研完成 | [20260524__agent-knowledge-base.md](./20260524__agent-knowledge-base.md) |
| 2026-05-24 | Agent 可观察 / 可验证 / 可实施（伴生 5）— 3 层观测 + Trace schema + DeepEval 评测 + A/B 灰度 + 5 周 35 人日详细路径 + 回滚 | **占位 / 待补** | `20260524__agent-obs-eval-rollout.md`（计划列出但尚未撰写） |
