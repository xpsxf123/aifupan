# Incidents — 外部反馈真相源

本目录是 `failure_memory.py` 的**第二数据源**（第一数据源是 `archive/index.md` 的 Fix 行）。

**核心动机：** `archive/index.md` 只记录"我们主动归档过的事"——存在自我审查偏差。**线上事故、客户投诉、QA 抓到未归档的 bug**——这些 ground truth 必须有独立入口，才能形成"事故 → reflex 提醒"的真闭环。

---

## 何时写入事故文件

任一条件触发：

| 来源 | 例子 |
|---|---|
| **线上事故** | 服务 5xx 突增、定时任务卡死、消息积压、数据错乱 |
| **客户/运营反馈** | 真实客户报障、运营群截图、客服工单 |
| **QA 系统外发现的 bug** | UAT 阶段、灰度阶段抓到的回归 |
| **Post-mortem 结论** | 复盘会确认的根因（即使已修） |
| **Archive-time RCA**（推荐自动化）| `@debugger` 产出 RCA 报告 + 后续 Change 任务修复时，Archive 阶段顺手 drop 一份 incident |

**不要写：**
- 开发自测发现的 bug（写到 archive Fix: 即可）
- 重构 / 优化 / 性能调整（用 archive PATCH 行）
- 设计阶段已被 Review 抓到的（属于流程内闭环，不是事故）

---

## 文件命名约定

```
YYYY-MM-DD__<area>__<short-slug>.md
```

- `<area>`：模块名（`replay-words` / `replay-order` / `replay-crm` / ...），用于 reflex 提示时分类
- `<short-slug>`：英文短描述，3-6 词，下划线连接，例：`anchor_task_stuck_running`

例：
- `2026-05-18__replay-words__anchor_task_stuck_running.md`
- `2026-05-19__replay-order__payment_callback_idempotency_lost.md`

---

## 文件内容格式

```markdown
---
date: 2026-05-18              # 事故发生日期（不是写入日期）
area: replay-words            # 模块
severity: P0 | P1 | P2 | P3   # P0=客户阻断, P1=核心功能受损, P2=部分降级, P3=偶发
source: prod-alert | customer-report | qa-found | post-mortem | rca-archive
status: fixed | mitigated | open
---

# 一句话标题（事故的本质，≤50 字，failure_memory 会读这行）

## Symptom
观察到的现象（1-3 句）。例：主播分析任务卡在 RUNNING 状态超过 30 分钟无进展。

## Root cause
真实根因（1-3 句）。例：CRM Kafka 拥塞导致下游消费阻塞，主任务等待外部回调超时但未释放分布式锁。

## Fix / mitigation
做了什么。例：增加 Kafka 消费 timeout + 主任务超时强制释放锁 + 监控告警。

## Reflex（最重要——这条是 LLM 写代码时该 reflex 检查的）
未来在 <相关代码区域> 写代码时，必须 <具体动作>。例：所有跨服务等待回调的任务，必须有强制超时释放锁的兜底逻辑。
```

---

## failure_memory 如何使用本目录

1. `failure_memory.py` 每次触发（UserPromptSubmit hook，30min cooldown）会扫本目录
2. 解析 frontmatter `date` + `area`，文件首行 `# ` 抽出标题
3. 与 `archive/index.md` 的 Fix 行**合并按日期排序**，取最近 3 条
4. stderr 输出形如：
   ```
   [failure-memory] Recent mistakes to reflex-check (last 14 days, top 3):
     - 2026-05-15 [archive]: Fix: tb_crm_* 缺 create_id/update_id...
     - 2026-05-18 [incident@replay-words]: 主播分析任务卡死在 RUNNING
     - 2026-05-19 [incident@replay-order]: 支付回调幂等失效
   ```

LLM 看到这条提示，会自然去读对应 incident 文件（一行路径即可定位），把 Reflex 段落带入当前任务的判断。

---

## 排除规则

- 文件名以 `_` 开头（如 `_TEMPLATE.md`）→ 不扫描
- `README.md` → 不扫描
- 缺少有效 frontmatter `date` → 跳过该文件（不报错）
- 日期超出 14 天窗口 → 跳过

---

## 自动萃取入口（PATCH #5 起可用）

