# CLAUDE 工作流使用手册

> ReviewAnalysis 项目 AI 辅助开发工作流完整指南  
> 适用环境: .NET Framework 4.7.2 + WinForms + CefSharp + SQLite  
> 生成日期: 2026-05-28

---

> **关于 `/h-` 命令的说明**：`/h-` 命令（如 `/h-explore`、`/h-implement`）是快速接入某个工作流 Phase 的便捷入口，但**并非必须使用**。你完全可以用自然语言直接描述需求，AI 助手会根据 CLAUDE.md 协议自动识别意图、判定的风险等级、挂载对应skill/Agent并进入正确的开发管线。命令只是快捷方式，工作流本身不依赖命令触发。

---

## 一、概述

### 1.1 这套工作流是什么

一套嵌入在 `.claude/` 目录中的 AI 辅助开发工作流系统，包含**规则(Rules)、命令(Commands)、智能体(Agents)、技能(Skills)、知识库(Wiki)、脚本(Scripts)** 六大组件。它通过定义清晰的开发阶段(Phase)和角色(Mounted Role)，让 AI 助手按照规定的流程执行开发任务，确保代码质量和知识沉淀。

### 1.2 核心设计理念

| 理念 | 说明 |
|---|---|
| **双 SSOT 入口** | `CLAUDE.md` 是系统入口，`.claude/rules/` 是规则权威来源 |
| **风险驱动** | 每个任务按 TRIVIAL/LOW/MEDIUM/HIGH 风险分级，高风险任务走完整审批流程 |
| **6 Phase 管线** | Explorer → Propose → Review → Implement → QA → Archive，按需走完整或部分 Phase |
| **知识自动沉淀** | 每次任务完成自动生成 WAL(Write-Ahead Log)知识片段，防止经验丢失 |
| **代码规范强制** | Skills 提供 31 个维度的 C# 编码规范，Agent 在 Implement 阶段自动挂载检查 |

### 1.3 快速上手

```
# 查状态 — 看看有什么在进行的任务
/h-status

# 开始一个新功能 — STANDARD 流程
/h-explore
/h-openspec <slug> --risk medium
/h-design <run_id>
/h-implement
/h-qa
/h-archive

# 修一个 Bug
/h-fix-bug <描述>

# 需求分解
/h-decompose <slug> <prd文件>

# 生成代码脚手架
/h-generate <EntityName>
```

---

## 二、系统入口 — CLAUDE.md

### 2.1 文件位置

`/CLAUDE.md` (项目根目录)

### 2.2 职责

- AI 助手的**启动引导文件**，每次会话自动加载
- 定义 `Hard Rules`（硬性规则，违反即停止）
- 声明行为规范（中文优先、最小代码、手术式修改）
- 映射 4 种模式(Vibe/Patch/Research/Standard)
- 列出所有 SSOT 文件的位置
- C#/.NET Framework 4.7.2 约束速查表

### 2.3 关键内容

```markdown
## Hard Rules
- Anti-loop: max 3 retries per gate/linter, max 2 for dotnet build
- 中文优先
- <HARD-GATE id="design-before-code"> — 必须先有设计文档才能写代码
- <HARD-GATE id="csproj-register-new-cs"> — 新 .cs 文件必须注册到 csproj
- <HARD-GATE id="evidence-before-archive"> — 归档必须有证据映射
```

---

## 三、规则系统 (.claude/rules/)

规则是工作流的**最高权威来源**，所有组件（Agent、Skill、Command）的行为都由规则文件定义。

### 3.1 `.claude/rules/lifecycle.md` — 生命周期

| 用途 | 定义 6 个 Phase 的流程、每个 Phase 挂载哪些 Agent、产出什么文件 |
|---|---|
| 核心概念 | Phase 1 Explorer → Phase 2 Propose → Phase 3 Review → Phase 4 Implement → Phase 5 QA → Phase 6 Archive |
| 4 种 Profile | LEARN(只读) / PATCH(轻量) / STANDARD(完整) / RESEARCH(研究) |

**解决的问题：** AI 助手知道在什么阶段该做什么、不该做什么，避免"跳过设计直接写代码"。

### 3.2 `.claude/rules/risk-profiles.md` — 风险分级

| 用途 | 三维影响评估(影响范围 × 破坏性变更 × 业务冲击)定级，替代关键词匹配 |
|---|---|
| 核心思想 | 先评估变更的实际影响，关键词只作为「关注信号」提醒复核，不自动升级 |
| 三维度 | Blast Radius(波及文件/层级/消费方) · Breaking Change(契约/协议/Schema) · Business Impact(核心管线中断风险) |
| 定级规则 | 三维取最高分：0=TRIVIAL, 1=LOW, 2=MEDIUM, 3=HIGH |

**解决的问题：** 避免「改一行注释含 auth 字样被误判 HIGH」和「改 ASR 时间戳无关键词被漏判」两种情况。关键词不是风险，实际影响才是。

### 3.3 `.claude/rules/policy.md` — 安全策略

| 用途 | 安全约束、资源限制、Hook 配置、Commit 策略、反膨胀规则 |
|---|---|
| 预算机制 | Wiki ≤ 3 文档/会话, Code ≤ 8 文件/会话 |
| 重试上限 | `dotnet build` 最多 2 次, Linter 最多 3 次 |

**解决的问题：** 防止 AI 助手陷入无限循环、超出范围修改文件、随意 commit。

