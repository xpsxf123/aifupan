---
name: "trae-skill-index"
description: "Skill navigator. List active skills, pick the right sequence per scenario or lifecycle phase."
---

# Trae Skill Index — Central Navigator

42 skills under `.claude/skills/<name>/SKILL.md`. Lazy-load — read a SKILL.md only when you actually need it.

**Scope:** Claude Code may surface external skills (`lark-*`, `claude-api`, `loop`, `schedule`, etc.) globally. Ignore those unless asked by name.

---

## Quick Start — 常见场景

| 场景 | Skill 序列 |
|---|---|
| 任何非平凡输入（PRD / bug / 需求） | `requirement-intake` → 按下表分流 |
| 写新功能（Standard） | `brainstorming` → `task-decomposition-guide` → L1–L7 Java 标准 → `test-driven-development` → `ultraqa` → `remember` |
| 处理 PRD | `product-manager-expert` → `prd-task-splitter` → 新功能流 |
| 修 bug | `systematic-debugging` → `test-driven-development` → `verify` |
| 代码审查 / QA | `code-review-checklist` → `linter-severity-standard` → `ultraqa` |
| HIGH risk / 安全相关 | `security-review-checklist` → `adversarial-review` → `code-review-checklist` → `verify` |
| AI 重写后的清理 | `ai-slop-cleaner` → `code-review-checklist` |
| 记一个设计决策 | `architecture-decision-records` |
| 知识沉淀 | per-change → archive 自动归档；threshold 触发 → `@harvest <module>` 走 `knowledge-harvester` 萃取；跨会话 → `remember` |
| 跨会话失败提示 | `local-code-intelligence`（仅 `failure_memory.py`；UserPromptSubmit hook 自动注入，无需手动调用）|
| 特殊场景（A 应急 / B 迁移 / C 破坏性 API / D 性能 / E 依赖 / DEBUG / EPIC / MAINTENANCE） | 参 `.claude/rules/lifecycle.md` Part 2 |

---

## Lifecycle Phase Map (STANDARD)

完整 Zone A–G 互斥与组合规则 → `.claude/rules/skill-precedence.md`。

| Phase | Role | 默认 Skills |
|---|---|---|
| Pre-Explorer | — | `requirement-intake`（非平凡输入时）|
| Explorer | Requirement Engineer | `brainstorming` → `cognitive-bias-checklist` → `spec-quality-checklist` → `devops-requirements-analysis` （`local-code-intelligence` 通过 UserPromptSubmit hook 自动注入，不在 Explorer 主动链上）|
| Propose | System Architect | `brainstorming` → `architecture-decision-records`（HIGH 强制 ≥2 ADR）→ `task-decomposition-guide` → `writing-plans` → `devops-system-design` → `devops-task-planning` |
| Review | System Architect | `code-review-checklist`（自检）+ HIGH 加 `adversarial-review`（one isolated round）+ `devops-review-and-refactor` |
| Implement | Lead Engineer + Focus Guard | L1–L7 Java 标准（见下 §3）→ `writing-plans` → `systematic-debugging` / `test-driven-development` → `devops-feature-implementation` |
| Cleanup (optional) | Lead Engineer | `ai-slop-cleaner` |
| QA | Code Reviewer | `code-review-checklist` → `linter-severity-standard` → `devops-testing-standard` → AC≥4 或 HIGH 时 `ultraqa` → HIGH 涉敏感面再加 `security-review-checklist` |
| Archive | Documentation Curator | `verify` → `remember`（archive 归档由 `@documentation-curator` 完成；per-module 知识萃取通过 `@harvest` 单独触发，不在 Archive 阶段做）|

---

## Skill 分组

### 1. 入口 & 路由

| Skill | 用途 |
|---|---|
| [requirement-intake](../requirement-intake/SKILL.md) | 任何非平凡输入的前哨：PRD / bug / signal / 安全报告 |
| [product-manager-expert](../product-manager-expert/SKILL.md) | PRD 生成（Mode A）或摄入 → AC + 实现队列（Mode B）|
| [prd-task-splitter](../prd-task-splitter/SKILL.md) | PRD 拆出任务、估时、依赖图 |

### 2. DevOps 生命周期（项目原生编排）

| Skill | 阶段 |
|---|---|
| [devops-lifecycle-master](../devops-lifecycle-master/SKILL.md) | **MASTER** — feature/bugfix 入口编排 |
| [devops-requirements-analysis](../devops-requirements-analysis/SKILL.md) | PDD / SDD 需求分析 |
| [devops-system-design](../devops-system-design/SKILL.md) | FDD / SDD 系统设计 |
| [devops-task-planning](../devops-task-planning/SKILL.md) | 任务拆解 |
| [devops-feature-implementation](../devops-feature-implementation/SKILL.md) | Phase 4 实现 |
| [devops-review-and-refactor](../devops-review-and-refactor/SKILL.md) | 审查与重构 |
| [devops-testing-standard](../devops-testing-standard/SKILL.md) | TDD 阶段 |
| [devops-bug-fix](../devops-bug-fix/SKILL.md) | bug 诊断 / 复现 / 修复 / 验证 |

