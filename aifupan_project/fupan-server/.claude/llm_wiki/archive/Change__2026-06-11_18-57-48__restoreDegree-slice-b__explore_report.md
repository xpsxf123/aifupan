# Explorer Report — restoreDegree-slice-b

## Specification Inference

- **Current（代码库今天保证的）：**
  - `ScriptMonitorBll.triggerReport:449-453` 对 `monitorType=1` 硬抛 70014（功能未上线），任何还原度触发请求被拒
  - `ScriptMonitorBll.triggerReport:480-483` 对 `monitorType=1` 前置校验抛 70005 占位（TODO，标准稿能力未就绪时拒绝）
  - `ScriptMonitorBll.autoTriggerReport:555` 注释"还原度永不调本方法"，monitorType=1 从未被自动触发
  - `ScriptMonitorMqHandler:107-108` 收到 fidelity tag 直接 log.warn + ConsumeResult.SUCCESS，不执行任何生成
  - `ScriptMonitorGenerateBll`（质检 3+1 范式）+ `ScriptMonitorPatrolGenerateBll`（巡检多切片范式）已就绪，`ScriptMonitorReportWriteService`（finishReport/restoreOrFail/markNotApplicable）已就绪
  - `StandardScriptService.findValid(tenantId, userId, secUid)` 已由 Slice A 建立，可跨模块按 secUid 三元组查有效标准稿
  - 5 套提示词（cueType=18/19/25/26/27）已由 sql/replay-34.sql INSERT 落库；systemKv key `script_monitor_fidelity_inter_ai_model` / `script_monitor_fidelity_merge_ai_model` 已配

- **Required（本 Slice 完工后需要保证的）：**
  - `triggerReport(monitorType=1)` 通过 4 道关卡（参数合法 + 资源归属 + 有效标准稿 + Token 余额 ≥ 100000）后建 report + 投递 MQ fidelity tag
  - `autoTriggerReport` 支持 `monitorType=1`，在还原度开关开启 + 有标准稿 + Token 足时自动触发，mirror 质检/巡检 B7 路径
  - `ScriptMonitorMqHandler` 收到 fidelity tag 时路由到新建的 `ScriptMonitorFidelityGenerateBll.generate(report, originalStatus, withhold)`
  - `ScriptMonitorFidelityGenerateBll` 内部串行执行 4 次 AI 调用（cueType=18 或 25 三次独立 + cueType=19 或 26 一次合并），按 `speechMode` 选循环/非循环提示词；失败时归还剩余预扣 + status=GENERATE_FAILED + originalStatus 恢复
  - 生成完成后合并报告写 MongoDB `tb_script_monitor_report_body.content`；主表 `reportBodyId` + `score` + `deviation_summary` + `summary_json` 回填；status=GENERATED

- **Delta（真正需要关闭的 Gap）：**
  1. 解除 3 处限闸（G 项）：删除 70014 分支 + 替换 480-483 占位为真实标准稿查询 + autoTriggerReport 加 monitorType=1 分支
  2. MqHandler fidelity tag 路由从 skip 改为 `fidelityGenerateBll.generate(...)`
  3. 新建 `ScriptMonitorFidelityGenerateBll`（4 次串行 AI 编排 + `speechMode` 分支 + `cycleDurationMinutes` 填充 + Token settle per call + 失败补偿）
  4. 跨模块查 `StandardScript`：新建 `StandardScriptFeign` SPI（replay-generic 声明 + replay-words 实现 + replay-ai 消费）
  5. `autoTriggerReport` monitorType=1 前置校验：`StandardScriptFeign.findValid` 确认有效标准稿存在（mirror B7 质检/巡检路径）

---

## Acceptance Criteria (Given / When / Then)

### Happy Path

**AC-001（triggerReport 手动触发 monitorType=1 全链路）**

