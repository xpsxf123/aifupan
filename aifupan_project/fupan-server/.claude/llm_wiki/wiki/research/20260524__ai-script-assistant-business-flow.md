# Research — AI 问答 · 话术助手业务与处理流程

**Date:** 2026-05-24
**Question source:** 用户原文 — "分析AI问答的话术助手的业务以及处理流程"
**Status:** 调研完成（基于 wiki + 代码 grep 验证，未跑接口）
**Audience:** 开发 + PM / 产品评审（双受众）
**Scope:** (c) AI 问答主链路 + HTML / 纠正 / 分享全链路 — 以 `askType = 6 / AI_SCRIPT_ASSISTANT` 为锚点
**Sister docs:** 后续若决定补充，建议新建 `wiki/api/openapi_api.md`（覆盖 `replay-api/.../controller/openapi/` 入口）+ `wiki/domain/ai_assistant_taxonomy.md`（16 种 askType 分类表）

---

## 1. 问题陈述（用户原文）

> 分析AI问答的话术助手的业务以及处理流程

拆解：
- **业务**：话术助手对用户是什么、给谁用、输入输出、与诊断 / CueWords / CustPrompt 的关系
- **处理流程**：用户发起一次提问 → 系统拼提示词 → 调模型 → 流式回传 → 落 MongoDB → 后续 HTML / 纠正 / 分享的全链路

报告按 §2 业务与产品语义 → §3 系统拓扑与端到端流程 → §4 数据落点与状态机 → §5 Gap 分析 → §6 推荐路径 → §7 待澄清 组织。

---

## 2. 业务与产品语义

### 2.1 话术助手在 16 种 askType 中的位置

`com.jiuyu.replay.common.constant.AiEnums.askType` 共 **16 个 code (0-15)**：对话型助手 0-11（用户可选触发）+ 异步增强用 12 / 14 / 15 + 诊断流专用 13。话术助手是 code=6：

| code | 枚举 | 业务定位 | 装配器（仅 sourceType=0/视频） |
|---|---|---|---|
| 0 | OPERATION | 运营助手 | `videoSentenceImpl` |
| 1 | VIOLATION | 违规助手 | `videoSentenceImpl` |
| 2 | BARRAGE | 弹幕助手 | `barrageSentenceImpl` |
| 3 | SCREENSHOT | 截图助手 | `screenshotVideoSentenceImpl` |
| 4 | BOARD | 看板助手 | `viewingConfuseSentenceImpl` |
| 5 | IMPORTANT_SCREENSHOT | 重要弹幕提示词 | （走默认 fallback） |
| **6** | **AI_SCRIPT_ASSISTANT** | **AI 话术助手** | **`videoSentenceImpl`**（**与运营 / 违规共用**） |
| 7 | NATURAL_ORIGINAL_TEXT | 自然原文提示词 | （走默认 fallback） |
| 8 | OPTIMIZE_ORIGINAL_TEXT | 优化原文提示词 | （走默认 fallback） |
| 9 | TRADE_RECOMMEND | 行业推荐 | （走默认 fallback） |
| 10 | EXTRACT_VIDEO | 提取文案优化 | （走默认 fallback） |
| 11 | ANCHOR_KEYWORD | 主播关键词获取 | （走默认 fallback） |
| 12 | AI_HTML_PROMPT | AI 结果转 HTML | 非对话助手，HTML 异步增强用 |
| 13 | DATA_DIAGNOSIS | 数据诊断提示词 | 走诊断流，不算独立助手 |
| 14 | CHECK_AI_CORRECT | 检查 AI 内容 | 非对话助手，异步增强用 |
| 15 | AI_CORRECT_CONTENT | AI 纠正内容 | 非对话助手，异步增强用 |

来源：[`ai_domain.md` §四](../domain/ai_domain.md) askType 枚举表 + `replay-api/.../logic/ai/factory/AiFactoryUtils.java` 路由 map。

### 2.2 话术助手产品语义（**需 PM 确认**）

> **本节"产品定位"内容由代码语义反推 + 推断，未与 PM 确认。落产品文档前请由 PM 校稿。**

**代码语义可确定的事实：**

| 维度 | 事实 |
|---|---|
| 入口位置 | 客户端的视频复盘页面，与运营 / 违规 / 弹幕 / 截图 / 看板等助手并列 |
| 适用源 | 仅 `sourceType=0`（视频）— `AiFactoryUtils` 中 `"0-6"` 显式映射；其他 sourceType 走 fallback 装配，行为退化 |
| 装配出的"基础数据" | 与运营 / 违规助手**完全一致**（共享 `videoSentenceImpl`）：脚本段落 + 在线人数 + 弹幕段落 + 段落开始 / 结束时间 — 按 `videoTimeOneList` 时间窗截取 |
| 仅有的差异 | 系统提示词不同 — `tb_cue_words` 中按 cueType 配的话术助手提示词文本（具体文本未在本次调研深扫，留 G3） |
| 历史 | `ScriptSentenceImpl.java`（命名最像"话术"的类）类级 Javadoc 标注 `已经不用了`，曾是话术助手的专属装配器，已被合并到 `videoSentenceImpl` |

**推断的产品语义（待 PM 确认 / 改写）：**

- **是什么**：基于视频脚本（直播录屏的文字稿）+ 实时在线人数 / 弹幕段落，向 AI 提问以**生成话术建议** — 例如"针对这段在线人数下降的脚本，给出 3 条改写建议"。
- **与运营 / 违规助手的差异**：(1) 系统提示词的指令意图不同（话术 = 生成 / 优化话术；运营 = 分析运营效果；违规 = 检测违规风险）；(2) 装配出的素材 100% 相同，差异完全由提示词驱动。
- **未支持**：文件 / 对比分析场景下话术助手无显式实现（依赖 fallback 行为），即话术助手是**视频专属**助手。
- **企业版门槛**：未在代码中看到对话术助手专属的版本锁定，企业版门槛仅在 `CustPromptController.save`（自定义提示词）显式校验 `OrderFeign.currentOrder().level >= 20`。

