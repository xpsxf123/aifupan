# 技术债务台账

> 项目级技术债登记。开发过程中遇到"知道但当前任务不便处理"的历史债务 / 重复实现 / 应收敛但暂不迁移的存量代码，统一登记于此，便于团队定期 review 并安排独立清理任务。
>
> **使用规则：**
> - 每条债务有唯一编号 `DEBT-NNN`（顺序递增）。
> - 发现即登记，不要"心里记着"。
> - **每条必须用 `### DEBT-NNN — 标题` h3 + 字段表格范式**（DEBT-001 为范本），表格至少含「发现日期 / 来源任务 / 风险等级 / 状态」四项；DONE 时加「关闭日期」并补关闭说明。**禁止省略表格写成纯段落**（DEBT-012~022 早期违规已在 2026-06-04 收敛）。
> - 状态 ∈ `{OPEN, DONE, WONT-FIX}`。修复后**同 commit 内**改状态 + 关联 commit hash（短哈希前 7 位）。
> - 风险等级 ∈ `{HIGH, MEDIUM, LOW}`（旧标 P1/P2/P3 等价：P1=HIGH / P2=MEDIUM / P3=LOW）。
> - 单独的债务清理任务（PATCH 或 Standard）一次处理一条或一批同主题，与新功能解耦。
> - 相关代码处可留 `// see DEBT-NNN` 注释（仅当影响理解必要时；外科手术原则，能不留就不留）。

---

## 进行中 / 待处理

### DEBT-001 — Token 预扣"结算编排"在 api 层重复 3 份，应迁移至 order 收敛 SPI

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-05-30 |
| 来源任务 | 话术智能监控 B4 / `.claude/runs/Change__2026-05-22_script-monitor/` |
| 风险等级 | MEDIUM |
| 状态 | OPEN |

**背景：** B4 设计阶段调研发现，预扣 Token 的底层原子（`isPropertyHaveAiToken` / `removeTempUserProperty` / `UserPropertyImpl.use`）已在 `replay-order` 收敛唯一实现，但**"查 aiTokenNum 余额 → 按余额封顶 → setNum 负 → 清预扣缓存 → 实扣"这段结算编排**在 api 层至少重复了 3 份。

**散落点（影响范围）：**
1. `replay-api/.../logic/order/impl/UserPropertyLogicImpl.java:267-298` — `assetsMinusOrPlusReal`
2. `replay-api/.../logic/third/impl/AiRelatedLogicImpl.java:813-831` — `aiTokenUse`（SSE AI 问答路径）
3. `replay-api/.../logic/words/impl/DataScreenshotLogicImpl.java:320-346` — 数据截图扣 Token

预扣 `withhold` 构造、返还 `removeTempUserProperty` 调用也在多处各自封装。

**推荐处理方案：** 在 `replay-order` 新建一组高内聚的 AI Token SPI（`withholdAiToken` / `settleAiToken` / `returnAiToken`），把"结算编排"沉到 order 唯一实现；调用方只保留 `withhold → try{业务} catch{return} → settle` 三行薄骨架。然后**逐一迁移**上述 3 处，删除重复实现。

**为什么 B4 不一并迁移：** 这 3 处全在线上 AI 问答 / 数据截图主路径，迁移有回归风险。B4 当前任务（话术质检报告生成）选择"范围 A（克制）"——只让新代码（质检生成）从一开始用收敛 SPI，存量迁移作为独立技术债延后处理，避免一次改动同时背新功能 + 跨模块重构两个风险。

**关联：** B4 的 B4-0 子任务（"在 order 建立 AI Token 收敛 SPI"）是本债务清理的前置；B4-0 完成后本债务即可启动迁移。建议作为独立 Standard MEDIUM 任务执行，需 AI 问答 / 数据截图相关接口的回归测试覆盖。

---

### DEBT-003 — 异步生成场景直调 `aiTokenUseRecordFeign.saveAiTokenUseRec` 应收敛到 SPI

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-05-30 |
| 来源任务 | 话术智能监控 B4-2b（lead-engineer 报告 Pre-existing Issue） |
| 风险等级 | LOW |
| 状态 | OPEN |

**背景：** B4-2b 实现质检报告异步生成时，AC-5 反退化护栏 `grep` 命中 `ConversationBll` 第 559 / 794 行直调 `aiTokenUseRecordFeign.saveAiTokenUseRec`。这是"赠送能力（HTML 美化 / AI 内容纠正）异步生成只记台账不扣余额"的模式，与 DEBT-001 的"预扣/结算/返还三段式"不是同一编排范式，但同样属于"调用方直调底层 Token SPI 而非走收敛入口"。

**影响范围：**
1. `replay-ai/.../bll/ConversationBll.java:559` — `serviceGenerateHtml` 异步 HTML 生成后记台账
2. `replay-ai/.../bll/ConversationBll.java:794` — `doServiceCorrectAiContent` AI 内容纠正后记台账

可能还有其他业务域 / api logic 层同款直调，本任务未全量 `grep` 确认。

**推荐处理方案：** 在 `AiTokenWithholdFeign`（B4-0 SPI）扩一个方法 `recordAiTokenOnly(Long userId, AiReturnDataVo aiReturn, AiTokenUseRecordBo recordBo)`（只记台账不扣余额），上述两处迁移到该 SPI。同时全量 grep `saveAiTokenUseRec` 调用方一并迁移。