### 3.4 `.claude/rules/dispatch-template.md` — 派发协议

| 用途 | 何时派发子 Agent、派发 Prompt 必须包含哪 5 个 Section、如何验证子 Agent 返回 |
|---|---|
| 派发条件 | AC ≥ 4 或跨领域 或安全审计 → 派发子 Agent |

**解决的问题：** 保证主 Agent 向子 Agent 派发任务时信息完整，防止子 Agent 因信息不足而浪费计算资源。

### 3.5 `.claude/rules/skill-precedence.md` — 技能挂载规则

| 用途 | 定义 6 个 Zone(A/B/C/D/N/U/M)、每个 Zone 的技能组合规则、Phase 与 Zone 的对应关系 |
|---|---|
| Zone N | 23 个领域专用技能(按代码变更内容自动挂载) |

**解决的问题：** 确保在合适的时机挂载合适的技能。比如修改 SQLite 代码时自动挂载 `csharp-repository-pattern`。

### 3.6 `.claude/rules/wal-policy.md` — 知识沉淀策略

| 用途 | 何时写 WAL、WAL 文件格式、Confidence/Evidence 标签规范、跨链接要求 |
|---|---|
| STANDARD 强制 | MEDIUM 及以上必须写 WAL；HIGH 必须包含 Architecture + Rules 维度 |

**解决的问题：** 每次代码变更后的经验能被结构化记录，下次类似问题时可以直接检索。

---

## 四、开发管线 — 6 Phase 流程

### 4.1 Phase 1: Explorer（需求探索）

```
触发: /h-explore
挂载 Agent: ambiguity-gatekeeper + requirement-engineer + focus-guard
产出: focus_card.md + explore_report.md
```

**解决的问题：** 需求不清晰时避免直接编码。先理解需求范围、识别歧义、确定边界。

### 4.2 Phase 2: Propose（方案设计）

```
触发: /h-propose 或 /h-openspec
挂载 Agent: system-architect
产出: openspec.md (HIGH 风险额外产出 ADR)
```

**解决的问题：** 设计先行。明确改动范围、技术方案、验收标准、回滚计划。

### 4.3 Phase 3: Review（方案评审）

```
触发: Phase 2 完成后自动进入
挂载 Agent: system-architect(自审) + code-reviewer(HIGH) + security-sentinel(HIGH)
检查: 工程规范 / 架构 / 线程安全 / 安全漏洞 / 性能
```

**解决的问题：** 防止有问题的设计进入实现阶段。

### 4.4 Phase 4: Implement（代码实现）

```
触发: /h-implement (需 Phase 3 通过)
挂载 Agent: lead-engineer + focus-guard + security-sentinel
产出: 修改的 .cs 文件 + 更新的 .csproj
```

**解决的问题：** 按照 openspec.md 的边界安全实现代码，防止范围漂移。

### 4.5 Phase 5: QA（质量验证）

```
触发: /h-qa
挂载 Agent: code-reviewer + documentation-curator
执行: dotnet build + 测试 + delivery_capsule.md
```

**解决的问题：** 确保代码编译通过、测试通过、产出物完整。

### 4.6 Phase 6: Archive（知识归档）

```
触发: /h-archive (Phase 5 PASS 后自动)
挂载 Agent: knowledge-extractor + documentation-curator
产出: WAL 知识片段 + 更新 KNOWLEDGE_GRAPH.md 索引
```

**解决的问题：** 知识不会随着任务结束而丢失。

---

## 五、命令系统 (.claude/commands/) — 20 个命令

命令是用户与工作流交互的主要入口。以下按使用场景分类：

### 5.1 Phase 专用命令（7 个）

| 命令 | 用途 | 使用示例 |
|---|---|---|
| `/h-explore` | 进入 Phase 1 需求探索 | `/h-explore` |
| `/h-openspec` | 生成 openspec.md 并绑定到 _active.json | `/h-openspec asr-onnx-fix --risk medium` |
| `/h-propose` | 进入 Phase 2 方案设计 | `/h-propose` |
| `/h-design` | 派发 system-architect 填写设计方案 | `/h-design run_20260528_001` |
| `/h-implement` | 进入 Phase 4 代码实现 | `/h-implement` |
| `/h-qa` | 进入 Phase 5 质量验证 | `/h-qa` |
| `/h-archive` | 进入 Phase 6 知识归档 | `/h-archive` |

### 5.2 工作流管理命令（3 个）

| 命令 | 用途 | 使用示例 |
|---|---|---|
| `/h-status` | 查看所有 run 状态概览 | `/h-status --all` |
| `/h-research` | RESEARCH 模式入口，生成研究报告 | `/h-research asr-model-eval --scope deep` |
| `/h-decompose` | PRD/EPIC → INVEST 子任务分解 | `/h-decompose new-platform prd.md` |

### 5.3 开发辅助命令（5 个）

| 命令 | 用途 | 使用示例 |
|---|---|---|
| `/h-generate` | 从实体名生成全套数据访问层模板 | `/h-generate ProductAnalysis` |
| `/h-impact` | 改动前影响范围分析 | `/h-impact AsrRecognize --type method` |
| `/h-legacy-code` | 遗留代码(God Class)结构化分析 | `/h-legacy-code Form1 --deep` |
| `/h-distill-from-code` | 代码→Wiki 知识对账(检测过时文档) | `/h-distill-from-code domain:asr topic:"时间戳"` |
| `/h-from-ticket` | 外部 Ticket(GitHub Issue)→openspec 转换 | `/h-from-ticket github 42 --risk high` |

