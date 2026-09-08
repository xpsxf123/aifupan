<!-- module: ai -->
<!-- area: domain -->
<!-- persistence: MongoDB + MySQL -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-ai/, replay-api/src/main/java/com/jiuyu/replay/api/logic/ai/ -->

# AI Domain — 业务概念与词汇表

> replay-ai 模块核心业务概念定义。Agent 在 Explorer/Propose 阶段必须使用此术语表，避免领域漂移。
>
> 持久化：**问答类**走 MongoDB（`replay_ai_conversation` / `replay_ai_conversation_html`），**诊断配置 / 用户自定义提示词 / 分享记录**走 MySQL（`tb_diagnosis_cue` / `tb_diagnosis_model` / `tb_cust_prompt` / `tb_share_link_record`）。

---

## 一、模块定位

replay-ai 承担直播复盘平台的 **AI 助手** 能力：

1. **AI 问答（Conversation）**：用户对视频 / 文件 / 对比分析三类来源发起 AI 提问，记录 Q/A 双方对话流水（MongoDB）。
2. **AI 诊断（Diagnosis）**：基于主播或视频维度，按预设提示词批量生成诊断报告（**内容诊断** + **数据诊断**），结果回填到对应视频明细的 `hasDiagnosisReport` 标记位。
3. **用户自定义提示词（CustPrompt）**：企业版以上用户可维护私有提示词，与系统提示词（`tb_cue_words`）并存。
4. **AI 结果增强**：
   - **HTML 可视化**：将 AI 回答二次调用模型转成 HTML 文件，上传 OSS（图表化输出）。
   - **AI 纠正（Correct）**：客户端或服务端再次调用模型，对原回答做格式 / 内容纠正。
5. **链接分享（ShareLinkRecord）**：把一组问答 `qaCode` 打包成可分享链接，带失效时间。

---

## 二、核心概念

