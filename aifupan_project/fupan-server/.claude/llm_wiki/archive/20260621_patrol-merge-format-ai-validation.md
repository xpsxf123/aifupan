spec_mode: STANDARD
risk: MEDIUM
frontend-facing: false
triggers: [domain, data, adr]

# 互动巡检合并报告 AI 格式校验与纠正

## 1. Context
- **Business goal (one sentence):** 互动巡检"合并多份报告"AI 输出格式经常不规范（漏 aifupan 标签 / 被代码围栏包裹 / 表格跑歪），需在落库前自动校验，不合格则用 AI 纠正后再输出。
- **Scope of change:**
  - `replay-common` `AiEnums.askType` 新增 2 个 cueType（校验提示词 28 / 纠正提示词 29）
  - `replay-ai` `ScriptMonitorPatrolGenerateBll.generate()` 合并调用后插入「AI 校验 → AI 纠正(≤1) → 代码兜底 → 降级」环节
  - `sql/replay-38.sql` 新增 cueType 28/29 提示词草稿 INSERT（运营审）
  - 单测 `ScriptMonitorPatrolGenerateBllTest`
- **Dependencies consulted:** 现有合并流程 `ScriptMonitorPatrolGenerateBll.java:356-397`、`buildSummary` `SUMMARY_TAG_PATTERN:145`、既有纠错先例 `replay-words/AiContentCorrectService`（评估后不复用，见 ADR-1）。
- **Explorer hand-off:** 设计经会话内多轮对齐（程序校验时效性不足 → 改 AI 校验 + 提示词即规范单一事实源 + 代码极小兜底）。

## 2. Domain Model
- **Enum updates（`AiEnums.askType`，DB 持久化为 cueType int code）：**
  - `INTERACTION_PATROL_FORMAT_PROMPT(28, "互动巡检-格式校验并纠正提示词")` — 稳定元指令：对照规范**一次性**判定+修复（合规仅回哨兵 `__FORMAT_OK__`，否则回纠正后正文）
- **新增 systemKv 键（运营配，缺失则回退默认模型）：** `script_monitor_patrol_format_ai_model` = 一个快/廉价模型 code。
- **单次调用决策（用户反馈）：** 校验与纠正合并为**一次** AI 调用（不分两步），降低延迟与 token——尤其"格式经常不正确"时省一半调用。详见 ADR-1。
- **关键不变量：** 格式规范**内嵌在 cueType=28 提示词内**（运营可维护），需与合并提示词 cueType=21 的格式要求保持一致；代码不喂合并提示词，仅喂合并报告输出。规范在 DB 配置（非 Java 代码），格式要求变更时运营改 cueType=28，无需改代码。

## 2.5 Business Architecture
Not applicable — 仅 replay-common 增枚举常量 + replay-ai 内部逻辑，无跨模块业务编排 / 上下游系统交互。

## 3. API Contract
Not applicable — 无 Controller / 接口契约变更；报告正文字段形状不变，仅内容质量提升。

## 4. Data Model
非表结构变更（无 DDL）；仅向既有提示词表新增 2 条配置行 + systemKv 1 条。

### 4.1 配置数据（`sql/replay-38.sql`，草稿待运营审）
- cueType=28 格式校验并纠正元指令（单次调用，示意）：
  > 你是格式校验并修复器。给出「格式规范」和「待处理输出」。仅针对**格式与结构**（标签齐全、分段、表格列、是否被代码块包裹等），**不判断内容准确性**（你没有原始数据）。若完全合规：只输出标记 `__FORMAT_OK__`，不要其他字符。若不合规：只输出修复格式后的完整正文，**严禁改任何内容/数字/时间/排序/判定**，不要解释、不要用代码块包裹。
- systemKv：`script_monitor_patrol_format_ai_model`（运营在后台配快模型 code；本次不写死）。

### 4.5 Lifecycle
- 配置缺失（systemKv 模型 / cueType 提示词取不到）→ 跳过校验纠正，直接落原文 + `log.warn`（不阻断报告生成）。

## 5. Business Logic
### 5.1 Happy path（在 `generate()` 合并调用 `:366` 之后、写正文之前插入）
1. 合并得到 `mergedContent = finalResult.getContent()`。
2. 格式规范已内嵌在 cueType=28 提示词内（运营维护），代码不再喂合并提示词。
3. **单次 AI 校验并纠正**：载快模型（systemKv `script_monitor_patrol_format_ai_model`）+ 格式提示词（cueType=28，作 system）；user 仅传 `mergedContent`，`chatCompletion(...)`。
4. 返回含哨兵 `__FORMAT_OK__` → 合规，`finalContent = mergedContent`；否则视返回为纠正后正文，过代码兜底硬查后 `finalContent=返回值`。
5. 落库与 summary 改用 `finalContent`（替换原 `finalResult.getContent()`）。

