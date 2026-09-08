# 豆包 Responses API 迁移 & DeepSeek 上下文缓存 — 测试手册

## 一、改动概述

### 1.1 改了什么

**改动 A：豆包 Responses API 迁移**

`doubao-seed-2-0-pro-260215` 从火山引擎**旧 Context API** 迁移到 **Responses API**。

| 项目 | 旧 | 新 |
|---|---|---|
| 接口地址 | `POST /api/v3/context/chat/completions` | `POST /api/v3/responses` |
| 多轮上下文机制 | Context API 自动管理 | `previous_response_id` 链式传递 |
| 上下文 ID | 火山自动生成 | 首次 UUID → 火山返回 `resp_xxx` |
| 触发条件 | 所有豆包请求 | C# `webVersion >= 2.6.0.5` → `useModelWay = 0` |

**改动 B：DeepSeek 上下文缓存**

DeepSeek 新增多轮对话上下文缓存，通过 Redis 存储完整消息历史，支持上下文裁剪。

| 项目 | 旧 | 新 |
|---|---|---|
| 上下文机制 | 无，每次独立对话 | Redis 存储消息历史数组 |
| 上下文 ID | 无 | UUID，始终不变 |
| 多轮对话 | 不支持 | `ai:deepseek:contextId:{UUID}` 存历史 |
| 上下文裁剪 | 无 | `trimMessages()` 超限裁剪旧消息 |

**改动 C：视频/文件提示词格式优化**

`minuteParagraphContent()` 的段落指标从逐行"本段xxx"改为紧凑的三行分组格式，减少 prompt token 消耗。

| 项目 | 旧 | 新 |
|---|---|---|
| 每段落行数 | ~12 行（每指标一行） | 3 行（时间/数据/内容） |
| 格式 | `\n本段在线人数：XX\n本段语速：XX\n...` | `段落N时间:(...)\n段落N数据:(...)\n段落N内容：...` |
| 影响范围 | — | AI 问答 prompt、AI 诊断、自然/优化原文生成 |

### 1.2 改动文件

**豆包 Responses API — 服务端（Java）：**

| 文件 | 改动 |
|---|---|
| `replay-third/.../DoubaoAiModelImpl.java` | `createContext()` 存 instructions 到 Redis；`contextChatCompletionStream()` 重写为 Responses API |
| `replay-api/.../AiRelatedLogicImpl.java` | `assemblePrompt()` 加版本判断 + 创建上下文；`ask()` 加 contextId 更新 |
| `replay-third/.../AiModelBll.java` | `getUseModelWay()` 删除硬编码 `return 0`，恢复字典查询 |
| `replay-words/.../PromptAssemblyResultVo.java` | 新增 `systemPrompt` 字段 |

**豆包 Responses API — C# 客户端：**

| 文件 | 改动 |
|---|---|
| `Ai/impl/DoubaoChatService.cs` | `ContextChatCompletionStream()` 重写为 Responses API + SSE 解析 |
| `Ai/impl/ChatCompletionsAsk.cs` | 豆包走 Responses API 分支，回调更新 contextId |
| `Ai/impl/AbstractAsk.cs` | 存储 systemPrompt；`addAiTokenUseRecord()` 加 reasoningTokens |
| `Ai/Model/DouBaoChatStreamDto.cs` | 新增 Responses API 字段 |
| `Ai/Model/CompletionsDto.cs` | 新增 `responseId` 字段 |
| `Dto/PromptAssemblyResultDto.cs` | 新增 `systemPrompt` 字段 |

**DeepSeek 上下文缓存 — 服务端（Java）：**

| 文件 | 改动 |
|---|---|
| `replay-third/.../DeepSeekAiModelImpl.java` | `createContext()` 新增：消息历史 JSON 数组存入 `ai:deepseek:contextId:{UUID}`；`contextChatCompletionStream()` 新增：读历史 → 裁剪 → 追加用户提问 → 调 API → 追加助手回复 → 回写 Redis；`trimMessages()` 新增：超限裁剪旧消息；`setUsage()` `cachedTokens` 从 `prompt_cache_hit_tokens` 取值 |

**DeepSeek 上下文缓存 — C# 客户端：**

| 文件 | 改动 |
|---|---|
| `Ai/impl/DeepSeekChatService.cs` | `requestDeepSeekProxy()` 统一上下文/非上下文模式，通过 `/aiRelated/chatStreamProxy` 代理调用 |
| `Ai/impl/DeepSeekAsk.cs` | `useModelWay==0` 走 `ContextChatCompletionStream`，否则走普通流式 |

**视频/文件提示词格式优化 — 服务端：**

| 文件 | 改动 |
|---|---|
| `replay-api/.../VideoSentenceImpl.java` | `minuteParagraphContent()` 段落指标格式从逐行改为三行分组（段落N时间/段落N数据/段落N内容） |

### 1.3 涉及功能（需回归）