| 概念                                  | 定义                                                                                    | 关联概念                       | 状态机                                |
|-------------------------------------|---------------------------------------------------------------------------------------|----------------------------|------------------------------------|
| **Conversation (AI 问答记录)**          | 一次完整 Q&A 在 MongoDB 中按"提问 + 回答"两条文档存储，通过 `qaCode` 串成对子                                  | sourceId, askType, AskType | `htmlStatus` / `aiCorrectStatus`   |
| **QA Code**                         | 一问一答两条记录共享同一 `qaCode`，用于聚合导出 / 分享 / 二次处理                                              | Conversation               | —                                  |
| **ContextId (会话 ID)**               | 同一聊天上下文（多轮对话）的串联标识                                                                    | Conversation               | —                                  |
| **CompletionId**                    | 一次 chat completion 接口调用的唯一标识，关联 `tb_ai_token_use_record`                              | Conversation, AiTokenUseRecord | —                                  |
| **ConversationHtml**                | Conversation 文档的 HTML 内容副表（MongoDB），按 `conversationId` 关联，存放生成的 HTML 字符串              | Conversation               | `isDelected`                       |
| **DiagnosisCue (诊断提示词配置)**          | 主播或视频维度选定的诊断提示词，按 `(sourceId, sourceType, cueWordsId, userId, tenantId)` 唯一            | CueWords, Conversation     | `qaStatus` 0/1/2/3                 |
| **DiagnosisModel (诊断模型配置)**         | 主播或视频维度选用的 AI 模型，按 `(sourceId, sourceType, userId, tenantId, diagnosisType)` 唯一       | DiagnosisCue, AiModel      | —                                  |
| **DiagnosisType (诊断类型)**            | 0：内容诊断（基于文案）/ 1：数据诊断（基于看板 + 截图）                                                       | DiagnosisCue, AiCueButton  | `AiEnums.diagnosisType`            |
| **DiagnosisSourceType (诊断来源类型)**    | 诊断 cue 挂载位置：0 主播（secUid）/ 1 视频（videoId）                                               | DiagnosisCue, AnchorUrl    | `AiEnums.diagnosisSourceType`      |
| **AutoDiagnosis (自动诊断)**            | 视频录制完成后，自动从主播默认 cue 复制到本视频并触发提问（受主播 `isAutoDiagnosis` / `isDataDiagnosis` 开关控制）       | AnchorUrlUser, DiagnosisCue | —                                  |
| **CustPrompt (用户自定义提示词)**           | 企业版以上用户私有提示词，标题 ≤ 10 字，内容 ≤ 30000 字（受 systemKv `cust_prompt_content_max_count` 控制）   | User                       | —                                  |
| **AskType (助手类型)**                  | **16 种 askType (code 0-15)**：对话型 0-11（运营 / 违规 / 弹幕 / 截图 / 看板 / 重要弹幕 / AI 话术 / 自然原文 / 优化原文 / 行业推荐 / 提取文案 / 主播关键词）+ 异步增强 12 / 14 / 15 + 诊断流 13                          | Conversation, CueWords     | `AiEnums.askType`                  |
| **CueWordsType (提示词来源类型)**          | 0 系统（`tb_cue_words`） / 1 用户（`tb_cust_prompt`）                                         | CueWords, CustPrompt       | `AiEnums.cueWordsType`             |
| **AiHtml (AI 结果转 HTML)**            | 把 Q 类型回答二次喂给 HTML 模型，生成可视化 HTML 文件并存 OSS                                              | Conversation, AiModel      | `AiEnums.htmlStatus`               |
| **HtmlType (HTML 生成方式)**            | 0 服务器生成 / 1 客户端生成                                                                     | AiHtml                     | `AiEnums.htmlType`                 |
| **AiCorrect (AI 纠正)**               | 对 Q 类型回答再调一次模型做内容纠正，纠正后内容覆盖原 `content`，自动保留 deepThinking 区块                          | Conversation               | `AiEnums.correctStatus`            |
| **CorrectType (纠正来源类型)**            | 0 服务器纠正 / 1 客户端纠正                                                                     | AiCorrect                  | `AiEnums.correctType`              |
| **DeepThinking (深度思考块)**            | 模型回答中包裹在 `<div class='deepThinking'>...</div>` 的思考内容，HTML 与纠正都要先 strip 再处理            | Conversation               | —                                  |
| **ShareLinkRecord (分享链接记录)**        | 用户把若干 `qaCode` 列表打包为 JSON 存入数据库，对外作为分享链接载体，可设过期天数                                    | Conversation, QA Code      | `urlStatus` 0 正常 / 1 已失效            |
| **AiTempToken (AI 临时 Token)**       | 客户端走前端直连模型时需要的临时凭证，由 replay-third 通过 `AiFeign#getAnalysisTempToken` 下发                | AiModel                    | —                                  |
| **TokenUseRecord (Token 消耗记录)**     | 每次 `chatCompletion` 都通过 `AiTokenUseRecordFeign` 写一条计费记录                               | Conversation               | —                                  |

---

## 三、状态机定义

### `qaStatus` — 诊断报告状态（DiagnosisCueEntity）
| 值 | 状态 | 说明 |
|---|---|---|
| 0 | WAITING | 待生成 |
| 1 | GENERATING | 生成中 |
| 2 | SUCCESS | 生成成功 |
| 3 | FAIL | 生成失败 |

转换：`saveDiagnosisCue / saveDiagnosis` → WAITING；客户端开始问答 → GENERATING（记录 `qaHandleTime`）；问答完成 → SUCCESS；客户端关闭 / 超时（`diagnosis_timeout_report` 分钟）→ FAIL。

### `htmlStatus` — HTML 生成状态（ConversationEntity）
| 值 | 状态 | 说明 |
|---|---|---|
| 0 | WAITING | 待生成 |
| 1 | GENERATING | 生成中 |
| 2 | SUCCESS | 生成成功 |
| 3 | FAIL | 生成失败 |

