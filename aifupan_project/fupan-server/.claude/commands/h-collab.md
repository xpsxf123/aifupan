---
description: 为当前 task 生成跨团队（前端/三方/QA/Ops）协作交付件（API 契约 / 流程 / 数据映射 / 集成 / 自定义），并起 collab 状态机跟踪 sign-off。
argument-hint: <slug> [--type api|process|data|integration|custom]
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
---

输入：`$ARGUMENTS`

前置：`<slug>` 对应的 `<run_dir>/openspec.md` 已通过 `/h-design`。本命令用于 Propose → Implement 之间需外部团队对齐时。

> 与 `@frontend-api-doc-writer` 的分工：本命令产 **per-task handoff + sign-off state**；`@frontend-api-doc-writer` 产 **永久模块 API 字典**（`wiki/frontend-api/<module>.md`）。本命令的 deliverable 可引用 / 嵌入 frontend-api wiki 内容，但保留 per-task 的 Open Questions / Feedback Log。

## 步骤

### 1. 解析 + 冲突检查

- `<slug>` 必填。缺 → 停 `Pass the slug as first argument.`。
- `--type` 可选，缺则 step 3 推断。
- 已存在 `.claude/runs/collabs/*_<slug>_collab.md`：
  - status=`SIGNED_OFF` → 停 `Collab for <slug> already signed off. Run /h-collab-update <slug> to add follow-up.`。
  - status 其他 → `AskUserQuestion`：「overwrite draft」/「continue from existing → 跳到 step 7 readout」。

### 2. 读 openspec / task_brief

定位 `<slug>` 对应 `<run_dir>/openspec.md`（从 launch_spec Artifact 列查），读取：

- frontmatter: `risk` / `module` / `frontend-facing`
- §1 Context 第一行 Goal
- §5 Business Logic
- §7 ACs
- §3 API Contract（若有）
- ticket frontmatter `ticket_ref` / `ticket_url`（若来自 `/h-from-ticket`）

不存在 → 停 `No openspec for slug <slug>. Run /h-brief first.`。

### 3. 推断 deliverable type

`--type` 给了 → 用之。否则按下表推断：

| 信号 | type |
|---|---|
| `frontend-facing: true` 或 §3 有 endpoint 定义 | `api` |
| §5 描述多方流程（关键词：流程 / 串联 / 触发 / 状态机 / upstream / downstream） | `process` |
| §4 / §5 含字段映射 / schema 对齐 / 数据字典 | `data` |
| §1 / §5 提到第三方 / 集成 / 外部系统（结合 `replay-third` 模块） | `integration` |
| 都不像 | 走 **两层 AskUserQuestion**（见下） |

**两层 AskUserQuestion**（每题 ≤4 选项，命中工具上限）：

**Q1（必发，header=`Deliverable type`, 4 选 1）：**

| Option label | 选中处理 |
|---|---|
| `API contract（前后端 endpoint 契约）` | → `type=api` |
| `Process flow（多方业务流程串联）` | → `type=process` |
| `Data exchange（字段映射 / 三方集成）` | → 触发 Q2 |
| `Custom（自由格式）` | → `type=custom` |

**Q2（仅 Q1 选 `Data exchange` 时发，header=`Data exchange direction`, 2 选 1）：**

| Option label | 选中处理 |
|---|---|
| `内部字段映射 / schema 对齐` | → `type=data` |
| `外部系统 / 第三方对接` | → `type=integration` |

禁单题 ≥5 选项；禁静默选 `custom`。

### 4. 内容补缺

各 type 检查 openspec 是否够用，缺则按表 `AskUserQuestion`：

| type | 缺什么时问 |
|---|---|
| api | endpoint 列表（method + path + 一行用途） |
| process | 业务流程的编号步骤、每步谁触发、对方收到什么 |
| data | 字段映射表（field / type / 源系统 / 目标系统 / 备注） |
| integration | 我们提供给三方什么 + 期望三方提供什么 |
| custom | 自由描述要覆盖的内容 |

收到的回答 step 5 用，同步进 collab 状态文件 `open_questions` 起始值。

≤2 轮 ask；仍不够 → 停 `Insufficient content to generate a useful deliverable. Provide details for at least the core section.`。

### 5. 写 deliverable

路径：`.claude/runs/collabs/<YYYYMMDD>_<slug>_deliverable.md`。

各 type 模板骨架（顶部统一头：`# <type-name> — <task name>` + 一行 `Generated: <date> | Task: <slug> | Status: DRAFT`）：

**api**：Overview（§1 一句话）→ Endpoints（每个 endpoint：METHOD path / Purpose / Request 字段表 / Response 200 字段表 / Error Codes 表）→ Breaking Changes → Open Questions

**process**：Flow Overview（2 句）→ Step-by-Step 表（Step / Trigger / Our Side / External Side / Output）→ Decision Points → Error / Exception Handling → Open Questions

**data**：Field Mapping 表（Our Field / Type / Their Field / Type / Notes）→ Enum Values → Validation Rules → Open Questions

**integration**：What We Provide → What We Expect → Authentication（method / token lifetime / refresh） → Error Handling（retry / timeout / fallback） → Open Questions

**custom**：用户描述内容 → Open Questions

`Open Questions` 段从 step 4 收到的开放问题填入；无则空段（留给 /h-collab-update 填）。

### 6. 写 collab 状态文件

路径：`.claude/runs/collabs/<YYYYMMDD>_<slug>_collab.md`。仅 YAML frontmatter（与 /h-collab-update 双向兼容）：

```yaml
---
slug: <slug>
type: api | process | data | integration | custom
status: PENDING_REVIEW
task_brief: <run_dir>/openspec.md
deliverable: .claude/runs/collabs/<YYYYMMDD>_<slug>_deliverable.md
created: <YYYY-MM-DD>
reviewers: ""
open_questions: []      # step 4 收到的开放问题
feedback_log: []
signed_off_by: ""
signed_off_date: ""
---
```

`Edit` 最新 launch_spec 中 `<slug>` 行，Artifact 列单元格内用 `<br>` 软换行追加 `COLLAB:<YYYYMMDD>-<slug>`。**禁追加 ` | `**（破坏 5 列 schema）：

```
| <slug> | IN_PROGRESS | <phase> | <run_dir>/openspec.md<br>COLLAB:<YYYYMMDD>-<slug> | - |
```

Status 保持 IN_PROGRESS。

### 7. 输出

```
[Collab] slug=<slug> | type=<type> | status=<CREATED|EXISTING>
  deliverable: <path>
  state:      <path>
  launch_spec: COLLAB marker 已追加到 Artifact 列

Next（手动）：
  1. 打开 deliverable 审稿
  2. 通过 Lark / 云效评论 / 邮件传给外部团队（命令不发消息）
  3. 收到回执后跑 /h-collab-update <slug>

Implement 是否阻断由人决定：
  - deliverable 含未决设计点（影响代码结构）→ 等 --signoff
  - 仅信息性同步 → 可并行 Implement
```

## 硬约束

- 可改：新建 deliverable 文件、新建 collab state 文件、launch_spec Artifact 列。
- 禁源码 / openspec / task_brief 改。
- 外部通信全手动，禁发 Lark / 云效 / 邮件 / GitHub 评论。
- Implement 不自动阻断；COLLAB marker 是信息性。
- 内容空缺 ask ≤2 轮，仍不够直接停，不生空 deliverable。
- type 推断不出强制 ask 用户，禁静默选 custom。
