---
description: Plan Deviation Reflection → @documentation-curator 归档 openspec.md → wiki_linter → 翻 launch_spec DONE → 提示 @harvest（不自动触发）。
argument-hint: [<run_dir 或 slice_id>]
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Agent
---

前置：Implement 完成 + `/h-gates --phase 5_QA` PASS（或 WARN 显式接受）。

## 步骤

### 1. 定位 run_dir + openspec.md

复用 `/h-resume` 步骤 1 的定位逻辑。校验：

- `<run_dir>/openspec.md` 存在。
- launch_spec 对应行 Phase ∈ {`5_QA`, `6_Archive`}；否则停 → 让用户先 `/h-gates --phase 5_QA`。

### 2. Plan Deviation Reflection（必问用户）

显式问：

> 本次实现相对最初计划是否有偏离？如有，1–3 行说明：
> 1. 偏离的事实（与 openspec.md 原约定不同的具体点）
> 2. 偏离的原因（新发现 / 计划有 bug / 范围扩张）
> 3. 偏离的代价（多花时间 / 触发 Boundary Exception / 改了 SQL/接口）

用户回复**全文**追加到 `<run_dir>/openspec.md` 末尾：

```markdown
## Plan Deviation Reflection
<date: YYYY-MM-DD>

**Deviation:** <…>
**Reason:** <…>
**Cost:** <…>
```

无偏离 → 写 `## Plan Deviation Reflection\nNone — execution matched plan.`。

### 3. 派 `@documentation-curator`

按 `dispatch-template.md` 派。Inputs：

```
- run_dir: <run_dir>
- openspec: <run_dir>/openspec.md
- archive_target_pattern: .claude/llm_wiki/archive/YYYYMMDD_<slug>.md
- archive_index: .claude/llm_wiki/archive/index.md
- launch_spec: <path>
- launch_spec_row_intent: <slug 或 slice_id>
```

`@documentation-curator` 做：

1. `<run_dir>/openspec.md` → `archive/<YYYYMMDD>_<slug>.md`
2. 追加 changelog 行到 `archive/index.md`
3. 跑 `delivery_capsule_gate.py`（如果产了 capsule）
4. 跑 `wiki_linter.py`

也可直接调 `python3 .claude/scripts/tools/archive_session_artifacts.py --run-dir <run_dir> --slug <slug>`（同时归档 openspec / focus_card / current_task / explore_report / launch_spec）。`--run-dir` 必填，传具体 `.claude/runs/Change__<ts>/`，缺则脚本去 `.claude/runs/` 父目录找文件失败。

校验返回：`subagent_return_gate.py --task-kind extract`。

### 4. 按 trigger 追加 curator

- 改了 `.claude/rules/` / `.claude/scripts/` / `.claude/agents/` / `.claude/skills/` 或 openspec 含 architecture trigger → 派 `@architecture-curator`（写 `wiki/architecture/<topic>.md`）。
- `git diff --name-only HEAD~..HEAD .claude/skills/` 有输出 → 派 `@skill-graph-curator`（同步 `trae-skill-index/SKILL.md`）。

### 5. wiki_linter 二次保险

```bash
python3 .claude/scripts/wiki/wiki_linter.py
```

| exit | 处理 |
|---|---|
| 0 PASS | 继续 step 6 |
| 2 FAIL | 输出失败行，建议用户跑 `@librarian` / `@knowledge-architect`，不自动修；**不阻断 step 6 翻 DONE**（wiki 健康异步处理） |

（脚本仅 0/2 两档，无 WARN）

### 6. 翻 launch_spec DONE

用 `Edit` 把对应行：

- `Status` → `DONE`
- `Phase` → `6_Archive`
- `Artifact/Log` → 归档后 `archive/YYYYMMDD_<slug>.md`

### 7. Harvest 提示（不自动触发）

```bash
python3 .claude/scripts/wiki/harvest_threshold.py --all
```

exit 1 → 输出 `Modules ripe for harvest: <X>, <Y>. Run @harvest <module> when convenient.`。

### 8. 输出

```
[Archive Done]
  archived: archive/YYYYMMDD_<slug>.md
  launch_spec: <path>  (<slug|S-XXX> → DONE)
  wiki_linter: PASS|WARN|FAIL
  harvest_hint: <none | "ripe: X, Y">
Next: /h-resume (查 launch_spec 是否解锁新 slice) 或 /h-decompose 起新任务。
```

## 硬约束

- 必问 Plan Deviation；不允许跳过。
- openspec 移动只由 `@documentation-curator` 或 `archive_session_artifacts.py` 做。
- archive 文件禁止手工 `rm`（`policy.md` Part 3：only `@knowledge-harvester` may delete）。
- launch_spec 翻 DONE 用 `Edit`，禁止 Write 覆盖。
- 禁止自动派 `@knowledge-harvester`（破坏性合并，须用户显式批准）。
- wiki_linter FAIL 禁止自动跑 `@librarian`（anti-loop）。
