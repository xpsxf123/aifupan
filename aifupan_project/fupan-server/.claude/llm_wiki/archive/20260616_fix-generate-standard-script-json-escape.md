---
spec_mode: STANDARD
risk: MEDIUM
frontend-facing: false
module: replay-words
triggers: [tech-arch]
slice_id: -
launch_spec: ../launch_spec_20260616_104721.md
---

# fix-generate-standard-script-json-escape

## 1. Context
- **Business goal (one sentence):** 解决标准稿生成接口（`POST /replay/script-monitor/generateStandardScript`）在 AI 输出 content 字段内含未转义 `"` 时 fastjson 解析失败抛 70008 的问题，通过后端容错重试 + 提示词强化 + SpEL 旁路修复三层手段，保持 JSON 输出格式不变。
- **Scope of change:**
  - `replay-words` 模块 `ScriptMonitorStandardScriptBll.java` — `generateStandardScript` 方法内 `parseJsonOutput` catch 块新增 1 次 AI 重发重试
  - `replay-api` 模块 `StandardScriptController.java:56` — `@NoRepeatSubmit(key = "generateStandardScript")` → `key = "'generateStandardScript'"`（SpEL 字符串字面量）
  - 新 SQL 文件 `sql/replay-37.sql` — UPDATE `tb_cue_words` cueType=27 提示词强化 escape 示例
- **Dependencies consulted:**
  - `bug_raw.md`（本 run_dir）— 错误日志 + 调用链 file:line + RCA
  - `explore_report.md`（本 run_dir）— Spec Inference + AC + Hidden Scope
  - 历史 archive `20260608_patrol-summary-md-cast.md`（同源 JSON 解析问题，本次不改输出格式）
- **Explorer hand-off:** `.claude/runs/Change__2026-06-16_10-47-21/explore_report.md`

## 2. Domain Model

Not applicable — 本次无新业务术语、无状态机变更、无枚举新增。仅修复 AI 输出解析容错逻辑，不引入新领域概念。

## 2.5 Business Architecture

Not applicable — 本次改动在单 BLL 类内（`replay-words`），不新增跨模块调用路径，不改 Feign/MQ 边界。`aiFeign.chatCompletion` 是既有上游调用，本次仅在 catch 块复用现有调用。

## 3. API Contract

Not applicable — 无新增 / 变更 Controller endpoint，无 request/response schema 变更（`frontend-facing: false`，`§3` 不写）。

## 4. Data Model

Not applicable — 本次变更为 `UPDATE` 数据（修改 `tb_cue_words` cueType=27 的 `problem` 字段内容），不含 DDL（无 `CREATE/ALTER/DROP TABLE/INDEX`），无新持久化形态。SQL 文件仅含纯 DML。

## 5. Business Logic

### 5.1 Happy path（step-by-step，无变更）
1. Controller 收到 `POST /generateStandardScript` 请求，`@NoRepeatSubmit` AOP 获取分布式锁。
2. BLL `generateStandardScript` 方法执行 Token 预扣（`withhold`）。
3. 调用 `aiFeign.chatCompletion(modelConfig, message, userId)` 获取 AI 响应（60s 超时）。
4. 调用 `parseJsonOutput(content)` 解析 AI 返回 JSON 数组。
5. AI 首次返回合规 JSON → `parseJsonOutput` 解析成功 → 无重试。
6. 调用 `returnAiToken` 不触发（仅失败时触发），按 totalTokens × 1.5 系数 settle 一次。
7. 返回 `StandardScriptVo`。

