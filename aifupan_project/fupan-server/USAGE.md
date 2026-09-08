# 使用指南（给人类看的）

这份文档解释 **怎么用 `.claude/` 这套 AI 工作流和 Claude Code 一起干活**。
机器读的契约在 `CLAUDE.md`；这一份是给团队成员、新人、PM 和不想读机器格式的人看的。

> TL;DR — 改一行代码？直接说想改啥。改 API / 数据库 / 鉴权 / 跨 3 个模块？前面加 `@standard`。前端要跟你对接？openspec 加一行 `frontend-facing: true`。要跑固定多步流水线？直接敲 `/h-*` 命令。

---

## 1. 我能拿这套工作流干什么

| 你想做的事 | 怎么开口 | 会发生什么 |
|---|---|---|
| 改一段文案 / 改一行 bug / 重命名变量 | 直接描述，比如"把 XX 文案改成 YY" | AI 直接动手，不写设计文档 |
| 修一个生产 bug | `@quickfix 线上 XX 报 NPE，复现路径是 ...` 或 `/h-fix-bug --severity p1 --production` | 走应急通道：先复现 → 根因 → 修 → secrets 扫描 → 完成 |
| 写一个新功能（动 API / DB / 鉴权 / ≥3 个模块）| `@standard 我要加 XX 功能，需求是 ...` 或 `/h-brief "<意图>"` | 走完整生命周期：先调研 → 写 openspec → 你审 → 你批 → 写代码 → QA → 归档 |
| 处理一份 PRD | 把 PRD 内容贴进来 / 给链接 | AI 拆任务、估时、画依赖图、列接口契约（命令版：`/h-decompose`）|
| 从云效 ticket 起步 | `/h-from-ticket manual <ref>` + 粘贴 ticket 全文 | AI 跑 ambiguity gate → 推断 risk → 起 openspec 骨架 + launch_spec PENDING 行 |
| 让前端团队同步对接 | openspec 顶部写 `frontend-facing: true` + `module: replay-XX`；或跑 `/h-collab <slug>` | AI 在 `.claude/llm_wiki/wiki/frontend-api/<module>.md` 出永久契约；`/h-collab` 额外起 per-task sign-off 状态机 |
| 跨团队反馈跟踪 | `/h-collab-update <slug>` | 录入 FE / 三方反馈；`--signoff` 解除阻断 |
| 查"上次那个东西在哪" | "去 wiki 翻一下 XX" | AI 走知识库 drill-down，不瞎猜路径 |
| 整理积累的零散文档 | `@gc` 或 `@librarian` | 跑维护流，合并 DRAFT 片段、清死链 |
| 把异常 / 客服反馈录成事故 | `@incident` 或 `/h-incident <异常>` | 按模板起草 → 5 点自检 → 你确认才写盘 |

---

## 2. 三档模式：Vibe / PATCH / Standard

工作流分三档，**默认 Vibe**。AI 会根据描述自动选档；你也可以用 `@` 前缀强制。

### Vibe（最轻）

- 一行修 / 改文案 / 改注释 / 解释代码 / 单行配置
- 不写 openspec、不开 run_dir、不归档

### PATCH（中间档）

- ≤3 文件、≤50 行、**单模块**、**不动公共面**（API / DB schema / 鉴权 / 错误码）
- 创建轻量 run_dir `.claude/runs/PATCH__<时间>/` + `focus_card.md` 锁住文件白名单
- scope_guard hook + secrets_linter hook 都跑
- Archive 仅 1 行 changelog（不归档 openspec）

> ⚠️ 中途发现碰到公共面 → AI 自动升档到 Standard 并 emit `[Mode Change] PATCH → Standard`，**代码不丢**。

### Standard（完整流程）

任一即升档——

- 改了 **对外 API**（路径 / 方法 / 请求或响应字段）
- 改了 **数据库 schema**（destructive DDL：改既有列、删列、加 NOT NULL 不带 default）
- 改了 **鉴权策略** 或 **错误码体系**
- 改动跨 **≥ 3 个 jiuyu 模块**
- 触发 **`replay-common` / `replay-generic`** 这类共享基建

升级后跑完整六阶段：Explorer → Propose → Review → [HIGH 风险时停下来等你批] → Implement → QA → Archive。

