---
description: 异常 / 日志 / 客服反馈 → 按 _TEMPLATE.md 起 incident 草稿 → 5 点质量自检 → 用户确认 → 写入 incidents/。
argument-hint: <异常 / stack trace / 日志 / 一句话症状>
allowed-tools: Read, Write, Bash, Grep, Glob
---

输入：`$ARGUMENTS`

按 `.claude/llm_wiki/incidents/README.md` 入口 B 协议把外部事故落成 `.claude/llm_wiki/incidents/YYYY-MM-DD__<area>__<slug>.md`。

## 步骤

### 1. 输入检查

`$ARGUMENTS` 空 → 输出 `"Paste an exception / log / customer report after /h-incident; or run @debug if root cause unknown."` 停。

### 2. 字段抽取（不编造）

**先尝试自动抽取：**

| 字段 | 抽取 | 自动可推时的值 / 否则 |
|---|---|---|
| `date` | 日志时间戳 | 推不出 → 今天 |
| `status` | "已修" → fixed / "缓解" → mitigated / 否则 open | 默认 open |
| `area` | `com.jiuyu.replay.<X>` → `replay-<X>` | 推不出 → 进 ask 队列 |
| `severity` | 关键词推断（客户阻断 P0 / 核心 P1 / 部分降级 P2 / 偶发 P3） | 推不出 → 进 ask 队列 |
| `source` | "客服" → customer-report / "线上告警" → prod-alert / "QA" → qa-found / "复盘" → post-mortem | 推不出 → 进 ask 队列 |
| `slug` | 英文 3-6 词，下划线，描述事故本质 | 不确定 → 进 ask 队列 |

**反编造红线**：

- Root cause 不明 → `<待补充：未在日志中找到根因，建议跑 @debug>`
- Reflex 不明 → `<待补充：根因确定后再填>`
- Fix 未知 → `<待补充：…>`

**ask 队列处理：** 队列里的字段分两通道处理：

| 通道 | 字段 | 形态 |
|---|---|---|
| AskUserQuestion 一次调用、多题（≤4 题，每题 2-4 选项） | `severity` / `source` | enum |
| 自然语言一次性合并问 | `area` / `slug` | 自由文本 |

**AskUserQuestion 题目（仅缺失字段入题；都不缺则跳过此调用）：**

| Q# | 何时发 | header | options |
|---|---|---|---|
| Q1 | `severity` 缺 | `Severity` | `P0 (服务不可用 / 客户阻断)` / `P1 (核心功能降级)` / `P2 (部分降级)` / `P3 (偶发 / QA 环境)` |
| Q2 | `source` 缺 | `Source` | `prod-alert` / `customer-report` / `qa-found` / `其他 (post-mortem / rca-archive)` |
| Q3 | 仅当 Q2 选 `其他` 后触发 follow-up | `Source detail` | `post-mortem (复盘会议)` / `rca-archive (archive 中既存 incident 反流)` |

**自然语言 ask（仅 `area` / `slug` 缺时，一次性合并问，禁拆两段）：**

```
> 帮我补两个字段（未在 stack/日志中自动推出）：
> 1. area：影响哪个 jiuyu 模块？（如 replay-words / replay-order；推不准就答"未确认"）
> 2. slug：3-6 个英文词描述事故本质，下划线分隔（如 anchor_task_stuck_running）
```

不允许静默 default 任何 ask 队列字段。

### 3. 起草

读 `.claude/llm_wiki/incidents/_TEMPLATE.md`，按它的 frontmatter + 4 段顺序填。禁止自创字段或调段顺序。

文件名：`incidents/<YYYY-MM-DD>__<area>__<slug>.md`。

### 4. 5 点质量自检（主 agent 自答，不达标回 3 重起草）

1. 标题行 ≤50 字 且 描述事故本质？
2. Root cause 是真根因，不是"重启就好了"？
3. Reflex 含至少一个具体类/方法/文件/约束级检查动作？（不允许"以后小心" / "ensure best practices"）
4. 未来某 LLM 在改 `<area>` 模块代码时被 inject 这条，能反应过来"我现在写的可能踩坑"吗？
5. `<待补充>` 是真的"我推不出来"，不是"我懒得写"？

任何一条 No → 回 3 重起草。任何一条 "不确定" → 留 `<待补充>`。

### 5. 展示 + 确认

**先一次性展示草稿全文（不分段提问）：**

```
[Incident Draft]
file: .claude/llm_wiki/incidents/<filename>.md

---
<frontmatter>
---

# <标题>

## Symptom
<…>

## Root cause
<…>

## Fix / mitigation
<…>

## Reflex
<…>
---

Quality self-check:
  [✓] Title <50 chars, captures essence
  [✓] Root cause is the actual cause
  [✓] Reflex names specific class/method/constraint
  [✓] Future LLM editing <area> would be alerted
  [✓] <待补充> markers honest, not lazy
```

**随后 AskUserQuestion（破坏性写盘确认）：**

| 题 | header | options |
|---|---|---|
| Q1 | `Write incident to disk?` | `写盘 (保存到 incidents/)` / `取消 (放弃草稿)` / `改字段后重展示` |

### 6. 写盘 / 取消 / 改字段

- Q1 选 `写盘` → `Write` 落盘到 `incidents/<filename>.md`。
- Q1 选 `取消` → `[Incident Skipped] draft discarded.`。
- Q1 选 `改字段后重展示` → follow-up AskUserQuestion：

| 题 | header | options |
|---|---|---|
| Q2 | `修改哪个部分?` | `frontmatter (date/area/severity/source/status/slug)` / `正文段 (Symptom/Root cause/Fix/Reflex)` / `标题行` / `重新起草整篇 (回 step 3)` |

- Q2 选前 3 项 → 自然语言一次性问 "改成什么"（用户给具体新值）→ `Edit` 草稿（不写盘）→ 回 step 5 重展示。
- Q2 选 `重新起草整篇` → 回 step 3。

`改字段` 路径 ≤ 2 轮（即 step 5 最多重展示 2 次），第 3 轮仍要改 → 停 `[Incident Skipped] too many revisions, restart with /h-incident.`

### 7. 写盘后

```
[Incident Saved] <file>
If a Change task needed to apply the fix, run /h-brief or /h-decompose.
```

## 何时不用本命令

按 incidents/README.md "不要写"清单：

- 开发自测发现的 bug → 用 archive Fix: 行。
- 重构 / 优化 / 性能调整 → 用 archive PATCH 行。
- 设计阶段 Review 已抓的 → 流程内闭环。

识别出 → 提示用户走对应渠道，不写 incident。

## 硬约束

- 格式严格按 `_TEMPLATE.md`，不许改名 / 重排段。
- 绝不静默写盘；必须用户走 step 5 AskUserQuestion 选 `写盘`（禁 `Y/n` 自然语言）。
- 绝不编造任何字段；不确定 → `<待补充>`。
- 文件名严格 `YYYY-MM-DD__<area>__<short-slug>.md`。
- `severity` / `source` 推不出必须 AskUserQuestion（不静默 default）；`area` / `slug` 推不出走自然语言一次性合并问。
- step 5 → step 3 「重新起草」/ step 5 重展示循环 ≤ 2 轮。
- 一次只处理 `$ARGUMENTS` 给的一条。