### 5.2 重试路径（本次新增）
1. 步骤 3 `aiFeign.chatCompletion` 首次成功，但 AI 返回 content 内含未转义 `"`。
2. 步骤 4 `parseJsonOutput` 抛 `BusinessException(70008)`（fastjson `illegal fieldName`）。
3. **catch 块新增逻辑（本次改动）**：`catch` 语句**仅包裹** `List<TimeAxisItemVo> items = parseJsonOutput(content)` 这**一行调用**（不可覆盖上方的 `aiFeign.chatCompletion` 调用行）。捕获 `BusinessException`，检查错误码 == 70008 **且** `exception.getMessage()` 含关键字 `"JSON parse"`（架构师拍板：采用 message 关键字方案，因为禁止改 `parseJsonOutput` 本体）。三处 70008 来源区分：
   - `parseJsonOutput` line 322（`aiContent` 为空）— 抛 70008 但 message **不含** `"JSON parse"` → **不重试**，直接上抛
   - `parseJsonOutput` line 339（`JSON.parseArray` 解析异常后封装）— 抛 70008 且 message **含** `"JSON parse"` → **触发重试**
   - `parseJsonOutput` line 346（list 为空）— 抛 70008 但 message **不含** `"JSON parse"` → **不重试**，直接上抛
4. 记录 `log.warn("[T21 标准稿生成] 解析失败触发重试 userId={} 第1次", userId)`。
5. **重发一次** `aiFeign.chatCompletion(modelConfig, message, userId)`（相同参数，无 sleep/backoff）。
6. 对二次 AI 响应再次调用 `parseJsonOutput(content2)`：
   - **二次成功**：将 首次 totalTokens + 二次 totalTokens 累加，按 1.5 系数 **settle 一次**，返回 `StandardScriptVo`。
   - **二次失败（再抛 70008 或其他异常）**：调用 `returnAiToken` 一次（归还预扣），上抛 70008 给上层。

### 5.3 重试边界（重要）
| 触发条件 | 是否重试 | 原因 |
|---|---|---|
| `parseJsonOutput` 抛 `BusinessException(70008)` | 是（1 次） | 唯一重试入口，JSON escape 失败属随机性输出问题 |
| AI `status=1`（BLL line 154 主动抛 `BusinessException(70008)`） | 否 | catch 范围限定（不包覆此行），错误在调用层结果校验而非 JSON 解析 |
| Feign 网络 / 超时异常（`catch Exception` 路径） | 否 | 错误在网络 / 传输层而非 JSON 解析层，重试无意义且放大延迟 |
| `withhold` 预扣失败抛 70001 | 否 | Token 问题，与 JSON escape 无关 |
| 其他 `BusinessException`（非 70008）| 否 | 非解析失败，不在重试域内 |
| 任何 `Exception`（非 BusinessException）| 否 | 通用异常不触发重试，直接上抛 |

重试硬上限：**1 次**（最多 2 次 AI 调用：首次 + 1 次重试）。不可通过配置项、环境变量调高，硬编码常量 `MAX_JSON_PARSE_RETRY_COUNT = 1`。

### 5.4 Token settle 时序（OQ3 决策）
- 架构师决策：**累加后 settle 一次**。
- 首次 AI 调用产生 `aiResult1`（`AiReturnDataVo`）含 `totalTokens_1`；重试产生 `aiResult2` 含 `totalTokens_2`（若有）。
- **二次成功时 settleResult 构造**：以 `aiResult2` 为基础对象（promptTokens / completionTokens / modelName 等字段均取 `aiResult2`），**手动 set** `settleResult.setTotalTokens(aiResult1.getTotalTokens() + aiResult2.getTotalTokens())`；其余字段不再手动 override。
- settle 调用点：仅在最终成功路径（首次成功 or 重试成功）调一次 `settleAiToken(withhold, userId, settleResult, recordBo)`；参数中的 settleResult 含已累加的 totalTokens。
- 失败路径：调 `returnAiToken` 一次（不 settle）。
- 禁止：settle 两次（首次成功 settle + 重试成功再 settle = 台账 2 行，双扣）。

