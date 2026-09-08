# Slim Spec Schema — LOW 风险 5 行模板

借鉴 java-harness-agent 的 Slim Spec 设计。用于 **LOW 风险 PATCH** 任务，避免全量 openspec 的仪式负担。

> **Disambiguation.** Slim **Spec** 是 **Phase 2** 的设计契约（事前），用于代码落地前对齐 scope + AC。Slim **WAL** 是 **Phase 6** 的事后稳定知识（一段话总结），详见 [.claude/rules/wal-policy.md §7](../../../.claude/rules/wal-policy.md)。两者命名相近但生命周期不同，不可互换。

## 何时用 Slim Spec

| 条件 | 用 Slim Spec |
|---|---|
| Risk = TRIVIAL (<3 行) | 否（连 spec 都不写，inline 解释） |
| Risk = LOW + 单文件 + 无 cross-cutting | ✅ |
| Risk = MEDIUM 或 HIGH | 否（用完整 openspec.md）|

## 模板

```markdown
# Slim Spec — <task title>

Run ID: `<YYYYMMDD_HHMMSS>_<slug>`
Status: `DRAFT | APPROVED | IMPLEMENTED`
Intent: `Change` | Profile: `@patch` | Risk: `LOW`

- **Goal**: <一句话目标>
- **Scope**: <≤3 文件，列出绝对路径>
- **AC**: <≤3 可测试断言，编号 AC1/AC2/AC3>
- **Rollback**: <如何回滚——单 git revert / 删除单文件等>
```

## 完整示例

```markdown
# Slim Spec — 修复 AnchorBll 空集合访问

Run ID: `20260601_142000_anchor_empty_list_fix`
Status: APPROVED
Intent: Change | Profile: @patch | Risk: LOW

- **Goal**: 当 anchor 列表为空时，避免 IndexOutOfRangeException
- **Scope**: `Bll/Anchor/AnchorBll.cs:243`
- **AC**:
  - AC1: 空列表时返回 default 而非异常
  - AC2: 单元素列表时返回 list[0]（行为不变）
- **Rollback**: `git revert <commit>`
```

## 与完整 openspec.md 的区别

| 字段 | openspec.md (8 节) | Slim Spec (4 字段) |
|---|---|---|
| Context & Motivation | ✅ 必须 | 隐含在 Goal 中 |
| Scope | ✅ 详细 | ✅ ≤3 文件 |
| Design | ✅ 必须 | ❌ 跳过（LOW 不需要） |
| Public contract changes | ✅ 必须 | ❌ 跳过 |
| Acceptance Criteria | ✅ ≥3 项 | ✅ ≤3 项 |
| Risk & Rollback | ✅ 必须 | ✅ Rollback 一句话 |
| Validation Plan | ✅ 必须 | ❌ 隐含为 dotnet build 通过 |
| Approval Gate | ✅ 必须（HIGH/MEDIUM） | ❌ 不需要 |

## 升级触发

如果在 Implement 中发现实际 Blast Radius 超过 Scope，**必须** 立即停下并升级为完整 openspec.md：

```text
[Plan Invalidation]
原 Slim Spec 假设 scope = 1 文件，实际发现需改 X 文件。
升级为完整 openspec.md（@standard，risk MEDIUM），重走 Propose → Approval Gate。
```

## 存放路径

- 草稿：`.claude/runs/<run_id>/slim_spec.md`
- Archive：移到 `.claude/wiki/archive/slim_specs/` 或保留在 run 目录

## 与 WAL 的关系

LOW 风险 Slim Spec 任务 Archive 时 **可选** 写 Slim WAL（一段话）。详 [.claude/rules/wal-policy.md §7](../../../.claude/rules/wal-policy.md)。