写盘有两条自动化路径，**两条都经过用户确认门——main agent 不会默默写 incident 文件**。

### 入口 A：`@debugger` 在 RCA 时产出 `[Incident Draft]`

触发条件：Scenario DEBUG（用户 `@debug` 或 main agent dispatch `@debugger`），且 Phase 1 推断的 `Source ≠ dev-found`。

流程：
1. `@debugger` 完成 RCA 后，在 return 块末尾追加 `[Incident Draft]` 段，含完整 frontmatter + Symptom / Root cause / Fix / Reflex 四段。
2. **debugger 不写盘**（Hard Limit L150：read-only on `.claude/`）。
3. Main agent 收到 return，向用户展示草稿 + 拟定的文件名，问："Drop this to incidents/? (Y/n)"
4. 用户 Y → main agent 用 `Write` 工具创建文件；N → 跳过。

draft 内容字段来源：
| Incident 字段 | 来源 |
|---|---|
| `date` | RCA Phase 1 Observed 中的日志时间戳，或当天日期 |
| `area` | RCA Phase 2 定位的 `replay-<module>` |
| `severity` | 由 main agent 询问用户（debugger 不擅自定级）|
| `source` | RCA Phase 1 Source 字段（已 in {prod-alert, customer-report, qa-found, post-mortem}）|
| `status` | RCA 中 Proposed Fix 已有 → `fixed`；只缓解 → `mitigated`；未修 → 不 emit draft |
| `Symptom` | RCA Phase 1 Observed + Reproducer 浓缩 |
| `Root cause` | RCA Phase 4 root cause statement（已是模板化句式）|
| `Fix` | RCA Proposed Fix 段，带 file:line |
| `Reflex` | **由 debugger 提炼**——"未来该 reflex 检查的具体动作"，要带类/方法/约束 |

skip 条件（debugger 不 emit draft）：
- `source = dev-found`（用 archive Fix 行即可，不污染 incidents/）
- `status = open` 且无 Proposed Fix（incidents/ 是行动 reflex 库，不是 ticket 系统）
- `Root Cause = "not found"`（无可萃取的 reflex 内容）

### 入口 B：`@incident` 简捷符 —— 用户粘日志/异常文本

触发：用户输入以 `@incident` 开头的消息，后跟异常日志、stack trace、症状描述、客服工单内容等。

main agent（Vibe 模式，无 dispatch）执行：

1. **解析**：从日志/文本里抽取 → 异常类、stack top 的 `file:line`、package 路径、时间戳、客户/环境 hint。
2. **推断**：
   - `area` ← package 推断（`com.jiuyu.replay.words.*` → `replay-words`），多模块时选 stack top 所在
   - `date` ← 日志时间戳，缺失则当天
   - `source` ← 文本里"客户/UAT/告警/线上"等关键词推断；不确定则问用户
   - `severity` ← 默认 P2，询问用户校正
   - `status` ← 默认 `open`；若用户附带"已修复"信息则 `fixed`
3. **生成 slug**：英文短描述，3-6 词，下划线连接（main agent 自己造，必要时问用户）。
4. **draft**：按 `_TEMPLATE.md` 填出完整 markdown。Symptom / Root cause / Fix / Reflex 四段尽量从日志推断；信息不足的段标 `<待补充>` 而非编造。
5. **确认**：展示完整 draft + 文件名给用户，问："写入 `incidents/<filename>.md`? (Y/n / 改 <字段>)"
6. **写盘**：用户 Y → `Write` 工具创建文件。

**main agent 反编造红线：**
- Root cause 不明 → 写 `<待补充：未在日志中找到根因，建议跑 @debug>` ——**不要编造可信但错误的根因**
- Reflex 不明 → 写 `<待补充：根因确定后再填>`
- 比起写一份漂亮但虚构的 incident，**留 `<待补充>` 让人后续填**更有价值

### 不在 PATCH #5 内的下一步

- `incident_log.py` CLI helper：交互式建文件，对人类作者友好
- 外部 ingest 适配器：Sentry webhook / Jira tag → 自动 drop 文件
- 季度 retrospective：扫 30+ 天前未关闭的 incident，提示回访

当前两条入口足以覆盖主要场景：开发期外部 bug 走 `@debug` 自动萃取；线下事后总结走 `@incident` 粘贴萃取。