### 5.6 SpEL 旁路修复（OQ1 决策）
- 架构师决策：**修法 A**，`key = "'generateStandardScript'"`（加单引号，SpEL 字符串字面量）。
- 原因：`NoRepeatSubmitAop` 对所有非空 key 均做 `parser.parseExpression(key).getValue(context)` 解析，`"generateStandardScript"` 被当作标识符而非字面量，故报 `EL1007E`。加单引号后 SpEL 正确识别为字符串，lockKey = `prefix + "generateStandardScript"`，实现预期全局共享防抖。
- 修法 B（留空）理论可行但意图不明确，且 `""` 会走 `joinPoint.getSignature().toString()` — 是方法完整签名，偏长且隐式。
- **仅修 line 56**，不改同文件 `confirmStandardScript`（line 73）及其他 Controller（外科手术原则）；`confirmStandardScript` 的同款 WARN 登记 TECH-DEBT。

### 5.7 提示词强化（P1 SQL）
- UPDATE `tb_cue_words` WHERE `cue_type = 27`（`STANDARD_SCRIPT_GENERATE`）。
- 修改 `problem` 字段内容：在现有"必须转义双引号"文字说明基础上，将示例 JSON 中的 `"` 统一改为真实 `\"` 字面量写法，让 AI 看到的 few-shot 范例本身就是合规 escape 的 JSON。
- 不动 `cue_type` 编号、不动 `trade_id` 路由、不动其他字段。

## 5.5 Technical Architecture

### 5.5.1 触发说明
本次触发 §5.5 的原因：在 BLL 方法内新增了一次 `aiFeign.chatCompletion` 跨模块 Feign 调用（重试路径），并涉及 Token settle 调用时序变更，属于"cross-module Feign call"触发条件。

### 5.5.2 Cross-Module Communication（重试路径新增调用）
| Channel | From → To | Contract | Failure handling |
|---|---|---|---|
| Feign | replay-words（BLL 重试）→ replay-third（AiFeign） | `aiFeign.chatCompletion(modelConfig, message, userId)` — 现有接口，无变更 | 重试调用本身若抛异常，不再二次重试，直接 returnAiToken + 上抛 70008 |
| Feign | replay-words（settle）→ replay-order（Token） | Token settle（现有）— 累加后调一次 | 与现有行为一致 |

### 5.5.3 Async Tasks
Not applicable — 无 MQ、无 @Scheduled。

### 5.5.4 Cache Strategy
Not applicable — 无 Redis cache 变更。

### 5.5.5 Transaction Boundary
Not applicable — `generateStandardScript` 路径不含数据库写操作（AI 生成不落库），无 `@Transactional` 需求。Token settle/returnAiToken 由下游 Feign 自管事务。

### 5.5.6 Observability（OQ2 决策）
- 架构师决策：重试触发日志用 **`log.warn`**。理由：这是异常恢复路径，可观测性优先（oncall dashboard 能发现重试频率异常）；info 级别在生产日志量下容易被淹没。如重试频率正常（<5%），warn 噪音可接受；若频繁触发则说明 AI 模型或提示词需要关注。
- 日志格式：`log.warn("[T21 标准稿生成] 解析失败触发重试 userId={} 第{}次", userId, retryCount)`
- **重试成功时**（info 级，不污染 warn 流）：`log.info("[T21 标准稿生成] 解析重试成功 userId={} retryCount={} totalTokens={}", userId, retryCount, totalTokensSum)`；禁含 referenceScript / AI content / 提示词内容
- 失败最终上抛时：`log.warn("[T21 标准稿生成] 重试后解析仍失败 userId={} 已尝试{}次", userId, retryCount)`（现有 BusinessException 异常日志会自动打印）

## 6. Non-Functional Constraints (Hard Constraints)

### 实现期硬约束（lead-engineer 按此执行）