| 功能 | 影响 |
|---|---|
| C# AI 问答（运营助手 type=0 / 违规助手 type=1） | 豆包走 Responses API |
| 服务端 AI 问答 | 豆包走 Responses API |
| AI 追问/多轮对话（豆包） | 上下文通过 `previous_response_id` 保持，contextId 每次更新 |
| AI 追问/多轮对话（DeepSeek） | 上下文通过 Redis 消息历史保持，contextId 始终不变 |
| DeepSeek 上下文缓存 | 首次提问创建缓存，追问时追加到消息历史并回写 |
| DeepSeek 上下文裁剪 | 历史超 `contextSize` 限制时自动裁剪旧消息 |
| 视频/文件提示词格式 | 段落指标从逐行改为分组格式，影响 AI 问答/诊断/原文生成 |
| 深度思考展示 | 前端折叠区域展示思考过程 |
| Token 消耗记录 | 新增 `cached_tokens`、`reasoning_tokens` 字段 |

### 1.4 不受影响（抽测确认）

通义千问、非流式问答、AI 诊断、HTML 生成、旧版 C# 客户端 — 均走 Chat Completions。

---

## 二、数据流

### 2.1 C# 路径

```
C# 前端
  │
  ▼
AbstractAsk.AskStream()
  │
  ├─ 1. AiRelatedApi.assemblePrompt(dto) → 服务端 /ai/assemblePrompt
  │     返回: assembledPrompt, contextId, identity, systemPrompt, modelConfig(含临时token)
  │     日志: 【C# assemblePrompt】返回
  │
  ├─ 2. DoubaoChatService.ContextChatCompletionStream()
  │     直连火山 POST /api/v3/responses
  │     SSE 流式返回 → 实时推送前端
  │     日志: 【Responses API】开始请求 → 请求体 → HTTP状态码 → 流结束 → 完成汇总
  │
  └─ 3. AiUtils.updateContextId(dto) → 服务端 /ai/updateContextId
        更新 Redis contextId (UUID → resp_xxx)
        日志: 【C# Ask】更新contextId
```

### 2.2 服务端路径

```
前端/内部调用
  │
  ▼
AiRelatedLogicImpl.ask()
  │
  ├─ 1. assemblePrompt()
  │     - webVersion >= 2.6.0.5 → useModelWay = 0
  │     - 查 Redis contextId，无则 createContext() → UUID
  │     日志: 【assemblePrompt】创建新上下文 / 复用已有contextId
  │
  ├─ 2. contextChatCompletionStream() → DoubaoAiModelImpl.contextChatCompletionStream()
  │     调火山 POST /api/v3/responses，流式返回
  │     日志: 【Responses API】开始请求 → 开始流式调用 → 流完成 → Token用量
  │
  └─ 3. 更新 Redis contextId (UUID → resp_xxx)
        日志: 【ask】更新Redis contextId / 刷新Redis TTL
```

### 2.3 Redis Key

| Key | 内容 | TTL |
|---|---|---|
| `{contextRedisKey}`（由 SentenceMark 按 sourceType+sourceId+type 维度生成） | contextId（UUID 或 `resp_xxx`） | 3300s |
| `ai:doubao:contextId:{UUID}` | allContent（系统背景数据） | 3600s（仅首次设） |

### 2.4 contextId 生命周期

```
首次提问:
  assemblePrompt → Redis 无 → createContext() → UUID
    → Redis: {contextRedisKey}=UUID, ai:doubao:contextId:{UUID}=allContent
    → Responses API: instructions=allContent, store=true
    → 火山返回 resp_xxx → Redis: {contextRedisKey}=resp_xxx

第N次提问:
  assemblePrompt → Redis 有 resp_xxx
    → Responses API: previous_response_id=resp_xxx
    → 火山返回 resp_yyy → Redis: {contextRedisKey}=resp_yyy

过期:
  Redis TTL 到期 → 重新走首次提问
```

### 2.5 Token 数据流

```
火山返回:
  response.usage.input_tokens                              → prompt_tokens
  response.usage.output_tokens                             → completion_tokens
  response.usage.total_tokens                              → total_tokens
  response.usage.input_tokens_details.cached_tokens        → cached_tokens
  response.usage.output_tokens_details.reasoning_tokens    → reasoning_tokens

服务端: DoubaoAiModelImpl → AiRelatedLogicImpl.saveAiTokenUseRecord() → ai_token_use_record 表
C#:     DoubaoChatService → ChatCompletionsAsk → AbstractAsk.addAiTokenUseRecord() → 服务端接口 → ai_token_use_record 表
```

### 2.6 DeepSeek 路径

```
C# 前端
  │
  ▼
DeepSeekAsk.ask()
  │
  ├─ 1. AiRelatedApi.assemblePrompt(dto) → 服务端 /ai/assemblePrompt
  │     返回: assembledPrompt, contextId (UUID), identity
  │     contextId 始终为 UUID，不随轮次变化
  │
  ├─ 2. DeepSeekChatService.ContextChatCompletionStream()
  │     POST /aiRelated/chatStreamProxy (服务端代理)
  │     请求体: {modelId, identity, realContent, contextId, useModelWay=0}
  │     SSE 流式返回 → 实时推送前端
  │
  └─ 3. 不更新 contextId（DeepSeek contextId 始终不变）
```

