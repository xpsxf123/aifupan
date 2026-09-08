---
description: 闭环 Archive → MR。跑 pre-PR gate（secrets + scope）→ 从 openspec §1/§7/Plan Deviation 生成云效 MR 的 title + body → 用户到云效 UI 创建 → 回填 URL 到 launch_spec。
argument-hint: [slug] [--base <branch>]
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
---

输入：`$ARGUMENTS`

云效平台无 `gh pr create` 等价 CLI，本命令做的是**生成 + 回填**：跑 gate、组装文案、等用户去云效 UI 粘贴创建、把 MR URL 收回 launch_spec。

前置：`/h-archive` 已跑（含 Plan Deviation Reflection），launch_spec 对应行 Status=`DONE`。

## 步骤

### 1. 起 TaskList + 定位归档后的 openspec

按 [`tasklist-usage.md`](../rules/tasklist-usage.md) **Pattern D** 起 4-task pipeline（强制）：pre-gate / compose body / user create MR / writeback URL。每进入对应 step 前 → in_progress；step 完 → completed。

- `$ARGUMENTS` 给 slug → 找 `.claude/llm_wiki/archive/<YYYYMMDD>_<slug>.md`（按 mtime 倒序取最新匹配）。
- 缺省 → 读最新 `launch_spec_*.md` 取 Status=DONE 且最近的一行，其 Artifact 列指向归档文件。
- 都找不到 → 停 `No archived openspec — run /h-archive first.`。

读出：slug / risk / spec_mode / §1 Context Goal / §7 ACs / Plan Deviation Reflection / frontmatter `ticket_ref` & `ticket_url`（若 /h-from-ticket 起的）。

### 2. 解析剩余参数

- `--base <branch>` 默认 `release`（本项目惯例：feature → release 分支；从最近 git log "Merge #N into release from ..." 推断）。可改成 `master` 若用户指定。

### 3. Pre-PR gates

```bash
# 1. Secrets 扫描（--paths nargs=+ glob list，不能引号包整串）
git diff --name-only HEAD~1 HEAD | xargs -r python3 .claude/scripts/gates/secrets_linter.py --paths
# 2. 工作区干净
git status --porcelain
```

| 检查 | 退出码 | 处理 |
|---|---|---|
| secrets_linter | 0 | 继续 |
| secrets_linter | 1 (WARN) | 透传给用户，`AskUserQuestion` 是否仍继续；不静默 |
| secrets_linter | 2 (FAIL) | 停 `Secrets FAIL — fix before creating MR.` |
| git status | 非空 | 停 `Uncommitted changes detected. Commit before /h-pr.` |

> 本步骤不跑 `scope_guard`（post-archive 阶段 focus_card 已无效）。

### 4. 生成 MR title

格式：`<type>(<scope>): <一句话>`

- `<type>`：按优先级 — (1) 归档 openspec changelog 行前缀（`feat` / `fix` / `refactor` / `chore` / `docs`）；(2) openspec frontmatter `type:` 字段；(3) 都缺 → **AskUserQuestion 4 选 1**（禁静默 default `feat`）：

  | header | options |
  |---|---|
  | `Commit type for MR title?` | `feat (新功能)` / `fix (bug 修复)` / `refactor (重构 / 不改行为)` / `chore/docs (杂项 / 文档)` |
- `<scope>`：**默认读归档 openspec frontmatter `module` 字段**（如 `words` / `order`）。frontmatter 缺 → fallback 读归档 openspec 内嵌的 `## Allowed Scope` 段首个文件路径推 jiuyu 模块名（`replay-words` → `words`）。两者都不行 → `AskUserQuestion` 让用户选模块（禁默认 `general`）。
- `<一句话>`：openspec §1 Context 第一行 Goal，去末尾句号。

长度 cap 72 字符；超 → 截到 68 + `...`。

> 本步骤禁读 focus_card（已归档），只读归档 openspec 的 `## Allowed Scope` 段。`@documentation-curator` 归档时禁删该段。

### 5. 生成 MR body

```markdown
## Summary
<§1 Context 2-3 句，从 Human Section rationale 摘>

## Acceptance Criteria
<§7 ACs 列表，每条用 then 子句简化>
- [x] AC-1: <一句话产出>
- [x] AC-2: <一句话产出>
...

## Plan Deviations
<Plan Deviation Reflection 段全文，无则 "None — implemented as specified.">

## Risk & Rollback
<openspec §6（若有）一句话；HIGH risk 必填回滚步骤；LOW/MEDIUM 可省略>

## Links
<若 ticket_ref + ticket_url 存在：>
- 关联需求：<ticket_url>（#<ticket_ref>）
<否则整段省去>

---
*Generated from `<归档 openspec 相对路径>`*
```

禁塞 Allowed Scope / Hard Constraints / Machine Section 内容。

### 6. 用户确认

显示完整 title + body，`AskUserQuestion`：

> 这个 MR 描述对吗？

| 选项 | 处理 |
|---|---|
| 对，我去云效创建 MR | 进 step 7 |
| 改一下（我描述改动） | follow-up 问改什么，应用，重显示 |
| 取消 | 停，不动 launch_spec |

≤2 轮 edit；第 3 轮 → 停 `Too many edit rounds. Edit the archived openspec directly and re-run /h-pr.`。

### 7. 等用户去云效创建

输出：

```
请在云效 Codeup 创建合并请求：
- Source: <git rev-parse --abbrev-ref HEAD>
- Target: <base>
- Title:
  <title>
- Body（复制粘贴）：
---
<完整 body>
---

创建后把 MR URL 粘贴回来。
```

`AskUserQuestion`：

> 云效 MR 的 URL 是？

收到 URL（必须以 `http` 开头）→ 解析 MR 编号（云效 URL 模式：`https://codeup.aliyun.com/<org>/<repo>/-/merge_requests/<id>` 或类似；从末段 `/merge_requests/(\d+)` 取 N）。提不出编号 → 仍接受 URL，编号字段填 `unknown`。

不粘 URL（用户选"取消"）→ 停，不动 launch_spec。

### 8. 回填

**归档 openspec 末尾追加：**

```markdown
## MR
- URL: <URL>
- Number: !<N>     # 云效习惯用 ! 前缀（GitLab 系）
- Base: <base>
- Created: <YYYY-MM-DD>
- Status: open
```

**launch_spec：** 找对应 DONE 行，Artifact 列单元格内用 `<br>` 软换行追加 `MR !<N>`。**禁追加 ` | `**（破坏 5 列 schema）：

```
| <slug> | DONE | 6_Archive | archive/YYYYMMDD_<slug>.md<br>MR !<N> | - |
```

Status 保持 DONE。

### 9. 输出

```
[PR] slug=<slug> | mr=!<N> | base=<base>
  archived_openspec:  <path>（已追加 ## MR 段）
  launch_spec:        <path>（Artifact 列单元格内 <br> 追加 MR !<N>，不破坏 5 列）
  gates:              secrets=<OK|WARN>
Next: 让 reviewer 在云效审；合入后人工跑下 `/h-resume` 看是否解锁新 slice。
```

## 硬约束

- 可改：归档 openspec（仅追加 `## MR` 段）、launch_spec Artifact 列。
- 禁源码改、禁 wiki 改、禁 rerun gate。
- secrets gate FAIL 直接停。
- 不跑测试。
- 不 push、不 force push。
- edit 轮数 ≤2。
- URL 必须以 `http` 开头。
- Links 段仅在 ticket_ref ∧ ticket_url 都存在时写，禁猜 ticket 编号。