**待 PM 确认的开放点：**
- Q: 话术助手的核心 ROI / 用户痛点是什么？是"生成新话术"还是"优化既有话术"？
- Q: 是否计划把话术助手扩展到文件 / 对比分析场景（即支持非视频源）？
- Q: 话术助手是否锁定企业版？目前代码无显式门槛但产品页可能有。
- Q: `ScriptSentenceImpl` 死代码是否可删？

### 2.3 与上下文实体的关系

```
┌─────────────────────────────────────────────────────────────┐
│  话术助手提问 (askType=6)                                    │
│     │                                                        │
│     ├─ 读取系统提示词 tb_cue_words (cueType=6)                │
│     │   OR 用户自定义提示词 tb_cust_prompt (企业版及以上)      │
│     │     ↑                                                  │
│     │     └─ AiRelatedBll.getAiPromptWord2 处理回退逻辑       │
│     │                                                        │
│     ├─ 不写 tb_diagnosis_cue（诊断流专用）                    │
│     ├─ 不进 diagnosis_cue_type 字典 → 不触发                  │
│     │      hasDiagnosisReport 回写副作用                      │
│     │                                                        │
│     └─ 写 replay_ai_conversation MongoDB (Q+A 双文档)         │
│              ↓                                                │
│         可 → ConversationBll.serviceGenerateHtml (HTML 生成)  │
│         可 → ConversationBll.serviceCorrectAiContent (纠正)    │
│         可 → ShareLinkRecordBll.save (qaCode 打包分享)         │
└─────────────────────────────────────────────────────────────┘
```

**关键边界**：话术助手是一次性对话型助手（与"诊断"流的批量异步生成不同）。诊断流写 `tb_diagnosis_cue` + 状态机驱动；话术助手不写 `tb_diagnosis_cue`，只写 MongoDB conversation 流水。

---

## 3. 系统拓扑与端到端处理流程

### 3.1 模块拓扑

```
┌──────────────────────────────────────────────────────────────────┐
│ Web 客户端                              C# 桌面客户端              │
│   │                                       │                       │
│   │ ① POST /openapi/ai/ask                │ ② POST /openapi/ai/   │
│   │    (SSE 服务端代理, 服务端持 Key,      │    assemblePrompt     │
│   │     服务端自己落库 + 计费)             │    (拼好 prompt 给客户端，│
│   │                                       │     客户端自己调模型)   │
│   │                                       │                       │
│   │                                       │ 或 POST /openapi/ai/  │
│   │                                       │    chatStreamProxy    │
│   │                                       │    (服务端代理流式，  │
│   │                                       │     不落库 / 不计费)   │
│   ▼                                       ▼                       │
├──────────────────────────────────────────────────────────────────┤
│ replay-api / controller / openapi / AiRelatedController            │
│   ↓                                                                │
│ replay-api / logic / third / impl / AiRelatedLogicImpl             │
│   ├─ ask(emitter)                        ── Web SSE 主路径         │
│   ├─ assemblePrompt(askRequestBo)        ── 提示词装配核心          │
│   ├─ chatStreamProxy(emitter)            ── C# 桌面 SSE 代理        │
│   ├─ contextChatCompletionStream         ── useModelWay==0          │
│   ├─ chatCompletionStream                ── useModelWay==1          │
│   └─ saveAiTokenUseRecord                ── token 计费             │
│        ↳ AiFactoryUtils.getSentenceMark(sourceType, askType)       │
│           ↳ 话术助手 → VideoSentenceImpl                           │
│             (装配脚本段落 + 在线人数 + 弹幕)                       │
│        ↳ ModelFactoryUtils.getAiModel(resourceType).chatStream()   │
│           ↳ 字节豆包 / DeepSeek / 其它（replay-third 实现）        │
├──────────────────────────────────────────────────────────────────┤
│ ③ 落库（Web SSE 由服务端直接落；C# 客户端自己 POST 落库）           │
│ replay-ai / controller / ConversationController.C02                │
│   ↓                                                                │
│ replay-ai / bll / ConversationBll.saveConversationData             │
│   ├─ ConversationRse.saveAll → MongoDB replay_ai_conversation     │
│   │     (Q + A 两条文档共享 qaCode，按 contextId 串多轮)           │
│   └─ saveConversationDataPost (副作用 — 话术助手不命中)            │
│                                                                     │
│ ④ 后续异步增强（用户在客户端按"生成HTML / 让 AI 纠正 / 分享"按钮）  │
│   ├─ ConversationBll.serviceGenerateHtml (异步线程池)              │
│   ├─ ConversationBll.serviceCorrectAiContent (异步线程池)          │
│   └─ ShareLinkRecordBll.save (同步打包 qaCode 列表)                │
└──────────────────────────────────────────────────────────────────┘
```

涉及模块：`replay-api`（入口 + prompt 装配 + SSE + 后续异步增强协调）/ `replay-ai`（问答落库 + HTML / 纠正 / 分享 BLL）/ `replay-third`（模型实现 + API Key）/ `replay-words`（视频脚本 + 弹幕 + 提示词文本 `tb_cue_words`）/ `replay-order`（token 计费 `tb_ai_token_use_record` + 企业版校验）/ `replay-power`（鉴权 `UserFeign.getLocalUser`）/ `replay-system`（字典 `diagnosis_cue_type` / `ai_model_min_web_version` / `ask_require_additional`）/ `replay-common`（线程池 `generateHtmlExecutor` + Redis）。

### 3.2 端到端流程 — Web SSE 主路径（`POST /openapi/ai/ask`）

**这是话术助手在 Web 端的主要执行路径。服务端持模型 API Key、SSE 流式回写、服务端自己落 MongoDB + 写计费 + 解预扣。**