### 快捷词（路由提示）

在提问前贴一个，AI 按它走：

| 快捷词 | 含义 |
|---|---|
| `@vibe` | 强制 Vibe；写代码 >1 行会自动升档 |
| `@patch` / `@quickfix` | 强制 PATCH 档（不再是 Vibe 别名） |
| `@standard` | 强制完整流程 |
| `@learn` / `@read` | 只读模式 |
| `@debug` | 派 @debugger 找根因，禁动业务代码 |
| `@incident` | 录入事故到 incidents/ |
| `@gc` / `@librarian` | 整理 wiki、合并 DRAFT |
| `@harvest <module>` | 阈值触发的知识抽取 |

可选参数：`--risk medium|high`、`--test "<cmd>"`、`--yes`（自动确认）、`--strict-vibe`（@vibe 不许自动升档）。

---

## 2.5 Slash Commands（人工触发的工作流入口）

`@shortcut` 是给主 agent 的**路由提示**（让它走对档位）；`/h-*` 是**显式工作流入口**（敲一条命令跑一条多步流水线）。两者互补：

| 你想做的 | 用 |
|---|---|
| 我懒得描述档位，让 AI 自己判断 | 自然语言 + `@` 前缀 |
| 我想跑一条**固定多步流水线**（如 ticket → openspec → design） | `/h-*` 命令 |

### 12 个命令的全景

```
入口         /h-from-ticket    /h-fix-bug
                ↓
设计         /h-brief    /h-decompose    /h-design
                ↓
对接         /h-collab → /h-collab-update    （前端 / 三方 sign-off）
                ↓
状态         /h-resume    /h-gates
                ↓
归档         /h-archive
                ↓
交付         /h-pr        （生成云效 MR title+body，UI 手动创建，命令回填 URL）

旁路         /h-incident  （事故录入，任何时候都能跑）
```

### 每个命令一句话

| 命令 | 一句话 |
|---|---|
| `/h-from-ticket <src> <ref>` | 把云效 ticket / 反馈转成 openspec 骨架 + launch_spec PENDING |
| `/h-brief "<意图>" [--slice]` | 起单个 openspec.md 骨架 + launch_spec IN_PROGRESS |
| `/h-decompose <PRD>` | PRD / EPIC → N 个 vertical slice + DAG launch_spec |
| `/h-design [run_dir]` | 派 `@system-architect` 填 openspec（HIGH 强制 ≥2 ADR） |
| `/h-resume [slice]` | 只读：定位 IN_PROGRESS 任务，给 Next Action |
| `/h-gates --phase <…>` | 跑 phase + scenario gate 套件；FAIL 起 incident 草稿 |
| `/h-collab <slug>` | 生成跨团队协作 deliverable + sign-off 状态机 |
| `/h-collab-update <slug>` | 录入外部反馈；`--signoff` 解除阻断 |
| `/h-fix-bug` | bug 全链路：root-cause → 风险定档 → fix scope → 走 PATCH 或 `/h-brief` |
| `/h-incident <异常>` | 把事故 / 日志 / 客服反馈按 `_TEMPLATE.md` 录进 incidents/ |
| `/h-archive [run_dir]` | Plan Deviation 反思 → 归档 openspec → 翻 launch_spec DONE |
| `/h-pr [slug]` | pre-PR gate + 生成云效 MR title+body → 用户去 UI 创建 → 回填 URL |

### 云效平台适配点

我们 git + CI 用阿里云**云效**（Codeup + Flow），没有 GitHub `gh` 那样的成熟 CLI。所以：

- `/h-from-ticket` MVP 用 `--source manual` + 粘贴 ticket 全文（云效 OpenAPI 后续可选）
- `/h-fix-bug` 取 ticket 同上，其余 root-cause / incident / launch_spec 编排平台无关
- `/h-pr` **不自动**创建 MR——生成 title + body 给你去云效 UI 粘贴，你创建好回填 URL，命令把 URL 写进 launch_spec

---

## 3. Standard 模式下你会看到什么

每个阶段 AI 都会在屏幕上打标记，告诉你它在哪一步。