- **重试次数硬上限 = 1**：禁止配置化、禁止通过环境变量调整，硬编码常量 `private static final int MAX_JSON_PARSE_RETRY_COUNT = 1`（在 `ScriptMonitorStandardScriptBll` 类顶部声明）。
- **Token settle 一次性规则**：累加 `totalTokens_1 + totalTokens_2` 后 settle 一次；禁止在重试成功路径再次 settle；禁止在失败路径 settle（只 returnAiToken）。
- **catch 范围精确限定**：`try { List<TimeAxisItemVo> items = parseJsonOutput(content); } catch (BusinessException e) { ... }`——`try` 块**仅包裹 `parseJsonOutput` 调用这一行**，上方的 `aiFeign.chatCompletion` 调用**不在 try 块内**，其异常直接上抛。
- **不触发重试的异常范围**：`catch` 只捕获 `BusinessException` 且 `e.getCode() == 70008` **且** `e.getMessage() != null && e.getMessage().contains("JSON parse")`（对应 parseJsonOutput line 339 fastjson 解析异常路径）；`parseJsonOutput` line 322（aiContent 空）和 line 346（list 为空）抛出的 70008 message 不含此关键字，**不触发重试**；所有其他 `Throwable` 走现有上抛逻辑。
- **无重试间隔**：直接重发，不得加 `Thread.sleep()` / `@Retryable(backoff=...)` / 任何 backoff；AI 输出随机性本身是期望的间隔分散机制。
- **DI 使用构造器注入**（本文件已有 `@AllArgsConstructor` / `@RequiredArgsConstructor` — 遵循现有方式，禁 `@Autowired` / `@Resource`）。
- **禁止改 fastjson 解析 mode**：不加 `Feature.AllowUnQuotedFieldNames` 等宽松特性，保持严格解析。
- **外科手术原则**：仅改 `generateStandardScript` 方法的 catch 块，不动同文件其他方法（`confirmStandardScript` / `deletBySecUid` / `parseJsonOutput` 本体等）。
- **SQL 文件**：新建 `sql/replay-37.sql`，仅含 1 条 `UPDATE tb_cue_words`，文件头注释注明版本和变更原因。SQL 占位符使用 `'...'` 纯字符串，不含 `${}` / `#{}` 占位（MyBatis XML 外执行的 DML）。
- **SpEL 修复**：仅改 `StandardScriptController.java` line 56 的 `key` 参数，不改注解其他属性（timeout / message 等）。
- **TECH-DEBT 登记**：同文件 `confirmStandardScript:73` 的同款 SpEL WARN 问题，在实现完成后登记到 `docs/TECH-DEBT.md`。

### Forbidden（明确禁止）
- DO NOT 改 `parseJsonOutput` 方法本体（不动 fastjson 解析逻辑）
- DO NOT 改 AI 输出格式（保留 JSON 数组结构，不评估 Markdown 路线）
- DO NOT 改 settle / withhold / returnAiToken 的调用顺序（仅在 catch 块内插重试逻辑）
- DO NOT 动 `replay-words/.../producer/impl/*.java`
- DO NOT 动 `replay-words/.../service/impl/*.java`
- DO NOT 动 `replay-third/.../AiFeign.java` 接口契约
- DO NOT 动 `pom.xml` fastjson 版本
- DO NOT 动其他 Controller 的 `@NoRepeatSubmit` 注解
- DO NOT 动 `tb_system_kv`（QA DB 连错假象，无关）

## 7. Acceptance Criteria (Testing)

### 7.1 后端重试逻辑（Mockito 单测 — ScriptMonitorStandardScriptBll）