| 步骤 | 调用点 | 关键动作 | 数据落点 |
|---|---|---|---|
| ① 鉴权 | 全局拦截器 → `UserFeign.getLocalUser()` | 解析 token → `UserCacheVo(id, activeTenantId, nickName)` | — |
| ② 取提示词 | 客户端预调 `GET /replay/ai/aiRelated/getAiPromptWord2?cueWordsId&cueWordsType&lastConversationId` | `AiRelatedBll.getAiPromptWord2`：① 按 `cueWordsType` 查 `tb_cue_words` 或 `tb_cust_prompt`；② 解析 `#{reason}` 占位符（占位符前后 4 字符做锚点反扫 `lastConversation.realContent`）；③ 检测 `的优化目的：` / `的优化动作：` 子串置 `optimizeActions=1`；④ AES 加密回传 | 读 `tb_cue_words` / `tb_cust_prompt` / MongoDB `replay_ai_conversation` |
| ③ 发起 SSE | `POST /openapi/ai/ask` 入参 `AskRequestBo`（`sourceId / sourceType / type=askType / realContent / cueWordsId / cueWordsType / contextId / videoTimeOneList / otherObj`） | `AiRelatedController.ask` (line 97) → 创建 `CustomizeSseEmitter(20 分钟超时)` → 调 `AiRelatedLogic.ask(askRequestBo, emitter)` | — |
| ④ 同步装配 | `AiRelatedLogicImpl.ask` (line 135, `@Transactional`) → `assemblePrompt(askRequestBo)` (line 208) | 1. `getAiModelBo`（按用户偏好或系统默认）2. `setPrompt`（拼系统提示词）3. **`AiFactoryUtils.getSentenceMark(sourceType=0, askType=6)` → `videoSentenceImpl`**（装配脚本段落 + 在线人数 + 弹幕；按 `videoTimeOneList` 截取时间窗）4. `setSpeed` / `setAiOptionConfigData`（截图 + 看板）5. `setQuestion` / `setPlaceholder` 6. `extraBuildLastOutString`（重新提问场景）7. Redis 上下文缓存 `useModelWay==0` 时 `aiModel.createContext` 创建并落 `contextRedisKey`（TTL = `contextTimeout - 5min`） | Redis（contextId 缓存）+ `tb_ai_token_use_record`（createContext 也写一条 token 记录） |
| ⑤ 预扣资产 | `withhold(userId, 20000L)` (line 154) | 调用 `userPropertyBll` 预冻 20000 token | Redis 临时预扣 |
| ⑥ 异步流式调模型 | `ExecutorUtil.customPool.execute(...)` (line 157) | `useModelWay==0` → `contextChatCompletionStream`；`useModelWay==1` → `chatCompletionStream` — 流式调豆包 / DeepSeek / 字节AI，逐 token `emitter.sendMessage` | SSE 流；异常 `emitter.sendMessage("error")` + `sendStop2` |
| ⑦ 计费 | 异步线程内 `saveAiTokenUseRecord(askRequestBo, aiModelBo, aiReturnDataVo, "")` (line 563) | 通过 `AiTokenUseRecordFeign` 写一条 `tb_ai_token_use_record`：`useSourceType=sourceType`（不是 `CONVERSATION=6`，注意 — 见 G7）/ `assistantType=askType=6` / `tenantId` / `userId` / token 数 / requestId | `tb_ai_token_use_record` |
| ⑧ 服务端落库 | `lastSendMessage(emitter, askRequestBo, aiReturnDataVo)` (line 176) → 内部拼 problem + answer 两条 `ConversationVo` → `conversationBll.saveConversationData(...)` (line 701) | **服务端直接调 `ConversationBll.saveConversationData`** — 客户端**不需要**再调 `/replay/ai/conversation/saveConversationData`；最后发 "returnData" 事件给客户端，附两条文档的 id | MongoDB `replay_ai_conversation`（Q+A 双文档共享 `qaCode`，按 `contextId` 串多轮） |
| ⑨ 扣账 | `useProperty(askRequestBo, aiReturnDataVo, propertyHave, tempToken)` (line 179) | 按 `aiModelBo.consumeMultiple` 计算实际消耗倍数 → 写永久消耗记录 | `replay-order` 用户资产表 |
| ⑩ 解预扣 + 关 SSE | finally 块 (line 186) | `userPropertyBll.removeTempUserProperty(...)` → `emitter.sendStop2()` | — |

### 3.3 端到端流程 — C# 桌面 `assemblePrompt` 路径

**桌面客户端拿到完整 prompt + contextId + 模型配置后，自己直连模型。落库 + 计费由客户端单独发起。**

| 步骤 | 调用点 | 关键动作 |
|---|---|---|
| ① | `POST /openapi/ai/assemblePrompt` (line 210, `@UserLock`) | 服务端调 `AiRelatedLogicImpl.assemblePrompt` 拼词（与 Web 路径 ④ 完全相同 — 含 createContext + 写 createContext 的 token 记录），返回 `PromptAssemblyResultVo { assembledPrompt, identity, contextId, modelConfig }` |
| ② | 客户端拿 prompt + 自己持 API Key（或 `AiTempToken`） | 直连豆包 / DeepSeek SDK，自己处理流式回写 |
| ③ | 客户端 `POST /replay/ai/conversation/saveConversationData` (`ConversationController.C02`) | 客户端拼好 Q+A 两条 `ConversationBo` → 服务端落 MongoDB |
| ④ | 客户端 `POST /openapi/ai/saveAiTokenUseRecord` (line 140) | 客户端按自己拿到的 `usage` 字段拼 `AiTokenUseRecordBo` → 服务端写 `tb_ai_token_use_record` |

**注意：** 这条路径下，服务端**只负责拼词 + 接受落库 / 计费 RPC**，**不知道**客户端是否真的成功调到了模型，也**不解预扣**（因为 `assemblePrompt` 路径不预扣）。

### 3.4 端到端流程 — C# 桌面 `chatStreamProxy` 路径（备用 SSE 代理）