```
[Route] Mode=Standard | Risk=HIGH | Phases={Explorer, Propose, Review, Approval, Implement, QA, Archive}
[Phase 1 — Explorer]
  ...在调研已有代码、把需求翻译成 Given/When/Then ACs
[Phase 2 — Propose]
  ...在写 .claude/runs/<intent>__<时间>/openspec.md
[Phase 3 — Review]
  ...自检 + HIGH 还会跑一轮 adversarial-review
[Approval Gate]  ← HIGH 风险会停在这里等你批
  "请审 openspec 的 Human Section，回复 同意 / 部分同意 / 拒绝"
[Phase 4 — Implement]
  ...开始写代码，每个文件都过 scope_guard 检查
[YIELD]  ← 写完代码 + 编译通过会停下来，问你"可以进 QA 吗"
[Phase 5 — QA]
  ...跑 mvn / 测试 / 代码审查
[Phase 6 — Archive]
  ...把 openspec 归档进 .claude/llm_wiki/archive/，给 wiki 留 DRAFT 笔记
```

**记住三个会停下来等你的位置：**

1. **HIGH 风险 Approval Gate** — openspec 写好之后停下来，**你不批就不会动一行代码**。
2. **Implement → QA 之间的 YIELD** — 代码写完 + 编译通过，AI 会停下来问"现在可以进 QA 吗"，避免一路狂奔到底改不动。
3. **`[Boundary Exception Request]`** — AI 想动你没授权的文件，会停下来发请求，等你回"批 / 不批"。

---

## 4. 产物都放在哪

```
back-fupan-server/
├── CLAUDE.md                          ← AI 看的入口（行为契约）
├── USAGE.md                           ← 你正在读
├── .claude/
│   ├── commands/                       ← 12 个 slash 命令（/h-brief, /h-design, /h-fix-bug ...）
│   ├── runs/<intent>__<时间>/          ← 任务进行中的工作目录
│   │   ├── openspec.md                ← 这次任务的设计文档（HIGH 风险你要审这个）
│   │   ├── focus_card.md              ← Allowed Scope 白名单
│   │   └── current_task.md            ← 当前进度
│   ├── runs/collabs/                   ← /h-collab 产的跨团队 deliverable + sign-off 状态
│   ├── llm_wiki/
│   │   ├── archive/                   ← 历次任务归档的 openspec（可追溯）
│   │   ├── incidents/                 ← 生产事故 / 客服反馈 / RCA 报告
│   │   └── wiki/
│   │       ├── domain/                ← 业务术语 / 状态机
│   │       ├── api/                   ← 内部 API 索引（给后端 AI 看）
│   │       ├── frontend-api/          ← 前端契约（给前端团队 / 前端 AI 看）
│   │       ├── data/                  ← 数据库表 / ER
│   │       ├── architecture/          ← 架构决策 / ADR
│   │       ├── specs/                 ← 进行中或最近关闭的 spec
│   │       └── preferences/           ← 安全 / 反模式 / 红线
│   ├── agents/                        ← 15 个角色（lead-engineer / code-reviewer ...）
│   ├── skills/                        ← 42 个技能（每个 SKILL.md 都是单点知识）
│   ├── rules/                         ← 工作流规则（lifecycle / policy / dispatch / skill-precedence）
│   └── scripts/                       ← Hook 脚本和 gate 检查脚本
└── sql/                                ← DB migration（DDL 进这里）
```

**几个高频去处：**

- 要看某次任务怎么落地的 → `.claude/llm_wiki/archive/YYYYMMDD_<slug>.md`
- 要给前端发对接文档 → `.claude/llm_wiki/wiki/frontend-api/<module>.md`
- 要看 sign-off 状态 → `.claude/runs/collabs/<YYYYMMDD>_<slug>_collab.md`
- 要看项目红线 / 反模式 → `.claude/llm_wiki/wiki/preferences/`
- 要看 slash 命令清单 → `.claude/commands/`
- 要看技能清单 → `.claude/skills/trae-skill-index/SKILL.md`

---

## 5. 常见场景的"标准开口"

抄下面这些就行，按需替换 `<...>`。**自然语言** 和 **slash 命令** 两路都列出。

### 5.1 修一个 bug

**自然语言：**
```
线上 <模块> 报 <异常>，复现路径是 <步骤>。
排查根因，给出修复方案；不要顺手清理别的东西。
```