Given 已登录用户，sourceType=0/1 + sceneType 合法，按 (tenantId+userId+secUid=video.anchorSecUid) 在 tb_standard_script 有 is_deleted=0 记录，Token 余额 ≥ 100000，
when POST `/replay/script-monitor/triggerReport`（monitorType=1），
then HTTP 200 + `R<Boolean>(true)`；tb_script_monitor_report 写入一行 status=GENERATING；MQ 本地消息表写入 fidelity tag 一条。不再抛 70014。

**AC-002（MQ Consumer fidelity tag → FidelityGenerateBll 生成）**

Given tb_script_monitor_report 一行 status=GENERATING，MQ consumer 收到 fidelity tag，
when `ScriptMonitorFidelityGenerateBll.generate(report, originalStatus, withhold)`，
then 按 speechMode 取 cueType=18(或 25) + cueType=19(或 26) 提示词，串行执行 4 次 AI 调用；合并报告写 MongoDB + 主表 reportBodyId 回填；status=GENERATED；score/deviation_summary/summary_json 来自 AI 合并报告 `<aifupan-data-block>` 标签解析；settleAiToken 每次调用各 settle 一次（共 4 次）。

**AC-003（autoTriggerReport monitorType=1 自动触发路径 B7）**

Given 复盘完成事件触发 `ScriptMonitorApi.autoTriggerForVideo`，`isScriptFidelityMonitor=1` 开关开启，按 (tenantId+userId+secUid) 有有效标准稿，Token 余额 ≥ 100000，
when `autoTriggerReport(videoId, userId, tenantId, monitorType=1)`，
then report 新行 status=GENERATING 写入 + MQ fidelity tag 投递；方法返回 true；不抛异常；trigger_source='auto'。

**AC-004（循环模式 speechMode=1 全链路，cycleDurationMinutes 填充）**

Given 有效标准稿 speechMode=1，cycleDurationMinutes=30，
when FidelityGenerateBll 填充提示词占位符，
then cueType=25（循环对比）/ cueType=26（循环合并）被选中，`#{cycleDurationMinutes}` 占位符被填充为 "30"，4 次 AI 调用正常执行，status=GENERATED。

---

### Edge Cases

**AC-005（triggerReport monitorType=1 无标准稿 → 70005）**

Given 按 (tenantId+userId+secUid) 在 tb_standard_script 无 is_deleted=0 记录，
when POST `triggerReport`（monitorType=1），
then 抛 `BusinessException(70005)`（SCRIPT_MONITOR_STANDARD_SCRIPT_NOT_CONFIRMED）；不创建 report 行；不预扣 Token。

**AC-006（triggerReport monitorType=1 第一道闸已移除验证）**

Given `ScriptMonitorBll.java:449-453` 原 monitorType=1 硬抛 70014 分支已删除，
when POST `triggerReport`（monitorType=1, 其余参数合法），
then 不抛 70014；进入后续校验链（资源归属 → accountType → Token → 标准稿）。

**AC-007（AI 第 2 次调用失败 → 归还剩余预扣 + GENERATE_FAILED + originalStatus 恢复，mirror 质检失败补偿）**

Given 4 次 AI 调用串行，第 1 次独立报告（idx=0）成功 settle，第 2 次（idx=1）AI 返回 status=1（失败），
when `ScriptMonitorFidelityGenerateBll.generate`，
then `returnAiToken(withhold)` 被调用 1 次（仅一次，不双重归还）；`restoreOrFail(reportId, originalStatus, oldBodyId, oldSummaryJson, errorMsg)` 被调用；report status=GENERATE_FAILED（原 status 非 GENERATED 场景）。

**AC-008（speechMode=1 但 cycleDurationMinutes=null → 70013，不调 AI）**

Given 标准稿 speechMode=1，cycleDurationMinutes=null（tb_standard_script 该字段为 null 或未传），
when FidelityGenerateBll 组装循环模式提示词 fillPlaceholder，
then 抛 `BusinessException(70013, SCRIPT_MONITOR_PARAM_INVALID)`；不预扣 Token；不发起任何 AI 调用（在 preDeduct 之前或在提示词组装阶段校验）。