### 5.2 Branches & exceptions
| Branch | Trigger | Handling | 结果 |
|---|---|---|---|
| 已合规 | 返回含 `__FORMAT_OK__` | 用合并原文 | 输出原文 |
| 已纠正 | 返回非哨兵且过代码兜底硬查 | `finalContent=返回值` | 纠正后输出 |
| 纠正结果不合格 | 返回非哨兵但兜底硬查 false | 降级 `finalContent=mergedContent` + `log.warn` | 输出原文 |
| 配置缺失 | cueType=28 提示词取不到（loadSingleCueWord 抛错被捕获）/ 返回空 | catch 后 `log.warn` | 输出原文 |
| 调用异常 | chatCompletion 抛错 | catch 后 `log.warn` | 输出原文（绝不阻断报告） |
| 代码兜底告警 | `finalContent` 缺 `<aifupan-data-block>` 或含 ``` 代码围栏 | `log.warn`（可观测，不阻断） | 仍输出 finalContent |

### 5.3 重试与幂等
- **单次调用**（校验+纠正合一，无重试、无循环）。
- 属系统内部 QA 调用，无外部副作用，天然幂等。

### 5.4 代码兜底硬查（稳定不变量，与运营格式审美无关）
- `<aifupan-data-block>` 必须存在（`buildSummary` 正则依赖）。
- 正文不得被 ``` 代码围栏包裹（前端解析依赖）。
- 仅 `log.warn` 告警，不改变"输出 finalContent"的行为。

## 5.5 Technical Architecture
Not applicable — 复用既有 `aiChatFeign.chatCompletion`，无新增跨模块拓扑 / MQ / 定时任务 / Redis / 分布式锁 / 事务边界。

## 6. Non-Functional Constraints (Hard Constraints)
- **Token（明确边界）：** **仅**「格式校验」「格式纠正」这两个新增调用产生的 token **不累加进 `totalTokens`、不结算、不计入台账**（系统内部 QA，不计费给用户）。**切片 / 合并调用的 token 照常累加 `totalTokens` 并经 `settleAiToken` 结算，与本功能上线前完全一致**——本功能不得改变既有切片/合并 token 的计费行为。
- **可靠性优先：** 本功能为 QA 增强，任何失败路径都必须降级为"输出原合并结果"，**绝不能让报告生成失败**——所有校验/纠正逻辑包在 try-catch 内。
- **内容安全：** 纠正提示词明令"只修格式、严禁动内容/数字/时间/排序/判定"，防止纠正模型篡改业务数据。
- **DO NOT：** 不用 `@Autowired`（构造器注入）；不硬编码格式标签清单做主校验（时效性，规范以合并提示词为准）；不复用 `AiContentCorrectService`（见 ADR-1）。
- **Performance：** 合并本属 60-90s 异步链路，新增 1-2 次快模型调用延迟可接受；仅在合并成功后执行。

## 7. Acceptance Criteria (Testing)
- **AC-001（格式已正确，不纠正）：** Given 合并输出符合规范, when 校验返回 `conform=true`, then 不触发纠正、落库内容=原合并输出。
- **AC-002（格式错误，纠正成功）：** Given 合并输出缺标签/被代码围栏, when 校验 `conform=false` 且纠正结果通过兜底, then 落库内容=纠正后内容。
- **AC-003（纠正后仍不合格，降级）：** Given 纠正结果仍缺 data-block, when 1 次纠正用尽, then 降级落原文 + `log.warn`。
- **AC-004（异常不阻断）：** Given 校验/纠正 chatCompletion 抛异常或 JSON 解析失败, when 捕获, then 落原文、报告状态正常 GENERATED。
- **AC-005（token 不计入）：** Given 触发校验+纠正, when 结算, then `totalTokens` 不含校验/纠正 token、settle 金额与无此功能时一致。
- **AC-006（配置缺失降级）：** Given systemKv 模型或 cueType 提示词取不到, when 合并完成, then 跳过校验纠正、落原文 + `log.warn`。
- **Unit test requirements：**
  | AC-id | Method under test | Assertion |
  |---|---|---|
  | AC-001 | `generate()`（mock 校验返 conform=true） | 不调用纠正模型；body.content=原文 |
  | AC-002 | `generate()`（校验 false→纠正返合规） | body.content=纠正结果 |
  | AC-003 | `generate()`（纠正仍缺 data-block） | body.content=原文；warn |
  | AC-004 | `generate()`（校验 chatCompletion 抛错） | body.content=原文；report=GENERATED |
  | AC-005 | `generate()` | settle 的 totalTokens 不含校验/纠正增量 |
  | AC-006 | `generate()`（模型/提示词 null） | 跳过；body.content=原文 |