**命令版（推荐用于生产事故）：**
```
/h-fix-bug --severity p2 --production
然后粘贴 bug 描述：期望 / 实际 / 复现 / 错误信息
```

触发：根因调查（@debugger）→ 查 incidents/ 历史相似 bug → 风险定档 → 生产事故自动起 incident → 你确认 fix scope → 进 PATCH 或 Standard。

### 5.2 写一个新接口（动 API）

**自然语言：**
```
@standard
我要在 replay-<模块> 加一个接口 POST /xxx/yyy/zzz：
- 功能：<一句话>
- 输入：<字段列表>
- 输出：<字段列表>
- 鉴权：<谁能调>
- 前端要跟我对接（frontend-facing: true, module: replay-<模块>）
```

**命令版：**
```
/h-brief "在 replay-<模块> 加 POST /xxx/yyy/zzz 接口" --risk medium
/h-design                    （等 openspec 骨架完成后跑）
/h-collab <slug> --type api  （前端要对接时）
```

HIGH 风险，会停在 Approval Gate，openspec 给你审。批了之后会自动产出 `wiki/frontend-api/replay-<模块>.md` 给前端。

### 5.3 加一张表

**自然语言：**
```
@standard --risk high
我要加一张 tb_xxx_yyy 表，字段是 <...>，索引 <...>。
迁移 SQL 放 sql/ 下，命名按现有规范。
```

**命令版：**
```
/h-brief "加 tb_xxx_yyy 表" --risk high
/h-design
```

强制 HIGH（destructive DDL），会触发 migration_gate.py 校验 DDL，归档时会进 `wiki/data/<模块>_data.md` DRAFT。

### 5.4 处理一份 PRD

```
/h-decompose
然后粘贴 PRD 链接或全文
```

按 prd-task-splitter 拆任务清单 + 依赖图 + 估时，每个 slice 起独立 openspec 骨架，串成 DAG launch_spec。

### 5.5 从云效 ticket 起步

```
/h-from-ticket manual <ticket-ref>
然后粘贴云效 ticket 全文（标题 + 描述 + 验收标准）
```

跑 ambiguity gate → 推断 risk → 起 openspec 骨架 + launch_spec PENDING。PRD 类自动转 `/h-decompose`；Bug 类自动转 `/h-fix-bug`。

### 5.6 整理 wiki

```
@gc
整理一下 wiki，重点看 .claude/llm_wiki/wiki/<area>/。
```

### 5.7 让 AI 复盘上次的活

```
@learn
读 .claude/llm_wiki/archive/<YYYYMMDD>_<slug>.md，告诉我那次改了啥、为什么这么改。
```

### 5.8 会话中断后续接

```
/h-resume
```

只读，定位最新 IN_PROGRESS 任务，告诉你当前 phase + Next Action。多个 slice 时给你列表选。

---

## 6. 前端跨团队协作（独门设计）

前端团队也在用 Claude Code，他们要的是**可以直接喂给 AI 的 markdown 契约**，不是 Knife4j 链接。两条路径：

### 路径 A：自动产文档（永久模块字典）

1. 后端在 openspec 顶部声明：
   ```
   frontend-facing: true
   module: replay-words
   ```
2. Propose 阶段结束（openspec §3 API Contract + §4 Data Model 字段写完整后）**Implement 之前**，AI 会自动 dispatch `@frontend-api-doc-writer` → 产出 `.claude/llm_wiki/wiki/frontend-api/replay-words.md`。
3. 前端 git pull 这个文件，给他们的 AI 当上下文，就能并行开发。
4. Archive 阶段会再跑一次反向模式（从代码扫），如果代码字段漂了和文档对不上，会用 `[Field Drift Detected]` 告警，让架构师决定改代码还是改文档。

### 路径 B：per-task handoff + sign-off 状态（/h-collab）

适用于：API 契约 / 业务流程 / 数据映射 / 三方集成需要外部团队**正式确认**才能 Implement 的场景。

```
/h-collab <slug>                       # 起 deliverable + sign-off 状态机
# 你手动把 deliverable 给前端 / 三方（Lark / 云效评论 / 邮件）
# 收到回复后：
/h-collab-update <slug>                # 录入反馈
/h-collab-update <slug> --signoff      # 最终签字，解除阻断
```

