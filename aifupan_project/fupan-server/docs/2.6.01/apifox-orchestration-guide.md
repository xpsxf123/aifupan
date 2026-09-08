# Apifox 5 场景编排作业指导书 — 话术智能监控质检 v1

> 配套 OAS：`docs/2.6.01/apifox-import-script-monitor.yaml`
> 接口范围：B4 已实现 5 个 endpoint（reportStatus / batchReportStatus / triggerReport / confirmRead / qualityReportDetail）
> 测试套件目标：5 个跨接口场景全部跑通 → CLI 可一键回归 → Claude Code skill 触发

---

## 0. 环境前置（必须满足才能跑场景）

| # | 项 | 检查 |
|---|---|---|
| 1 | 后端 dev/test 环境跑着 | `curl http://${host}:6606/actuator/health` 200 OK |
| 2 | MySQL/Redis/MongoDB/RocketMQ 全部可访问 | 后端启动 log 无 ERROR |
| 3 | XXL-Job `scanAndSendMessageJob` cron 配着（每 10s 扫一次） | XXL-Job 控制台确认 |
| 4 | XXL-Job `scriptMonitorReportZombieRecovery` cron 配着（每 5min） | XXL-Job 控制台确认 |
| 5 | 火山引擎 Ark AI 模型 token 充足 | `tb_user_property aiTokenNum >= 200000` |
| 6 | 测试租户/用户有视频复盘数据（ASR 非空） | `tb_anchor_video.exist_barrage=1` 且 `sentence_mark_vo` 非空 |
| 7 | 阿里云 RocketMQ 控制台已建 dev/test 环境的 `script_monitor_*_topic` + `replay_script_monitor_*_group` | 控制台截图 |

不满足 ① ② = 全 5 场景跑不通；不满足 ⑦ = MQ 消息不消费、场景 1 永久 GENERATING。

---

## 1. Apifox 准备（一次性，5-10 分钟）

### 1.1 导入接口 OAS

1. 打开 Apifox 项目 **爱复盘-服务端**（projectId=5641599）
2. 顶部菜单 **设置 → 数据管理 → 导入**
3. 选 **OpenAPI / Swagger** 格式
4. 上传 `docs/2.6.01/apifox-import-script-monitor.yaml`
5. 选项：保留已有接口；新接口归入 `话术智能监控` 目录
6. 确认导入 → 5 个接口进入 Apifox

### 1.2 配环境变量

Apifox **环境管理** 新建/编辑 **dev** 环境：

| 变量 | 类型 | 示例值 | 说明 |
|---|---|---|---|
| `baseUrl` | URL | `http://localhost:6606`（或 dev host） | 后端服务地址 |
| `userToken` | String | `eyJxxx...`（dev 用户登录拿到） | 用户 Token，写入接口请求 Header |
| `tenantId` | Number | `100`（dev 测试租户） | 用于断言 |
| `userId` | Number | `1`（dev 测试用户） | 用于断言 |
| `testAnchorVideoId` | String | `1928xxx...`（一个有完整 ASR 的录制视频 ID） | sourceType=0 用 |
| `testAnchorUrlUserId` | Number | `1929xxx...`（已绑定的主播 ID） | 各场景共用 |

### 1.3 配公共 Header

在 Apifox **公共参数 → 全局 Header** 加：
- Key: `Token`，Value: `{{userToken}}`

这样 5 个接口都自动带 Token，不用每个用例手填。

### 1.4 AI 一键生成单接口测试用例

对 5 个接口分别：
1. 点接口 → "运行"标签下选 "AI 生成测试用例"
2. Apifox AI 根据接口定义（枚举/必填/边界）自动生成 8-15 个用例（正向/负向/边界/安全）
3. 检查 → 批量采纳（不需要的用例删掉）

预期：每个接口 ~10 个用例 × 5 接口 = ~50 个单接口测试用例。

---

## 2. 5 场景编排（核心，一次性，半天）

每个场景在 Apifox **自动化测试 → 测试场景** 新建，按下面表格拖步骤。

### 场景 1：质检报告生成完整闭环（Happy Path）★最重要

**前置数据**：dev 环境用户余额 ≥ 200000 token；`{{testAnchorVideoId}}` 视频 ASR 非空、无 status=2 GENERATED 报告。