**为什么 B4-2b 不一并修：** 超出 B4-2b Allowed Scope（`ConversationBll` 不在 scope）；且 `ConversationBll.serviceGenerateHtml` 是线上 HTML 美化主路径，有回归风险。新代码（B4-2b 质检生成）通过 `settleAiToken` 内部已隐含记台账，零新增 `saveAiTokenUseRec` 直调，AC-5 护栏验证通过。

**关联：** 与 DEBT-001 同源（都是 AI Token SPI 散落），建议合并为"AI Token 编排全局收敛"清理任务一次处理。

---

### DEBT-004 — `anchorVideoFeign.GetByVideoId()` 大写命名违规（存量 SPI）

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-01 |
| 来源任务 | 话术智能监控 B4 修补包-3 review（code-reviewer 报告 M4） |
| 风险等级 | LOW |
| 状态 | OPEN |

**背景：** B4 `ScriptMonitorBll.loadSingleVideo` 调用 `anchorVideoFeign.GetByVideoId(...)`，方法名首字母大写违反 Java lowerCamelCase 命名规范。这是存量 Feign 接口命名问题，不在 B4 引入，但 B4 新代码调用了它。

**推荐处理方案：** 在 `AnchorVideoFeign` 接口加规范别名 `getByVideoId`（@Override 转发到大写方法），逐步迁移调用方，最后删除大写版本。需要全量 grep 现网所有调用方，统一切换。

**为什么 B4 不一并改：** 涉及现网所有 Feign 调用方（words/ai/api 多模块），单独一次性改造更安全。

---

### DEBT-005 — `settleAiToken` 成功但 `finishReport` 失败的 Token 一致性裂口

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-01 |
| 来源任务 | 话术智能监控 B4 修补包-3 review（code-reviewer 报告 P5） |
| 风险等级 | LOW |
| 状态 | OPEN |

**背景：** `ScriptMonitorGenerateBll.generate()` 顺序 = `finishReport(GENERATED)` → `settleAiToken`。若 finishReport 成功但 settleAiToken 失败，进 catch 又跑 returnAiToken，结果 = 状态 GENERATED + Token 未实扣（白嫖一份报告）。

**推荐处理方案：** 引入"对账补偿表"，settle 失败时写一行待对账记录 + 定时任务扫描重试 settle；catch 内部区分 settle 阶段失败 vs 其他阶段失败，避免错误调 returnAiToken。

**为什么暂不改：** B4 当前先以 MQ 重试 + XXL-Job 僵尸恢复兜大头，settle 失败概率极低；待生产实际触发后再评估改造紧急度。

---

### DEBT-006 — `generate()` ASR / 中间报告合并文本长度无上限

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-01 |
| 来源任务 | 话术智能监控 B4 修补包-3 review（code-reviewer 报告 P6） |
| 风险等级 | LOW |
| 状态 | OPEN |

**背景：** `ScriptMonitorGenerateBll.tryLoadAsrText` 拼接全部 ASR 句子、`generate()` 用 `\n---\n` 拼接 3 份中间报告，无字数 cap。长直播间（4-6h 大主播）可能超 AI 模型上下文窗口（32k-128k token），chatCompletion 返 status=1 整批回滚。

**推荐处理方案：** 加字数 cap（如 50000 字 ≈ 25k token），超长 truncate 或按段落拆段分批生成再二次合并。

**为什么暂不改：** B4 当前业务侧报告字数预期可控，未达到必须处理的优先级。

---

### DEBT-007 — `generate()` 缺少任务级可见性（卡在哪一步无法定位）

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-01 |
| 来源任务 | 话术智能监控 B4 修补包-3 review（code-reviewer 报告 P7） |
| 风险等级 | LOW |
| 状态 | OPEN |

**背景：** 用户提交后只能看 `status=1/2/3/4`，卡在哪一步（取 ASR / AI 调用第 X 次 / 合并 / 写 Mongo / settle）完全黑盒。出问题只能翻 log，不能按 reportId grep 跨服务请求链。

**推荐处理方案：** `tb_script_monitor_report` 加 `generate_step` 字段（VARCHAR），`generate()` 关键步骤更新进度；前端可展示"质检中（步骤 3/6）"。

**为什么暂不改：** 当前用户量小，运维侧手动 grep log 仍可定位；后续运维压力增大再做。

---

### DEBT-008 — `listByCueType` SPI 不支持租户级提示词自定义

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-01 |
| 来源任务 | 话术智能监控 B4 修补包-3 review（code-reviewer 报告 M3） |
| 风险等级 | LOW |
| 状态 | OPEN |

**背景：** B4 修补包-3 在 `CueWordsProducerImpl.listByCueType` 加 `tenantId=0` hardcode 过滤（仅查平台通用提示词）。如果业务方未来需要租户级自定义质检/还原度/巡检提示词，需扩 SPI 链路：`CueWordsFeign.getCueWordsByType(cueType, tenantId)` → `CueWordsApi` → `CueWordsProducer` → `Impl` 全链路加 tenantId 参数。