**AC-009（autoTriggerReport monitorType=1 已在 GENERATING → skip，mirror 现有 B7 质检 skip 逻辑）**

Given 同一 (sourceType+sceneType+sourceId+monitorType=1) 的 report 已存在且 status=GENERATING，
when `autoTriggerReport(videoId, userId, tenantId, monitorType=1)`，
then 方法返回 false；不创建新 report 行；不重复投递 MQ；日志 warn "[B7 自动触发] 报告已在生成中"。

**AC-010（手动 triggerReport 命中已存在 GENERATING → 短路防重，mirror AC-5 Dispatch Constraint B7 范式）**

Given 同 (sourceType+sceneType+sourceId+monitorType=1) 已有 status=GENERATING report，
when 用户手动 POST `triggerReport`（monitorType=1），
then `findReport` 命中 GENERATING + `createGeneratingTaskAndReturn` 内部幂等短路（同现有质检/巡检防重逻辑）；不重复扣 Token；不重复投递 MQ；返回 HTTP 200（原有 report）。

**AC-011（质检 monitorType=0 + 巡检 monitorType=2 回归，限闸解除后不影响现有链路）**

Given G 项三处限闸代码改动后，
when POST `triggerReport(monitorType=0)` 和 `triggerReport(monitorType=2)`，
then 质检/巡检触发链不受影响；MqHandler 质检/巡检分支路由正常；`ScriptMonitorGenerateBll` / `ScriptMonitorPatrolGenerateBll` 无异常；日志无 NPE / ClassCastException。

**AC-012（ASR 数据为空 → NOT_APPLICABLE + returnAiToken，mirror 质检范式）**

Given report status=GENERATING，ASR 数据 `sensitiveWordsFeign.getAnalysisData` 返回空，
when `ScriptMonitorFidelityGenerateBll.generate`，
then `returnAiToken(withhold)` 被调用；`markNotApplicable(reportId, reason)` 被调用；status=NOT_APPLICABLE；不抛异常到 MqHandler。

---

## Hidden Scope（callers / dependents 受影响但未在 PRD 中点名的代码点）

1. **`replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorMqHandler.java:107-108`** — fidelity tag 当前 skip 分支需改为 `doFidelityGenerate(report, msg, messageKey)`（或直接调 `fidelityGenerateBll.generate(report, originalStatus, withhold)`）。需确认 doFidelityGenerate 私有方法签名与 doGenerate / doPatrolGenerate 保持对称。

2. **`replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorBll.java:resolveMqTag`（大约 :633-644）** — 已有 `tagFidelityMonitor` 分支返回对应 tag，无需修改；确认 `ScriptMonitorMqProperties.BusinessTags.tagFidelityMonitor` 已在配置里赋值（否则 resolveMqTag 返 null，触发 MQ 投递失败路径）。

3. **`replay-ai/src/main/java/com/jiuyu/replay/ai/api/ScriptMonitorApi.java`（B7 入口）** — `autoTriggerForVideo` 实现里需在 `isScriptFidelityMonitor=1` 判断分支中：a. 通过 `StandardScriptFeign` 确认有效标准稿存在；b. 调用 `scriptMonitorBll.autoTriggerReport(videoId, userId, tenantId, monitorType=1)`。与质检/巡检并列触发，失败不阻断其他监控类型。

4. **`replay-generic/src/main/java/com/jiuyu/replay/generic/feign/words/`**（新建 `StandardScriptFeign`）— 跨模块 SPI 新通道，replay-ai 的 `ScriptMonitorFidelityGenerateBll` 和 `ScriptMonitorBll`（480-483 标准稿校验）需注入此 Feign。replay-words 模块需新建 `StandardScriptApi implements StandardScriptFeign` 暴露 `findValid` 方法。

