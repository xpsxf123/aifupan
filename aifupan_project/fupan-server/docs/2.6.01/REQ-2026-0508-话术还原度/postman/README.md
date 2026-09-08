# 话术还原度 Postman 测试集

> 配套：[fidelity.md](../../../.claude/llm_wiki/wiki/frontend-api/fidelity.md)（接口契约）+ [TEST-GUIDE-话术还原度.md](../TEST-GUIDE-话术还原度.md)（20 个 TC 详细用例）

## 文件清单

| 文件 | 用途 |
|---|---|
| `fidelity.postman_collection.json` | 9 接口集合（含 collection-level Bearer Auth + test 脚本断言） |
| `fidelity.postman_environment.json` | test 环境变量（baseUrl / 账号 / 动态 token / 动态 reportId 等） |
| `README.md` | 本文（使用指南） |

---

## 快速开始

### 1. 导入

Postman / Apifox UI：

1. **导入 collection**：`File → Import → fidelity.postman_collection.json`
2. **导入 environment**：`Environments → Import → fidelity.postman_environment.json`
3. **右上角切到 `fidelity-test` 环境**

### 2. 填环境变量

只需填 3 个，其他都是自动 / 默认值：

| 变量 | 值来源 | 必填 |
|---|---|---|
| `baseUrl` | 测试环境 API gateway URL（如 `https://api-test.ifupan.com`）| ✅ |
| `secUid` | 测试主播 secUid（从测试环境取一个真实活跃直播间）| ✅ |
| `videoId` | 测试视频 ID（该 secUid 下已 ASR 转写完成的录制视频）| ✅ |

下面这些**不要手填**，会被对应接口的 test 脚本自动写入：

- `token` ← Login 接口
- `standardScriptId` ← confirmStandardScript
- `reportId` ← reportStatus（取 monitors[monitorType=1].reportId）
- `generatedTimeAxisScript` ← generateStandardScript 中间变量

### 3. 跑通顺序

**首跑（约 3-5 min，含 AI 调用）**：

```
0. Auth → Login                                  ← 必须先跑，写 token
1.1 anchorBasicConfig                            ← 验环境通、看开关状态
2.1 generateStandardScript    (AI ~30s)          ← 生成 timeAxisScript（不落库）
2.2 confirmStandardScript                        ← 落库，写 standardScriptId
2.3 standardScriptDetail                         ← 验证落库成功
1.2 setMonitorEnabled                            ← 开还原度开关
3.1 triggerReport                                ← 异步触发，~60-120s 后完成
3.2 reportStatus  (轮询，60s 一次直到 status=2)   ← 写 reportId
4.1 fidelityReportDetail                         ← 拿正文 + 验证 isRead=1
```

**回归（已有报告时，约 30s）**：

```
0. Login → 1.1 anchorBasicConfig → 3.2 reportStatus → 4.1 fidelityReportDetail
```

### 4. Collection Runner（一键全跑）

`Collection → Run → 按下面参数`：

| 选项 | 设置 |
|---|---|
| Iterations | 1 |
| Delay | 2000ms（接口间留 2s 缓冲，AI 接口前需要更久——见下方"已知限制"）|
| Data File | 无 |
| Environment | fidelity-test |

> **不推荐 Collection Runner 一次跑通全流程**：3.1 → 3.2 之间需要等 60-120s AI 生成；Postman delay 上限不够。建议分两次跑：
> - 第一次跑 `0 → 2.x → 1.2 → 3.1`，跑完去喝咖啡
> - 第二次跑 `3.2 → 4.1`

---

## 边界 / 异常用例（手动跑）

下面是 TEST-GUIDE 里 P0/P1 用例的快速对应：