deliverable 落在 `.claude/runs/collabs/<YYYYMMDD>_<slug>_deliverable.md`，state 落在 `<…>_collab.md`。两条路径**互补**：路径 A 是永久字典，路径 B 是 per-task handoff。

**A/B 共有特点：**
- 一个模块 / 任务一个 md 文件
- 共用响应封装 `R<T>` 引用 `_response_envelope.md`，不内联重复
- Snowflake ID 自动转 String（前端 JS Number 精度问题）
- LocalDateTime / BigDecimal 自动标记成 String + ISO-8601 / 千分位

---

## 7. 出问题怎么办

### 7.1 AI 卡住不动 / 一直在重试

工作流硬规则：**同一个 gate / linter 最多重试 3 次，`mvn compile` 最多 2 次**。超过会主动停下来问你。如果你看到 AI 在死循环——
- Ctrl+C 中断
- 直接说"停一下，我们换个思路"

### 7.2 Scope Guard 拦了正常的修改

如果某个文件确实需要动但不在 Allowed Scope 里，AI 会发：
```
[Boundary Exception Request]
File: replay-XX/.../YYY.java
Reason: <为什么需要>
```
你回"批"就放行，回"不批"就走绕路方案。

**真要紧急绕过**（极少数情况，比如 hook 自己挂了）：
```bash
CLAUDE_SCOPE_GUARD_BYPASS=1 <你的命令>
```
**单次有效，不要写进 settings 或 shell profile**。commit message 要说明用了为啥。

### 7.3 secrets_linter 报警

PostToolUse hook 默认是软告警（看到就改）。如果想验证它真的能阻塞：
```bash
CLAUDE_POST_HOOK_BLOCK=1 <你的命令>
```

### 7.4 openspec 写错了

任何阶段 AI 发现 openspec 的核心假设站不住，会发：
```
[Plan Invalidation]
Discovery: <文件:行号 或 测试输出>
Invalidated Assumption: <被推翻的约束>
Impact: <哪些 AC 不再可信>
Proposed Action: ROLLBACK_TO_PROPOSE | ROLLBACK_TO_EXPLORER
```
不会自己扩 scope 救火，会等你决定。

### 7.5 Hook 全部失灵 / 想看 hook 在干啥

```bash
# 看最近的 hook 日志
ls -lt .claude/scripts/harness/*.log 2>/dev/null

# 手动跑一次 scope_guard
python3 .claude/scripts/gates/scope_guard.py --focus-card <focus_card 路径>

# 手动跑一次 secrets 扫描
python3 .claude/scripts/gates/secrets_linter.py --paths "<文件>"
```

### 7.6 /h-collab sign-off 找不到之前的 state

跨会话 `/h-collab-update <slug>` 靠 slug 在 `.claude/runs/collabs/` 里 find。文件名格式：`<YYYYMMDD>_<slug>_collab.md`。如果 slug 拼错或忘了，直接 `ls .claude/runs/collabs/` 看。

---

## 8. 项目硬约束（写代码前必知）

这些是项目个性，AI 默认会遵守，但你 review 时也盯一下：

| 约束 | 违反后果 |
|---|---|
| DI 用**构造器注入**，禁 `@Autowired` 和 `@Resource` | checkstyle FAIL |
| Controller 返回 `R<T>`（`com.jiuyu.replay.generic.vo.common.R`），禁裸返业务对象 | api-standard FAIL |
| 分页用 `PageUtils<T>` 包装 `IPage` | review FAIL |
| ID 用 `SnowflakeManager.nextValue()`；实体 `@TableId(type = IdType.INPUT)` | review FAIL |
| 软删除**手动**设 `isDeleted` 字段，禁 `@TableLogic` | review FAIL |
| 时间戳**手动** `LocalDateTime.now()` 设 `createDate / updateDate` | review FAIL |
| 写方法加 `@Transactional(rollbackFor = Exception.class)` | review FAIL |
| 列表查询必须带 `tenantId` 过滤 | review FAIL（越权） |
| SQL 占位用 `#{}` 禁 `${}` | review FAIL（注入） |
| `IN (...)` 列表 > 500 用 `Lists.partition(ids, 500)` 分批 | review FAIL（性能） |
| 跨模块调用走 `replay-generic` 的 Feign 接口，禁跨模块直连 Dao | review FAIL |
| 敏感字段（密码 / 手机 / 身份证）不入日志 | review FAIL（泄露） |
| **不许自动 commit / push；不许 `--no-verify` 等绕过** | 等你明说才会做 |