### 5.4 故障处理命令（3 个）

| 命令 | 用途 | 使用示例 |
|---|---|---|
| `/h-fix-bug` | Bug 修复管线(确认根因前禁止写代码) | `/h-fix-bug asr-recognize-fail --production` |
| `/h-runbook` | 异常诊断恢复(输入症状→搜索 Wiki/代码→输出修复报告) | `/h-runbook "ASR 识别结果后半段文字丢失"` |
| `/h-incident` | 生产事故结构化记录 | `/h-incident asr-outage --production` |

### 5.5 协作命令（2 个）

| 命令 | 用途 | 使用示例 |
|---|---|---|
| `/h-collab` | 生成团队协作交付物(API 契约/流程对齐) | `/h-collab platform-api --type api` |
| `/h-collab-update` | 记录外部团队反馈、签收解除阻塞 | `/h-collab-update platform-api --signoff` |

---

## 六、智能体系统 (.claude/agents/) — 10 个 Agent

Agent 是被主 AI 助手在特定 Phase 派发的子任务执行者。

### 6.1 Plan/Design 角色

| Agent | 模型 | 挂载 Phase | 职责 |
|---|---|---|---|
| `ambiguity-gatekeeper` | haiku | Phase 1 | 需求歧义检查，产出 focus_card.md |
| `requirement-engineer` | sonnet | Phase 1 (STANDARD) | 需求分析，产出 explore_report.md |
| `system-architect` | sonnet | Phase 2 | 架构设计，产出 openspec.md (+ADR) |

### 6.2 Implement/Guard 角色

| Agent | 模型 | 挂载 Phase | 职责 |
|---|---|---|---|
| `lead-engineer` | sonnet | Phase 4 | 代码实现，产出 .cs 文件修改 |
| `focus-guard` | haiku | Phase 1,4,5 | 范围守护，阻止超出边界(ScopeFrame)的修改 |
| `security-sentinel` | haiku | Phase 3,4 | 安全扫描，检测硬编码密钥/密钥/Token |
| `sqlite-reviewer` | haiku | Phase 4 (按 diff) | SQLite 代码审查，检测 SQL 注入和性能问题 |

### 6.3 QA/Archive 角色

| Agent | 模型 | 挂载 Phase | 职责 |
|---|---|---|---|
| `code-reviewer` | sonnet | Phase 5 | 代码审查，产出 issue 列表和 AC 覆盖图 |
| `documentation-curator` | sonnet | Phase 5,6 | 文档管理，产出 delivery_capsule.md 和索引更新 |
| `knowledge-extractor` | sonnet | Phase 6 | 知识提取，产出 WAL 知识片段 |

---

## 七、技能系统 (.claude/skills/) — 31 个 Skill

技能是 C# 编码规范的权威参考。按 Zone 分类：

### 7.1 Zone A — 代码阶段技能（4 个）(每次 Implement 必挂载)

| Skill | 层级 | 用途 |
|---|---|---|
| `csharp-architecture-designer` | L1 | 模块边界设计、命名空间划分、GC 内存策略 |
| `csharp-engineering-standards` | L2(必装) | C# 编码核心标准：命名、async/await、异常处理 |
| `csharp-data-governance-standards` | L3 | SQLite 数据治理：表结构、迁移、性能 |
| `csharp-task-planner` | L4 | 任务拆解：接口优先、逐步构建、验证序列 |

### 7.2 Zone B — 审查阶段技能（1 个）(互斥挂载)

| Skill | 用途 |
|---|---|
| `csharp-code-review` | C# 代码审查清单：安全、异步、资源管理、架构规则 |

### 7.3 Zone N — 领域专用技能（23 个）(按代码变更内容按需挂载)

| Skill | 触发条件 | 核心内容 |
|---|---|---|
| `csharp-result-pattern` | Result\<T\> 返回类型 | Result 单子模式、Error 类型、Map/Bind/Tap |
| `csharp-structured-logging` | ILogger\<T\> / Serilog | 结构化日志、correlation ID、日志级别 |
| `csharp-audit-trail` | 审计日志 | SQLite 审计表、IAuditService、变更追踪 |
| `csharp-unit-testing` | 测试文件 | xUnit + NSubstitute + FluentAssertions |
| `csharp-options-pattern` | App.config | 强类型配置、IOptionsManager\<T\> |
| `csharp-fluent-validation` | 输入验证 | FluentValidation、声明式校验、控制器集成 |
| `csharp-domain-entity` | 实体类 | DDD 实体、WinForms 绑定兼容、DisplayModel |
| `csharp-repository-pattern` | Db/ 文件 | Repository 接口、SQLite 实现、批量查询 |
| `csharp-pipeline-behaviors` | 请求处理管线 | 装饰器链、Logging/Validation/Transaction Behavior |
| `csharp-background-jobs` | 后台任务 | Quartz.NET、WinForms 生命周期集成、重试策略 |
| `csharp-winforms-patterns` | WinForms 编辑 | MVP 适配、UserControl 组合、CefSharp 生命周期 |
| `csharp-debugging-guide` | 运行时调试 | 死锁诊断、内存泄漏检测、SQLite 锁排查 |
| `legacy-code-understanding` | God Class 分析 | 依赖图、副作用清单、Strangler Fig 策略 |
| `csharp-multithreading-pitfalls` | lock/Task | 线程安全、死锁模式、UI 线程封送 |
| `csharp-dependency-injection` | DI/Service | 构造函数注入、接口优先、单例管理 |
| `csharp-event-bus-pattern` | 模块通信 | 进程内事件总线、弱引用、解耦 |
| `csharp-plugin-architecture` | 动态扩展 | 反射加载、接口契约、插件生命周期 |
| `csharp-network-io-guidelines` | HttpClient | 单例复用、超时、重试、连接池 |
| `csharp-resource-governor-pattern` | 后台任务 | 统一资源治理、线程池、文件 I/O |
| `csharp-platform-module-dev` | 平台模块 | 基类继承、统一枚举、Adapter 模板 |
| `csharp-gui-guidelines` | WinForms 控件 | 释放、UI 线程封送、CefSharp 集成 |
| `prd-review-checklist` | PRD 评审 | 功能完整性/兼容性/性能/安全 7 维度检查 |
| `tech-selection-framework` | 技术选型 | 许可证/.NET FW 兼容/包大小/社区活跃度评分 |