**为什么 B4 不一并改：** 当前需求只用平台通用提示词，hardcode tenantId=0 最小改动；扩 SPI 等真实需求出现时做。

**关联演变：** 本 SPI 在 2026-06-01 经历两次精化 —
（1）原 `listMonitorPrompts(monitorType)` 通过新增 `tb_cue_words.monitor_type` 字段路由 → 回滚为 `listByCueType(cueType)` 通过 `AiEnums.askType` 新增枚举值路由，零 DDL 改动。
（2）3 枚举（16=质检/17=还原度/18=巡检，每个 cueType 多条记录靠 sort 区分生成/合并）→ 6 枚举（16/17 质检生成+合并，18/19 还原度生成+合并，20/21 巡检生成+合并；每个 cueType 单条独立记录），遵循现网 13/14/15 一对一映射范式。

---

### DEBT-009 — B4 D-7 fail-safe 路径（GENERATED 重生成失败回旧成功）未真测

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-01 |
| 来源任务 | 话术智能监控 B4 修补包-4（测试期 e2e 闭环） |
| 风险等级 | LOW |
| 状态 | OPEN |

**背景：** B4 设计文档 D-7 "GENERATED 重生成失败必须保留旧 reportId 不变"。代码实现是 `triggerReport` 进入重生成走 `status=GENERATING` 路径，若 `ScriptMonitorGenerateBll.generate()` 抛异常则进 `markFailed` 不动 reportId。但测试期 e2e 验证时，重生成两次都成功了，**fail-safe 分支未被实际命中**。

**推荐处理方案：** mock `aiBll.chatCompletion` 抛异常 / 强制 settleAiToken 失败 / 关掉 AI 模型 endpoint，构造 generate 失败场景，验证：
1. reportId 不变（保持原 GENERATED 报告 ID）
2. isRead / confirmedRecords 不被破坏
3. status 仍可手动重置为 FAILED 后再次触发

**为什么暂不改：** 已有 39 单测覆盖单元级；e2e 真实失败构造成本高（需要临时改环境），上线后用真实异常监控兜底。

---

### DEBT-010 — B4 场景 5 Token 扣返 fail-safe 未真测

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-01 |
| 来源任务 | 话术智能监控 B4 修补包-4（测试期 e2e 闭环） |
| 风险等级 | LOW |
| 状态 | OPEN |

**背景：** B4 设计文档场景 5："generate() 失败时 catch 块调 `returnAiToken` 把预扣 Token 退回"。代码实现已在 `ScriptMonitorGenerateBll.generate()` 的 catch 中调用 `aiTokenWithholdFeign.returnAiToken`，但测试期 e2e 5 个场景跑通时，generate 全部成功，**fail-safe 分支同样未被实际命中**。

**推荐处理方案：** 与 DEBT-009 合并测，构造 generate 失败场景：
1. 触发前查 `tb_type_surplus.surplus_num` 余额 X
2. 触发 generate（让它失败）
3. 再查 `tb_type_surplus.surplus_num` 必须等于 X（已回滚）
4. 验证 `tb_user_property_details` 无负数实扣记录
5. 验证 `tb_temp_user_property` 预扣记录已清

**关联：** 与 DEBT-005（settle 成功 finishReport 失败的 Token 一致性裂口）相关，但 DEBT-010 验证的是 settle 之前的失败路径（returnAiToken），DEBT-005 是 settle 之后的兜底问题。两者一起做 fail-safe 覆盖更高效。

---

### DEBT-011 — 手动 triggerReport 拒绝 monitorType=2 互动巡检，B7 自动触发失败后无人工兜底

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-02 |
| 来源任务 | 话术智能监控 B7 e2e 测试 |
| 风险等级 | MEDIUM |
| 状态 | DONE |
| 修复 commit | 0526df6（互动巡检 T31-T36 同 PR）|

**背景：** B7 实现了"录制完成自动触发互动巡检报告"链路（status=2 → ScriptMonitorFeign.autoTriggerForVideo → autoTriggerReport → 直发 MQ）。但**手动** triggerReport 接口对 monitorType=2 仍返 70014 "话术还原度 / 互动巡检功能未上线"。

**影响：** 自动触发的巡检任务失败 / 用户想重新生成时，**没有人工兜底路径** —— 用户在前端点"重新生成"按钮（如果有）会撞 70014。

**散落点：** ScriptMonitorBll.triggerReport 内部 loadCueWords 或类似前置校验把 monitorType IN (1, 2) 都拒（B4 时一并暂禁了还原度和巡检手动）。

**推荐处理方案：** 改 ScriptMonitorBll.triggerReport 的拦截逻辑：
1. monitorType=1 (还原度) → 仍拒 70014（B6 暂禁还原度开关，自动也不会触发）
2. monitorType=2 (巡检) → **放开**，走与质检（monitorType=0）相同的手动校验路径（Token + 自有账号 + 套餐版本，不校监控位剩余）

**为什么 B7 不一并修：** B7 的 scope 是"自动触发挂载 + MQ 双轨改造"，手动巡检放开属于另一个功能点，避免 scope 蔓延导致 review 复杂度爆炸。