## 8. Frontend Contract Publishing
- `frontend-facing: false` — 后端内部质量增强，报告正文字段形状不变，无需前端协同。

## 9. Architecture Decision Records

### ADR-1: 自包含 AI 单次校验+纠正（格式规范内嵌 cueType=28 DB 提示词）而非复用 AiContentCorrectService、也非硬编码 Java 代码清单

**Status:** proposed
**Date:** 2026-06-20
**Deciders:** 用户 + 主 agent

#### Context
合并报告格式常跑歪。校验手段有三类候选：硬编码代码清单、复用 replay-words 的 `AiContentCorrectService`、自包含 AI 校验。合并提示词由运营随时改、会加新格式要求，校验逻辑必须不过时。

#### Decision
在巡检 Bll 内自包含实现：用**单次** AI 调用（cueType=28 提示词作 system、合规回哨兵 `__FORMAT_OK__` 否则回纠正后正文）完成校验+纠正。**格式规范内嵌在 cueType=28 DB 提示词中（运营维护，与 cueType=21 同步）**，而非硬编码进 Java、也非运行时喂整段合并提示词；仅保留极小的代码兜底（data-block 存在 / 无代码围栏）守护代码消费方硬依赖。

**为何单次而非两步（用户反馈采纳）：** 独立"AI 校验"判定不可靠（模型说 OK ≠ 真能解析），且确定性判定已由代码兜底承担；分两步在"格式经常不正确"时要多花一次调用。单次（合规回极短哨兵、否则回纠正文）成本在两种情况下都 ≤ 两步，且更省延迟。

#### Alternatives Considered
**Alternative A：硬编码代码校验清单（逐个标签 regex）**
- Pros：确定性、零 token、可单测。
- Cons：**运营改提示词加新格式要求时不会自动跟上，需改代码**（时效性硬伤）。
- Why not chosen：与"格式规范随运营演进"矛盾。

**Alternative B：复用 replay-words `AiContentCorrectService`**
- Pros：现成两步校验+纠正骨架。
- Cons：它的"检查"是 AI 判断通用文本清理（非对照规范）；在 replay-words 需新建 Feign SPI 跨模块；用 dictDataFeign scene 配置，与巡检 systemKv/cueType 范式不一致。
- Why not chosen：耦合更重、语义不符，比自写还复杂。

#### Consequences
**Positive**
- 格式规范在 DB(cueType=28)，运营可改、无需改 Java 代码（格式要求变更不必发版）。
- 单次调用：合规回极短哨兵、不合规回纠正文，成本/延迟 ≤ 两步。
- 自包含、复用 Bll 既有模型/提示词/AI 调用范式，无新增跨模块依赖。

**Negative**
- 依赖 AI 判定，非 100% 确定性 → 用代码兜底 + 降级缓解。
- 多 1 次快模型调用（异步链路内，token 不计费给用户）。
- 格式规范与合并提示词(cueType=21)是两处配置，需运营同步维护。

**Risks（and mitigation）**
- Risk：模型误判放水 → Mitigation：代码兜底硬查 data-block/代码围栏 + `log.warn` 可观测。
- Risk：模型篡改内容 → Mitigation：cueType=28 提示词严令只修格式 + 后续可加内容长度/关键字段抽样比对（本期不做）。
- Risk：运营改 cueType=21 格式但漏改 cueType=28 规范 → 校验标准滞后；Mitigation：SQL 注释明示同步约定，后续可加一致性巡检（本期不做）。

#### Archive target
`.claude/llm_wiki/wiki/architecture/adrs/NNNN-patrol-merge-format-ai-validation.md`（Archive 由 @architecture-curator 分配编号）

---

## 做什么 / 为什么
**现状：** 互动巡检合并多份报告时，AI 输出格式经常不规范（漏 aifupan 标签、被代码块包裹、表格跑歪），导致前端渲染异常或摘要抠不出。
**需要：** 合并结果落库前自动校验格式，不合格用 AI 纠正后再输出；纠正不了则降级输出原文并告警，绝不阻断报告。
**范围：** AiEnums 加 1 个 cueType(28) + 巡检 Bll 合并后插入校验/纠正环节 + cueType 提示词 SQL（内嵌格式规范，运营审）+ 单测。

## 怎么做
Java 代码不硬编码格式清单（会过时）；格式规范内嵌在 cueType=28 DB 提示词里（运营可维护，与合并提示词 cueType=21 同步），**一次** AI 调用判定+修复：合规回哨兵 `__FORMAT_OK__` 用原文、否则用纠正文，代码只保留 data-block/代码围栏两条硬兜底。校验/纠正 token 不计费给用户。详见 §5 + ADR-1。