```
服务端 chatStreamProxy
  │
  ├─ 1. 从 Redis 读消息历史: GET ai:deepseek:contextId:{UUID}
  │     返回 JSON 数组: [{"role":"system","content":"..."}, {"role":"user","content":"..."}, ...]
  │
  ├─ 2. trimMessages() 裁剪超限历史（保留首条 system，从旧到新删除）
  │
  ├─ 3. 追加当前用户提问 → 调 DeepSeek API (OkHttp POST /chat/completions)
  │     SSE 流式解析 → 逐 chunk 推送 C#
  │
  └─ 4. 追加助手回复到消息数组 → 回写 Redis
        SET ai:deepseek:contextId:{UUID} = [...] TTL=3600s
```

### 2.7 Redis Key（全貌）

| Key | 内容 | TTL | 模型 |
|---|---|---|---|
| `{contextRedisKey}` | contextId（UUID 或 `resp_xxx`） | 3300s | 豆包/DeepSeek |
| `ai:doubao:contextId:{UUID}` | allContent（系统背景数据，纯文本） | 3600s（仅首次设） | 豆包 |
| `ai:deepseek:contextId:{UUID}` | 消息历史 JSON 数组 `[{"role":"...", "content":"..."}]` | 3600s（每次追问刷新） | DeepSeek |

### 2.8 豆包 vs DeepSeek contextId 生命周期对比

```
豆包:
  首次: assemblePrompt → Redis 无 → createContext() → UUID
         → Redis: {contextRedisKey}=UUID, ai:doubao:contextId:{UUID}=allContent
         → Responses API: instructions=allContent, store=true
         → 火山返回 resp_xxx → Redis: {contextRedisKey}=resp_xxx
  第N次: assemblePrompt → Redis 有 resp_xxx
         → Responses API: previous_response_id=resp_xxx
         → 火山返回 resp_yyy → Redis: {contextRedisKey}=resp_yyy
  过期: Redis TTL 到期 → 重新走首次

DeepSeek:
  首次: assemblePrompt → Redis 无 → createContext() → UUID
         → Redis: {contextRedisKey}=UUID, ai:deepseek:contextId:{UUID}=[{"role":"system",...}]
         → DeepSeek API: messages=历史数组
         → 助手回复追加到数组 → Redis 回写（contextId 不变）
  第N次: assemblePrompt → Redis 有 UUID（不变）
         → DeepSeek API: messages=历史数组+新提问
         → 助手回复追加到数组 → Redis 回写（contextId 不变）
  过期: Redis TTL 到期 → 重新走首次（新 UUID）

关键差异:
  - 豆包 contextId 每次回答后变化（UUID → resp_xxx → resp_yyy）
  - DeepSeek contextId 始终不变（始终是首次的 UUID）
  - 豆包依赖火山服务端维护上下文，DeepSeek 自行在 Redis 维护完整消息历史
```

### 2.9 视频/文件提示词格式变更

**变更位置：** `VideoSentenceImpl.minuteParagraphContent()`（`questionContent=0` 时走）

**旧格式（逐行）：**
```
本段自然时间：12:30:00
本段开始时间：00:05:00
本段结束时间：00:10:00
本段在线人数：1500
本段在线人数增加：200人
本段语速：180字/分钟
本段发弹幕条数：45条
本段成交人数：12
本段互动率：3.0%
本段成交率：0.8%
本段销售额：15800
本段uv价值：10.53
本段内容：大家好欢迎来到直播间...
```

**新格式（三行分组）：**
```
段落1时间:（开始时间：00:05:00，结束时间：00:10:00，自然时间：12:30:00）
段落1数据:（在线人数：1500，比上段增加200人，语速：180字/分钟，弹幕条数：45条，成交人数：12，互动率：3.0%，成交率：0.8%，销售额：15800，uv价值：10.53）
段落1内容：大家好欢迎来到直播间...
```

**影响链：**
```
minuteParagraphContent()
  → AiSentenceMark.getContent() (VideoSentenceImpl)
    → SentenceMark.setAskQuestion() → realContent（组装到 AI prompt）
      → AiRelatedLogicImpl.assemblePrompt() → prompt 发送给 AI 模型
```

---

## 三、日志指南

### 3.1 C# 端日志

**位置：** `D:\project\ReviewAnalysis\bin\Debug\logs\`

| 目录 | 文件格式 | 写入方法 | 内容 |
|---|---|---|---|
| `logs/` | `log_yyyyMMdd_N.txt` | `FileUtils.log()` | 通用日志（含 AI 问答全部关键日志） |
| `logs/error/` | `log_yyyyMMdd_N.txt` | `FileUtils.LogError()` | 错误日志 |

**日志格式：**
```
HH:mm:ss
动作:xxx
内容

```

**测试时 grep 关键词：**

```bash
# === 查看完整 AI 问答链路 ===
grep "【C# assemblePrompt】\|【C# Ask】\|【C# Token记录】" logs/log_20260611_1.txt

# === 查看 Responses API 调用详情 ===
grep "【Responses API】" logs/log_20260611_1.txt

# === 查看 Chat Completions 调用 ===
grep "【Chat Completions" logs/log_20260611_1.txt