| Step | 类型 | 接口/动作 | 关键参数 | 提取变量 | 断言 |
|---|---|---|---|---|---|
| 1 | API | POST triggerReport | `{sourceType:0, sceneType:0, sourceId:"{{testAnchorVideoId}}", monitorType:0}` | — | `res.body.code == 0` |
| 2 | 等待 | — | 60s（等 MQ 消息表轮询发送 + consumer 消费起步） | — | — |
| 3 | API | POST batchReportStatus | `{sources:[{sourceType:0, sceneType:0, sourceId:"{{testAnchorVideoId}}"}]}` | `reportId = res.body.data[0].monitors[0].reportId`<br/>`status = res.body.data[0].monitors[0].status` | `res.body.code == 0`<br/>`status in [1,2]`（生成中或已生成）|
| 4 | 条件 | If `status == 1` | 跳到 Step 5；否则 Step 7 | — | — |
| 5 | 等待 | — | 60s（继续等 AI 调用完成；总 P95 5-10min，最坏 20min）| — | — |
| 6 | API | POST batchReportStatus | 同 Step 3 | 重新覆盖 `status` | 循环回 Step 4（For loop ≤ 20 次，超时则 fail） |
| 7 | 断言 | — | — | — | `status == 2`（GENERATED） |
| 8 | API | POST confirmRead | `{reportId:{{reportId}}, confirmRole:1}` | — | `res.body.code == 0` |
| 9 | API | GET qualityReportDetail | `?reportId={{reportId}}` | — | `res.body.code == 0`<br/>`res.body.data.isRead == 1`<br/>`res.body.data.crashCount >= 0`<br/>`res.body.data.confirmedRecords[0].confirmRole == 1`<br/>`res.body.data.confirmedRecords[0].confirmedBy != null` |

**预期跑通时长**：5-15 分钟。如果 20 分钟仍 status=1 → 检查 RocketMQ 消费是否正常、AI 模型是否响应。

---

### 场景 2：GENERATING 中重复触发应拒（并发幂等）

**前置数据**：场景 1 跑过、有一份 `reportId` 处于 status=1 GENERATING（可在场景 1 跑到 Step 3 后立即截图保留 reportId，或专门起一个不等异步完成的"快速触发"）。

| Step | 类型 | 接口/动作 | 关键参数 | 断言 |
|---|---|---|---|---|
| 1 | API | POST triggerReport | `{sourceType:0, sceneType:0, sourceId:"{{testAnchorVideoId}}", monitorType:0}`（第一次触发，建状态 1） | `res.body.code == 0` |
| 2 | API | POST triggerReport | 同 Step 1（**立刻**第二次触发，**不等异步**） | `res.body.code != 0`<br/>`res.body.msg contains "重复触发"`<br/>或拦截 `请勿重复触发，请稍后再试`（@NoRepeatSubmit 拦的） |

**预期**：第二次触发被 `@NoRepeatSubmit` 拦或被 `handleExisting` GENERATING 分支拦。

---

### 场景 3：FAILED 重置为 GENERATING（retry 路径）

⚠️ **此场景需要数据准备**：先准备一条 `status=3 GENERATE_FAILED` 的报告。

**两种准备方式**：
- 方式 A：SQL 直接 `UPDATE tb_script_monitor_report SET status=3 WHERE id={{reportId}}`（场景 1 跑完后修改）
- 方式 B：等 ScriptMonitorReportZombieHandler 自动把过期 GENERATING 改 FAILED（需要 30min）

| Step | 类型 | 接口/动作 | 关键参数 | 断言 |
|---|---|---|---|---|
| 0 | SQL 前置（手工） | — | 把 `{{reportId}}` 改 status=3 | — |
| 1 | API | POST triggerReport | 同 sourceId+monitorType | `res.body.code == 0` |
| 2 | API | POST batchReportStatus | 查询 status | `status == 1`（已重置为 GENERATING） |

**预期**：`handleExisting` 对 FAILED 走 "重置为 GENERATING 重试" 分支。

---

### 场景 4：GENERATED 重新触发不破坏旧成功（CRIT-1 修复回归）★关键

⚠️ **此场景需要场景 1 先成功**，留下一份 `status=2 GENERATED` 报告。

| Step | 类型 | 接口/动作 | 关键参数 | 提取变量 | 断言 |
|---|---|---|---|---|---|
| 1 | API | GET qualityReportDetail | `?reportId={{reportId}}` | `oldCrashCount = res.body.data.crashCount`<br/>`oldBodyHash = sha1(res.body.data.reportContent.substring(0, 200))` | — |
| 2 | API | POST triggerReport | 同 sourceId+monitorType（**触发重新生成**） | — | `res.body.code == 0` |
| 3 | 等待 | — | 10s（避免立刻查到旧状态）| — | — |
| 4 | API | POST batchReportStatus | 查询 status | — | `status in [1, 2]`（生成中或已生成）<br/>**关键：reportId 仍是 `{{reportId}}`，没换新 ID** |
| 5 | 条件等待 | If `status == 1` | 继续等到 status 变 | — | — |
| 6 | API | GET qualityReportDetail | `?reportId={{reportId}}` | 新 `crashCount` / `bodyHash` | `crashCount >= 0`（重新生成成功；如果重新生成失败，应回滚到 oldCrashCount） |

