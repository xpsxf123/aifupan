---
description: 转云效 ticket / 需求 / 反馈为 openspec.md 骨架 + launch_spec PENDING 行；/h-brief 模糊起点前置入口。
argument-hint: <source> <ticket-ref-或-粘贴-描述> [--slim] [--risk low|medium|high]
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Agent
---

输入：`$ARGUMENTS`

`/h-brief` 假设意图已清晰；本命令处理"刚从云效 / 客服 / 反馈来"的模糊原始输入，跑 ambiguity gate 后产生 openspec 骨架。

## 步骤

### 1. 解析参数

- `<source>` 必填：`yunxiao` / `manual`。其他值停。
  - `yunxiao`：MVP 不支持自动 fetch，退化等同 `manual`，提示用户从云效 UI 复制需求内容粘贴进来。
  - `manual`：直接粘贴。
- `<ticket-ref-或-描述>` 必填：云效 ticket ID（如 `#PROJ-1234`）或一段任意描述。
- `--slim`：强制 LOW risk + SLIM spec。
- `--risk low|medium|high`：显式覆盖。

任一必填缺失 → 停 `Usage: /h-from-ticket <yunxiao|manual> <ticket-ref-or-desc> [--slim] [--risk ...]`。

冲突检查：在最新 `.claude/runs/launch_spec_*.md` 中 `grep -F "| <kebab-ticket-ref> |"` 或扫 `.claude/runs/Change__*/openspec.md` frontmatter `ticket_ref: <ref>` 命中 → 停 `Existing task for this ref. Use /h-resume <slug> or rename ticket-ref.`，不允许覆盖。

### 2. 获取 ticket 内容

`source=yunxiao` 或 `manual` 都走粘贴模式：

`AskUserQuestion`：

> 把云效 ticket 的内容粘贴进来：标题 + 描述 + 验收标准（若有）。

写入 `<run_dir>/ticket_raw.md`（`new_run_dir.py --intent Change` 起 run_dir）。

slug 推断：从 ticket 标题或前 5 词转 kebab-case（如 `add-voice-recycle-bin`）。

### 3. 分类输入

读 ticket 内容，按下表分类：

| 信号 | 推断分类 | 处理 |
|---|---|---|
| 多个标题段 / 多 section / 列出多个独立 feature | **PRD** | **Ask 确认** → 选 `切到 /h-decompose` 终止 / 选 `继续 brief（按单 feature 处理）` 走 step 4 |
| 含「bug / 报错 / 异常 / 不工作 / 报 500 / 复现步骤」关键词 ∧ 不含「需求 / 增加 / 设计」 | **Bug** | **Ask 确认** → 选 `切到 /h-fix-bug --severity p2` 终止 / 选 `继续 brief` 走 step 4 |
| 含「需求 / 功能 / 增加 / 支持」+ 单一 feature | **Idea/Feature** | 继续 step 4 |
| 含「客户反馈 / 用户希望 / 投诉」 | **Feedback** | 继续 step 4 |
| 含「安全 / 漏洞 / CVE / 越权」 | **Security** | 强制 risk=HIGH，继续 step 4 |
| 含「合规 / GDPR / 隐私 / 法律」 | **Compliance** | 强制 risk=HIGH，继续 step 4 |

PRD / Bug 推断禁硬停，必走下表 AskUserQuestion：

| 触发分类 | header | options |
|---|---|---|
| PRD | `Looks like a PRD?` | `切到 /h-decompose（多 feature 拆分）` / `继续 brief（按单 feature 处理）` / `取消` |
| Bug | `Looks like a bug report?` | `切到 /h-fix-bug --severity p2` / `继续 brief` / `取消` |

不可识别 → 走 **两层 AskUserQuestion**（每题 ≤4 选项，命中工具上限）：

**Q1（必发，header=`Ticket type`, 4 选 1）：**

| Option label | 选中处理 |
|---|---|
| `Change (Idea / Feedback / 单一 feature)` | → 继续 step 4 |
| `PRD (多 feature / 多章节)` | → 停，输出 `This ticket looks like a PRD. Run /h-decompose instead.` |
| `Bug report (复现步骤 / 异常 / 报错)` | → 停，输出 `This is a bug report. Run /h-fix-bug --severity p2 instead.` |
| `Special (Security / Compliance)` | → 触发 Q2 |

**Q2（仅 Q1 选 `Special` 时发，header=`Special category`, 3 选 1）：**

| Option label | 选中处理 |
|---|---|
| `Security (漏洞 / 越权 / CVE)` | → 强制 risk=HIGH，继续 step 4 |
| `Compliance (GDPR / 隐私 / 法律)` | → 强制 risk=HIGH，继续 step 4 |
| `Other (按 Idea/Feature 走)` | → 继续 step 4，不强制 HIGH |

禁单题 ≥5 选项；禁静默 default。

### 4. Ambiguity gate

按 `dispatch-template.md` Example 3 派 `@ambiguity-gatekeeper`，inputs = ticket 全文 + 分类结果。