**当 C# 桌面客户端不能直连模型时（如出口 IP 受限、不持 Key），走服务端 SSE 代理。**

| 步骤 | 调用点 | 关键动作 |
|---|---|---|
| ① | `POST /openapi/ai/chatStreamProxy` (line 191 / 实现 line 973) | 服务端按 `bo.modelId` 拉模型配置 → 异步线程池调 `aiModel.chatCompletionStream` → 流式 `emitter.sendMessage` 回客户端 |
| ② | 客户端收完 token + 拿 `returnData`（含 `aiReturnDataVo`） | 客户端按需自己 `POST /replay/ai/conversation/saveConversationData` 落库 + `POST /openapi/ai/saveAiTokenUseRecord` 写计费 |

**关键差异（与 `ask` 主路径相比）：**
- `chatStreamProxy` **不调 `assemblePrompt`** — 客户端必须自己拼好 prompt 传进来（`bo.identity` + `bo.realContent`）
- **不预扣 / 不写 `saveAiTokenUseRecord` / 不落 MongoDB** — 完全由客户端善后

---

## 4. 数据落点

### 4.1 MongoDB `replay_ai_conversation`（每次提问 2 条文档）

```
{ qaCode: "<uuid>", contextId: "<ctx>", type: "A", realContent: "<用户原始提问>",
  sourceId: "<videoId>", sourceType: 0, askType: 6, cueWordsId: 123, cueWordsType: 0,
  userId, tenantId, createTime, ... }
{ qaCode: "<uuid 同上>", contextId: "<ctx>", type: "Q", content: "<模型回答>",
  completionId: "<requestId>", lastConversationId: "<上一轮 A 的 _id>",
  htmlStatus: 0, aiCorrectStatus: 0, sourceId, sourceType, askType=6,
  userId, tenantId, createTime, ... }
```

注意：**业务命名反人类** — `type="Q"` 存的是 AI 的**回答**，`type="A"` 存的是用户的**提问**（见 [`ai_domain.md` §五](../domain/ai_domain.md)）。

### 4.2 MySQL `tb_ai_token_use_record`（`replay-order` 模块）

每次 chatCompletion / createContext 都写一条计费记录：

| 字段 | 值 | 说明 |
|---|---|---|
| `useSourceType` | `askRequestBo.sourceType`（0 视频 / 1 文件 / 2 对比） | **代码 line 606 实际是 sourceType，不是 AiEnums.useSourceType.CONVERSATION=6** — 与 `ai_domain.md` §四 `useSourceType` 枚举表的 6=CONVERSATION 不符，存疑（G7） |
| `useSourceId` | `askRequestBo.sourceId` | videoId / fileId / 对比分析 ID |
| `assistantType` | `askRequestBo.type` (= askType = 6) | 话术助手时为 6 |
| `userId` / `tenantId` | 当前用户 | 隔离 |
| `requestId` | `aiReturnDataVo.requestId` | 关联 MongoDB Conversation 的 `completionId` |
| `realTotalTokens` | 模型返回 | 按 `consumeMultiple` 倍数换算后扣 ai-token |

### 4.3 Redis

| 用途 | Key 模式 | TTL |
|---|---|---|
| 上下文缓存 | `sentenceMark.getContextRedisKey(askRequestBo)` 生成 | `contextTimeout - 5min`（`contextTimeout` 实际值未在本次扫描确认，留 G5） |
| 临时预扣 | `userPropertyBll.withhold` / `removeTempUserProperty` 内部维护 | 短期，调用结束即释放 |

### 4.4 OSS（后续异步增强用）

| 用途 | Bucket | Key 前缀 | 触发点 |
|---|---|---|---|
| HTML 输出 | `replay-images` | `ai-html/{snowflakeId}.html` | `ConversationBll.serviceGenerateHtml` |
| 内容诊断 PDF | `replay-ai-data` | `diagnosisFile/yyyy/MM/dd/{videoId}.pdf` | 诊断流（话术助手不触发） |
| 数据诊断 PDF | `replay-ai-data` | `dataDiagnosisFile/yyyy/MM/dd/{videoId}.pdf` | 诊断流（话术助手不触发） |
| 分享数据文本 | `replay-ai-data` | `dataTxt/...` | `ShareLinkRecordController.S06` |

---

## 5. 后续异步增强链（对话生成后的 3 条延伸路径）

> 用户在客户端拿到回答后，可选触发以下 3 种延伸操作。话术助手生成的 conversation 与其他 askType 共享同一套增强链，无特殊路径。**详细接口契约见 [`ai_api.md`](../api/ai_api.md) C07-C17 / S01-S06。**

### 5.1 路径 A — HTML 可视化（C08 / C09 / C07 / C12）

**模式：** 同步改状态 (`htmlStatus: 0→1`) + 异步线程池 `generateHtmlExecutor` 跑模型 → 成功改 2 / 失败改 3 / 超时被动清扫（`html_generate_expiration_time` 默认 20min，由 C03 `conversationPage` 列表接口扫到）。

**两种变体：**
- 服务端走全套（C08）：服务端拼 prompt + 异步调 `AiFeign.chatCompletion` + 写 token 记录（`useSourceType=CONVERSATION=6` / `askType=12`）+ OSS 上传 `ai-html/{snowflakeId}.html`
- 客户端走半套（C09 → 自调模型 → C07 回传）：服务端只拼 prompt 给客户端 + 客户端回传 `htmlSavePath` + SUCCESS

### 5.2 路径 B — AI 内容纠正（C14 / C15 / C13 / C17）

**模式：** 与 HTML 同构（**共用同一 `generateHtmlExecutor` 线程池**）。状态机 `aiCorrectStatus: 0→1→2/3`，超时 `correct_generate_expiration_time` 默认 20min。

**关键：** 客户端纠正变体下，服务端**自动合并原 deepThinking 区块**到 content 头部（用户看不到 deepThinking 仍能拿到完整纠正 + 思考链）。

### 5.3 路径 C — qaCode 打包分享（S05 / S02）