**关联：** 与 B3 还原度能力（standardScriptId / generateStandardScript / confirmStandardScript / fidelityReportDetail）一起做更高效 —— 还原度上线时这块拦截逻辑都要改。建议合并到 B3 批次。

---

### DEBT-012 — ScriptMonitorBllTest.triggerReport 2 用例 MQ mock 缺失

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-02 |
| 来源任务 | reportStatus secUid 改造（pom excludes 删除暴露 pre-existing fail） |
| 风险等级 | MEDIUM |
| 状态 | OPEN |

**背景：** B7 改造（2026-06-02）把 triggerReport 从"伪同步 MQ"升级为"业务直发 + XXL-Job 兜底"路径，内部走 `RocketMqBll.syncSendAndDeliverToTopic`。但 `ScriptMonitorBllTest` 中 2 个 trigger 用例 mock 未补该 RocketMqBll 路径，致 `ApplicationException(code=70013, msg=报告生成任务提交失败)`。本次（report-status-secuid 任务）删除 pom excludes 暴露此 pre-existing fail，外科手术原则下用 `@Disabled` 临时跳过。

**影响范围：** `replay-ai/src/test/java/com/jiuyu/replay/ai/bll/ScriptMonitorBllTest.java` 两个方法：
- `triggerReport_质检首次触发_创建生成任务`
- `triggerReport_已成功报告重新触发_置为生成中状态`

**推荐处理方案：** 在 `@BeforeEach` mock RocketMqBll，让 `syncSendAndDeliverToTopic` 默认返成功；或显式 mock 该方法返 true。预计 ≤ 10 行测试代码改动。

---

### DEBT-013 — MongoDB script_monitor_intermediate_report 集合无 TTL 清理

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-03 |
| 来源任务 | B7 改造期发现 |
| 风险等级 | MEDIUM |
| 状态 | OPEN |

**背景：** 中间报告（3 份/质检报告）存 Mongo `script_monitor_intermediate_report`，跑完合并后**不删除**。重新生成时 `deleteByReportId` 会覆盖，但从未重新生成的报告会一直保留中间产物（合并完成后已无业务用途）。

**影响：** 每日 N 个质检报告 → 3N 条中间文档永久积累。1000 报告/天 = 110 万条/年。

**推荐处理方案：** Mongo `@Indexed(expireAfterSeconds=...)` TTL 索引或 XXL-Job 定时 `deleteByReportIdsAndStatusIn(GENERATED)`。保留 7 天足够 D-7 失败回滚窗口。

---

### DEBT-014 — RocketMQ Listener consumptionThreadCount 未显式配，吃 SDK 默认 20

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-03 |
| 来源任务 | B7 改造期发现 |
| 风险等级 | HIGH |
| 状态 | OPEN |

**背景：** 项目 2 个 `@RocketMQMessageListener`（ScriptMonitorMqListener、RocketMqListener）都没显式 `consumptionThreadCount`，吃 `rocketmq-client-java:5.0.7` 默认 20。SDK 版本升级会静默改变并发行为。

**影响：** 100 条 MQ 任务排队时同时处理 20 条 → 5 × 单条耗时（质检 ~45s = 3.75 分钟）；burst 场景前端体感慢。

**推荐处理方案：** `RocketMQConfigBeanPostProcessor` 自定义 ConsumerBuilder 显式 `setConsumptionThreadCount(N)`（注解不支持该字段）。N 取值需配合豆包 QPS 配额（典型 32-64）。

---

### DEBT-015 — ScriptMonitorMqListener 锁 lease 60s < AI 最大耗时 90s

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-03 |
| 来源任务 | B7 改造期发现 |
| 风险等级 | MEDIUM |
| 状态 | OPEN |

**背景：** `tryLock(30, 60, TimeUnit.SECONDS)` 锁持有 60s，但单条质检任务 AI 调用慢时可达 80-90s。锁过期后 broker 重投会进入第二个 consumer 线程，靠 `tb_mq_message_record.messageStatus` + `report.status == GENERATED` 双幂等兜底跳过。**功能正确但浪费 1 个 MQ 线程位**。

**推荐处理方案：** 改 `tryLock(30, 180, SECONDS)`。或动态根据 monitorType 调整（质检 180s / 还原度 60s / 巡检 90s）。

---

### DEBT-016 — CallerRunsPolicy 雪崩链（5 个池借调用方线程，可能打挂 Tomcat）

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-03 |
| 来源任务 | B7 改造期线程池审计 |
| 风险等级 | HIGH |
| 状态 | OPEN |

**背景：** `threadPoolTaskExecutor / generateHtmlExecutor / asyncInnerExecutor / customPool / videoContentGenPool` 5 个池满后用 `CallerRunsPolicy` 让调用方阻塞跑。调用方往往是 Tomcat 工作线程（max=200）—— 极端 burst 时 Tomcat 线程被借去跑业务，HTTP 入口直接挂。

**推荐处理方案：** 关键池改 `AbortPolicy` + 业务层 fallback（如返"系统繁忙稍后再试"），宁可丢任务不阻 Tomcat。同时为 5 个池加 `RejectedExecutionHandler` 日志计数，定位真实触发频率。

---

