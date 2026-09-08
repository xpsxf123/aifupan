---
description: 只读。定位 IN_PROGRESS 任务，抽 openspec.md 关键字段，输出 Next Action。
argument-hint: [<slice_id 或 launch_spec 路径>]
allowed-tools: Read, Bash, Grep, Glob
---

**只读** — 不 Write、不 Edit、不 dispatch。

## 步骤

### 1. 定位 launch_spec

顺序：

1. `$ARGUMENTS` 给路径 / slice_id。
2. 环境变量 `CLAUDE_ACTIVE_RUN_DIR`。
3. `ls -t .claude/runs/launch_spec_*.md 2>/dev/null | head -1`。

都没 → 输出 `[Resume] No active launch_spec. Start with /h-brief or /h-decompose.` 停。

### 2. 解析 State Machine

读 launch_spec，找 `Status` ≠ `DONE` 的行：

| Status | 含义 |
|---|---|
| `IN_PROGRESS` | 当前任务 — 按 Phase 给 Next Action |
| `WAITING_APPROVAL` | HIGH risk 等批准 — 提示读 Human Section |
| `PENDING` | 未启动 — 列未 DONE 的 depends_on |
| `FAILED` | 卡住 — 透传 `Failed_Reason` |

### 3. 抽 openspec.md 关键字段

从该行 `Artifact/Log` 列读出 `<run_dir>/openspec.md`，**仅抽**：

- frontmatter: `risk`, `frontend-facing`, `module`, `triggers`, `slice_id`
- §1 Context 第一行 Goal
- §7 AC 计数（`grep -c '^- AC-'`）
- §9 ADR 计数（`grep -c '^### ADR-'`）
- focus_card.md 是否 finalized（无 `<TBD>` 字样）

### 4. 推断 Next Action

| Phase | openspec 状态 | Next |
|---|---|---|
| `-` / 空 | 骨架不存在 | `/h-brief "<intent>" [--slice S-XXX]` |
| `1_Explorer` | ticket 入口骨架，无 explore_report.md | 主 agent inline 写 `<run_dir>/explore_report.md`（Spec Inference + 3-5 AC + Allowed Scope 草稿）后 `/h-design`；复杂场景（Tier-2 触发）改派 `@requirement-engineer` |
| `1_Explorer` 且已有 explore_report.md | Explorer 完 | `/h-design [<run_dir>]` |
| `2_Propose` 且 focus_card 含 `<TBD>` | 设计未完 | `/h-design [<run_dir>]` |
| `2_Propose` 且 focus_card finalized | 等 review | `/h-gates --phase 3_Review`（MEDIUM）或人审 + 批准（HIGH） |
| `3_Review` | review 中 | `/h-gates --phase 3_Review`；通过后进 Implement |
| `3.5_Approval` | 等批准 | 让用户读 Human Section 回复"批准"/"拒绝 + 原因" |
| `4_Implement` | 写代码中 | 继续 Implement；完成后 `/h-gates --phase 5_QA` |
| `5_QA` | QA 中 | `/h-gates --phase 5_QA`；通过后 `/h-archive` |
| `6_Archive` | 待归档 | `/h-archive` |

### 5. 输出

```
[Resume]
launch_spec: <path>
active: <slice_id> | run_dir: <…> | phase: <…> | risk: <…> | frontend-facing: <…>
openspec: ACs=N | ADR=K | focus_card=<finalized|TBD>
goal: <§1 第一行>
pending_upstream: <list 或 none>
blocked_reason: <Failed_Reason 或 none>

>>> Next: <一句 + 具体命令>
```

多个非 DONE slice → 先给入度=0 的；其余末尾 `Other pending: [S-002, S-003]` 一行带过。

## 硬约束

- 任何 Write/Edit 调用即视为执行错误。
- 不 dispatch sub-agent。
- 抽字段只读 Machine Section 必要项，禁止带回 §5 Business Logic 全文。
