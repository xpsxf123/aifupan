---
description: 记录外部团队（前端/三方/QA/Ops）对 /h-collab deliverable 的反馈；可 --signoff 解除 Implement 阻断。跨会话靠 slug 定位状态文件。
argument-hint: <slug> [--signoff] [--reviewer <name>]
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
---

输入：`$ARGUMENTS`

前置：`/h-collab <slug>` 已创建 `<run_dir>/collabs/<YYYYMMDD>_<slug>_collab.md` 状态文件。

## 步骤

### 1. 解析参数

- `<slug>` 必填，缺 → 输出 `Pass the slug as first argument.` 停。
- `--signoff` 可选：标记 SIGNED_OFF + 从 launch_spec Artifact 列去掉 COLLAB marker。
- `--reviewer <name>` 可选：记录评审方（如 `frontend-team` / `payment-vendor`）。

### 2. 定位 collab 状态文件

```bash
find .claude/runs/collabs -name "*_<slug>_collab.md" 2>/dev/null | sort | tail -1
```

找不到 → 输出 `No collab found for slug <slug>. Run /h-collab <slug> first.` 停。

读 YAML frontmatter 抓：`status` / `type` / `deliverable` / `task_brief` / `open_questions` / `feedback_log`。再读 `deliverable` 路径下的文件内容以便后续按反馈改。

### 3. 收反馈（非 --signoff 时）

`--signoff` 已显式给 → 跳到 step 5 直接 SIGNED_OFF。

否则 `AskUserQuestion`：

> 外部团队的反馈是哪种？（可多选）

| 选项 | 含义 |
|---|---|
| Approved as-is | 原样认可 → 隐含 --signoff，问 reviewer 名字 |
| Questions / clarifications | 提了问题 → 逐个 ask 是否当场作答 |
| Requested changes | 要求改 deliverable → 问描述，问是否现在改 |
| Blocker | 提了阻断点 → 记 BLOCKER，提示 Implement 不应继续 |

按选项分支收集内容。

### 4. 应用反馈

| 反馈类型 | 动作 |
|---|---|
| Approved | reviewer 名 + signed_off_date 写进状态文件 |
| Questions（当场作答） | Q&A 对追加进 feedback_log |
| Questions（暂留） | 加进 `open_questions` 列表 |
| Changes（同意改）| Edit deliverable 文件按用户描述改；同步更新 deliverable 末尾 `## Open Questions` |
| Changes（拒绝改）| 记录为 open change request 进 feedback_log |
| Blocker | severity=BLOCKER 进 feedback_log，状态置 `BLOCKED` |

### 5. 更新 collab 状态文件

`Edit` 追加进 `feedback_log` 数组：

```yaml
- date: <YYYY-MM-DD>
  reviewer: <name 或 "unknown">
  summary: <一行：本次反馈核心>
  questions_added: <N>
  changes_applied: yes | no
  blockers: <描述 或 "none">
```

同步刷新 `open_questions`（加新的、删已解决的）。

若 `--signoff` 或 "Approved as-is"：

```yaml
status: SIGNED_OFF
signed_off_by: <reviewer>
signed_off_date: <YYYY-MM-DD>
open_questions: []
```

### 6. 更新 launch_spec（仅 SIGNED_OFF）

最新 `launch_spec_*.md` 找 `<slug>` 行，从 Artifact 单元格内移除 `<br>COLLAB:<YYYYMMDD>-<slug>` 后缀（与 `/h-collab` step 6 写入对称）。Status 保持 IN_PROGRESS。

### 7. 输出

```
[Collab Update] slug=<slug> | reviewer=<name 或 -> | status=<PENDING_REVIEW|SIGNED_OFF|BLOCKED>
  feedback_rounds=<N total> | open_questions=<K remaining> | deliverable_changes=<applied|none>

如果 SIGNED_OFF：
  COLLAB marker 已从 launch_spec 去除。Next: 继续 Implement 或 /h-resume。

如果 PENDING_REVIEW：
  <K> 个 open question 待答。Next: 把更新后的 deliverable 再传给外部团队，回执后再 /h-collab-update <slug>。

如果 BLOCKED：
  Blocker: <描述>
  Next: 与外部团队解决阻断点；本任务 Implement **不应继续**直到解除。
```

## 硬约束

- 可改：`.claude/runs/collabs/*_<slug>_collab.md`、`<deliverable>`（用户同意改时）、launch_spec Artifact 列（仅 sign-off 时去 COLLAB marker）。
- 禁源码改、禁 task_brief / openspec 改。
- BLOCKED 不动 launch_spec Status，仅在 Report 里明示。
- 改 deliverable 必须用户显式同意，禁静默改。
- 不重跑。