- `[Status]: PASS` → step 5。
- `[Status]: BLOCK` → 把澄清问题转给用户，回答完重派。max 2 轮，第 3 轮仍 BLOCK → 停。

校验：`python3 .claude/scripts/gates/subagent_return_gate.py --task-kind audit --return-file <tmp>`。

### 5. 推断 risk

优先级（先匹配先用）：

1. `--risk` arg → 直接用，不发 Ask
2. `--slim` → LOW，不发 Ask
3. 分类 = Security / Compliance → HIGH（已在 step 3 强制），不发 Ask
4. ticket 内容关键词推断 — **HIGH 关键词带反例兜底**，命中后**必须 Ask 用户确认**（关键词推断常误判，禁静默升档）：

| 关键词 | 候选 risk | 反例（不该升档时） |
|---|---|---|
| `breaking change / 不兼容` | HIGH | — |
| `改接口 / 改字段` | HIGH | "新增字段" / "新增接口" 应为 MEDIUM |
| `auth / 权限 / 鉴权 / 角色` | HIGH | "新增菜单权限" / "新增角色 enum 值" 应为 MEDIUM |
| `migration / 迁数据 / ALTER TABLE` | HIGH | "新建表" / "新建索引" 应为 MEDIUM (isolated additive) |
| `enhancement / feature / 新增` | MEDIUM | — |
| `chore / 文档 / docs / 小调整` | LOW | — |

**HIGH 候选 + 反例可能命中 → AskUserQuestion 3 选 1 让用户拍板（HIGH / MEDIUM / 取消重述）**。
**MEDIUM / LOW 候选无反例 → 直接采用，不发 Ask。**
5. 全无关键词命中 → `AskUserQuestion` 3 选 1（LOW / MEDIUM / HIGH，附说明），禁静默 default。

LOW → `spec_mode: SLIM`；MEDIUM/HIGH → `spec_mode: STANDARD`。

### 6. 写 openspec.md 骨架

路径：`<run_dir>/openspec.md`。按 `.claude/llm_wiki/schema/openspec_schema.md` schema。

Frontmatter：

```yaml
spec_mode: STANDARD | SLIM
risk: LOW | MEDIUM | HIGH
frontend-facing: TBD by /h-design
module: <推断或 TBD>
triggers: []
launch_spec: <相对路径>
ticket_source: yunxiao | manual
ticket_ref: <原始 ref>
```

Body：

- SLIM → 4 段（Change Summary / Scope / Risk & Rollback / Verification & Evidence），从 ticket 内容填
- STANDARD → §1 Context 写一句 Goal（从 ticket 标题）；§5 / §7 写占位 `TBD by /h-design`；ticket 体里若有显式「验收标准 / Acceptance Criteria」段 → 转 Given/When/Then 填入 §7；其他条件段不写

同步写 `<run_dir>/focus_card.md`，Allowed Scope 占位 `<TBD by /h-design>`。

### 7. 结构体检

```bash
python3 .claude/scripts/wiki/schema_checker.py <run_dir>/openspec.md
```

FAIL → 报错给用户，不自动补；不重派。

### 8. 绑 launch_spec

无现存 → 新建 `.claude/runs/launch_spec_<YYYYMMDD_HHMMSS>.md`（表头同 `/h-brief` step 5）。追加一行（**严格 5 列** `Intent | Status | Phase | Artifact/Log | Failed_Reason`，risk 写 openspec frontmatter 不入表）：

```
| <slug> | PENDING | 1_Explorer | <run_dir>/openspec.md | - |
```

Phase 强制 `1_Explorer`。risk 仅在 openspec frontmatter，不入 launch_spec。

### 9. 输出

```
[From-Ticket] source=<…> | ref=<…> | slug=<slug>
  classification: <Idea/Feature|Feedback|Security|Compliance|Other>
  risk: <LOW|MEDIUM|HIGH> | spec_mode: <STANDARD|SLIM>
  openspec: <run_dir>/openspec.md
  launch_spec: <slug> → PENDING (1_Explorer)
  AC status: <EXTRACTED N ACs | PLACEHOLDER (none in ticket)>
  ambiguity_gate: PASS<-after-N-questions>

Next:
  - /h-brief <slug> --launch-spec <path>  ← 进 Explorer + Propose
  - 或先手动跑 explore（grep 现有代码、读 wiki）后 /h-design
```

## 硬约束

- 可改：新建 `<run_dir>/{openspec.md, ticket_raw.md, focus_card.md}`、新建或追加 launch_spec。
- 禁源码改、禁现有 openspec 覆盖。
- risk 禁静默 default，必走 arg / 分类强制 / 关键词命中 + Ask / 用户回答 之一。
- PRD / Bug 类不在本命令处理，分别走 /h-decompose / /h-fix-bug。
- ambiguity 迭代 ≤2 轮，第 3 轮 BLOCK 停。
- MVP `yunxiao` 等同 `manual`，不调云效 OpenAPI。