### DEBT-017 — AI 上游 QPS 无限流兜底

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-03 |
| 来源任务 | B7 改造期 AI 调用审计 |
| 风险等级 | HIGH |
| 状态 | OPEN |

**背景：** AI Feign (`aiFeign.chatCompletion`) 调用豆包/DeepSeek。当前没有 QPS 限流：MQ 消费 20 并发 × 4 次/条 = 瞬时 80 QPS；叠加 customPool 128 + asyncInnerExecutor 128 → 极端可瞬时 300+ QPS 打豆包，**触发 429 限流后无熔断 / 退避**。

**推荐处理方案：** `aiFeign.chatCompletion` 调用点统一过 Redisson `RateLimiter`，配豆包账号 QPS 上限的 80%；429 后指数退避重试。

---

### DEBT-018 — asyncExecutor @Deprecated 静默 bug：CallerRunsPolicy 未生效

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-03 |
| 来源任务 | B7 改造期线程池审计 |
| 风险等级 | LOW |
| 状态 | OPEN |

**背景：** `ThreadPoolConfig.asyncExecutor()` 写了 `new ThreadPoolExecutor.CallerRunsPolicy();` 但没 `setRejectedExecutionHandler(...)` 注入。实际走 `ThreadPoolTaskExecutor` 默认（`AbortPolicy`），跟注释意图相反。**因 `@Deprecated` 不在用、不影响线上**，是静默死代码 bug。

**推荐处理方案：** 删除 `asyncExecutor` Bean 整段（确认无任何 `@Qualifier("asyncExecutor")` 引用后）。

---

### DEBT-019 — UploadFileSimpleInfoVo 未暴露 trade_id，质检文件源行业兜底通用

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-04 |
| 来源任务 | 话术质检按行业取提示词改造（commit 791fae320） |
| 风险等级 | MEDIUM |
| 状态 | DONE |
| 关闭日期 | 2026-06-10 |

**背景：** 话术质检改造按 `tb_anchor_video.trade_id` 取提示词后，文件源（sourceType=1）走 `SensitiveWordsFeign.getUploadFileInfo` 返回的 `UploadFileSimpleInfoVo` **不含 trade_id 字段**（只暴露 fileId/userId/tenantId/fileName/uploadTime/fileType/fileDuration/fileWordNum）。`ScriptMonitorGenerateBll.resolveTradeId` 因此对文件源直接兜底 `tradeId=1`（通用行业），与录制视频按行业取的语义不一致。

**关闭说明：** 2026-06-10 测试反馈"上传文件质检走通用提示词不走行业定制"后实施 PATCH 修复：`UploadFileSimpleInfoVo` 加 `Long tradeId` 字段；`SensitiveWordsBll.getUploadFileInfo` 装配补 `setTradeId(info.getTradeId())`；`ScriptMonitorGenerateBll.resolveTradeId` 文件源分支打开走 SPI 取 tradeId，未取到回退 1L。测试锁 AC-3a（非兜底）+ AC-3b（null 兜底）双场景。

---

### DEBT-021 — CueWordsApi 层缺少行业父链回溯单测覆盖

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-04 |
| 来源任务 | 话术质检按行业取提示词改造（commit 791fae320 / test-engineer 评估） |
| 风险等级 | LOW |
| 状态 | OPEN |

**背景：** `CueWordsApi.getCueWordByTradeAndType`（含行业父链回溯 + 兜底 trade_id=1 算法）目前仅由 `ScriptMonitorGenerateBllTest` 在 Bll 层 mock 验证 happy path；CueWordsApi 内部的"按 tradeIds 顺序找第一个有数据行业"算法无单测覆盖。`replay-words` 模块零单测基建，不在本 PR 范围补建。

**推荐处理方案：** `replay-words` 模块补建 Mockito 单测基建后，针对 `CueWordsApi.getCueWordByTradeAndType` 增加三个用例：(1) 子行业有配置直接命中；(2) 子无配置回退父行业；(3) 全链路无配置兜底 trade_id=1。同时补 `CueWordsProducerImpl.listByTradeIdsAndCueType` 的 isDeleted 过滤验证。

---

### DEBT-022 — 质检 ASR 时间标签拼接的剩余边界场景未覆盖

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-04 |
| 来源任务 | ASR 时间锚点 PATCH（commit 3a4b59d25 / code-reviewer + test-engineer 评估） |
| 风险等级 | LOW |
| 状态 | OPEN（原 3 条边界中 1 条已在登记当日 PATCH 内清掉） |

**背景：** `ScriptMonitorGenerateBll.tryLoadAsrText` 段落时间锚点（PATCH 2026-06-04）目前覆盖了三个主路径：(a) items 含完整 startTime/endTime → 拼时间；(b) items=null 或 startTime/endTime 任一 null → 退化无时间；(c) 文件源（前置 resolveTradeId 兜底）。以下边界场景未在单测中显式验证，依赖现网行为：

- **OPEN** — 段落只有 1 个 item（首尾同一 WordListItemVo）：因首词只填 startTime、末词只填 endTime 是 ASR 服务的隐式契约，单 item 时 endTime 必为 null，会自然退化为无时间标签（已被 null 守卫处理）。
- ~~items 含 null 元素（list 非空但首/末元素为 null）：当前代码会抛 NPE~~ → **DONE**（commit 3a4b59d25 增加 `first != null && last != null` 防御）。
- **OPEN** — 畸形 ASR 时间倒错（startTime > endTime）：当前代码不校验，会输出 `[段落 N 时间 00:00:10-00:00:03]` 这种倒序串到 prompt，属上游 ASR 数据契约假设。