**关键断言**：步骤 4 的 reportId 必须 = 步骤 1 的 reportId（CRIT-1：handleExisting 对 GENERATED 不删除旧记录，只改 status=1 复用同一行）。如果新建了一条 status=1 的报告 → CRIT-1 退化。

---

### 场景 5：Token 扣返断言（Token 一致性）

**前置数据**：构造一种"生成必失败"的场景，比如：
- 方式 A: SQL 把 `tb_cue_words` 质检提示词 problem 字段清空 → loadCueWords 抛异常 → 进 catch returnAiToken
- 方式 B: 临时改 AiModelFeign 的模型 endpoint 让 chatCompletion 失败

| Step | 类型 | 接口/动作 | 关键参数 | 提取变量 | 断言 |
|---|---|---|---|---|---|
| 1 | API | GET 当前用户 Token 余额（**借用** UserPropertyController 或其他能查 token 余额的接口） | — | `tokenBefore` | — |
| 2 | API | POST triggerReport | 同 sourceId+monitorType（**预扣 100000**）| — | `res.body.code == 0`（提交成功，预扣已扣）|
| 3 | API | 查 Token 余额 | — | `tokenWithheld` | `tokenWithheld == tokenBefore`（实扣未发生，预扣 walk-around；实际看 isPropertyHaveAiToken 内部 setNum 的副作用，可能此处实测 tokenBefore - 100000） |
| 4 | 等待 | — | 60s（等异步 generate 失败 + 进 catch + returnAiToken） | — | — |
| 5 | API | 查 Token 余额 | — | `tokenAfter` | `tokenAfter == tokenBefore`（返还预扣，余额回到 baseline） |
| 6 | API | POST batchReportStatus | 查询 status | — | `status == 3`（GENERATE_FAILED） |

**关键断言**：Step 5 余额 = Step 1 余额（returnAiToken 真的返还）。这是 Token 一致性最关键的护栏。

---

## 3. 把 5 场景塞进 1 个测试套件

在 Apifox **自动化测试 → 测试套件** 新建：

- 名称：`script-monitor-b4-quality`
- 包含 5 个场景（按上面顺序）
- 失败策略：单场景失败不阻断其他场景（让 CLI 一次跑完拿到 5 个独立结果）

**拿到套件 ID**：套件详情页 URL 末尾的数字串（如 `12345678`），或 "持续集成" 标签下查看的 CLI 启动命令里的 `-t` 参数值。

把这个 ID 告诉 Claude，我会建 skill 时塞进 `scenarios/script-monitor.md` 的 `apifox run ... -t <ID>` 位置。

---

## 4. 你拿到套件 ID 后给我

格式（贴给 Claude Code）：

```
Apifox 测试套件已编排完成。
- 项目 ID: 5641599
- 套件 ID: <这里填>
- 环境 ID: <这里填，dev 环境的 ID>
- 套件名: script-monitor-b4-quality
- 包含 5 场景（happy / 重复触发 / FAILED 重试 / GENERATED 保护 / Token 扣返）
```

我据此建 `.claude/skills/apifox-testing/SKILL.md` + `scenarios/script-monitor.md`，你后续在终端对我说 `跑 script-monitor 测试` 就能一键执行。

---

## 5. 常见问题

**Q: 场景 1 卡 status=1 超 20 分钟**
A: 检查：① `tb_mq_message_record` 有 status=0 待发送消息但 XXL-Job scanAndSendMessageJob 不跑 → 检查 XXL-Job 控制台；② RocketMQ consumer 没起 → 看后端启动 log；③ AI 模型 token 余额不足 → SQL 加额度。

**Q: 场景 4 reportId 变了**
A: CRIT-1 修复退化。回 `ScriptMonitorBll.handleExisting` 检查 GENERATED 分支是否还在 "update 旧记录 status=1" 而不是 "新建一条"。

**Q: 场景 5 余额没返还（tokenAfter < tokenBefore）**
A: `returnAiToken` 调用失败。看 `[script-monitor-mq-handler]` log，可能 settle 已实扣（DEBT-005 Token 一致性裂口）。

**Q: Apifox AI 生成的单接口用例质量差**
A: Apifox AI 依赖接口的 description / enum / required 完整度。如果 OAS YAML 描述足够准确（本 YAML 已较完整），AI 生成应该 OK。否则手动补 1-2 个关键用例。