### 3. Java 后端标准（L1–L7，Implement 阶段层级；Zone A）

| Skill | 用途 |
|---|---|
| [global-backend-standards](../global-backend-standards/SKILL.md) | **MASTER 后端标准索引** — 写任何 Java 之前先读 |
| [java-engineering-standards](../java-engineering-standards/SKILL.md) | 三层架构 / POJO 子包 / 实体审计字段 / 命名 |
| [java-backend-api-standard](../java-backend-api-standard/SKILL.md) | API 设计：禁 path variable / R<T> / @Validated |
| [java-backend-guidelines](../java-backend-guidelines/SKILL.md) | 防守编程 / 内存装配 / PageUtils |
| [utils-usage-standard](../utils-usage-standard/SKILL.md) | 工具类使用规范 |
| [error-code-standard](../error-code-standard/SKILL.md) | 统一错误响应 / 领域异常 |
| [mybatis-sql-standard](../mybatis-sql-standard/SKILL.md) | 反 JOIN / 索引使用 / `#{}` 占位 |
| [java-javadoc-standard](../java-javadoc-standard/SKILL.md) | Javadoc 风格 |
| [checkstyle](../checkstyle/SKILL.md) | K&R / camelCase / 严格 Javadoc |

### 4. 通用推理与思维（移植自 java-harness-agent）

| Skill | 何时用 |
|---|---|
| [brainstorming](../brainstorming/SKILL.md) | Explorer / Propose — 方案生成 |
| [task-decomposition-guide](../task-decomposition-guide/SKILL.md) | Propose / Review — 通用任务拆解（不依赖 PRD）|
| [writing-plans](../writing-plans/SKILL.md) | Propose / Implement — 写实施计划 |
| [systematic-debugging](../systematic-debugging/SKILL.md) | DEBUG / QA — 系统化根因分析 |
| [test-driven-development](../test-driven-development/SKILL.md) | Implement — Red → Green → Refactor |
| [architecture-decision-records](../architecture-decision-records/SKILL.md) | 架构决策；HIGH 风险 ≥2 ADR 强制 |
| [cognitive-bias-checklist](../cognitive-bias-checklist/SKILL.md) | Propose — 防认知偏差漂移 |
| [spec-quality-checklist](../spec-quality-checklist/SKILL.md) | openspec 自检 |
| [local-code-intelligence](../local-code-intelligence/SKILL.md) | 跨会话失败提示（`failure_memory.py`，UserPromptSubmit hook 自动注入） |
| [adversarial-review](../adversarial-review/SKILL.md) | HIGH Review — 一轮对抗式审查 |

### 5. QA & 验证

| Skill | 用途 |
|---|---|
| [code-review-checklist](../code-review-checklist/SKILL.md) | **MANDATORY** 代码审查清单 |
| [linter-severity-standard](../linter-severity-standard/SKILL.md) | FAIL / WARN / IGNORE 等级与 bypass 协议 |
| [ultraqa](../ultraqa/SKILL.md) | AC ≥ 4 或 HIGH 风险 QA |
| [verify](../verify/SKILL.md) | Archive 前最后一道验证 |
| [security-review-checklist](../security-review-checklist/SKILL.md) | HIGH 风险触及 auth / data / secrets |

### 6. 知识与归档

| Skill | 用途 |
|---|---|
| [remember](../remember/SKILL.md) | Archive — 跨会话记忆沉淀 |
| [api-documentation-rules](../api-documentation-rules/SKILL.md) | API 文档落地（与 wiki/api/ 对齐）|
| [database-documentation-sync](../database-documentation-sync/SKILL.md) | DB 文档落地（与 wiki/data/ 对齐）|
| [ai-slop-cleaner](../ai-slop-cleaner/SKILL.md) | AI 重写后的死代码 / 重复 / 过度抽象清理 |

### 7. Meta

| Skill | 用途 |
|---|---|
| [skill-creator](../skill-creator/SKILL.md) | 新增 SKILL.md（会提示在此索引注册）|
| [skill-graph-manager](../skill-graph-manager/SKILL.md) | **MANDATORY** skill 知识图双向链接维护 |
| [trae-skill-index](./SKILL.md) | This file |

---

## Related

- `.claude/agents/` — 角色目录；每个 agent 列出它依赖哪些 skill
- `.claude/rules/lifecycle.md` — 各 phase 何时触发
- `.claude/rules/skill-precedence.md` — Zone A–G 完整 phase × skill 映射与互斥规则