把若干 `qaCode` 序列化为 JSON 列表存 `tb_share_link_record.codes` 字段 → 返回 `ShareSaveVo { id, host }`（host = `systemKv.client_front_domain_name`）→ 客户端拼 `host + path + id` 得分享 URL。S02 查看时从 MongoDB 反查 conversation 集合，捕获 `MongoSocketOpenException` 包装为 "MongoDB连接 Timed out"。过期清扫由 `ShareLinkRecordBll.updateUrlExpire(days)` 外部调度触发（非 `@Scheduled`）。

---

## 6. Web SSE vs C# 桌面 — 责任划分对照表（G8 已升 P1）

| 职责 | Web `POST /openapi/ai/ask`（SSE 主路径） | C# 桌面 `assemblePrompt`（拼词 + 客户端直连） | C# 桌面 `chatStreamProxy`（SSE 代理备用） |
|---|---|---|---|
| **prompt 装配** | 服务端 `assemblePrompt` 内部完成 | 服务端 `assemblePrompt` 返回完整 prompt | **不装配** — 客户端自己拼好传 `bo.identity + bo.realContent` |
| **持 API Key** | 服务端 | 客户端（自己持 Key 或用 `AiTempToken`） | 服务端 |
| **createContext + 上下文缓存** | 服务端 `assemblePrompt` 内做 | 服务端 `assemblePrompt` 内做 | **不做** — 单次无上下文调用 |
| **调模型（流式）** | 服务端 → SSE 回写 | 客户端 → 客户端自己处理 | 服务端 → SSE 回写 |
| **token 计费 `tb_ai_token_use_record`** | 服务端在异步线程内写（line 563） | 客户端按需 `POST /openapi/ai/saveAiTokenUseRecord` | **不写** — 完全由客户端负责 |
| **落库 MongoDB conversation** | 服务端 `lastSendMessage` 内自动落（line 701） | 客户端 `POST /replay/ai/conversation/saveConversationData` | **不落** — 客户端按需自己 POST |
| **预扣 / 解预扣 ai-token** | 服务端 `withhold(20000)` + finally `removeTempUserProperty` | **不预扣**（无对应字段） | **不预扣** |
| **实际扣账 `useProperty`** | 服务端按 `consumeMultiple` 倍数扣 | 客户端 RPC 触发服务端扣 | 客户端 RPC 触发 |
| **错误恢复** | finally 解预扣 + `sendStop2` | 无服务端兜底（客户端崩了 token 算白扣） | 无服务端兜底 |

**核心差异总结：**
- **Web SSE 主路径** = "一站式" 服务端搞定所有事情，客户端只收 SSE 流
- **C# 桌面 assemblePrompt 路径** = 服务端只拼词 + 接收落库 / 计费 RPC，客户端责任最重
- **C# 桌面 chatStreamProxy 路径** = 服务端只代理流式，不做任何落地工作

**疑虑（待开发确认）：** assemblePrompt 路径下，如果客户端崩溃 / 网络中断，token 配额是否会被白扣（因为不预扣不解扣）？看代码似乎无补偿机制，建议在 G8 推进时验证。

---

## 7. Gap 分析

| # | 数据点 | 现状 | 缺什么 | 工作量 | 优先级 |
|---|---|---|---|---|---|
| G1 | `AI_SCRIPT_ASSISTANT` 产品语义 | 仅枚举名 + `AiFactoryUtils` 注释一行；本报告 §2.2 已基于代码反推一版，但需 PM 校稿 | PM 一段话产品定义 + 与运营 / 违规助手的产品差异 + 是否锁版本 | **S**（PM 出一段话） | P1 |
| G2 | `videoSentenceImpl` 装配出的 prompt 模板 | 代码可读但无实际 prompt sample | dump 一份实际 prompt：运营 / 违规 / 话术助手三者拼出的完整字符串对比 | **M**（dump + 整理） | P1 |
| G3 | `tb_cue_words` 中 `cueType=6` 的实际提示词文本 | **已完成 — 见 [`cue-words-sample.md`](./20260524__ai-script-assistant-cue-words-sample.md)（话术助手专题）+ [`cue-words-askType-inventory.md`](./20260524__cue-words-askType-inventory.md)（全 16 askType × 1314 prompt 图鉴）**：发现 sort 不稳定 + `cue_word` 误导 + 5 个 askType (9/10/11/14/15) 走 systemKv/硬编码/死代码、不存 tb_cue_words + `account_type` 疑似平台变体 | — | **已完成** |
| G4 | wiki 多处描述偏差 | (a) `ai_api.md` line 24 写"未使用 SSE" — `AiRelatedController#ask`/`chatStreamProxy` 实用 SseEmitter；(b) `ai_domain.md` line 45/100 写"14 种 askType" — 实际 16 个 code (0-15)；(c) `ai_domain.md` §四 `useSourceType=6=CONVERSATION` 不完整，应分两层（见附录 A）；(d) `ai_domain.md` §六.1 "USER → tb_cust_prompt" 不完整，`tb_cue_words` 也能装租户私有 prompt（见伴生文档） | **已完成（PATCH 2026-05-24，本会话）** — `ai_api.md` SSE 描述 + `ai_domain.md` 3 处全部修订到位；新建 `openapi_api.md` 推后单独立任务 | **M** | **已完成** |
| G5 | `contextTimeout` 配置点 | line 274 用 `contextTimeout - 5min` 写 Redis TTL，值来源未确认 | 配置点（systemKv? application.yml? 硬编码?） + 超时后是否自动重建 context | **S**（一次 grep） | P2 |
| G6 | `useModelWay` 0 / 1 / 2 含义 | 0=有上下文流式 / 1=无上下文流式 / 其他=未知 | 决策来源（按 model code 静态映射？字典？） | **S**（一次 grep） | P2 |
| G7 | `tb_ai_token_use_record.useSourceType` 值口径 | `AiRelatedLogicImpl.saveAiTokenUseRecord` line 606 写入 `askRequestBo.sourceType`（0 视频 / 1 文件 / 2 对比），但 `ai_domain.md` §四 `useSourceType` 枚举表标记 6=CONVERSATION — **二者口径不一致** | **已完成（附录 A）** — 全代码 grep 10 个调用点，结论：`useSourceType` 是两层语义（直接对话写 sourceType / 异步增强写 CONVERSATION=6）；wiki §四"6=CONVERSATION"的标注不完整 | — | **已完成** |
| G8 | Web SSE vs C# 桌面责任划分 | 本报告 §6 已补全对照表 | （已完成）后续若有 assemblePrompt 路径补偿机制疑虑（崩溃白扣 token）需开发验证 | — | 已完成；补偿机制是 P2 留观 |
| G9 | 话术助手的扣费 / 配额规则 | 仅看到预扣 20000 + `consumeMultiple` 倍数扣账 | 完整扣费公式 + 各模型 `consumeMultiple` 默认值 + 用户资产池设计 | **M**（需读 `userPropertyBll` + `replay-order` 文档） | P2 |
| G10 | `ScriptSentenceImpl` 死代码处置 | 类级注释 `已经不用了`，但仍注册 Spring Bean | 评估是否可删（grep 调用方应为 0） | **S** | DROP |
| G2 | `videoSentenceImpl` 装配出的 prompt 模板 | **已完成（附录 B）** — 基于 `VideoSentenceImpl.minuteParagraphContent` + `getTextParamsContent` + `AiRelatedLogicImpl.assemblePrompt` line 262 拼出 prompt 骨架样本；运营/违规/话术三者**装配结构一致**，差异 100% 在系统提示词文本 | — | — | **已完成** |