### 7.4 Zone D/M — 归档/元技能（3 个）

| Skill | 触发条件 | 用途 |
|---|---|---|
| `failure-memory-helper` | Archive 时有失败记录 | 记录和检索失败经验 |
| `linter-severity-standard` | post-hook | 定义 FAIL/WARN/IGNORE 判定标准 |
| `csharp-build-resolver` | dotnet build 失败 | 编译错误诊断和修复 |

---

## 八、Wiki 知识库 (.claude/wiki/)

### 8.1 知识库结构

```
.claude/wiki/
├── KNOWLEDGE_GRAPH.md          # 知识索引总表
├── purpose.md                  # 系统目标
├── schema/                     # 文档格式规范
│   ├── openspec_schema.md      #   openspec.md 模板
│   ├── adr_schema.md           #   ADR 决策记录模板
│   ├── slim_spec_schema.md     #   轻量级 Spec 模板
│   └── subagent_contract_schema.md  # 子 Agent 返回格式
├── templates/                  # 文件模板
│   ├── current_task.template.md
│   ├── explore_report.template.md
│   ├── focus_card.template.md
│   └── openspec.template.md
├── archive/                    # 冷存储
└── wiki/                       # 知识主体
    ├── domain/                 # 业务领域(8 个文档 + 13 个 WAL)
    ├── api/                    # API 文档(6 个文档 + 3 个 WAL)
    ├── data/                   # 数据模型(4 个文档)
    ├── architecture/           # 架构(3 个文档 + 1 个 ADR + 1 个 WAL)
    ├── preferences/            # 编码规范(6 个文档 + 15 个 WAL)
    ├── reviews/                # 代码审查记录(4 个文档)
    ├── specs/                  # 活跃设计提案(10 个 openspec)
    └── testing/                # 测试标准(1 个文档)
```

### 8.2 WAL 系统 (Write-Ahead Log) — 知识沉淀

WAL 是项目的**经验数据库**。每次 STANDARD 任务完成时自动写入知识片段。

**维度分类：**

| 维度 | 目录 | 已积累 |
|---|---|---|
| Domain(领域) | `wiki/domain/wal/` | 13 条 |
| API | `wiki/api/wal/` | 3 条 |
| Rules(规则) | `wiki/preferences/wal/` | 15 条 |
| Data | `wiki/data/wal/` | 0 条 |
| Architecture | `wiki/architecture/wal/` | 1 条 |

**每条 WAL 事实必须携带：**
```markdown
[Confidence: HIGH | MEDIUM | LOW]
[Evidence: <file>:<line> | <commit-hash>]
```

---

## 九、脚本系统 (.claude/scripts/)

### 9.1 门禁脚本 (gates/) — 8 个

| 脚本 | 用途 | 执行时机 |
|---|---|---|
| `ambiguity_gate.py` | 需求歧义检查 | 任务开始时 |
| `delivery_capsule_gate.py` | 交付胶囊格式验证 | Phase 5 QA |
| `focus_card_gate.py` | 范围卡片格式验证 | Phase 1 Explorer |
| `scope_guard.py` | 修改范围检查 | Phase 4 Implement |
| `secrets_linter.py` | 密钥泄露检测 | 每次 .cs 文件编辑后 |
| `skill_index_linter.py` | Skill 索引一致性检查 | Phase 6 Archive |
| `wal_template_gate.py` | WAL 模板格式验证 | Phase 6 Archive |
| `writeback_gate.py` | 写回完整性检查 | Phase 6 Archive |

### 9.2 Wiki 工具 (wiki/) — 7 个

| 脚本 | 用途 |
|---|---|
| `wiki_linter.py` | Wiki 死链和行数上限检查 |
| `anti_bloat_check.py` | 反膨胀阈值检查(文件 ≤ 3000 行) |
| `compactor.py` | WAL 碎片压缩合并 |
| `graph_checker.py` | 知识图谱完整性检查 |
| `pref_tag_checker.py` | 首选项标签检查 |
| `schema_checker.py` | OpenSpec schema 验证 |
| `zero_residue_audit.py` | 零残留审计 |

### 9.3 其他工具