| TC | 怎么跑 | 期望 |
|---|---|---|
| TC-07 未确认稿开开关 | 用新 secUid（没标准稿）跑 1.2 setMonitorEnabled | code=70005 |
| TC-08 无标准稿触发 | DB 软删标准稿后跑 3.1 triggerReport | code=70005 |
| TC-09 上传文件触发 | 改 3.1 body 的 sourceType=1 sceneType=1 | code=70014 |
| TC-10 跨租户访问 | 启用 `anotherTenantToken` 变量 + 用 B 租户的 reportId 跑 4.1 | code=70011 |
| TC-13 非自有账号 | 切个 accountType≠0 的账号 token 跑 1.2 / 3.1 | code=70004 |
| TC-14 Token 不足 | 后台调低 AI Token < 100000 后跑 2.1 / 3.1 | code=70001 |
| TC-16 报告已删除 | 软删报告后跑 4.1 | code=70012 |
| TC-17 参数校验失败 | speechSpeed=99 / referenceScript="" 跑 2.1 | code=70013 |
| TC-18 防重提交 | 5s 内连发 2.1 / 2.2 / 3.1 / 1.2 | 第 2 次：请勿重复提交 |

---

## 断言策略

每个接口的 test 脚本至少做以下断言：

1. **HTTP 200**（collection-level 通用）
2. **R\<T\> 信封字段** `code` / `msg` / `data`
3. **业务码** `code=0`（happy path）或 `code ∈ 预期错误码集`（边界）
4. **关键字段** 必填项 + 类型 + 取值范围（如 status ∈ 0-4）
5. **Snowflake 序列化** 校验 ID 字段是 string + 15-20 位数字（防 long 精度丢失回归）
6. **业务语义专属断言**：
   - `2.1 generateStandardScript`：响应**不含** standardScriptId（验证不落库）
   - `3.2 reportStatus`：还原度 summary 是 Markdown（用 `JSON.parse` 应失败）
   - `4.1 fidelityReportDetail`：status=2 时 isRead 自动置 1 + confirmedAt 非空

失败的断言会在 Postman Test Results 面板红字标出。

---

## 已知限制 / 排查

| 现象 | 排查 |
|---|---|
| Login 401 / token 写不进 | 看 Response Body：是 70013（参数）还是 70011（账号）；账号被冻结时业务侧手动改 status |
| `{{baseUrl}}` 解析空 | 没切环境 / environment 没生效 — 看右上角环境下拉是不是选了 fidelity-test |
| 3.1 triggerReport 70014 | 你传了 sourceType=1（上传文件），改回 sourceType=0 |
| 3.2 status 一直停在 1 | AI 调用挂了 / 超时 — 后端日志看 `reportId={reportId}` 的 ERROR；正常 60-120s 出结果 |
| 4.1 isRead 一直 0 | 你**不是录制人**（非创建人查不写）— 用 Login 账号查自己创建的报告 |
| AI 调用 70001 | 测试账号算力不足 — 后台 `ai_token_use_record` 看消耗，给账号补 token |
| Snowflake 断言失败（reportId 是 number） | 后端 JacksonSerializerConfig 没全局 ToStringSerializer — 应是 framework 回归，需查后端 |

---

## 维护

| 改了什么 | 该改这里 |
|---|---|
| 接口新增 / 删除 / 路径变 | `fidelity.postman_collection.json` 的对应 item |
| 响应 VO 字段增减 | 对应 item 的 test 脚本断言 |
| 测试环境地址变了 | 用户自己改 environment.baseUrl |
| 新增账号 / token 类型 | environment 加 variable |

> 本 collection 与 fidelity.md / TEST-GUIDE 三者**任一变动必须同步**。fidelity.md 是 SSOT。

---

## 后续路线（暂未做）

- **Newman CLI 集成**：`npx newman run fidelity.postman_collection.json -e fidelity.postman_environment.json --reporters cli,htmlextra` — 用于 CI / 回归报告
- **traceId 串日志**：当前测试环境若用 Skywalking，可在每个 request 加 `X-Trace-Id: smoke-{{$randomUUID}}` header，失败时按 traceId 反查日志
- **Mock AI 路径**：另起一份 collection，跳过真实 AI 调用，用预置 status=2 数据走详情 / 状态查询的回归