超时阈值由 `html_generate_expiration_time` (systemKv, 默认 20 分钟) 控制，查询列表时被动检查并改 FAIL。

### `aiCorrectStatus` — AI 纠正状态（ConversationEntity）
| 值 | 状态 | 说明 |
|---|---|---|
| 0 | NORMAL | 正常 |
| 1 | CORRECTING | 纠错中 |
| 2 | CORRECTED | 纠错完成 |
| 3 | FAIL | 纠错失败 |

超时阈值由 `correct_generate_expiration_time` (systemKv, 默认 20 分钟) 控制。

### `urlStatus` — 分享链接状态（ShareLinkRecordEntity）
| 值 | 状态 | 说明 |
|---|---|---|
| 0 | 正常 | 默认值 |
| 1 | 已失效 | 后台定时任务推过期天数后置位 |

---

## 四、枚举定义（`com.jiuyu.replay.common.constant.AiEnums`）

### askType — 助手类型（16 种，code 0-15）

**提示词存储分布**：11 种走 `tb_cue_words` / 3 种走 systemKv / 1 种代码硬编码 / 1 种死代码。详见 [research/20260524__cue-words-askType-inventory.md](../research/20260524__cue-words-askType-inventory.md) §2。

| code | 枚举名 | 说明 | 提示词存储 |
|---|---|---|---|
| 0 | OPERATION | 运营助手 | tb_cue_words (1071 条 / 49 行业，**最大宗**) |
| 1 | VIOLATION | 违规助手 | tb_cue_words (34 条) |
| 2 | BARRAGE | 弹幕助手 | tb_cue_words (72 条) |
| 3 | SCREENSHOT | 截图助手 | tb_cue_words (2 条) |
| 4 | BOARD | 看板助手 | tb_cue_words (5 条) |
| 5 | IMPORTANT_SCREENSHOT | 重要弹幕提示词 | tb_cue_words (3 条) + systemKv `important_barrage_ai_code` |
| 6 | AI_SCRIPT_ASSISTANT | AI 话术助手 | tb_cue_words (41 条 / 4 行业) |
| 7 | NATURAL_ORIGINAL_TEXT | 自然原文提示词 | tb_cue_words (3 条) |
| 8 | OPTIMIZE_ORIGINAL_TEXT | 优化原文提示词 | tb_cue_words (2 条) |
| 9 | TRADE_RECOMMEND | 行业推荐 | systemKv `suggest_trade_ai_model`（仅模型 code；prompt 由 `anchorVideoBll.getIndustryPrompt` 动态拼装） |
| 10 | EXTRACT_VIDEO | 提取文案优化 | **死代码** — `AiEnums` 定义但全代码 0 引用 |
| 11 | ANCHOR_KEYWORD | 主播关键词获取 | **代码硬编码** — `AnchorUrlLogicImpl.java:978` = `"你是一个顶尖关键词内容提炼专家"` |
| 12 | AI_HTML_PROMPT | AI 结果转 HTML | tb_cue_words (1 条) + systemKv `html_ai_model_default` / `html_system_extra_prompt` |
| 13 | DATA_DIAGNOSIS | 数据诊断提示词 | tb_cue_words (80 条) + systemKv `diagnosis_ai_model_default` |
| 14 | CHECK_AI_CORRECT | 检查 AI 内容 | systemKv `check_ai_content_prompt` + `check_ai_content_model` + `check_ai_content_prompt_confirm` |
| 15 | AI_CORRECT_CONTENT | AI 纠正内容 | systemKv `correct_ai_content_prompt` + `correct_ai_content_model` |

### useSourceType — Token 使用来源（**语义分两层**）

写入 `tb_ai_token_use_record.use_source_type` 时按**调用场景**分两层规则（grep 验证：见 [research 附录 A](../research/20260524__ai-script-assistant-business-flow.md)）：