**推荐处理方案：** ASR 上游契约稳定后，倒错时间戳可在 `formatMsToHms` 调用前加 `Math.min/max` 兜底；单 item 段落若产品上希望保留时间标签，可对单 item 取 startTime 当起点 + endTime 当终点（即使二者重复也保留段时间锚点）。两项均非高频，优先级 LOW。

---

### DEBT-023 — 时间窗/epoch 转换类业务单测须 mock 非零起始时间（避免假阳性）

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-06 |
| 来源任务 | 互动巡检 T31-T36（commit 309f78e1e CRITICAL fix 后反思） |
| 风险等级 | MEDIUM |
| 状态 | OPEN |

**背景：** 互动巡检 `ScriptMonitorPatrolGenerateBll.queryDanmuForUnit` 实现时漏加 `video.getStartTime() + unitStartMs` 的 epoch 偏移，传给 TableStore 的时间窗等于在 1970-01-01 附近找弹幕，必然返空 → 巡检 100% 不工作。**该 bug 流过 lead-engineer 自测 + code-reviewer dispatch + test-engineer 跑测试 + 主 agent 走读 4 道关口都没发现**，最终在主 agent 模拟流程时才暴露。

**根因：** 早期单测 mock 的 `video.getStartTime()` 默认是 null 或返 0，整个测试链路里弹幕时间窗 `0 + unitStartMs` 永远是 `unitStartMs`，看起来"行为正确"，但生产环境 `video.startTime ≠ 0` 时全部失效。**单测假阳性：mock 默认值与生产值同构（都是 0）导致 bug 不暴露。**

**影响范围：** 项目所有"epoch 偏移 / 时间窗换算 / 视频内相对时间 ↔ 绝对时间"类业务单测：
- 巡检 `ScriptMonitorPatrolGenerateBllTest`（已修，commit 309f78e1e 加 `testGenerate_danmuTimeIsRelativeToVideoStart_notUtc` 显式 mock 非零 epoch）
- 质检 `ScriptMonitorGenerateBllTest`（同 `formatMsToHms` 范式，但未涉及 epoch 偏移，理论无该类 bug，但建议 audit）
- AI 问答 `BarrageSentenceImpl` 测试（如有）—— 现网范式，已被验证

**推荐处理方案：**
1. 单测约定：涉及 `AnchorVideoInfoVo.startTime` 的测试，**默认 mock 设非零 epoch**（如 `new Date(1732449900000L)` = 2025-11-24 20:05:00 CST），不允许默认 0 / null
2. `ScriptMonitorGenerateBllTest` audit：跑一次 grep `getStartTime`，确认所有 mock 都设非零；如有遗漏补全
3. 项目级 SKILL 沉淀：考虑在 `java-engineering-standards/SKILL.md` 或新建 `time-window-test-pattern.md` 加一条"时间窗单测的假阳性陷阱"

**为什么暂不全面 audit：** 巡检本批次已 fix，质检相关代码已上线验证（实际行为 OK），优先级 MEDIUM。后续质检若做新功能涉及时间窗，必须先 audit 旧测试。

---

### DEBT-024 — 互动巡检"时间轴校准"语义未定义，PRD 提及但未明确数据字段

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-06 |
| 来源任务 | 互动巡检 T31-T36（主 agent 模拟流程对照 PRD 8.1 发现） |
| 风险等级 | LOW |
| 状态 | OPEN |

**背景：** PRD `REQ-2026-0508-互动巡检` 多处提及"时间轴校准"：
- R-P0-002：自动生成前置"弹幕数据、主播 / 助手 ASR 逐字稿、**直播时间轴校准**均就绪"
- 八-1：数据来源"弹幕数据、主播 / 助手 ASR 逐字稿、**直播时间轴校准结果**"
- 8.1：数据质量约束"缺弹幕、缺 ASR 或**时间轴未校准**时不进入有效分析"

**问题：** "时间轴校准" 在代码 / 数据层面对应哪个字段 / 哪张表 / 哪个 Feign 查询，PRD 未明确，也没有可校验的接口契约。当前巡检 `ScriptMonitorPatrolGenerateBll.generate` **没有任何时间轴校准就绪校验逻辑**。

**潜在风险：** 如果实际业务存在"弹幕 epoch 与 ASR 视频内 ms 偏移对不上"的场景（如录制时长 vs 实际直播时长偏差、cos 弹幕上传时刻偏移），AI 看到时间错位的弹幕 + ASR 会做出错误判定。本批次未发现具体数据字段证据（弹幕 `recordDate` 和 ASR `WordListItemVo.startTime` 在质检 / AI 问答路径上已被验证可对齐）。