| 脚本 | 用途 |
|---|---|
| `triage_probe.py` | 5 信号风险自动评分 |
| `code_smell_detector.py` | C# 代码坏味自动检测(10 类) |
| `harness/engine.py` | 工作流引擎 |
| `tools/archive_session_artifacts.py` | 会话产物归档 |
| `tools/librarian_gc.py` | 知识库垃圾回收 |

---

## 十、运行记录 (.claude/runs/)

每次任务执行都会在 `.claude/runs/<run_id>/` 下留下完整的生命周期记录。

### 10.1 目录结构

```
.claude/runs/
├── _active.json                          # 当前活跃任务队列
├── <run_id_1>/                           # 每个 run 独立目录
│   ├── current_task.md                   #   当前任务清单
│   ├── focus_card.md                     #   范围卡片(Phase 1 产出)
│   ├── explore_report.md                 #   需求报告(Phase 1 产出, STANDARD)
│   ├── openspec.md                       #   设计文档(Phase 2 产出)
│   ├── delivery_capsule.md               #   交付胶囊(Phase 5 产出)
│   └── scratch/                          #   临时分析文件
└── <run_id_2>/
```

**解决的问题：** 任务可恢复、可追溯。如果会话中断，下次可以通过 `_active.json` 恢复上下文。

---

## 十一、使用场景示范

### 场景 1: 标准功能开发

```
# 步骤 1 — 查看状态
/h-status

# 步骤 2 — 创建 openspec
/h-openspec asr-vad-integration --risk medium

# 步骤 3 — 结构化设计(填入 §3 Design + §4 Public Contract)
/h-design

# 步骤 4 — 代码实现(自动挂载 engineering-standards + security-sentinel + focus-guard)
/h-implement

# 步骤 5 — 质量验证(自动 dotnet build + 测试)
/h-qa

# 步骤 6 — 知识归档(生成 WAL)
/h-archive
```

### 场景 2: Bug 修复

```
# 一键式 Bug 修复管线
/h-fix-bug asr-recognize-silent-segments-lost --production
```

AI 助手会自动：
1. 收集 Bug 详情 → 2. 查询历史 incident → 3. 搜索 WAL 中相关反模式
4. 构建根因假设(必须引用代码 file:line) → 5. 确认后才允许修改代码

### 场景 3: 客户异常诊断

```
/h-runbook "录播回放的 ASR 识别结果时间戳全是 0，文字内容正常"
```

AI 助手会自动：
1. 症状结构化提取 → 2. 搜索 `preferences/wal/` + `domain/wal/` 匹配类似问题
3. 搜索代码中相关方法和调用链 → 4. 构建根因假设和修复方案
5. 产出异常恢复报告

### 场景 4: 生成脚手架

```
# 从实体名一键生成 Entity + Repository + Validator + csproj + DI
/h-generate ProductAnalysis --table product_analysis
```

### 场景 5: 改动前影响分析

```
/h-impact AuthStatus --type field --depth 3
```

输出：所有读取/写入该字段的代码位置、间接影响链、消费方检查清单。

### 场景 6: 需求分解

```
/h-decompose new-life-platform prd_life_platform.md
```

输出：INVEST 子任务列表 + 逐任务 openspec 骨架 + `_active.json` 队列。

### 场景 7: 代码→Wiki 对账

```
# 检测 ASR 领域 wiki 文档是否与当前代码一致
/h-distill-from-code domain:asr topic:"时间戳对齐" --dry-run
```

### 场景 8: 遗留代码分析

```
# 分析 God Class 的可提取接缝和重构优先级
/h-legacy-code AnchorBll --deep
```

---

## 十二、完整目录结构及文件说明

以下列出 `CLAUDE.md` 及 `.claude/` 下**所有文件**及其职责说明。