**第一层：直接对话场景 — 按 `sourceType` 记录**（用户主动触发的助手对话）
| code | 枚举名 | 说明 | 写入代码点 |
|---|---|---|---|
| 0 | VIDEO | 视频对话（含话术助手 askType=6 等所有视频对话型助手） | `AiRelatedLogicImpl.java:606` (Web SSE 主路径) |
| 1 | FILE | 文件对话 | 同上（动态） |
| 2 | SYNC_ANALYSIS | 对比分析对话 | 同上（动态） |
| 3 | DATA_SCREENSHOT | 数据截图助手专用 | `DataScreenshotLogicImpl.java:260` |
| 4 | EXTRACT_VIDEO | 提取文案视频 | （字典预留） |
| 5 | ANCHOR_KEYWORD | 主播关键词获取 | `AnchorUrlLogicImpl.java:992` |

**第二层：二次增强场景 — 写 CONVERSATION**（系统对已存在 conversation 做异步增强）
| code | 枚举名 | 说明 | 写入代码点 |
|---|---|---|---|
| 6 | CONVERSATION | HTML 生成 + AI 纠正（对一条已存在 `replay_ai_conversation` 文档的二次处理） | `ConversationBll.java:556` (HTML) / `:791` (纠正) |

**对应 `assistantType` 字段（同表）：** 对话场景写实际助手 askType；异步增强场景写 12 (AI_HTML_PROMPT) / 15 (AI_CORRECT_CONTENT) / 14 (CHECK_AI_CORRECT)。

### diagnosisType — 诊断类型
| code | 枚举名 | 说明 |
|---|---|---|
| 0 | CONTENT_DIAGNOSIS | 内容诊断 |
| 1 | DATA_DIAGNOSIS | 数据诊断 |

### diagnosisSourceType — 诊断来源类型
| code | 枚举名 | 说明 |
|---|---|---|
| 0 | ANCHOR | 主播（sourceId = secUid） |
| 1 | VIDEO | 视频（sourceId = videoId） |

### cueWordsType — 提示词来源
| code | 枚举名 | 说明 |
|---|---|---|
| 0 | SYSTEM | 系统提示词（`tb_cue_words`） |
| 1 | USER | 用户自定义（`tb_cust_prompt`） |

### htmlStatus / htmlType / correctStatus / correctType / readStatus / assetCreationType
见 §三 / 上表，code 0/1/2/3 语义一致。

---

## 五、Conversation 文档关键字段

每次 AI 问答会在 MongoDB 写两条 `ConversationEntity`：

- `type = "Q"`：AI 的**回答**（业务上对应"提问触发后产出的答案"，命名反人类，按代码现状记录）
- `type = "A"`：用户的**提问**
- `qaCode`：一问一答共享同一 code，导出 / 分享 / 看记录都按 `qaCode` 聚合
- `completionId`：仅 Q 记录有效，对应 `tb_ai_token_use_record.requestId`，用于追溯模型与入参
- `realContent`：A 类型保存原始问题（含 `#{reason}` / `#{optimize}` 等占位符已替换的内容），列表查询时主动 set null 不下发

---

## 六、核心业务规则

### 1. 提示词来源回退
`AiRelatedBll#getAiPromptWord` 处理三种来源：
1. `cueWordsType == null` → 先查系统 `CueWordsFeign`，找不到回退查用户 `tb_cust_prompt`
2. `cueWordsType == 0`（SYSTEM）→ 仅查系统
3. `cueWordsType == 1`（USER）→ 仅查 `tb_cust_prompt`

返回结果支持 AES 加密（`AESUtil.encrypt`），客户端解密后才能拿到原文。