| AC-id | Given | When | Then | 验证方式 |
|---|---|---|---|---|
| AC-1 | AI 首次返合规 JSON | 调 `generateStandardScript` | `parseJsonOutput` 首次解析成功，**0 次**重试，Token settle 一次（totalTokens_1 × 1.5） | mock `aiFeign.chatCompletion` 返回合规 JSON；verify `aiFeign.chatCompletion` 调用次数 = 1 |
| AC-2 | AI 首次返 unescaped `"` 抛 70008（message 含 `"JSON parse"`），第二次返合规 JSON | 调 `generateStandardScript` | catch 触发重发，二次解析成功，Token settle 一次（(totalTokens_1 + totalTokens_2) × 1.5） | mock `aiFeign.chatCompletion` 依次返回：第1次不合规内容，第2次合规内容；verify 调用次数 = 2；verify `settleAiToken` 调用**一次**且入参 `settleResult.totalTokens == aiResult1.totalTokens + aiResult2.totalTokens`（精确断言累加值）；verify `returnAiToken` 调用次数 == 0 |
| AC-3 | AI 两次都返 unescaped JSON（两次都抛 70008）| 调 `generateStandardScript` | 抛 `BusinessException(70008)` 给上层；`returnAiToken` 调**一次**（不是两次） | mock 两次均返回不合规内容；verify `returnAiToken` 调用次数 = 1；verify 抛 70008 |
| AC-4 | AI 持续返不合规 JSON | 调 `generateStandardScript` | **最多 2 次 AI 调用**（首次 + 1 次重试），不存在第 3 次 | mock 无限返回不合规；verify `aiFeign.chatCompletion` 调用次数 = 2（`MAX_JSON_PARSE_RETRY_COUNT = 1` 硬上限） |
| AC-5 | AI `status=1` 返回（BLL line 154 路径抛 70008）| 调 `generateStandardScript` | 直接抛 70008，**不触发 parseJsonOutput 重试** | mock AI status=1；verify `aiFeign.chatCompletion` 调用次数 = 1 |
| AC-6 | `withhold` 预扣失败抛 70001 | 调 `generateStandardScript` | 抛 70001，不触发重试 | mock `withhold` 抛 70001；verify `aiFeign.chatCompletion` 调用次数 = 0 |

### 7.2 SpEL 旁路（手工验证）

| AC-id | Given | When | Then | 验证方式 |
|---|---|---|---|---|
| AC-7 | `StandardScriptController.java line 56` 改为 `key = "'generateStandardScript'"` 后服务启动 | 触发 `POST /replay/script-monitor/generateStandardScript` 一次 | 应用日志中**不再出现** `EL1007E: Property or field 'generateStandardScript' cannot be found on null` WARN | 观察 `[NoRepeatSubmitAop]` 日志，无 EL1007E；确认接口正常返回（锁键生效） |

### 7.3 提示词强化（人工抽样）

| AC-id | Given | When | Then | 验证方式 |
|---|---|---|---|---|
| AC-8 | `sql/replay-37.sql` 执行后，`tb_cue_words` cueType=27 提示词内含真实 `\"` 转义示例 | AI 调用 `generateStandardScript` 接口 ≥5 次（人工触发） | AI 输出 content 字段内的 `"` 均合规 escape（JSON `parseArray` 不再报 `illegal fieldName`），接口成功率 ≥ 80%（抽样 5 次中 ≥4 次成功） | 人工跑接口 ≥5 次，观察是否还触发 AC-2 重试路径或 AC-3 失败路径 |

### 7.4 单测文件位置建议
- 若存在：`replay-words/src/test/java/com/jiuyu/replay/words/bll/ScriptMonitorStandardScriptBllTest.java` — 补充 AC-1 ~ AC-6 六个 @Test 方法。
- 若不存在：新建同路径文件，mock `AiFeign` + `TokenService`（具体 Feign 接口名以源码实际为准）。

## 做什么 / 为什么
**现状：** `POST /replay/script-monitor/generateStandardScript` 接口对 AI 输出 JSON 的 escape 规范性 100% 依赖。AI 输出 content 字段内的 `"` 偶尔不按规范输出 `\"`（如生成"把"1"打在公屏"），fastjson2 2.0.51 严格模式立即报 `illegal fieldName input1` 解析失败 → 抛 70008 → 接口失败。这是 AI 非确定性输出与 fastjson 严格解析的固有耦合风险，提示词约束只能降概率不能归零。同时 `@NoRepeatSubmit(key="generateStandardScript")` 的 key 被当 SpEL 标识符解析，每次触发接口都打出 EL1007E WARN 噪音。

**需要：** 在 `parseJsonOutput` 抛 70008 时，catch 块新增最多 1 次 AI 重发重试（AI 随机输出，二次大概率合规）。两次都失败才真正抛错给上层，Token 按两次累加 settle 一次。同时强化 cueType=27 提示词的 escape 示例（降低重试触发频率），修复 SpEL WARN 噪音。