# === 查看 DeepSeek 代理调用 ===
grep "DeepSeek" logs/log_20260611_1.txt

# === 查看错误 ===
grep "【Responses API】异常\|【Responses API】错误\|失败\|Error\|CustomException" logs/log_20260611_1.txt

# === 查看 AI 最终回答 ===
grep "ai回答的问题" logs/log_20260611_1.txt

# === 查看 token 消耗 ===
grep "实际使用的token数量\|【C# Token记录】" logs/log_20260611_1.txt

# === 开发者模式：查看提示词 ===
grep "真实提问问题\|用户输入(input)\|系统提示词(instructions)\|提示词内容" logs/log_20260611_1.txt
```

**关键日志一览：**

| 日志标记 | 含义 | 何时出现 | 开发者模式 |
|---|---|---|---|
| `【C# assemblePrompt】返回` | assemblePrompt 接口返回的 contextId、useModelWay、各字段长度 | 每次提问 | 始终 |
| `【C# Ask】走Responses API` | 走 Responses API 路径，打印 contextId、model | useModelWay=0 | 始终 |
| `【C# Ask】走Chat Completions API` | 走 Chat Completions 路径 | useModelWay≠0 | 始终 |
| `【Responses API】开始请求` | isFirstTurn、contextId、model、thinkingType | 每次 Responses API 调用 | 始终 |
| `【Responses API】请求体` | 完整请求 JSON（超500字截断） | 每次 Responses API 调用 | 始终 |
| `【Responses API】关键参数` | hasInstructions、hasPreviousResponseId | 每次 Responses API 调用 | 始终 |
| `【Responses API】Request ID` | 火山返回的 X-Request-ID | 每次 | 始终 |
| `【Responses API】HTTP状态码` | HTTP 响应状态 | 每次 | 始终 |
| `【Responses API】流结束` | responseId + 完整 token 用量 | 流结束时 | 始终 |
| `【Responses API】完成汇总` | 总chunk数、思考/文本chunk数、长度 | 流完成后 | 始终 |
| `【Responses API】错误响应体` | 失败时的响应内容 | HTTP 错误时 | 始终 |
| `【Responses API】异常上下文` | 异常时的 model、contextId、isFirstTurn | 异常时 | 始终 |
| `【C# Ask】Responses API返回` | status、responseId、totalTokens | 每次 | 始终 |
| `【C# Ask】更新contextId` | 旧contextId → 新contextId | 成功后 | 始终 |
| `【C# Token记录】` | requestId、model、全部 token 字段 | 成功后 | 始终 |
| `DeepSeek代理请求失败` | HTTP 状态码 + 错误内容 | HTTP 错误时 | 始终 |
| `DeepSeek消息解析异常` | 解析异常详情 | SSE 解析异常时 | 始终 |
| `DeepSeek调用异常` | 异常消息 + 堆栈 | 整体异常时 | 始终 |
| `DeepSeek流式对话失败` | HTTP 状态码 + 错误信息 | 请求失败时 | 始终 |
| `【Responses API】用户输入(input)` | 用户问题原文 | 每次 Responses API | 仅开发者 |
| `【Responses API】系统提示词(instructions)` | instructions 内容 | 首次 | 仅开发者 |
| `【Chat Completions 流式】提示词内容` | 完整请求 JSON | Chat Completions 流式 | 仅开发者 |
| `【Chat Completions 非流式】提示词内容` | 完整请求 JSON | Chat Completions 非流式 | 仅开发者 |
| `真实提问问题` | askQuestion（assembledPrompt） | 每次 | 仅开发者 |

### 3.2 服务端日志

**位置：** `replay-api/logs/`

**测试时 grep 关键词：**

```bash
# === 查看 assemblePrompt ===
grep "【assemblePrompt】" logs/*.log

# === 查看 ask 流程 ===
grep "【ask】" logs/*.log

# === 查看 Responses API 调用 ===
grep "【Responses API】" logs/*.log

# === 查看 DeepSeek 上下文缓存 ===
grep "DeepSeek createContext\|DeepSeek contextChatCompletionStream\|DeepSeek上下文" logs/*.log

# === 查看错误 ===
grep "streamResponse error\|contextChatCompletionStream 异常\|调用ai问答错误\|DeepSeek.*failed\|DeepSeek.*error" logs/*.log

# === 查看 token 记录 ===
grep "saveAiTokenUseRecord" logs/*.log

# === DEBUG 级别：查看提示词（需调日志级别） ===
grep "【assemblePrompt】allContent\|【assemblePrompt】组装后的提示词\|【Responses API】用户输入\|【Responses API】系统提示词\|DeepSeek的提示词\|DeepSeek上下文输出内容" logs/*.log
```

**关键日志一览：**