**推荐处理方案：**
1. 与 PM / 弹幕采集团队对齐 "时间轴校准" 真实语义（如：是否对应 `tb_socket_collect_message.startDate vs tb_anchor_video.startTime` 一致性校验？或 cos 上传水印？）
2. 如确认对应某个具体校验，新增 `ScriptMonitorPatrolGenerateBll.checkTimelineAligned(video)` 调用，未校准走 NOT_APPLICABLE
3. 如确认"时间轴校准"是产品概念而非具体数据校验（只是"弹幕和 ASR 都有"），更新 PRD 8.1 删除"时间轴未校准"分支

**为什么暂不动：** 当前 PRD 字面 + 代码行为已通过质检 / AI 问答数百场生产数据验证（同一弹幕 + ASR 数据源），未观察到时间错位案例。优先级 LOW，待具体场景出现后再补。

---

### DEBT-025 — `StandardScriptController.confirmStandardScript:73` 同款 SpEL WARN 待修

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-16 |
| 来源任务 | fix-generate-standard-script-json-escape（`Change__2026-06-16_10:47:21`） |
| 风险等级 | LOW |
| 状态 | DONE |
| 关闭日期 | 2026-06-17 |
| 关联 PATCH | PATCH__standard-script-3fix__2026-06-17_18:43:12 |

**背景：** `StandardScriptController.java:56` 的 `@NoRepeatSubmit(key = "generateStandardScript")` 在 6-16 已修为 `key = "'generateStandardScript'"`（SpEL 字符串字面量），消除了 `NoRepeatSubmitAop` 把 key 当 SpEL 标识符解析时打出的 `EL1007E: Property or field 'generateStandardScript' cannot be found on null` WARN 噪音。

同文件 line 73 的 `@NoRepeatSubmit(key = "confirmStandardScript")` 存在同款问题（相同模式，未修 key 参数）。

**关闭说明：** 2026-06-17 在 `PATCH__standard-script-3fix` 批次顺手修。`StandardScriptController.java:73` 改为 `@NoRepeatSubmit(key = "'confirmStandardScript'")`（加单引号），与 L56 写法一致。timeout 保持默认 5s（confirm 不调 AI，不需要 generate 的 300s）。code-reviewer 在该批次主动指出，外科手术延伸合并修复，DEBT 消除。

---

### DEBT-026 — AiUtils.aiTokenConsumeMultiple ×1.5 系数双乘问题

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-16 |
| 来源任务 | fix-generate-standard-script-json-escape（`Change__2026-06-16_10:47:21`）code-reviewer round 2 |
| 风险等级 | MEDIUM |
| 状态 | DONE |
| 关闭日期 | 2026-06-16 |

**背景：** `AiUtils.aiTokenConsumeMultiple(tokenNum, multiple)` 内部先乘 `DEFAULT_CONSUME_MULTIPLE=1.5` 再乘传入的 `multiple`，`ScriptMonitorStandardScriptBll` 传 `TOKEN_CONSUME_MULTIPLE=1.5`，导致实际乘数为 `×2.25` 而非注释所写"×1.5 系数"。`ScriptMonitorGenerateBll` 等 sibling 使用 `modelConfig.getConsumeMultiple()`（从数据库取，通常 =1.0 或 ≤1.0），其乘积语义正确。两处行为不一致。

**影响范围：**
- `replay-words/.../bll/ScriptMonitorStandardScriptBll.java`（当前 TOKEN_CONSUME_MULTIPLE=1.5 → 实际消耗 ×2.25）
- `replay-words/.../bll/ScriptMonitorGenerateBll.java`（使用 `modelConfig.getConsumeMultiple()` 范式，语义正确）

**推荐处理方案：** 统一收敛 multiplier 语义到 AiUtils 内部一处：
1. 若 `DEFAULT_CONSUME_MULTIPLE=1.5` 是 AiUtils 内部保底系数，则所有业务传 `multiple` 应表达"额外倍率"，`TOKEN_CONSUME_MULTIPLE` 改为 `1.0`；
2. 或重新设计 AiUtils.aiTokenConsumeMultiple API，把内部 DEFAULT_CONSUME_MULTIPLE 暴露为参数或移到调用方。

**关闭说明：** 本批次 MR 评审反馈后修复 — `ScriptMonitorStandardScriptBll` 删除硬编码常量 `TOKEN_CONSUME_MULTIPLE=1.5`，改用 `modelConfig.getConsumeMultiple()` 范式对齐 sibling `ScriptMonitorGenerateBll`。实扣系数由 ×2.25 修正为 ×1.5×consumeMultiple（model 表通常取 1.0，实扣 ×1.5）。单测 AC-2 断言同步从 675 改为 450。同 commit 内归档（feature/2.6.03 post-MR-review reflex fix）。

---

## 已关闭

### DEBT-002 — B4-1 `CueWordsApi` 越过 Producer 层直连 Service

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-05-30 |
| 来源任务 | 话术智能监控 B4-1 / `.claude/runs/Change__2026-05-22_script-monitor/` |
| 风险等级 | LOW |
| 状态 | DONE |
| 关闭日期 | 2026-05-30 |

**背景：** B4-1 实现 `CueWordsApi.listByMonitorType(Integer monitorType)` 时，本地分层范式是 `Api → Producer → Service/Dao`，但 `CueWordsProducer` / `CueWordsProducerImpl` 不在 B4-1 的 Allowed Scope。sub-agent 选择直接在 `CueWordsApi` 注入 `CueWordsService`（跳过 Producer 层）完成查询，避免越界编辑。功能等价、编译通过、与 dispatch 契约一致。