**范围：** 单 BLL 文件（`ScriptMonitorStandardScriptBll.java`，仅 catch 块）+ 单 Controller 注解参数（`StandardScriptController.java:56`）+ 新 SQL 文件（`sql/replay-37.sql`，1 行 UPDATE）。不改 fastjson 解析 mode、不改 AI 输出格式、不改 Controller endpoint 契约、不改 settle/withhold 调用顺序。

## Plan Deviation Reflection

闭环时的实际偏差与计划相比（按发现顺序）：

### 偏差 1 — 模型回退诱因被误标，后澄清为 QA 环境 DB 连错的假象

**计划**（bug_raw 一稿）：把 `tb_system_kv.script_monitor_standard_script_ai_model = deepseek-v4-pro` 在 `tb_ai_model` 找不到、回退 `diagnosis_ai_model_list` list[0]，作为根因诱因之一登记。

**实际**：用户澄清"kv_key 都存在的，可能是数据库连错了，先不管"。模型回退是 QA 环境 DB 连接错误引发的日志噪音，**与根因无关**。根因纯粹是"fastjson2 严格模式 + AI 输出 escape 不稳定"的耦合风险。

**修正**：openspec / launch_spec / explore_report 均明示"模型回退是假象不当诱因"；P0 配置修复方案剔除出本任务（DBA 线下走）。

**经验**：QA 环境的日志噪音容易让 @debugger 把"假象诱因"当真根因纳入修复范围。下次类似排查应先问用户"环境配置是否符合预期"再决定是否当根因。

### 偏差 2 — Propose 阶段 reviewer 标 2 MAJOR + 4 MINOR，触发 architect slim revision

**计划**：architect 完成 §1/§5/§5.5/§6/§7，3 OQ 拍板。

**实际**（Round 1 reviewer）：
- **M-1** §5.2 / §6 catch 范围未明示"仅包 parseJsonOutput 调用行"，且 parseJsonOutput 内 3 处 70008 未区分 → lead 可能误让 catch 包覆 status=1 路径 → AC-5 实现风险
- **M-2** §5.4 / §7 AC-2 settleResult 构造方式未定义 → Mockito verify 参数无法精确断言
- **N-1** §5.5 标题与 §5.5 Technical Architecture 编号冲突
- **N-2** §5.3 status=1 与 Feign 异常混写两种代码路径
- **N-3** §5.5.6 重试成功路径日志规范缺
- **N-4** §7 AC-2 缺 verify returnAiToken=0 反向断言

**修正**：architect slim revision 全 closed（catch 仅包 parseJsonOutput 调用行 + message 关键字 `"JSON parse"` 区分三处 70008；settleResult 以 aiResult2 为基础 + setTotalTokens(t1+t2)；§5.5 重编号为 §5.6/§5.7；§5.3 拆两行；§5.5.6 加 info 日志；§7 AC-2 加 verify returnAiToken=0）。

**经验**：契约层"模糊词"（如"catch 块"未限定包覆边界、"累加"未明 settleResult 构造方式）是 lead-engineer 误读源头，下次 architect 应在 §5 用"伪代码片段"而非自然语言描述关键控制流。

### 偏差 3 — Implement Round 2 reviewer 标 2 MAJOR + 2 MINOR

**计划**：lead 按 architect 改 catch + for 循环 + settle + 日志 + SpEL + SQL + 单测 6 case。

**实际**（Round 2 reviewer）：
- **MAJOR-1** `MAX_JSON_PARSE_RETRY_COUNT = 1` **常量声明但未在控制流引用**（if-else 硬编码 1 次），改常量值无效果，误导维护者
- **MAJOR-2** 第二次 `parseJsonOutput` 抛异常时**缺少 "重试后仍失败" warn 日志**（§5.5.6 明确要求），oncall 无可观测信号
- **MINOR-3** 注释里的具体行号引用（322/346）过期
- **MINOR-4** line 184 日志 tag "重试后解析仍失败" 触发条件实为 AI 调用失败（非 parse 失败），语义不匹配