---

## 8. 推荐路径

| 优先级 | 行动 | 理由 | 当前状态 |
|---|---|---|---|
| ~~**P0**~~ | ~~修订 `ai_api.md` SSE 错误描述 + 新建 `wiki/api/openapi_api.md`~~ | ~~wiki 与代码事实偏差，会误导后续 agent~~ | **已完成（PATCH 2026-05-24）**；新建 `openapi_api.md` 推后到独立 backlog |
| **P1** | 让 PM 填补 G1 产品语义（§2.2 待 PM 确认部分） | 双受众文档（开发 + PM）必须有产品定义层 | 待 PM 介入 |
| ~~**P1**~~ | ~~补 G2 prompt 样本对比~~ | ~~没有这份样本，"话术助手到底拼出什么"是黑盒~~ | **已完成 — 见附录 B**（基于代码反推骨架，非运行时 dump） |
| ~~**P1**~~ | ~~补 G3 `tb_cue_words.cueType=6` 内容清单~~ | ~~这是话术助手的"灵魂"——提示词文本决定所有差异~~ | **已完成 — 见伴生文档 [cue-words-sample.md](./20260524__ai-script-assistant-cue-words-sample.md)；衍生 G3.1（开发确认租户级支持）/ G3.2（PM 确认子能力规范化）作为新 P1** |
| ~~**P1**~~ | ~~校对 G7 `useSourceType` 值口径~~ | ~~影响 token 计费统计的可解释性~~ | **已完成 — 见附录 A**（10 个调用点全代码 grep） |
| **P2** | 一次性扫清 G5 / G6 / G9：`contextTimeout` / `useModelWay` / 扣费公式 | 技术细节，不影响主流程理解 | 可攒到下次 "AI 问答深度调研 v2" |
| **P2 观察** | G8 补偿机制：assemblePrompt 路径客户端崩溃是否白扣 token | 需开发现场验证 | 留观，如出现客户投诉再追 |
| **DROP** | G10 `ScriptSentenceImpl` 死代码 | 风险低，等下次重构 `replay-api/logic/ai/` 时顺手删 | 不主动起任务 |

---

## 9. 用户决策记录（本会话已收）

第一轮：Q1 双受众（开发+PM，触发 §2.2 产品语义）/ Q2 全链路（触发 §5 异步增强链）/ Q3 暂缓 wiki SSE 修订（G4 延后）/ Q5 Web SSE + C# 桌面两路径都在用（触发 §6 三路径对照表）。第二轮：推进 P1 的 G2 + G7（落地为附录 A + B）。

---

## Source Material

### Wiki anchors（已读）
- [`.claude/llm_wiki/wiki/domain/ai_domain.md`](../domain/ai_domain.md) — askType 枚举、状态机、核心规则
- [`.claude/llm_wiki/wiki/architecture/ai_architecture.md`](../architecture/ai_architecture.md) — replay-ai 分层、ADR 1-6、跨模块通信（**注：SSE 错误实际在 `ai_api.md` line 24，非本文件 — 已 PATCH 修正**）
- [`.claude/llm_wiki/wiki/api/ai_api.md`](../api/ai_api.md) — `ConversationController` / `DiagnosisController` / `CustPromptController` / `ShareLinkRecordController` / `AiController` 契约
- [`.claude/llm_wiki/wiki/data/ai_data.md`](../data/ai_data.md) — MongoDB 2 collections + MySQL 4 tables 字段表

### 代码验证关键路径（grep + read）

| 路径 | 行 | 验证点 |
|---|---|---|
| `replay-api/.../controller/openapi/AiRelatedController.java` | 97 / 191 / 210 | `ask` (SSE) / `chatStreamProxy` / `assemblePrompt` 三个对外入口 |
| `replay-api/.../logic/third/impl/AiRelatedLogicImpl.java` | 135 / 157 / 208 / 563 / 701 / 973 | `ask` 主流程 / 异步线程池 / `assemblePrompt` 装配 / `saveAiTokenUseRecord` 计费 / `saveConversationData` 服务端落库 / `chatStreamProxy` 实现 |
| `replay-api/.../logic/ai/factory/AiFactoryUtils.java` | 30 | `"0-6", "videoSentenceImpl",// ai话术助手` 路由映射 |
| `replay-api/.../logic/ai/impl/ScriptSentenceImpl.java` | 32 | 类级 `@description: 已经不用了` |
| `replay-api/.../logic/ai/impl/VideoSentenceImpl.java` | 107 / 365 | `aiAssistantDisplay` 装配参数读取点 |
| `replay-ai/.../controller/ConversationController.java` | C01–C17 | 落库 + HTML / 纠正接口 |
| `replay-ai/.../bll/ConversationBll.java` | `serviceGenerateHtml` / `serviceCorrectAiContent` / `saveConversationDataPost` | 异步增强链 |