| 日志标记 | 级别 | 含义 |
|---|---|---|
| `【assemblePrompt】contextId不存在，开始创建新上下文` | INFO | 首次提问，创建 UUID |
| `【assemblePrompt】创建上下文成功` | INFO | contextId、allContent 长度 |
| `【assemblePrompt】复用已有contextId` | INFO | 第N次提问 |
| `【assemblePrompt】allContent` | DEBUG | 系统背景数据原文 |
| `【assemblePrompt】组装后的提示词 realContent` | DEBUG | 完整拼好的提示词 |
| `【ask】assemblePrompt完成` | INFO | contextId、useModelWay、webVersion |
| `【Responses API】开始请求` | INFO | isFirstTurn、contextId、model、内容长度 |
| `【Responses API】用户输入(input)` | DEBUG | 用户内容原文 |
| `【Responses API】系统提示词(instructions)` | DEBUG | 系统提示词原文 |
| `【Responses API】开始流式调用火山 API` | INFO | 开始流式 |
| `【Responses API】流完成` | INFO | responseId、总chunk/思考/文本chunk数 |
| `【Responses API】Token用量` | INFO | 完整 token 用量 |
| `【ask】Responses API返回` | INFO | 新旧 contextId、全部 token 字段 |
| `【ask】更新Redis contextId` | INFO | Redis key 更新 |
| `【ask】刷新Redis TTL` | INFO | TTL 刷新 |
| `DeepSeek createContext` | DEBUG | 创建上下文成功，含 contextId、msgKey |
| `DeepSeek context stream failed` | ERROR | HTTP 非 2xx，含状态码和响应体 |
| `DeepSeek contextChatCompletionStream error` | ERROR | 整体异常 |
| `DeepSeek SSE line parse error` | WARN | 单行 SSE 解析异常 |
| `DeepSeek trimMessages` | WARN | 裁剪后消息不足 |
| `streamResponse error` | ERROR | 流式调用出错 |
| `contextChatCompletionStream 异常` | ERROR | 整体异常 |

### 3.3 日志排查速查表

| 现象 | 先看 |
|---|---|
| AI 无响应/超时 | C# `【Responses API】HTTP状态码`、服务端 `streamResponse error` |
| 上下文丢失 | C# `【C# assemblePrompt】返回` → contextId；服务端 `【assemblePrompt】` |
| token 记录为 0 | C# `【Responses API】流结束` → token 字段；DB 直接查 |
| 无深度思考 | C# `【Responses API】完成汇总` → 思考chunk 是否为 0 |
| 提示词不对 | C# 开发者模式 `真实提问问题`；服务端 DEBUG `realContent` |
| DeepSeek 无上下文 | 服务端 `DeepSeek上下文缓存不存在或已过期` 是否出现 |
| DeepSeek 上下文丢失 | Redis `GET ai:deepseek:contextId:{UUID}` 检查是否存在 |
| Redis 未更新（豆包） | 服务端 `【ask】更新Redis contextId` 有没有打印 |

---

## 四、数据库与 Redis 验证

### 4.1 ai_token_use_record 表

```sql
SELECT id, tenant_id, user_id, use_source_type, use_source_id, assistant_type,
       model_name, request_id, finish_reason,
       prompt_tokens, completion_tokens, cached_tokens, reasoning_tokens, total_tokens,
       remarks, create_date
FROM ai_token_use_record
ORDER BY create_date DESC
LIMIT 20;
```

**正常值：**

| 字段 | 首次提问 | 追问 |
|---|---|---|
| `prompt_tokens` | > 0 | > 0 |
| `completion_tokens` | > 0 | > 0 |
| `total_tokens` | = prompt + completion | 同 |
| `cached_tokens` | ≥ 0 | 可能 > 0（缓存命中） |
| `reasoning_tokens` | > 0（深度思考开启） | > 0 |
| `request_id` | `resp_xxx` | `resp_yyy` |

### 4.2 Redis

```bash
# 查 contextRedisKey
redis-cli --scan --pattern "*ai:context:*<sourceId>*"

# 查值
redis-cli GET "<key>"

# 查 TTL
redis-cli TTL "<key>"

# 查 instructions（仅首次提问后存在）
redis-cli GET "ai:doubao:contextId:<UUID>"
```

**正常值：**

| 时机 | contextRedisKey 值 | TTL |
|---|---|---|
| 提问前 | nil | - |
| assemblePrompt 后 | UUID | ~3300s |
| Responses API 返回后 | `resp_xxx` | ~3300s |
| 第N次提问后 | `resp_yyy` | ~3300s（刷新） |

---

## 五、测试用例

### 5.1 C# 端 — AI 问答

#### TC-01：首次提问（新对话）

**前置：** Redis 无该 sourceId+type 的 contextRedisKey

**步骤：**
1. C# 客户端打开 AI 问答面板，选豆包模型
2. 输入"这个直播有什么问题？"，发送

**日志验证（C# `logs/log_yyyyMMdd_N.txt`）：**

```
步骤1 — assemblePrompt:
grep "【C# assemblePrompt】" → contextId应为UUID, useModelWay=0, systemPrompt长度>0

步骤2 — Responses API 请求:
grep "【Responses API】开始请求" → isFirstTurn=True, contextId=UUID
grep "【Responses API】关键参数" → hasInstructions=True, hasPreviousResponseId=False
grep "【Responses API】HTTP状态码" → OK

步骤3 — 流式返回:
grep "【Responses API】流结束"   → responseId=resp_xxx, inputTokens>0, outputTokens>0, reasoningTokens>0
grep "【Responses API】完成汇总" → 思考chunk>0, 文本chunk>0

步骤4 — 更新 contextId:
grep "【C# Ask】更新contextId"  → 旧=UUID, 新=resp_xxx
grep "【C# Token记录】"        → promptTokens>0, completionTokens>0, reasoningTokens>0
```

