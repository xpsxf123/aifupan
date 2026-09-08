# ADR Schema — Architecture Decision Record

借鉴 java-harness-agent 的 ADR 设计。**HIGH 风险任务必须产出 ADR**（与完整 openspec.md 并列），记录架构决策的上下文、考虑过的替代方案、最终选择的理由。

## 何时写 ADR

| 条件 | 写 ADR |
|---|---|
| Risk = HIGH（含 danger 关键字 / ABI / IPC 破坏 / 跨域） | ✅ **强制** |
| Scenario B（ABI 破坏）/ Scenario C（IPC 破坏）| ✅ **强制** |
| 引入新的 NuGet 依赖 | ✅ **强制** |
| MEDIUM 风险但涉及重要设计决策（如换通信模式 / 换缓存策略） | 推荐 |
| LOW / TRIVIAL | 否 |

## 模板

```markdown
# ADR-NNN: <Short Title>

- **Status**: `Proposed | Accepted | Deprecated | Superseded by ADR-XXX`
- **Date**: YYYY-MM-DD
- **Run ID**: `<YYYYMMDD_HHMMSS>_<slug>`
- **Risk**: HIGH

## Context
<决策的背景——为什么现在需要这个决策？外部约束是什么？业务驱动是什么？2-5 句话>

## Decision
<决策的精确陈述。"我们将采用 X 方案，原因是 Y 和 Z"。一段话>

## Consequences

### Positive
- <好处 1>
- <好处 2>

### Negative
- <坏处 1>
- <坏处 2>

### Neutral / Trade-offs
- <中性影响>

## Alternatives Considered

### Alternative A: <方案 A>
- **优点**: ...
- **缺点**: ...
- **被否决理由**: ...

### Alternative B: <方案 B>
- **优点**: ...
- **缺点**: ...
- **被否决理由**: ...

## Related ADRs
- [[ADR-XXX]] — <一句话关系>

## Related Specs
- [[../../runs/<run_id>/openspec.md]]

## Validation Criteria
- AC1（来自 openspec.md）：<可测试>
- 设计验证：<如何确认决策是对的>

## Rollback
- 如果决策被证明错误，如何回退？
- 单 git revert 是否足够？还是需要数据迁移？

[Confidence: HIGH | MEDIUM | LOW]
[Evidence: <openspec.md path / commit hash / test output>]
```

## 命名规范

```
.claude/wiki/wiki/architecture/adr/
├── ADR-001-workflow-refactor-java-harness.md
├── ADR-002-asr-engine-cif-fallback.md
└── ADR-003-...
```

ADR 编号 **全局递增**，跨 run 共享。

## 实际示例

```markdown
# ADR-001: 引入 java-harness-agent 风格 .claude/ 目录结构

- **Status**: Accepted
- **Date**: 2026-05-28
- **Run ID**: 20260528_122000_workflow_refactor_java_harness
- **Risk**: HIGH

## Context
原工作流文档分散在 `AGENTS.md` + `CLAUDE.md` + `.claude/runs/` + `.claude/rules/`，
存在重复定义、Phase × Role 映射散文化、Skill 无 Zone 分组等问题。
java-harness-agent 仓库示范了 `.claude/{rules,agents,wiki,skills,commands}` 单一结构。

## Decision
完全照搬 java-harness-agent 的目录结构。`.claude/` 成为权威 SSOT 入口，
`.claude/` 保留为底层物理存储（含 16 WAL + 23 skill + scripts），用符号链接桥接两入口。
CLAUDE.md 合并 AGENTS.md 成为单一入口。

## Consequences

### Positive
- 与 java-harness-agent 范式对齐，未来跨项目复用容易
- Skill 按 Zone 分组后互斥规则清晰，减少重复触发
- TRIVIAL 4 级风险减少小改动的仪式负担

### Negative
- 双入口（物理 + 符号链接）初期可能引起混淆
- Windows 端需要 git config core.symlinks=true
- 16 WAL 引用路径有 2 种写法（agents/llm_wiki vs .claude/wiki）

### Neutral / Trade-offs
- 文档总行数增加约 30%（新增 rules/agents/commands）
- 学习成本：现有协作者需熟悉新入口

## Alternatives Considered

### Alternative A: 渐进增强
保留 `.claude/runs/` 结构，只增量加入 5 个 java 高收益设计。
- **被否决**：用户明确选择"完全照搬"，且渐进方案不解决根本问题（如散文化 Phase × Role）

### Alternative B: 中度结构化
仅迁移 rules 部分，agents 保留为 role_matrix.json 中心化。
- **被否决**：违背 java-harness 单文件可读性优势

## Related ADRs
- 无前置 ADR

## Related Specs
- [[../../runs/20260528_122000_workflow_refactor_java_harness/openspec.md]]

## Validation Criteria
- AC1-AC14（来自 openspec.md §5）
- AC13: 16 WAL 字节级未变 ✅
- AC4: 符号链接生效 ✅

## Rollback
- 单 commit revert 可恢复（资产层面 `.claude/` 永不丢失）
- 删除 `.claude/{rules,agents,commands}` + `CLAUDE.md.bak` 恢复

[Confidence: HIGH]
[Evidence: openspec.md §3 Design + AC13 shasum 验证通过]
```

## ADR 索引

每个 ADR 写完 **必须** 加到 [.claude/wiki/wiki/architecture/index.md](../wiki/architecture/index.md)（或专门的 `architecture/adr/index.md`）。