> **重要 1**：`tb_cue_words` 表 schema 上**支持 `tenant_id ≠ 0` 的租户私有 prompt**（实际数据中已存在 45 条 — 见 [research 伴生文档](../research/20260524__ai-script-assistant-cue-words-sample.md) §4 + [askType-inventory.md](../research/20260524__cue-words-askType-inventory.md) §5）。`cueWordsType == 0` SYSTEM 分支的实际 WHERE 子句是否过滤 `tenant_id IN (0, currentTenantId)` 待 `CueWordsBll` 代码确认（G3.1 衍生问题）。"系统在 `tb_cue_words` / 用户在 `tb_cust_prompt` 分表"不是绝对边界。
>
> **重要 2**：`tb_cue_words` 不是唯一存储 — 16 种 askType 中 **5 种走 systemKv / 硬编码 / 死代码**：askType=9 (`suggest_trade_ai_model` + 动态拼装) / =10 (死代码) / =11 (硬编码 `AnchorUrlLogicImpl.java:978`) / =14 (`check_ai_content_prompt`) / =15 (`correct_ai_content_prompt`)。完整存储分布见 §四 askType 表。

### 2. 提示词占位符替换
`getAiPromptWord2` 解析 `#{reason}` 占位符：取占位符前后各 4 字符作为"锚点"，反向匹配上一条对话 `lastConversation.realContent`，把原 reason 抽出来填入。

支持检测 `的优化目的：` / `的优化动作：` 子串 → 置 `optimizeActions = 1`，下游知道这是一个优化任务。

### 3. 自动诊断（AutoDiagnosis）触发链
`DiagnosisCueBll#getAutoDiagnosisQuestions(videoId)`:

1. 主播 `isAutoDiagnosis == 1` → 复制主播的 CONTENT_DIAGNOSIS cue 到本视频，写入 `tb_diagnosis_model` + `tb_diagnosis_cue`，返回提示词列表给客户端
2. 主播 `isDataDiagnosis == 1` 且 `diagnosisGenerateNum > 已生成次数` 且看板存在 → 取 `tb_cue_words` 中 `cueType = DATA_DIAGNOSIS(13)` 的首条提示词，挂到本视频作为 DATA_DIAGNOSIS cue，并写入 Redis 计数器（`GenerateHtmlRedisOPerate`）防止重复消费配额

### 4. 诊断完成回传 `hasDiagnosisReport`
`ConversationBll#saveConversationDataPost`:

- 仅处理 `sourceType=0`（视频）、`cueWordsType=SYSTEM`、`cueWords.tenantId == 0L`（即官方定制提示词）的记录
- 按 `askType` 分发：
  - 字典 `diagnosis_cue_type` 命中 → 调 `anchorVideoDetailFeign.updateHasDiagnosisReport(videoId, 1, …)` 把内容诊断标记设为 1
  - `askType == AI_CORRECT_CONTENT(15)` 兼容旧版数据诊断 → 把数据诊断标记设为 1（注：代码注释为"DATA_DIAGNOSIS"，实际比对的是 `AI_CORRECT_CONTENT`，疑似遗留 bug，按代码现状记录）

### 5. HTML 生成（异步）
`ConversationBll#serviceGenerateHtml(id)`:

1. `proGenerateHtml` 同步：读会话 → 仅 type=Q 且 content 非空可生成 → 查行业 → 取 `tb_cue_words` 中 HTML 专用提示词 → strip deepThinking → 拼接成模型入参 → 改状态为 GENERATING（MongoDB updateFirst）
2. 异步线程 `generateHtmlExecutor.execute(() -> serviceGenerateHtml(conversationVo))`：取 `html_ai_model_default` 模型 → 调 `AiFeign#chatCompletion` → 写 `tb_ai_token_use_record`（useSourceType=CONVERSATION, askType=AI_HTML_PROMPT(12)）→ HTML 内容上传 OSS `ai-html/{snowflakeId}.html` → 回写 `htmlSavePath` + `htmlStatus=SUCCESS`；异常则 `htmlStatus=FAIL` + `htmlCreateError`

### 6. AI 纠正（异步）
`ConversationBll#serviceCorrectAiContent(id)`:

1. `proCorrectAiContent` 同步：读会话 → 仅 type=Q 可纠 → strip deepThinking → 取 `correct_ai_content_prompt` 拼提示词 → 改状态为 CORRECTING
2. 异步线程：取 `correct_ai_content_model` → 调 AI → 写 token 记录（askType=AI_CORRECT_CONTENT(15)）→ 合并原始 deepThinking + 纠正后内容回写 → CORRECTED；异常则 FAIL

### 7. 跨模块通信
- replay-ai → **replay-third**：`AiFeign` / `AiModelFeign` / `AiTokenUseRecordFeign` / `PropertiesFeign`
- replay-ai → **replay-words**：`CueWordsFeign` / `SensitiveWordsFeign` / `AnchorUrlFeign` / `AnchorVideoFeign` / `AnchorVideoDetailFeign` / `VideoDataViewingFeign`
- replay-ai → **replay-power**：`UserFeign`
- replay-ai → **replay-order**：`OrderFeign` / `AiTokenUseRecordFeign`
- replay-ai → **replay-system**：`DictDataFeign`（字典 `diagnosis_cue_type` / `ai_model_min_web_version`）
- replay-words → **replay-ai**：通过 `ConversationFeign#saveConversationData` / `DiagnosisCueFeign#listBySourceIds` 反向调用

### 8. 模型版本与客户端版本协商
`DiagnosisCueBll#shouldShowModel` / `#resolveDefaultModelCode`:

- 字典 `ai_model_min_web_version` 维护 `label = modelCode` / `value = 最低 webVersion`
- 列表过滤：客户端版本 < 最低版本则模型不显示
- 默认模型：若 `diagnosis_ai_model_default` 配置的模型对当前客户端版本不达标，兜底返回 `analysis-doubao-deep-seek-R1`

### 9. CustPrompt 配额
- `user_cust_prompt_num_max`（systemKv）→ 单用户上限条数，超出抛 `RuntimeException("自定义提示词数量已达到上限")`
- `cust_prompt_content_max_count`（systemKv, 默认 30000）→ 单条 `promptContent` 字符数上限
- 写入前 `OrderFeign#currentOrderByUserId` 校验 `order.level >= 20`（企业版及以上），否则抛 `BusinessException("请升级到企业版及以上使用")`

### 10. 分享链接过期
`ShareLinkRecordBll#updateUrlExpire(value)`：定时任务（外部调度）传入"过期天数"，扫描 `shareTime < now - days` 且 `urlStatus = 0` 的记录批量改 `urlStatus = 1` + `expireTime = now`。

---

## 七、租户隔离 (Hard Constraint)

| 文档 / 表 | 隔离字段 | 备注 |
|---|---|---|
| `replay_ai_conversation` | `userId + tenantId` | 复合索引 `sourceId_sourceType_userId_tenantId_askType_index` |
| `tb_diagnosis_cue` | `userId + tenantId` | 每条诊断 cue 强制带租户 |
| `tb_diagnosis_model` | `userId + tenantId` | 模型选择按用户隔离 |
| `tb_cust_prompt` | `userId` | 仅按用户隔离（私人提示词） |
| `tb_share_link_record` | `userId + tenantId` | 分享按当前活动租户 |

所有列表查询通过 `LambdaQueryWrapper.eq(*::getTenantId, user.getActiveTenantId())` 强制过滤；`UserFeign#getLocalUser` 取当前用户的 `activeTenantId`。

---

## 八、角色与权限

| 角色 | 能力 |
|---|---|
| 终端用户 | 调用 `replay/ai/**` 接口；只能读写自己 `userId + tenantId` 范围内的会话、诊断、自定义提示词 |
| 企业版及以上用户 | 解锁 `CustPrompt` 写入（`OrderFeign.currentOrder().level >= 20`） |
| 内部模块（replay-words） | 通过 `ConversationFeign / DiagnosisCueFeign` 反向写入诊断结果与查询诊断状态 |
| 系统管理员 | 维护 `tb_cue_words` 系统提示词、`systemKv` 配置项、字典项 `diagnosis_cue_type` / `ai_model_min_web_version` |