完整列表 → `CLAUDE.md §5`。

---

## 9. 我能怎么影响 / 改进这套工作流

| 想做的事 | 改哪 |
|---|---|
| 改阶段顺序 / 加新 phase | `.claude/rules/lifecycle.md` |
| 调安全 / commit 策略 | `.claude/rules/policy.md` |
| 加 / 改一个角色（agent） | `.claude/agents/<name>.md` |
| 加 / 改一个 slash 命令 | `.claude/commands/<name>.md` |
| 加一个技能（skill） | `.claude/skills/<name>/SKILL.md` + 同步 `trae-skill-index/SKILL.md` + `skill-precedence.md` |
| 加一条硬约束 | `CLAUDE.md §5` + `.claude/llm_wiki/wiki/preferences/` |
| 调 hook | `.claude/scripts/harness/<*>_hook.py` 和 `.claude/settings.json` |
| 加一条 gate | `.claude/scripts/gates/<name>.py` + 在 lifecycle.md Part 6 注册 |

> 关键原则：**rules 不反向声明 commands**——`.claude/rules/*.md` 是 agent 行为契约，`.claude/commands/*.md` 是人类主观工具。命令可以引用规则（phase 编码、marker 名），规则不该列举命令。改完任何一处 → 让 AI 跑一次 `@learn 自检` 看一致性。

---

## 10. FAQ

**Q：每次都得手动打 `@standard` 吗？**
A：不用。AI 会根据你描述的变更自动判断风险。只有它判错时你才需要强制。

**Q：HIGH 风险等批准的时候我能改 openspec 吗？**
A：能。直接说"openspec §3 的 XX 字段改成 YY"，AI 会就地改，然后重新让你审。

**Q：写到一半我反悔了想推翻重来？**
A：说"回到 Propose 阶段，重新设计 XX"。AI 会保留之前的产物（不删除归档），重新写 openspec。

**Q：我直接动 AI 没动过的文件，AI 会知道吗？**
A：知道。下次会话开始它会先看 git 状态，发现差异会问你。

**Q：能不能跳过 QA 直接归档？**
A：技术上能（说"跳过 QA"），但 Standard 模式会留下 `[QA SKIPPED]` 标记进 archive，可追溯。

**Q：`.claude/runs/` 要不要 commit？**
A：**不要**。已经在 `.gitignore`。归档完用 `rm -r .claude/runs/<已完成>__*` 清掉。

**Q：openspec 用什么语言写？**
A：机器看的字段（code / schema / path）用英文；解释 / 理由 / 上下文用中文。详见 `.claude/rules/policy.md` Part 3。

**Q：`/h-*` 命令是写在哪的？我能改吗？**
A：在 `.claude/commands/<name>.md`。每个就是一份 markdown 提示词，可以直接改。改完下次会话 Claude Code 会自动加载新版。

**Q：slash 命令和 `@` 快捷词冲突时谁优先？**
A：不冲突——是两个机制。`@` 是给 AI 的路由提示，`/h-*` 是显式工作流。比如 `@standard /h-brief "..."` 完全合法。

---

## 11. 拓展阅读（按需）

- **机器入口契约** → `CLAUDE.md`
- **Slash 命令目录** → `.claude/commands/`
- **完整生命周期** → `.claude/rules/lifecycle.md`
- **安全 / commit / dispatch 策略** → `.claude/rules/policy.md`
- **Sub-agent 派发模板** → `.claude/rules/dispatch-template.md`
- **技能编排矩阵 Zone A-G** → `.claude/rules/skill-precedence.md`
- **openspec 模板** → `.claude/llm_wiki/schema/openspec_schema.md`
- **知识库根** → `.claude/llm_wiki/KNOWLEDGE_GRAPH.md`
- **技能总索引** → `.claude/skills/trae-skill-index/SKILL.md`

---

> 工作流不是为了让流程好看，是为了让你**少回头**。
> 每一道 gate、每一个 yield、每一次归档，都是在替你把 1 周后的自己从一摊看不懂的代码里救出来。