**数据库验证：**
```sql
-- 最新记录 prompt_tokens>0, completion_tokens>0, reasoning_tokens>0, request_id=resp_xxx
```

**Redis 验证：**
```bash
GET <contextRedisKey>  → resp_xxx
GET ai:doubao:contextId:<UUID> → allContent（系统背景数据）
```

---

#### TC-02：追问（第N轮）

**前置：** TC-01 完成，Redis contextRedisKey = `resp_xxx`

**步骤：**
1. 点击追问，输入"针对上面的问题给优化方案"，发送

**日志验证：**
```
grep "【C# assemblePrompt】"    → contextId=resp_xxx（复用）
grep "【Responses API】开始请求" → isFirstTurn=False
grep "【Responses API】关键参数" → hasInstructions=False, hasPreviousResponseId=True
grep "【C# Ask】更新contextId"  → 旧=resp_xxx, 新=resp_yyy
grep "【C# Token记录】"         → cachedTokens可能>0
```

**预期：** AI 回答基于上文；Redis key 值更新为 `resp_yyy`；TTL 刷新。

---

#### TC-03：多轮连续对话（3轮）

**步骤：** 依次输入 3 个相关提问，每轮验证 contextId 更新为最新 `resp_xxx`。

---

#### TC-04：Redis 过期后重建

**步骤：**
1. 完成一次提问 → 手动 `DEL <contextRedisKey>`
2. 再问同一 sourceId+type

**日志验证：**
```
grep "【assemblePrompt】contextId不存在，开始创建新上下文" → 服务端日志
grep "【Responses API】开始请求" → isFirstTurn=True（重建）
```

---

### 5.2 深度思考

#### TC-05：思考内容正常展示

**步骤：** 任意提问，观察前端思考折叠区域和回答正文

**日志验证：**
```
grep "【Responses API】完成汇总" → 思考chunk>0, 文本chunk>0
```

#### TC-06：关闭深度思考

**步骤：** 如前端支持，关闭深度思考后提问

**预期：** `reasoning_tokens = 0`

---

### 5.3 服务端 ask

#### TC-07：服务端首次 ask

**步骤：** 用旧版 C# 客户端（不传 webVersion）提问，触发服务端路径

**日志验证（服务端）：**
```
grep "【assemblePrompt】contextId不存在"  → 创建新上下文
grep "【assemblePrompt】创建上下文成功"    → contextId=UUID
grep "【Responses API】开始请求"          → isFirstTurn=True
grep "【Responses API】流完成"            → responseId=resp_xxx
grep "【ask】更新Redis contextId"         → UUID → resp_xxx
grep "【ask】Responses API返回"           → 完整token信息
```

---

#### TC-08：服务端追问

**前置：** TC-07 完成

**步骤：** 同一 sourceId+type 再问一次

**日志验证：**
```
grep "【assemblePrompt】复用已有contextId" → contextId=resp_xxx
grep "【Responses API】开始请求"          → isFirstTurn=False
```

---

### 5.4 Token 记录

#### TC-09：字段完整性

**步骤：** 完成一次问答，查 DB：

```sql
SELECT prompt_tokens, completion_tokens, total_tokens,
       cached_tokens, reasoning_tokens, request_id, model_name
FROM ai_token_use_record ORDER BY create_date DESC LIMIT 1;
```

**预期：** prompt_tokens>0, completion_tokens>0, total_tokens=两者之和, reasoning_tokens>0, request_id 以 `resp_` 开头。

---

#### TC-10：reasoning_tokens

**步骤：** 开启深度思考提问一次 → 关闭再提问一次 → 对比两条记录的 `reasoning_tokens`。

**预期：** 开启>0，关闭=0。

---

### 5.5 异常场景

#### TC-11：超时

**步骤：** 选长场直播（2h+、弹幕多），开启数据截图+数据看板，提问

**预期：** 前端提示"当前人数分析过多，请稍后再试"；预扣退还；不崩溃

---

#### TC-12：content_filter 拦截

**步骤：** 输入敏感内容

**预期：** 前端提示 code=7007 "模型输出被内容审核拦截"

**日志：**
```
grep "content_filter\|7007" logs/log_*
grep "finishReason" logs/*.log
```

---

#### TC-13：并发提问

**步骤：** 同时 3-5 个不同直播间/类型提问

**预期：** 各自独立，不串上下文

---

### 5.6 回归

| # | 场景 | 操作 | 日志验证 |
|---|---|---|---|
| TC-14 | DeepSeek | 切换 DeepSeek 提问 | `grep "【Chat Completions"` → 走 Chat Completions |
| TC-15 | 通义千问 | 切换通义千问提问 | 正常 |
| TC-16 | 非流式问答 | 触发 AI 内容纠正 | 正常 |
| TC-17 | 旧版客户端 | 不传 webVersion 提问 | `grep "useModelWay"` → =1 |
| TC-18 | HTML/视频分析 | 使用对应功能 | 正常 |