**影响范围：** `replay-words/src/main/java/com/jiuyu/replay/words/api/CueWordsApi.java`（仅 `listByMonitorType` 一个方法越过 Producer）

**关闭说明：** 2026-05-30 回滚 `listByMonitorType`，改走通用 `pageCueWords`（`CueWordsPageBo.monitorType` 字段新增）。`CueWordsApi` 不再绕过 Producer 层，不再直注入 `CueWordsService`。取质检提示词的范式统一为 `cueWordsFeign.pageCueWords(new CueWordsPageBo().setMonitorType(0))`，与现网 `DiagnosisCueBll` 取诊断提示词范式完全一致，DEBT 自动消除。

---

### DEBT-020 — ScriptMonitorGenerateBll.buildTokenRecord 硬编码 useSourceType=VIDEO，文件源台账类型错误

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-04 |
| 来源任务 | 话术质检按行业取提示词改造 review（code-reviewer 报告 MAJOR-4） |
| 风险等级 | LOW |
| 状态 | DONE |
| 关闭日期 | 2026-06-04 |
| 关联 commit | 3a4b59d25 |

**背景：** `ScriptMonitorGenerateBll.buildTokenRecord` 设置 `bo.setUseSourceType(USE_SOURCE_TYPE_VIDEO)` 常量（值=0），忽略 `report.getSourceType()`。当 `report.sourceType=1`（上传文件）时，token 台账记录的 useSourceType 仍为 0，按来源类型统计 token 用量时数据失真。

**关闭说明：** 2026-06-04 ASR 时间锚点 PATCH 顺手清掉。`bo.setUseSourceType(report.getSourceType())` 改为动态取，与 report.sourceType 一致。修复后文件源 token 台账类型正确，统计不再失真。

---

### DEBT-021 — RedisWithholdVo 字段命名反直觉

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-11 |
| 来源任务 | restoreDegree-slice-a（test-engineer 写 mock 时实测踩坑） |
| 风险等级 | LOW |
| 状态 | OPEN |

**背景：** `replay-ai/.../RedisWithholdVo`（Token 预扣凭据 VO）的字段命名与直觉相反：

- `redisId`：实际类型 `Long`（按名字像 String）
- `withholdId`：实际类型 `String`（按名字像 Long）

test-engineer 写 `ScriptMonitorStandardScriptBllTest` 的 `aiTokenWithholdFeign.preDeduct` mock 时按命名直觉传值，首次编译报错才发现类型与名字不符；浪费 ~10 分钟排查。

**影响范围：**

- `replay-ai/src/main/java/com/jiuyu/replay/ai/vo/RedisWithholdVo.java`（字段定义）
- 所有调用 `aiTokenWithholdFeign.preDeduct / settleAiToken / returnAiToken` 的业务代码与单测（grep 命名引用范围较大）

**推荐处理：**

1. （首选）字段重命名 `redisId` → `withholdKey`（或 `withholdSeq`），`withholdId` → `requestId` 之类，让类型与名字一致；需 grep 全部引用点评估 blast radius 后批量改
2. （备选）保留命名，在字段加 Javadoc `@implNote 字段类型与命名相反 — 详见 DEBT-021`，至少减少踩坑成本

**未处理原因：** Slice A 不动 RedisWithholdVo（仅 mock 使用），重命名涉及面大，独立任务清理更稳；本批次仅留账。

---

### DEBT-027 — `StandardScriptServiceImpl.softDelete(Long id)` 缺 tenantId 过滤

| 字段 | 值 |
|---|---|
| 发现日期 | 2026-06-17 |
| 来源任务 | PATCH__standard-script-3fix__2026-06-17_18:43:12 code-reviewer MAJOR-1 |
| 风险等级 | LOW |
| 状态 | DONE |
| 关闭日期 | 2026-06-17 |
| 关联 PATCH | PATCH__standard-script-3fix__2026-06-17_18:43:12 round 2（UPDATE 模式改造） |

**背景：** `replay-words/.../repository/service/impl/StandardScriptServiceImpl.java:64` `softDelete(Long id)` 的 `LambdaUpdateWrapper` 只按 `id` 过滤，未携带 `tenantId` 条件，违反 CLAUDE.md §5 "LambdaUpdateWrapper 写库必带 tenantId 过滤"。

**关闭说明：** 2026-06-17 同 PATCH 的 round 2 把 `confirmStandardScript` 流程改为 UPDATE 模式（同一 (tenant, user, secuid) 永远只一行），不再调用 `softDelete`。`softDelete` 当前无活跃调用方（grep `standardScriptService\.softDelete\b` 全仓 0 hit），缺 tenantId 过滤变成无危害。方法本身保留为通用 Service 能力供未来下线/归档场景使用；若未来重新启用，须按"推荐处理方案"补 tenantId 参数。

**推荐处理方案（备查）：** 若未来 softDelete 被重新启用，签名改为 `softDelete(Long id, Long tenantId)`，`LambdaUpdateWrapper` 加 `.eq(StandardScriptEntity::getTenantId, tenantId)`。