5. **`replay-words/src/main/java/com/jiuyu/replay/words/api/StandardScriptApi.java`（新建）** — `StandardScriptFeign` 的 replay-words 实现，内部调用 `StandardScriptService.findValid(tenantId, userId, secUid)`。

6. **`replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorBll.java:480-483`** — TODO 占位替换：注入 `StandardScriptFeign` + 调 `findValid`，无结果抛 70005。需注意 secUid 来源：triggerReport 目前入参只有 `sourceId`（videoId），需通过 `loadSingleVideo` 拿到 `video.getAnchorSecUid()`（或等价字段名）后再查标准稿。需 grep 确认 `AnchorVideoInfoVo` 是否含 `secUid` 字段。

7. **`replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorBll.java:autoTriggerReport`** — 参数列表现有 `(String videoId, Long userId, Long tenantId, Integer monitorType)`，若加 monitorType=1 支持需注入 `StandardScriptFeign` + 在 monitorType=1 时校验标准稿存在（secUid 来自入参或从 video 取）。需确认 autoTriggerReport 当前是否可拿到 secUid（现有调用链传了 secUid 参数，见 `ScriptMonitorFeign.autoTriggerForVideo` 有 secUid 参数）。

8. **`replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorPatrolGenerateBll`（失败补偿对称参考）** — FidelityGenerateBll 须 mirror 巡检的 withhold=null 自动触发容错（`autoTriggerReport` 不预扣 Token，传 withhold=null；`returnAiToken(null)` 需幂等处理，mirror 巡检 :248 的 `try { aiTokenWithholdFeign.returnAiToken(withhold) }` 兜底）。

9. **`replay-ai/src/main/java/com/jiuyu/replay/ai/entity/ScriptMonitorReportEntity`** — 需确认 `score` / `deviation_summary` / `speech_speed` 字段已存在（launch_spec F 项描述从合并报告提取写入）。若字段未在 entity 中声明，需补充（本 Slice 不做 DDL，只是确认已有字段能写入）。

10. **ZombieHandler / reportStatus 定时扫描兼容性** — 若有 zombie job 扫 status=GENERATING 超时的 report 并重置为 NOT_GENERATED 或 GENERATE_FAILED，则 monitorType=1 的报告也会被扫到。需确认现有 ZombieHandler（如有）是否按 monitorType 过滤或全量处理，Slice B 无需改 ZombieHandler 但需登记 TECH-DEBT。

---

## Open Questions

以下 7 项为 launch_spec 中列出的 /h-design 候选议题（A-G），每项附初步技术分析供 `@system-architect` 参考；不替 architect 拍板。

**A. 标准稿铺排策略（Java 端 vs AI 端做循环铺排？）**

分析：提示词（`docs/2.6.01/REQ-2026-0508-话术还原度/循环话术对比提示词.md`）已在系统提示词中描述了循环判定规则（按 cycleDurationMinutes 铺排），AI 端已知道如何处理循环。Java 端的职责仅需：① 向 AI 传入整场 ASR + 标准稿 time_axis_script + cycleDurationMinutes 占位符；② 填充 `#{cycleDurationMinutes}` 为具体数值；③ 不做 Java 端时间轴切片/铺排。
推荐：Java 端只填占位符 + 整场喂入（与提示词约定一致，不引入 Java 端铺排逻辑）。
**需拍板点：** `#{standardScript}` 占位符的实际值填整个 time_axis_script JSON String？还是格式化后的文本？提示词里的格式约定需确认。

**B. AI 调用编排顺序（串行 vs 并行 vs 半并行）**