### 未深扫（留给 P1 / P2 跟进）

G3 `tb_cue_words.cueType=6` 实际数据（需 DB）/ G5 `contextTimeout` 配置源 / G6 `getUseModelWay` 字典源 / G9 `UserPropertyBll` 预扣配额公式。G7 已完成（附录 A）。

---

**报告完。下一步候选：(a) 让 PM 补 G1；(b) 查 DB 补 G3；(c) 暂存归档观望。**

---

## 附录 A — `tb_ai_token_use_record` 调用点全代码 grep（G7 解决）

**目标：** 校对 `useSourceType` 字段语义 — wiki `ai_domain.md` §四 标 `6=CONVERSATION`，但代码 `AiRelatedLogicImpl.java:606` 写入的是 `askRequestBo.sourceType`（0/1/2 三选），二者矛盾。

**结论先行：**
- `useSourceType` 是**两层语义**字段：
  - **直接对话场景** → 写 `sourceType`（0 VIDEO / 1 FILE / 2 SYNC_ANALYSIS）：所有 Web SSE 主路径（ask）、自然原文 / 优化原文 / 行业推荐 / 重要弹幕 / 主播关键词 等
  - **二次增强 / 衍生场景** → 写 `CONVERSATION (6)`：HTML 生成 / AI 纠正 — 表示"消耗来自对一条已存在 Conversation 的二次处理"
  - **特殊数据源** → `DATA_SCREENSHOT (3)` / `ANCHOR_KEYWORD (5)`：当不是对话场景而是数据 / 主播侧批量处理
- **话术助手（askType=6）实际写入：** `useSourceType = sourceType = 0`（视频）+ `assistantType = 6`
- **wiki §四是描述偏差**（不完整，只列了 CONVERSATION 这一类），建议修订成：`useSourceType` = "token 消耗的逻辑来源类别"，含两层语义

### 10 个调用点详表

| # | 文件:行 | useSourceType 值 | assistantType 值 | 场景 |
|---|---|---|---|---|
| 1 | `AnchorUrlLogicImpl.java:992` | `ANCHOR_KEYWORD` (5) | `ANCHOR_KEYWORD` (11) | 主播关键词获取（非对话） |
| 2 | `AnchorUrlLogicImpl.java:1053` | `VIDEO` (0) | `TRADE_RECOMMEND` (9) | 行业推荐 |
| 3 | `AnchorVideoLogicImpl.java:1459` | `VIDEO` (0) | `IMPORTANT_SCREENSHOT` (5) | 重要弹幕提示词 |
| 4 | `DataScreenshotLogicImpl.java:260` | `DATA_SCREENSHOT` (3) | `SCREENSHOT` (3) | 数据截图助手 |
| 5 | `TableStoreLogicImpl.java:215` | `VIDEO` (0) | `IMPORTANT_SCREENSHOT` (5) | 重要弹幕（图表数据驱动） |
| 6 | **`AiRelatedLogicImpl.java:606`** | **`askRequestBo.sourceType` (动态 0/1/2)** | **`askRequestBo.type` (动态 askType)** | **Web SSE 主路径 ask — 覆盖所有对话型助手 (askType 0-11，含话术助手 6)** |
| 7 | `AiRelatedLogicImpl.java:769` | `sourceType` (动态) | `CHECK_AI_CORRECT` (14) | 检查 AI 内容 |
| 8 | `ConversationBll.java:556` (replay-ai) | **`CONVERSATION` (6)** | `AI_HTML_PROMPT` (12) | HTML 生成（异步增强） |
| 9 | `ConversationBll.java:791` (replay-ai) | **`CONVERSATION` (6)** | `AI_CORRECT_CONTENT` (15) | AI 纠正（异步增强） |
| 10 | `AiApi.java:220` (replay-third) | `sourceType` (动态) | `NATURAL_ORIGINAL_TEXT` (7) / `OPTIMIZE_ORIGINAL_TEXT` (8) | 自然原文 / 优化原文 |

### 推荐 wiki 修订（在 G4 / wiki SSE 修订时一并做）

`ai_domain.md` §四 `useSourceType` 枚举表需修订为两层语义：(a) 对话场景写 sourceType (0/1/2/3/5)，(b) 异步增强写 CONVERSATION (6)。对应 assistantType：对话写实际助手 askType，异步增强写 12/14/15。具体修订文案在 G4 wiki 修订工单中起草。

---

## 附录 B — `videoSentenceImpl` 装配出的 Prompt 骨架（G2 解决）

**目标：** 厘清话术助手实际拼出的 prompt 长什么样。基于代码反推（非运行时 dump），值用 `<placeholder>` 占位。

**前置事实：** 话术助手 (`sourceType=0, askType=6`) 与运营 (`askType=0`) / 违规 (`askType=1`) **完全共用同一装配器** `videoSentenceImpl` —— 装配出的结构 100% 相同，**差异完全在系统提示词文本** (`tb_cue_words` 中按 cueType 配的不同 prompt 文本)。

### 最终 prompt 拼接公式

源：`AiRelatedLogicImpl.assemblePrompt` line 262

```
realContent =
    aiOptionConfigData          // (1) 截图 base64 + 数据看板段（可选）
  + anchorPrompt                // (2) 主播个人定制提示词（可选）
  + question                    // (3) sentenceMark.setAskQuestion 拼出的"基础数据 + 字段说明"
  + "\n\n问题："
  + askRequestBo.getRealContent() // (4) 系统提示词（来自 tb_cue_words.cueType=6 + 占位符替换）
  + extraBuildLastOutString     // (5) 重新提问场景追加（可选）
```