---

### 5.7 Redis

#### TC-19：contextRedisKey 更新

```bash
# 提问前
redis-cli GET "<contextRedisKey>"    → nil

# assemblePrompt 后
redis-cli GET "<contextRedisKey>"    → UUID

# 回答完成后
redis-cli GET "<contextRedisKey>"    → resp_xxx
redis-cli TTL "<contextRedisKey>"    → ~3300s
```

#### TC-20：instructions 缓存

```bash
redis-cli GET "ai:doubao:contextId:<UUID>"  → allContent
redis-cli TTL "ai:doubao:contextId:<UUID>"  → ~3600s
```

#### TC-21：TTL 刷新

**步骤：** 提问后等 1-2 分钟 → 追问 → TTL 应刷新回 ~3300s。

---

### 5.8 DeepSeek 上下文缓存

#### TC-22：DeepSeek 首次提问（创建上下文缓存）

**前置：** Redis 无该 sourceId+type 的 contextRedisKey

**步骤：**
1. C# 客户端切换到 DeepSeek 模型
2. 输入"这个直播有什么问题？"，发送

**日志验证（C# `logs/log_yyyyMMdd_N.txt`）：**
```
grep "【C# assemblePrompt】" → contextId=UUID, useModelWay=0
grep "使用的模型为"           → modelDefinition 含 DeepSeek
```

**日志验证（服务端）：**
```
grep "【assemblePrompt】contextId不存在" → 创建新上下文
grep "【assemblePrompt】创建上下文成功"   → contextId=UUID
grep "DeepSeek createContext"           → DEBUG: contextId=UUID, msgKey=ai:deepseek:contextId:{UUID}
```

**Redis 验证：**
```bash
GET <contextRedisKey>                    → UUID
GET ai:deepseek:contextId:<UUID>         → [{"role":"system","content":"..."}]
TTL ai:deepseek:contextId:<UUID>         → ~3600s
```

**数据库验证：**
```sql
-- 最新记录 prompt_tokens>0, completion_tokens>0, request_id 为 DeepSeek API 返回的 id
```

---

#### TC-23：DeepSeek 追问（上下文中追加）

**前置：** TC-22 完成，Redis `ai:deepseek:contextId:{UUID}` 存在

**步骤：**
1. 同一 sourceId+type，输入"针对上面的问题给优化方案"，发送

**日志验证（C#）：**
```
grep "【C# assemblePrompt】" → contextId=UUID（与 TC-22 相同）
```

**日志验证（服务端）：**
```
grep "【assemblePrompt】复用已有contextId" → contextId=UUID（与 TC-22 相同）
```

**Redis 验证：**
```bash
GET ai:deepseek:contextId:<UUID>
# 应包含 3-4 条消息: system + user(首次) + assistant(首次回复) + user(追问)
# 追问完成后还会多一条 assistant(追问回复)
TTL ai:deepseek:contextId:<UUID> → ~3600s（刷新）
```

**数据库验证：**
```sql
-- 最新记录 request_id 应为新的 DeepSeek API 返回 id
-- cached_tokens 可能 > 0（DeepSeek 缓存命中时 prompt_cache_hit_tokens > 0）
```

---

#### TC-24：DeepSeek 多轮连续对话（3轮）

**步骤：** 依次输入 3 个相关提问

**验证：**
```bash
# 3 轮后 Redis 消息数组应有 ~7 条消息
redis-cli GET ai:deepseek:contextId:<UUID> | python -c "import sys,json; print(len(json.loads(sys.stdin.read())))"
```

**预期：** contextId 始终不变；每轮后消息数组长度 +2（user + assistant）；TTL 每次刷新。

---

#### TC-25：DeepSeek 上下文裁剪

**前置：** 配置 `contextSize` 限制（如 50KB），或填充足够多轮对话使消息历史超过限制

**步骤：**
1. 连续追问直到消息历史超过 contextSize
2. 再追问一次

**日志验证（服务端）：**
```
grep "DeepSeek trimMessages" → WARN: all non-system messages trimmed
```

**Redis 验证：**
```bash
# 首条 system 消息保留，中间旧消息被裁剪
redis-cli GET ai:deepseek:contextId:<UUID>
# 第一条仍是 {"role":"system",...}，后续只保留最近几轮
```

---

#### TC-26：DeepSeek Redis 过期后重建

**步骤：**
1. 完成一次 DeepSeek 提问
2. 手动 `DEL ai:deepseek:contextId:<UUID>` 和 `DEL <contextRedisKey>`
3. 同一 sourceId+type 再提问

**日志验证（服务端）：**
```
grep "【assemblePrompt】contextId不存在" → 创建新上下文，新的 UUID
```

**预期：** 生成新的 UUID，重新创建上下文缓存，上轮对话不保留。

---

#### TC-27：DeepSeek 缓存命中（cached_tokens）

**步骤：**
1. 完成一次 DeepSeek 提问（含较长 system prompt）
2. 追问一次
3. 查 DB 看 cached_tokens