分析：
- 串行（mirror ScriptMonitorGenerateBll 范式）：实现最简，totalTokens 按单线程累加，失败中途 throw 直接走 catch，逻辑确定性强；缺点是慢（3 次独立 + 1 次合并，约 4 × 单次 AI 延迟，可能 60-90s）。
- 并行（3 次独立并行 + 1 次合并串行）：需引入 CompletableFuture 或线程池，Token 累加需同步（AtomicInteger），任一失败需 cancel 其余并统一 catch，复杂度显著升高；但速度约为串行 1/3。
- 半并行：介于两者，实现较并行略简。
推荐：串行（与质检范式完全镜像，确定性强；还原度是全新功能，首版以正确性为主，可 Slice D 优化为并行）。
**需拍板点：** 是否接受串行的速度（约 60-90s），还是要求并行以控制用户等待？

**C. 跨模块 standardScript 查询路径（新建 StandardScriptFeign vs 扩 ScriptMonitorFeign vs 违规 DAO 直连）**

分析：
- 新建 `StandardScriptFeign`（replay-generic 声明）：最符合 jiuyu 跨模块 SPI 原则（CLAUDE.md §5），职责单一，后续 Slice C/D 复用方便。成本：多建 1 个 Feign 接口 + replay-words 1 个 Api 实现 + replay-ai 注入。
- 扩 `ScriptMonitorFeign`：已有通道复用，少建一个接口，但语义混乱（ScriptMonitorFeign 本是"监控触发"的 SPI，标准稿查询与之语义不符）。
- DAO 直连：明确违反 CLAUDE.md §5 跨模块硬约束，直接 review FAIL。
推荐：新建 `StandardScriptFeign`。
**需拍板点：** 确认架构师同意；命名和方法签名（`findValid(Long tenantId, Long userId, String secUid) → StandardScriptInfoVo`，注意跨模块不能传 Entity 对象，需新建 `StandardScriptInfoVo` 在 replay-generic 的 `vo/words/` 下）。

**D. Token 边界（4 次 AI 调用每次 settle vs 一次性 settle 总 token）**

分析：
- 每次 settle（mirror 质检 buildSettleResult 范式：串行累加 totalTokens，最终一次性 settle）：台账记录一条（合并模型为代表），可追溯总消耗，实现简单；中途失败 → 已累加的 totalTokens 未 settle 直接 returnAiToken，对用户无损。
- 4 次分别 settle（含成功 settle 的无法退回）：partial settle 逻辑复杂，失败补偿时仅能 return 剩余预扣，已 settle 的 token 不退；台账 4 条记录，按模型维度可分析各次消耗。
实际上，"每次 settle" 有两种解读：① 每次 AI 调用后 settle 该次消耗（4 次独立 settle）；② 累加 totalTokens，最终合并后一次性 settle（1 次 settle）。
质检范式是方案②（totalTokens 循环累加，最终 1 次 settleAiToken）。
推荐：方案②（1 次 settle，mirror 质检范式）。
**需拍板点：** 确认 launch_spec D 项"每次 settle"是指①还是②；推荐②减少台账碎片。

**E. 失败恢复（同 AC-007）**

分析：完全 mirror `ScriptMonitorReportWriteService.restoreOrFail` 语义：
- originalStatus=GENERATED → 恢复旧成功报告（oldBodyId/oldSummaryJson 回填）
- originalStatus!=GENERATED → 置 GENERATE_FAILED
- returnAiToken 仅归还"预扣剩余"（方案②下 4 次 AI 调用期间有 settle 的那部分已消耗，无法退；但预扣是一次性扣的，settle 的部分在 settle 时已从预扣池里划走，returnAiToken 归还的是"预扣池中尚未 settle 的部分"）。
拍板点：已由 launch_spec 给出语义，无争议。

**F. 报告主表字段使用（score/deviation_summary/speech_speed/summary_json 来源）**