```
ReviewAnalysis/
│
├── CLAUDE.md                              # [系统入口] AI 助手启动引导 + Hard Rules + SSOT 索引
│
└── .claude/
    │
    ├── settings.json                      # [配置] Claude Code harness 全局设置
    ├── settings.local.json                # [配置] 本地覆盖设置(不入 git)
    │
    ├── agents/                            # 智能体定义(10 个)
    │   ├── ambiguity-gatekeeper.md        #   [Phase 1] 需求歧义检查 · haiku
    │   ├── code-reviewer.md               #   [Phase 5] 代码审查 · sonnet
    │   ├── documentation-curator.md       #   [Phase 5,6] 文档策展 · sonnet
    │   ├── focus-guard.md                 #   [Phase 1,4,5] 范围守卫 · haiku
    │   ├── knowledge-extractor.md         #   [Phase 6] 知识提取 · sonnet
    │   ├── lead-engineer.md               #   [Phase 4] 代码实现 · sonnet
    │   ├── requirement-engineer.md        #   [Phase 1] 需求工程 · sonnet
    │   ├── security-sentinel.md           #   [Phase 3,4] 安全扫描 · haiku
    │   ├── sqlite-reviewer.md             #   [Phase 4] SQLite 审查 · haiku
    │   └── system-architect.md            #   [Phase 2] 架构设计 · sonnet
    │
    ├── commands/                          # 用户命令(20 个)
    │   ├── h-archive.md                   #   [Phase 6] 知识归档入口
    │   ├── h-collab.md                    #   [协作] 生成团队协作交付物
    │   ├── h-collab-update.md             #   [协作] 记录外部团队反馈
    │   ├── h-decompose.md                 #   [规划] PRD→INVEST 子任务分解
    │   ├── h-design.md                    #   [Phase 2] 结构化设计步骤
    │   ├── h-distill-from-code.md         #   [知识] 代码→Wiki 知识对账
    │   ├── h-explore.md                   #   [Phase 1] 需求探索入口
    │   ├── h-fix-bug.md                   #   [修复] Bug 修复管线(根因优先)
    │   ├── h-from-ticket.md               #   [转换] Ticket→openspec 管线
    │   ├── h-generate.md                  #   [开发] 脚手架代码生成
    │   ├── h-impact.md                    #   [分析] 改动影响范围分析
    │   ├── h-implement.md                 #   [Phase 4] 代码实现入口
    │   ├── h-incident.md                  #   [运维] 生产事故记录
    │   ├── h-legacy-code.md               #   [分析] 遗留代码理解工具
    │   ├── h-openspec.md                  #   [Phase 2] openspec 脚手架生成
    │   ├── h-propose.md                   #   [Phase 2] 方案设计入口
    │   ├── h-qa.md                        #   [Phase 5] 质量验证入口
    │   ├── h-research.md                  #   [研究] RESEARCH 模式入口
    │   ├── h-runbook.md                   #   [诊断] 客户异常诊断恢复
    │   └── h-status.md                    #   [管理] 任务状态概览
    │
    ├── rules/                             # 规则系统(6 个 SSOT)
    │   ├── lifecycle.md                   #   [SSOT] Phase 流程 + 角色矩阵 + 防失控上限
    │   ├── risk-profiles.md               #   [SSOT] 风险分级 + 危险关键词 + Triage Probe
    │   ├── policy.md                      #   [SSOT] 安全约束 + Hook + Commit + 反膨胀
    │   ├── dispatch-template.md           #   [SSOT] 子 Agent 派发协议 + 5-section Prompt
    │   ├── skill-precedence.md            #   [SSOT] 技能 Zone + 挂载规则 + Phase 对应
    │   └── wal-policy.md                  #   [SSOT] WAL 触发条件 + 格式 + 置信度+证据标签
    │
    ├── scripts/                           # 自动化脚本(18 个)
    │   ├── triage_probe.py                #   [入口] 5 信号风险自动评分
    │   ├── code_smell_detector.py         #   [质量] C# 代码坏味自动检测(10 类)
    │   ├── harness/
    │   │   └── engine.py                  #   [引擎] 工作流引擎
    │   ├── gates/
    │   │   ├── ambiguity_gate.py          #   [门禁] 需求歧义检查
    │   │   ├── delivery_capsule_gate.py   #   [门禁] 交付胶囊格式验证
    │   │   ├── focus_card_gate.py         #   [门禁] 范围卡片格式验证
    │   │   ├── run.py                     #   [门禁] 运行相关
    │   │   ├── scope_guard.py             #   [门禁] 修改范围检查
    │   │   ├── secrets_linter.py          #   [门禁] 密钥泄露检测
    │   │   ├── skill_index_linter.py      #   [门禁] Skill 索引一致性
    │   │   ├── wal_template_gate.py       #   [门禁] WAL 模板验证
    │   │   └── writeback_gate.py          #   [门禁] 写回完整性检查
    │   ├── tools/
    │   │   ├── archive_session_artifacts.py # [工具] 会话产物归档
    │   │   └── librarian_gc.py            #   [工具] 知识库垃圾回收
    │   └── wiki/
    │       ├── anti_bloat_check.py        #   [维护] 反膨胀阈值检查
    │       ├── compactor.py               #   [维护] WAL 碎片压缩
    │       ├── graph_checker.py           #   [维护] 知识图谱完整性
    │       ├── pref_tag_checker.py        #   [维护] 首选项标签检查
    │       ├── schema_checker.py          #   [维护] OpenSpec schema 验证
    │       ├── wiki_linter.py             #   [维护] Wiki 死链+行数检查
    │       └── zero_residue_audit.py      #   [维护] 零残留审计
    │
    ├── runs/                              # 运行记录
    │   ├── _active.json                   #   [状态] 当前活跃+中断+待审批任务队列
    │   ├── 20260520_173700_svs_wordlist_fix/
    │   │   └── current_task.md            #     任务清单快照
    │   ├── 20260520_181500_workflow_isolation/
    │   │   └── current_task.md
    │   ├── 20260527_155959_asr_svs_ctc_collapse_itn_fix/
    │   │   ├── current_task.md
    │   │   ├── focus_card.md              #     范围卡片(Phase 1)
    │   │   └── openspec.md                #     设计文档(Phase 2)
    │   ├── 20260527_174457_asr_gpu_probe_diagnostic/
    │   │   └── current_task.md
    │   ├── 20260528_115738_asr_business_param/
    │   │   └── current_task.md
    │   └── 20260528_122000_workflow_refactor_java_harness/
    │       ├── current_task.md
    │       ├── delivery_capsule.md        #     交付胶囊(Phase 5)
    │       ├── explore_report.md          #     需求报告(Phase 1)
    │       ├── focus_card.md
    │       └── openspec.md
    │
    ├── skills/                            # C# 编码技能(31 个)
    │   ├── csharp-architecture-designer/  #   [Zone A·L1] 模块边界·命名空间·GC 策略
    │   ├── csharp-engineering-standards/  #   [Zone A·L2] 编码核心标准(必装)
    │   ├── csharp-data-governance-standards/ # [Zone A·L3] SQLite 数据治理
    │   ├── csharp-task-planner/           #   [Zone A·L4] 任务拆解·接口优先
    │   ├── csharp-code-review/            #   [Zone B] 代码审查清单
    │   ├── csharp-build-resolver/         #   [Zone D] 编译错误诊断
    │   ├── csharp-result-pattern/         #   [Zone N] Result 单子模式
    │   ├── csharp-structured-logging/     #   [Zone N] 结构化日志·Serilog
    │   ├── csharp-audit-trail/            #   [Zone N] 审计追踪·变更记录
    │   ├── csharp-unit-testing/           #   [Zone N] xUnit+NSubstitute
    │   ├── csharp-options-pattern/        #   [Zone N] 强类型配置
    │   ├── csharp-fluent-validation/      #   [Zone N] 声明式输入校验
    │   ├── csharp-domain-entity/          #   [Zone N] DDD 实体·WinForms 兼容
    │   ├── csharp-repository-pattern/     #   [Zone N] SQLite Repository
    │   ├── csharp-pipeline-behaviors/     #   [Zone N] 装饰器链·横切关注点
    │   ├── csharp-background-jobs/        #   [Zone N] Quartz.NET 调度
    │   ├── csharp-winforms-patterns/      #   [Zone N] MVP·UserControl·CefSharp
    │   ├── csharp-debugging-guide/        #   [Zone N] 死锁·内存泄漏·SQLite 锁
    │   ├── legacy-code-understanding/     #   [Zone N] God Class 分析·Strangler Fig
    │   ├── csharp-multithreading-pitfalls/#   [Zone N] 线程安全·死锁
    │   ├── csharp-dependency-injection/   #   [Zone N] DI·构造函数注入
    │   ├── csharp-event-bus-pattern/      #   [Zone N] 进程内事件总线
    │   ├── csharp-plugin-architecture/    #   [Zone N] 反射加载·插件契约
    │   ├── csharp-network-io-guidelines/  #   [Zone N] HttpClient·超时·重试
    │   ├── csharp-resource-governor-pattern/ # [Zone N] 资源治理·线程池
    │   ├── csharp-platform-module-dev/    #   [Zone N] 平台模块基类继承
    │   ├── csharp-gui-guidelines/         #   [Zone N] 控件释放·UI 封送
    │   ├── prd-review-checklist/          #   [Zone N] PRD 完整性 7 维检查
    │   ├── tech-selection-framework/      #   [Zone N] 选型评分矩阵
    │   ├── failure-memory-helper/         #   [Zone M] 失败经验自动记录
    │   └── linter-severity-standard/      #   [Zone M] FAIL/WARN/IGNORE 标准
    │
    └── wiki/                              # 知识库(核心)
        ├── KNOWLEDGE_GRAPH.md             #   [索引] 知识索引总表·WAL 索引
        ├── purpose.md                     #   [说明] 系统设计目标
        │
        ├── schema/                        # 文档格式规范
        │   ├── index.md                   #     规范索引
        │   ├── openspec_schema.md         #     openspec.md 标准模板
        │   ├── adr_schema.md              #     ADR 决策记录模板
        │   ├── slim_spec_schema.md        #     轻量级 Spec 模板
        │   └── subagent_contract_schema.md #    子 Agent 返回格式契约
        │
        ├── templates/                     # 可复用模板
        │   ├── README.md                  #     模板使用说明
        │   ├── current_task.template.md   #     current_task.md 模板
        │   ├── explore_report.template.md #     explore_report.md 模板
        │   ├── focus_card.template.md     #     focus_card.md 模板
        │   └── openspec.template.md       #     openspec.md 模板
        │
        ├── archive/                       # 冷存储
        │   └── index.md                   #     归档索引
        │
        └── wiki/wiki/                     # 知识主体
            ├── domain/                    # 业务领域(8 文档 + 13 WAL)
            │   ├── index.md               #     领域索引
            │   ├── anchor_live.md         #     主播·直播·录制·弹幕·WebSocket
            │   ├── ai_analysis.md         #     AI 分析·豆包大模型集成
            │   ├── asr_engine_pipeline.md #     ★权威★ ASR 双引擎架构
            │   ├── asr_module.md          #     历史 ASR 文档(腾讯云)
            │   ├── asr_sensevoice_small.md #    SenseVoiceSmall ONNX 推理
            │   ├── asr_local_engine_evaluation.md # 本地引擎可行性评估
            │   ├── asr_consumers.md       #     ASR 三大消费方映射
            │   ├── business_logic.md      #     BLL 层 20+ 业务类
            │   ├── infrastructure.md      #     CefSharp·HttpListener·SQLite
            │   ├── platform_integration.md #    6 个平台模块集成
            │   └── wal/                   #    领域 WAL(13 条)
            │       ├── 20260514_asr_local_engine_domain.md
            │       ├── 20260515_asr_engine_fixes_domain.md
            │       ├── 20260515_asr_robustness_rules.md
            │       ├── 20260515_naudio_wasapi_missing_rules.md
            │       ├── 20260515_onnx_seqlen_type_fix_rules.md
            │       ├── 20260518_asr_language_normalize_api.md
            │       ├── 20260518_asr_language_normalize_domain.md
            │       ├── 20260518_asr_merge_words_v2_rules.md
            │       ├── 20260518_asr_refactor_rules.md
            │       ├── 20260519_asr_gapms_offset_domain.md
            │       ├── 20260521_asr_audio_text_sync_domain.md
            │       ├── 20260526_audio_voice_enhancement_domain.md
            │       └── 20260526_svs_cif_underfire_fallback_domain.md
            │
            ├── api/                       # API 文档(6 文档 + 3 WAL)
            │   ├── index.md               #     路由索引
            │   ├── ai_api.md              #     AI 分析 API(14+2 endpoints)
            │   ├── anchor_api.md          #     主播管理 API(36+ endpoints)
            │   ├── shortvideo_api.md      #     短视频 API(10 endpoints)
            │   ├── system_config_api.md   #     系统配置 API(24+ endpoints)
            │   ├── video_analysis_api.md  #     视频分析 API(43+ endpoints)
            │   └── wal/                   #     API WAL(3 条)
            │
            ├── data/                      # 数据模型(4 文档)
            │   ├── index.md               #     数据索引
            │   ├── sqlite_tables.md       #     13 个 .db 文件·表结构
            │   ├── entity_models.md       #     VideoEntity·VideoSliceEntity
            │   └── tech_debt_register.md  #     技术债务登记册(16 条)
            │
            ├── architecture/              # 架构文档(3 文档 + 1 ADR + 1 WAL)
            │   ├── index.md               #     架构索引
            │   ├── project_overview.md    #     项目全貌·技术栈·模块图
            │   ├── adr/
            │   │   └── ADR-001-workflow-refactor-java-harness.md # ADR-001
            │   └── wal/
            │       └── 20260528_workflow_refactor_java_harness_architecture.md
            │
            ├── preferences/               # 编码规范(6 文档 + 15 WAL)
            │   ├── index.md               #     规范索引
            │   ├── coding_standards.md    #     C# 命名·async·异常·资源
            │   ├── architecture_rules.md  #     分层·DI·类设计·线程安全
            │   ├── platform_module_rules.md #   平台模块基类继承规范
            │   ├── security_rules.md      #     SQL 注入·密钥·数据校验
            │   ├── performance_rules.md   #     HTTP·线程·DB·缓存性能
            │   └── wal/                   #     规则 WAL(15 条)
            │
            ├── reviews/                   # 审查记录(4 文档)
            │   ├── index.md               #     审查索引
            │   ├── code_review_20260423.md #    全项目代码审查(16 发现)
            │   ├── architecture_maintainability_review_20260423.md # 架构审查(9 发现)
            │   └── fix_plan.md            #     修复计划(24 issue·126h)
            │
            ├── specs/                     # 设计提案(10 openspec)
            │   ├── index.md               #     提案索引
            │   ├── 20260513_asr_local_engine_prd.md
            │   ├── 20260513_asr_local_engine_unified.md
            │   ├── 20260513_asr_local_engine_whitepaper.md
            │   ├── 20260514_asr_local_engine_openspec.md
            │   ├── 20260526_asr_cross_chunk_word_merge.md
            │   ├── 20260526_ffmpeg_f1_f3_tuning.md
            │   ├── 20260526_fsmn_vad_smart_slicing.md
            │   ├── 20260526_svs_cif_underfire_fallback.md
            │   ├── 20260526_svs_confidence_threshold.md
            │   └── 20260526_video_convert_remux_first.md
            │
            └── testing/                   # 测试标准(1 文档)
                └── index.md               #     测试策略·门禁·证据模板
```

