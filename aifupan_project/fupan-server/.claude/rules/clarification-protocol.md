# Clarification Protocol

工作流命令 / 主 agent 向人类询问的统一协议。Claude Code `AskUserQuestion` 工具硬上限：**每题 2-4 选项、单次调用 ≤4 题**。本文件用来收敛 commands 中 5 种不同的澄清写法（结构化 / 选项越界 / 当文本框用 / 自然语言 Y/n / 硬停报用法）。

加载方式：按需 — command 写新询问点 / 主 agent 路由前判断澄清形态时 `Read`。

---

## Part 1 — 缺失要素 → 处理映射

| 缺失要素类型 | 统一处理 | 例 |
|---|---|---|
| **位置参数 / 必填 flag 缺** | 硬停 + 一行 usage | `/h-collab-update` slug 缺 → `Pass the slug as first argument.` |
| **三要素（verb / target / outcome）模糊** | 派 `@ambiguity-gatekeeper`（Tier-2 触发判定见 [policy.md](policy.md#tier-2--conditional)） | `/h-brief` step 1 |
| **单个 enum 字段缺**（severity / type / risk / source 等） | AskUserQuestion **单题**，options **强制 2-4 项**，超 4 必拆两层 follow-up | `/h-from-ticket` step 5 risk 兜底 3 选 1 |
| **多个 enum 字段同时缺** | AskUserQuestion **多题（≤4 题）**，每题独立 2-4 选项；缺 ≥5 字段拆两轮交互 | `/h-incident` step 2 缺 severity+source+status |
| **多行自由文本输入**（bug 描述 / 调研主题 / Plan Deviation 反思 / URL / slug） | 自然语言对话；用 `> 提示标题` 块引导，且必须把用户原文 `>` 引用存盘 | `/h-fix-bug` step 2 收 bug 详情 |
| **二 / 三选一破坏性确认**（写盘 / Archive / 改 launch_spec / 覆盖既有文件） | AskUserQuestion **2-3 选项**；**禁用** 自然语言 `Y/n/改` | `/h-incident` step 5 写盘确认 |
| **不可枚举开放回答**（URL / 复制粘贴 ticket 内容 / 字段映射表） | 自然语言对话（不强行套选项） | `/h-pr` step 7 收 MR URL |

---

## Part 2 — AskUserQuestion 硬约束

- **options 数 ∈ [2, 4]**：超 4 必须拆两层 follow-up（Q1 粗类 → 选中某项才发 Q2 细分）。命令文档必须明文给出"哪个选项触发哪个 follow-up"，禁含糊。
- **单 command 单次 AskUserQuestion 调用 ≤ 4 题**：多字段同缺优先级排序，超 4 字段拆两轮交互（先关键 4 字段，第二轮再补）。
- **不可静默 default**：enum 字段推不出必须 ask 或 ESCALATE；禁主 agent 静默选某值往下走。
- **破坏性操作必须显式确认**：写盘 / Archive / 改 launch_spec / 覆盖既有文件 / git push 必须 AskUserQuestion 而非自然语言 `Y/n`。与 [policy.md](policy.md) "user must reply literal CONFIRM" 一致，但用结构化选项替换文本确认。
- **sub-agent 禁调 AskUserQuestion**：sub-agent 不继承主上下文，无法向用户询问。需澄清字段必须返回 `[Status]: ESCALATE` + `[Reason]: <missing field>` 抛回主 agent 处理。

---

## Part 3 — 反模式（禁）

| 反模式 | 后果 |
|---|---|
| 单题 ≥5 选项 | Claude Code 工具运行时拒；提交即 bug |
| 多个 enum 字段散落多轮自然语言 ask | 主上下文污染 + 字段口径不一致 |
| 用 AskUserQuestion 当文本框（写 1 个 option 让用户改） | 误用工具；该走自然语言 |
| 自然语言 `Y/n` 当破坏性确认 | 与 policy.md 显式 CONFIRM 协议冲突 |
| 静默 default（enum 推不出就挑一个） | 与 jiuyu "不假设" 原则冲突 |
| sub-agent 内部 AskUserQuestion | sub-agent 无人类通道，调用即 hang |

---

## Part 4 — 反例 → 正例

**反例 A：5 选 1 越界**

```
AskUserQuestion: 选 type (api / process / data / integration / custom)
```

**正例 A：拆两层 4+2**

```
Q1: header="Deliverable type" options=[
  "API contract", "Process flow", "Data exchange", "Custom"
]
Q2 (仅 Q1=Data exchange): header="Data direction" options=[
  "内部字段映射", "外部系统集成"
]
```

**反例 B：多字段散落**

```
（step 2）问 severity ...
（step 2）问 source ...
（step 2）问 status ...
（step 2）问 area ...
```

**正例 B：一次性多题**

```
AskUserQuestion (one call, 3 questions):
  Q1: header="Severity"  options=["P0","P1","P2","P3"]
  Q2: header="Source"    options=["prod-alert","customer-report","qa-found","post-mortem"]
  Q3: header="Status"    options=["open","mitigated","fixed"]
```

**反例 C：自然语言 Y/n 当破坏性确认**

```
写盘? (Y / n / 改 <字段>)
```

**正例 C：AskUserQuestion 3 选 1**

```
AskUserQuestion:
  header="Write incident to disk?"
  options=["写盘","取消","改字段后重展示"]
（选"改字段后重展示" → follow-up Q 问改哪个字段，options ≤4）
```

---

## Part 5 — Command 作者 checklist

新写 / 修改 command 文档时逐条自检：

1. 每个 `AskUserQuestion` 调用点的 options 数 ∈ [2, 4] ？
2. 多字段缺失是否合并到一次 AskUserQuestion 多题（≤4 题）？
3. 破坏性确认是否走 AskUserQuestion，未用 `Y/n` 自然语言？
4. enum 字段推不出时有显式 ask 路径，未静默 default ？
5. sub-agent dispatch prompt 中**未要求 sub-agent** 直接 ask 用户（应让 sub-agent ESCALATE）？
6. follow-up 路径明文标 "选 X 时触发 Q2"，未含糊？

任一条 No → 改文档，再过一次。