分析：
- `score`（还原度评分 0-100）：从 AI 合并报告 `<aifupan-data-block>` 标签内 JSON 提取 `score` 字段；无则 null。
- `deviation_summary`（偏差摘要）：同标签内 JSON 提取 `deviationSummary` 或 `deviation_summary` 字段。
- `speech_speed`：选项①从 AI 报告提取；②Java 端估算（ASR 总字数 / ASR 总时长（秒） × 60 → 字/分钟）。方案②无需 AI 提供，更可靠。
- `summary_json`：存整个 `<aifupan-data-block>` 标签内 JSON String（mirror 质检/巡检 buildSummary 范式）。
需确认：AI 合并报告的 `<aifupan-data-block>` 标签内 JSON Schema（提示词约定的输出字段名称），特别是 score/deviation 字段名。需在 Implement 前读循环/非循环合并提示词的输出格式约定。
**需拍板点：** speech_speed 来源（AI vs Java 估算）；`<aifupan-data-block>` 内 JSON 字段名（需 architect 读提示词后确认）。

**G. 限闸解除三处（同已分析）**

分析：
- triggerReport 449-453 删除 70014 分支：直接删除 3 行 if-throw；注意要保留 HOTFIX 2026-06-06 的 sourceType-sceneType 合法性校验（:444-448），不要误删。
- triggerReport 480-483 替换占位：注入 `StandardScriptFeign`，通过 `loadSingleVideo` 拿到 `video.anchorSecUid`（需确认字段名），调 `StandardScriptFeign.findValid(tenantId, userId, secUid)`，null 时抛 70005。
- autoTriggerReport monitorType=1 支持：在 autoTriggerReport 中，monitorType=1 时加前置校验（StandardScriptFeign.findValid），无标准稿返回 false + warn（mirror 巡检无弹幕路径）；通过则继续 createGeneratingTaskAndReturn + resolveMqTag + syncSendAndDeliverToTopic。
**需拍板点：** `AnchorVideoInfoVo` 是否有 `anchorSecUid`/`secUid` 字段（否则 480-483 处无法取到 secUid；需 grep 确认，影响实现路径）。

---

## Recommended Allowed Scope（Implement 阶段 focus_card 候选清单）

以下为按功能域分组的精确文件路径（`@system-architect` 制作 focus_card 时直接抄入）：

### 1. 限闸解除 + autoTriggerReport 扩展
- `replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorBll.java`

### 2. MqHandler fidelity tag 路由
- `replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorMqHandler.java`

### 3. 新建还原度生成 Bll（核心）
- `replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorFidelityGenerateBll.java`（新建）

### 4. 跨模块 SPI 新通道
- `replay-generic/src/main/java/com/jiuyu/replay/generic/feign/words/StandardScriptFeign.java`（新建）
- `replay-generic/src/main/java/com/jiuyu/replay/generic/vo/words/StandardScriptInfoVo.java`（新建，跨模块传输 VO，不传 Entity）
- `replay-words/src/main/java/com/jiuyu/replay/words/api/StandardScriptApi.java`（新建，实现 StandardScriptFeign）

### 5. B7 autoTriggerForVideo 扩展（ScriptMonitorApi 实现层）
- `replay-ai/src/main/java/com/jiuyu/replay/ai/api/ScriptMonitorApi.java`

### 6. 配置属性确认（如 MQ BusinessTags 未配置 tagFidelityMonitor）
- `replay-ai/src/main/resources/application.yml`（或对应环境配置文件，确认 `business-tags.tag-fidelity-monitor` 值已配置）

### 7. 可能涉及（视 F 项拍板结果）
- `replay-ai/src/main/java/com/jiuyu/replay/ai/entity/ScriptMonitorReportEntity.java`（确认 score/deviation_summary/speech_speed 字段存在；若不存在需补字段）

---

## Wiki Sources Consulted

- `.claude/llm_wiki/archive/Change__2026-06-11_12-33-48__restoreDegree-slice-a__openspec.md`（Slice A 完工契约，Domain Model / API Contract / Data Model / Business Logic）
- `.claude/runs/launch_spec_20260611_185820.md`（Slice B Pre-decided Constraints / Non-Goals / 完工验收口径 / A-G 候选议题）