### 统计汇总

| 类别 | 数量 |
|---|---|
| CLAUDE.md | 1 |
| Rules (规则) | 6 |
| Commands (命令) | 20 |
| Agents (智能体) | 10 |
| Skills (技能) | 31 |
| Scripts (脚本) | 18 |
| Wiki 文档 | 66 |
| Wiki WAL 条目 | 32 |
| 设计提案 (Specs) | 10 |
| Run 运行记录 | 6 |
| Schema/Template | 9 |
| **总计** | **~205 个文件** |

---

## 十三、速查卡片

### 常用命令速查

| 想要做什么 | 命令 |
|---|---|
| 开始新功能 | `/h-openspec <slug>` |
| 修复 Bug | `/h-fix-bug <描述>` |
| 诊断客户问题 | `/h-runbook "<症状>"` |
| 分析改动影响 | `/h-impact <目标>` |
| 生成代码模板 | `/h-generate <EntityName>` |
| 理解遗留代码 | `/h-legacy-code <ClassName>` |
| 分解需求 | `/h-decompose <slug> <文件>` |
| 查看任务状态 | `/h-status` |
| 对齐 Wiki 与代码 | `/h-distill-from-code <scope>` |

### 风险等级速查

| 风险 | 流程 | 是否需要审批 |
|---|---|---|
| TRIVIAL | 直接改 | 否 |
| LOW | Explorer→Implement→QA | 否 |
| MEDIUM | 完整 STANDARD 流程 | 可选 |
| HIGH | 完整 STANDARD + ADR | **必须** |

### 文档产出速查

| Phase | 产出文件 |
|---|---|
| Phase 1 Explorer | focus_card.md + explore_report.md |
| Phase 2 Propose | openspec.md (+ ADR if HIGH) |
| Phase 3 Review | 检查结论 |
| Phase 4 Implement | .cs 文件修改 |
| Phase 5 QA | delivery_capsule.md + dotnet build PASS |
| Phase 6 Archive | WAL 知识片段 + 更新 KNOWLEDGE_GRAPH.md |