### 完整 prompt 样本（话术助手，questionContent=0 默认按段落分析）

```
[(1) aiOptionConfigData — 可选，仅当客户端开了截图或看板时拼接]
<截图 base64 数据：data:image/jpeg;base64,xxx... >
<看板数据：直播间在线人数曲线 / 商品销售曲线 / 流量来源... >

[(2) anchorPrompt — 可选，主播个人级 AI 提示词]
主播<anchorName>的个人话术风格：<主播在 tb_anchor_url.ai_prompt 字段配置的文本>

[(3) question — 由 sentenceMark.setAskQuestion 拼出，含两部分]
  ├─ (3a) sentenceMark.getTextParamsContent — 字段含义说明
  │      文本内的参数说明：
  │      本段自然时间：指的是转译成的文字段落开始时对应的视频播放时间。
  │      本段开始时间：指的是转译成的文字段落开始时对应的视频播放时间。
  │      本段结束时间：指的是转译成的文字段落结束时对应的视频播放时间。
  │      本段在线人数：指的是转译成的文字段落开始时对应的直播间在线人数。
  │      本段语速：指的是1分钟内说的字数。
  │      [按 aiAssistantDisplay 开关动态拼：弹幕条数 / 成交人数 / 互动率 / 成交率 / 销售额 / UV 价值]
  │      本场直播的视频时间是 <startStr> 到 <endStr>，共 <N> 秒的直播
  │
  └─ (3b) sentenceMark.getContent — minuteParagraphContent() 拼出的脚本段落
         以下是由 #{platform} 账号 [昵称为 <anchorName> 的] 直播间录屏成视频后转译成文字的直播全文脚本：

         本段自然时间：HH:mm:ss
         本段开始时间：HH:mm:ss
         本段结束时间：HH:mm:ss
         本段在线人数：<N>
         本段在线人数增加/减少：<M>人
         本段语速：<X>字/分钟
         [按 aiAssistantDisplay 开关动态拼：本段发弹幕条数 / 成交人数 / 互动率 / 成交率 / 销售额 / UV 价值]
         本段内容：<这一段的脚本文字>

         本段自然时间：HH:mm:ss
         ... (下一段 — 通常按视频 1-2 分钟一段，受 videoTimeOneList 时间窗截取)

         [总长度受 singleMaxNum = aiModelBo.wordsNum - 各 prompt 段长度估算 - 100 字 buffer 控制]

[(4) realContent — 系统提示词，来自 AiRelatedBll.getAiPromptWord2]
   即 tb_cue_words 中 cueType=6 (AI_SCRIPT_ASSISTANT) 的 prompt_content 字段（AES 加密前的明文）
   占位符 #{reason} 已用上一次对话的 realContent 锚定 4 字符前后匹配后填回原值
   <这里就是话术助手与运营/违规的唯一差异点：cueType 不同 → 提示词文本不同>

\n\n问题：
<用户实际输入的问题文本>

[(5) extraBuildLastOutString — 重新提问 (questionType=1) 场景，可选]
你给我的上次分析的回答结果是以下内容
<上一次 AI 回答（已 strip deepThinking）>
我对你上次分析的回答结果不满意的地方是：<optimizeText>
我这次的额外要求：<extraRequire>

<systemKv.again_ask_system_extra_prompt 配置的系统补充提示词>

请结合你上一次的回答结果和我不满意的地方，给我重新输出一份分析报告
```

### 运营 / 违规 / 话术三者 prompt 差异点（结论）

| 维度 | 运营助手 (askType=0) | 违规助手 (askType=1) | 话术助手 (askType=6) |
|---|---|---|---|
| 装配器 | `videoSentenceImpl` | `videoSentenceImpl` | `videoSentenceImpl` |
| `aiOptionConfigData` 段 | 完全相同 | 完全相同 | 完全相同 |
| `anchorPrompt` 段 | 完全相同 | 完全相同 | 完全相同 |
| `question` 段（脚本 + 字段说明） | 完全相同 | 完全相同 | 完全相同 |
| `realContent` 段（系统提示词） | `tb_cue_words` cueType=0 的文本 | `tb_cue_words` cueType=1 的文本 | `tb_cue_words` cueType=6 的文本 |
| 用户提问 | 用户输入 | 用户输入 | 用户输入 |

**核心结论：** 三种助手的"业务差异"100% 由 `tb_cue_words.cueType` 维度的提示词文本驱动，**与装配器无关**。要理解话术助手"做什么、怎么回答"，必须读 `tb_cue_words` 中 `cueType=6` 的 `prompt_content` 实际文本（G3 — 待开发查 DB 补）。

### 长度控制约束

- `singleMaxNum = aiModelBo.wordsNum - anchorPrompt.length() - askRequestBo.realContent.length() - aiOptionConfigData.length() - sentenceMark.getTextParamsContent().length() - 100`
- 设为 -1 时不截断
- 段落装配过程中按 `singleMaxNum` 动态截断 — 超长时**直接 break 不再追加新段落**，意味着长视频可能丢失尾段

### `aiAssistantDisplay` 开关（客户端可控）

源：`VideoSentenceImpl.minuteParagraphContent` line 107-129。客户端可传 `otherObj.aiAssistantDisplay` Map 覆盖默认开关：
- **默认开**：`startTime` / `natureTime` / `onlineNum` / `analysisChar`（开始结束时间 / 自然时间 / 在线人数 / 语速）
- **默认关**：`barrageNum` / `dealNum` / `interactionRate` / `dealRate` / `sales` / `uv`（弹幕 / 成交 / 互动率 / 成交率 / 销售额 / UV 价值）

不同助手可在同一视频上展示不同维度，但装配器对所有 askType 都是同一套逻辑。

