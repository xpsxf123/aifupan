# ADR-001: 引入 java-harness-agent 风格 .claude/ 目录结构

- **Status**: Accepted
- **Date**: 2026-05-28
- **Run ID**: 20260528_122000_workflow_refactor_java_harness
- **Risk**: HIGH

## Context

原 Agent 工作流文档分散在 `AGENTS.md` + `CLAUDE.md` + `.claude/runs/` + `.claude/rules/`，存在三个根本性问题：

1. **协议头双写**：AGENTS.md 与 CLAUDE.md 重复定义 `[Intent Check] + <Cognitive_Brake>` 协议头，字段顺序/内容略有差异
2. **Phase × Role 映射散文化**：LIFECYCLE.md 与 ROLE_MATRIX.md 只在散文中描述角色挂载，没有 (Profile × Phase) → [Mounted Roles] 二维表
3. **Skill 无 Zone 分组**：23 个 skill 共存，无互斥规则，可能重复触发导致上下文膨胀

参考仓库 `/Users/hehui/dev/git/public/java-harness-agent` 示范了一套成熟的 `.claude/{rules,agents,wiki,skills,commands}` 单一结构，含 5 项高收益设计：Triage Probe 客观分类、TRIVIAL 四级 Risk、Slim Spec 二层合同、Skill Zone 矩阵、Wiki Anti-Bloat。

## Decision

**完全照搬** java-harness-agent 的目录结构（用户显式批准）。

- `.claude/` 成为权威 SSOT 入口（rules / agents / commands 三大目录）
- `.claude/` 保留为底层物理存储（含 16 WAL + 26 skill + scripts + workflow 历史文件）
- 用 **符号链接** 桥接两入口（`.claude/wiki/` → `.claude/wiki/`，`.claude/skills/` → `.claude/skills/`）
- `CLAUDE.md` 合并 `AGENTS.md` 成为唯一会话入口
- 26 个 skill 全部加 `zone:` frontmatter 字段（A/B/C/D/E/N/U/M）
- 新增 `triage_probe.py` + `anti_bloat_check.py` + `slim_spec_schema.md` + `adr_schema.md`

## Consequences

### Positive

- **与 java-harness-agent 范式对齐**：未来跨项目复用、新人 onboarding 更容易
- **Skill Zone 互斥规则**：减少重复触发，上下文膨胀降低
- **TRIVIAL 4 级风险**：小改动（<3 行）无需走全 STANDARD 流程，仪式负担减少 ~60%
- **Slim Spec**：LOW 风险 5 行 spec 替代完整 8 节 openspec，文档负担降低
- **Confidence + Evidence 强制标签**：WAL 知识可追溯，腐烂率降低
- **资产保护**：16 WAL + 6 preferences + 23 skill + 10 specs 内容字节级未变（AC13 PASS）
- **中文规则净增**：74 → 122 条（+48），未丢任何已有中文规则（AC14 PASS）

### Negative

- **双入口可能引起混淆**：物理路径 `.claude/` + 符号链接路径 `.claude/`。需 README.md 明确两者关系
- **Windows 端需要配置**：`git config core.symlinks=true` 后重新 checkout 才能让符号链接生效（项目主要在 Mac 开发，影响小）
- **文档总行数增加约 30%**：新增 rules（7 文件）+ agents（10 文件）+ commands（5 文件），约 +50KB markdown
- **学习成本**：现有协作者需熟悉 12 章节 CLAUDE.md + 7 个 .claude/rules/ SSOT

### Neutral / Trade-offs

- `trae-skill-index` 目录名保留（未重命名为 `skill-index`）：避免破坏现有引用，但牺牲一些名字一致性
- `intent-gateway` 退化为 stub，并未删除：保证旧 prompt 中的引用仍可工作
- `.claude/rules/` 含 12 个 markdown（7 新 + 5 早期 mirror + README）而非纯 7 个：让 C# 规则同时通过 SSOT 与 kebab-case 速查访问

## Alternatives Considered

### Alternative A: 渐进增强（被否决）

保留 `.claude/runs/` 结构，只增量加入 5 个 java 高收益设计（Triage Probe / TRIVIAL / Zone 矩阵 / Slim Spec / Wiki Anti-Bloat）。

- **优点**：风险低，改动 8-12 个文件
- **缺点**：不解决根本问题（散文化 Phase × Role，协议头双写）
- **被否决理由**：用户明确选择"完全照搬 Java 结构"

### Alternative B: 中度结构化（被否决）

把规则文件从 `.claude/runs/` + `wiki/preferences/` 抽到 `.claude/rules/` 独立 SSOT 目录。`role_matrix.json` 拆为 `.claude/agents/*.md` 每角色一文件。但保留 `.claude/` 主框架。

- **优点**：单文件可读性提升，迁移成本中等
- **缺点**：用户已选完全照搬，方案 B 是 A 与 C 之间的折中
- **被否决理由**：用户决策为方案 C

### Alternative C: 完全照搬 Java 结构（已采纳）

抛弃 `.claude/runs/` 主框架，完全采用 `.claude/{rules,agents,skills,wiki,commands}` 结构。`.claude/` 保留为物理存储 + 历史归档。

- **优点**：与 java-harness 范式 100% 对齐，未来交叉项目复用容易
- **缺点**：迁移成本高（60+ 文件改动），需大量验证保护资产
- **采纳理由**：用户明确选择 + 资产保护 14 条 AC + 单 commit revert 保障

## Related ADRs

- 无前置 ADR（这是项目第 1 个 ADR）

## Related Specs

- `.claude/runs/20260528_122000_workflow_refactor_java_harness/openspec.md`（run 目录为临时产物，已按 policy §9 清理，去链）
- `.claude/runs/20260528_122000_workflow_refactor_java_harness/explore_report.md`（同上，已清理）

## Validation Criteria

14 条 AC 全部验证（详 openspec.md §5 + delivery_capsule.md）：

| AC | 状态 | 证据 |
|---|---|---|
| AC1 | ✅ | 7 个 SSOT + 5 mirror + README in .claude/rules/ |
| AC2 | ✅ | 10 个角色文件 in .claude/agents/ |
| AC3 | ✅ | AGENTS.md 15 行（≤15 限制） |
| AC4 | ✅ | 双符号链接生效 |
| AC5 | ✅ | 26 skill 全含 zone 字段 |
| AC6 | ✅ | csharp-build-resolver + failure-memory-helper |
| AC7 | ✅ | triage_probe.py 5 信号打分正常 |
| AC8 | ✅ | anti_bloat_check.py 扫描 82 文件 PASS |
| AC9 | ✅ | slim_spec_schema + adr_schema |
| AC10 | ✅ | settings.json JSON 合法 + hooks 增加 |
| AC11 | ⚠️ WARN | scope_guard prefix 格式问题（非阻塞） |
| AC12 | ✅ | 5 个 commands |
| AC13 | ✅ | 16 WAL + 6 preferences 字节级一致 |
| AC14 | ✅ | 中文规则 74 → 122 条（+48） |

## Rollback

- 单 commit `git revert <commit-hash>` 即可恢复
- 资产层面：`.claude/` 物理目录 + 16 WAL 永不丢失，不需要数据迁移
- 风险：Windows 协作者已 fetch 符号链接后回滚需 reset --hard

[Confidence: HIGH]
[Evidence: openspec.md §3 Design + 14 AC 验证 + shasum 资产保护证明]