**修正**：lead-engineer round 2 全 closed（for 循环用常量 + 二次 parse 失败补 warn + 注释去行号 + 日志 tag 拆"重试 AI 调用失败"）。round 3 reviewer **APPROVE**（0 finding，无 regression）。

**经验**：契约 §6 写"硬编码常量"时，lead 容易把"常量值=运行时不变"理解为"常量声明即可"。下次 architect 应在 §6 明示"常量必须在控制流中真实引用，改值能改变行为"。

### 偏差 4 — Implement Round 1 lead-engineer 发现 ×2.25 系数 pre-existing 问题

**计划**：架构师 §5.4 写"Token settle（×1.5 系数）"。

**实际**：lead 发现 `AiUtils.aiTokenConsumeMultiple(tokenNum, multiple)` 内部先乘 `DEFAULT_CONSUME_MULTIPLE=1.5` 再乘传入的 `multiple=1.5`，**实际乘 ×2.25 而非 ×1.5**。

**修正**：lead 保持 happy path 现有行为不变（外科手术），AC-2 单测断言用实际值 `675 = (120+180)×2.25`；同步登记 DEBT-026 建议未来统一收敛 multiplier 语义。

**经验**：lead 在 Implement 阶段对工具方法做实际验证发现了 pre-existing 不一致 — 这种"实现期反向校验契约"应作为标准工作流。"×1.5" 这类数字常量在契约 / 注释里出现时，architect 在 Propose 阶段应主动追到工具方法实现层验证。

### 偏差 5 — Allowed Scope 中途扩展 docs/TECH-DEBT.md

**计划**：focus_card Allowed Scope 锁定 BLL + Controller + SQL + 单测。

**实际**：lead 一轮发现 `confirmStandardScript:73` 同款 SpEL WARN 存量 + ×2.25 系数 pre-existing 问题 → 需要登 docs/TECH-DEBT.md 但 scope_guard 会阻拦。

**修正**：architect Round 1 finalize focus_card 时已将 `docs/TECH-DEBT.md` 纳入 Allowed Scope。lead Round 1/2 共登 DEBT-025（SpEL 同款 LOW）+ DEBT-026（×2.25 系数 MEDIUM）两条。

**经验**：契约层提前考虑"发现 pre-existing 问题时需要登 TECH-DEBT"这类副产物，把 `docs/TECH-DEBT.md` 默认纳入 Allowed Scope 即可，避免 Implement 阶段反复 boundary exception。

### 副产物 — SQL 提示词整体重写（不只加 escape 示例）

`sql/replay-37.sql` 实际改动 +75 行，比 round 1 计划的"加 escape 示例"扩大：
- 把示例话术从 round 1 假定的"普通商品" 整体换成"白酒（纯粮酒）" few-shot（6 个 timeRange 段完整流程）
- 新增 §4 标"极其重要" JSON 转义规范段，含错误 vs 正确 escape 字面对比示例

视为对 P1 提示词强化的合理增强（让 AI few-shot 信号更强）。需 QA 阶段产品 sign-off 新示例话术是否符合白酒类目业务意图。

### 未做事项（用户明确拍板）

- ❌ JSON → Markdown 改造（用户拍板保留 JSON 输出格式）
- ❌ P0 配置修复（tb_system_kv → tb_ai_model 由 QA 环境 DB 连错引发，DBA 线下走）
- ❌ AI 重试上限 > 1（架构师拍板 1 次重试足够，最坏 2×60s=120s 延迟可接受）
- ❌ fastjson lenient mode（无任何 Feature 能容忍 string value 内 unescaped `"`）
- ❌ 正则预处理 escape 修复（极脆弱）
- ❌ `confirmStandardScript:73` 同款 SpEL 修复（登 DEBT-025 独立修）
- ❌ `AiUtils.aiTokenConsumeMultiple ×2.25` 修复（登 DEBT-026 独立修）