**数据库验证：**
```sql
SELECT prompt_tokens, completion_tokens, cached_tokens, total_tokens
FROM ai_token_use_record
WHERE model_name LIKE '%deepseek%'
ORDER BY create_date DESC LIMIT 5;
```

**预期：** 追问时 `cached_tokens > 0`（DeepSeek prompt cache 命中，system prompt 被缓存），首次提问 `cached_tokens = 0`。

---

#### TC-28：DeepSeek 上下文缓存不存在

**前置：** Redis 无该 contextId 的缓存

**步骤：**
1. 先调 assemblePrompt 拿到 contextId
2. 手动 `DEL ai:deepseek:contextId:{UUID}`
3. 用该 contextId 发追问

**日志验证（服务端）：**
```
grep "DeepSeek上下文缓存不存在或已过期" → 抛出 RRException
```

**预期：** 返回错误提示，前端显示错误信息。

---

### 5.9 视频/文件提示词格式

#### TC-29：AI 问答 prompt 格式验证（视频场）

**前置：** 选择一个有数据的直播场次，开启数据看板/数据截图

**步骤：**
1. C# 或服务端发起 AI 问答（选豆包或 DeepSeek）
2. 开启开发者模式查看 prompt

**日志验证（C# 开发者模式）：**
```
grep "真实提问问题" logs/log_yyyyMMdd_N.txt
# 应看到 "段落1时间:（开始时间：...）" 而非 "本段开始时间：..."
# 应看到 "段落1数据:（在线人数：...）" 而非 "本段在线人数：..."
# 应看到 "段落1内容：" 而非 "本段内容："
```

**日志验证（服务端 DEBUG）：**
```
grep "【assemblePrompt】组装后的提示词 realContent" logs/*.log
# 确认格式为三行分组格式，没有旧的"本段xxx"逐行格式
```

---

#### TC-30：AI 问答 prompt 格式 — 关闭可选指标

**步骤：**
1. C# AI 问答面板，取消勾选部分指标（如关闭成交人数、销售额）
2. 开启开发者模式查看 prompt
3. 检查"段落N数据"行

**预期：**
- 关闭的指标不出现在 `段落N数据:（...）` 中
- 时间行和数据行的中文逗号分隔正确（无多余逗号、无 `（，`）

---

#### TC-31：AI 问答 prompt 格式 — 多段落

**前置：** 选择有 3 个以上段落的直播

**步骤：** 发起提问，开发者模式查看 prompt

**预期：**
- 每个段落三行：`段落1时间:...` `段落1数据:...` `段落1内容：...`
- 段落间以 `\n\n` 分隔
- 第一个段落前无多余换行

---

#### TC-32：AI 问答 prompt 格式 — 字数截断

**前置：** 选择长直播（弹幕多、段落多），开启所有数据指标

**步骤：** 发起提问

**预期：**
- 总 prompt 不超过 wordsNum 限制
- 最后一个段落内容被截断而非整段丢失
- AI 正常回答（不因 prompt 格式问题报错）

---

#### TC-33：自然/优化原文生成 — prompt 格式

**步骤：**
1. 在视频分析页面点击"生成自然原文"或"生成优化原文"
2. 查看生成的原文质量

**日志验证（服务端）：**
```
grep "SegmentTask\|VideoContentGenerator" logs/*.log
# 确认生成流程正常，段落分段生成成功
```

**预期：**
- 自然/优化原文生成成功，内容与视频段落对应
- 无格式异常导致的生成失败

---

#### TC-34：文件原文生成

**步骤：**
1. 上传一个音频/视频文件
2. 等待 ASR 转写完成
3. 触发自然/优化原文生成

**预期：**
- 文件也能正常生成原文（与视频共用同一套生成引擎和 prompt 格式）
- 生成内容质量与视频场一致

---

## 六、冒烟用例（提测必过）

| # | 用例 | 通过标准 | 快速验证命令 |
|---|---|---|---|
| 1 | C# 豆包首次提问 | 思考+回答正常流式展示，不报错 | `grep "【Responses API】完成汇总" logs/log_*` → 思考chunk>0 |
| 2 | C# 豆包追问 | 回答关联上文 | `grep "【Responses API】开始请求"` → 第一次 isFirstTurn=True，第二次 isFirstTurn=False |
| 3 | 豆包 token 记录 | 各字段有值 | SQL 查最新记录 |
| 4 | 豆包 Redis 更新 | UUID → resp_xxx | `redis-cli GET` |
| 5 | DeepSeek 首次提问 | 正常回答，contextId 为 UUID | `redis-cli GET ai:deepseek:contextId:{UUID}` → JSON 数组 |
| 6 | DeepSeek 追问 | 回答关联上文，contextId 不变 | `redis-cli GET ai:deepseek:contextId:{UUID}` → 消息数增加 |
| 7 | 服务端 ask | 日志完整 | `grep "【ask】Responses API返回" logs/*.log` → 有 token 信息 |
| 8 | 提示词格式 | prompt 用新格式 | 开发者模式看 prompt → "段落N时间/数据/内容" 无 "本段xxx" |
