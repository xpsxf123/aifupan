<!-- module: ai -->
<!-- area: api -->
<!-- persistence: MongoDB + MySQL -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-ai/src/main/java/com/jiuyu/replay/ai/controller/ -->

# AI API — 接口契约

> replay-ai 模块对外接口。**全部托管在 replay-ai 模块**自己的 controller 包下（`com.jiuyu.replay.ai.controller`），不与 replay-api 共享。replay-api 下没有 `controller/ai/` 子包，AI 相关在 `replay-api` 的只有 `logic/ai/` 助手实现（被 `replay-words` 调用，无 endpoint）。

---

## 一、鉴权与通用约定

| 项 | 约定 |
|---|---|
| 鉴权 | 走全局拦截器解析当前用户 token；接口内通过 `UserFeign#getLocalUser()` 获取 `UserCacheVo`（id + activeTenantId） |
| `@CrossOrigin` | 所有 controller 类级别开启 |
| 返回包装 | 一律返回 `R<T>`（`com.jiuyu.replay.generic.vo.common.R`），禁止裸返业务对象 |
| 文档注解 | 类级 `@Tag`、方法级 `@Operation`；DTO 字段 `@Schema` |
| `@NoRepeatSubmit` | 目前 controller 未显式标注，依赖业务幂等（如诊断 cue 按 `(sourceId, sourceType, cueWordsId)` 唯一去重） |
| 接口路径前缀 | `replay/ai/*` |
| 流式 / SSE | **本 controller 包（replay-ai/.../controller）不使用 SSE**；AI 流式问答的服务端入口在 **`replay-api/.../controller/openapi/AiRelatedController#ask`**（`POST /openapi/ai/ask`），用 `CustomizeSseEmitter` 服务端代理：服务端持 API Key + 流式回写 + 自动落 MongoDB + 写计费。备用：C# 桌面 `/openapi/ai/assemblePrompt`（服务端拼词 + 客户端直连，通过 `AiTempToken`）、`/openapi/ai/chatStreamProxy`（SSE 代理但不落库）。详见 [research](../research/20260524__ai-script-assistant-business-flow.md) §3 / §6 |

---

## 二、ConversationController — 问答记录

类路径：`replay-ai/.../controller/ConversationController.java`，前缀 `replay/ai/conversation`

| # | Method | Path | 入参 | 返回 |
|---|---|---|---|---|
| C01 | POST | `/updateLikesStatus` | `UpdateLikesStatusBo` | `R<String>` |
| C02 | POST | `/saveConversationData` | `List<ConversationBo>` | `R<List<ConversationVo>>` |
| C03 | POST | `/conversationPage` | `ConversationListBo` | `R<ConversationPage<ConversationVo>>` |
| C04 | POST | `/isExist` | `ConversationBo` | `R<Boolean>` |
| C05 | GET  | `/exportByQaCode` | `qaCodes: String` (逗号分隔) | 文件流（单条 .md / 多条 .zip） |
| C06 | GET  | `/getById` | `id: String` | `R<ConversationVo>` |
| C07 | POST | `/updateHtmlStatus` | `UpdateHtmlStatusBo` | `R<String>` |
| C08 | GET  | `/serviceGenerateHtml` | `id: String` | `R<ConversationVo>` |
| C09 | GET  | `/htmlProGenerateParams` | `id: String` | `R<ConversationVo>` |
| C10 | GET  | `/getHtmlTempToken` | — | `R<AiTempTokenVo>` |
| C11 | GET  | `/getHtmlSignUploadUrl` | — | `R<SignUploadUrlVo>` |
| C12 | POST | `/getHtmlStatus` | `List<String>` ids | `R<List<ConversationVo>>` |
| C13 | POST | `/updateCorrectStatus` | `UpdateCorrectStatusBo` | `R<String>` |
| C14 | GET  | `/serviceCorrectAiContent` | `id: String` | `R<ConversationVo>` |
| C15 | GET  | `/correctProGenerateParams` | `id: String` | `R<ConversationVo>` |
| C16 | GET  | `/getCorrectTempToken` | — | `R<AiTempTokenVo>` |
| C17 | POST | `/getCorrectStatus` | `List<String>` ids | `R<List<ConversationVo>>` |

### C02 saveConversationData — 写入问答流水

请求体：`List<ConversationBo>`（`com.jiuyu.replay.generic.bo.ai.ConversationBo`）

字段映射 → `ConversationEntity` 关键字段：`sourceId / sourceType / userId / tenantId / askType / cueWordsId / cueWordsType / qaCode / contextId / type (Q|A) / content / realContent / completionId / lastConversationId / optimizeText / extraRequire / questionType`

副作用（`saveConversationDataPost`）：
- 取 `dictDataFeign.dictDataListByCode("diagnosis_cue_type")` 决定哪些 `askType` 算内容诊断
- 命中内容诊断 → `anchorVideoDetailFeign.updateHasDiagnosisReport(videoId, 1, null, null, null)`
- `askType == AI_CORRECT_CONTENT(15)` → 调用 `updateHasDiagnosisReport(videoId, null, null, 1, null)`（数据诊断标记）

### C03 conversationPage — 分页查询

请求体：`ConversationListBo { sourceId, sourceType, type(=askType), page, limit }`

副作用：被动检查 `htmlStatus = GENERATING` 且超过 `html_generate_expiration_time` 分钟的记录 → 批量改 FAIL；`aiCorrectStatus = CORRECTING` 同理。

排序：`createTime DESC`，再按 `type` 升序（A 在前 Q 在后保证一问一答顺序）。

### C05 exportByQaCode — 导出 Markdown

- 入参 `qaCodes` 逗号分隔
- 单 qaCode → 直接输出 `<序号、问题文本>.md`
- 多 qaCode → 打包 zip 流式输出（UTF-8，文件名 `export_yyyyMMddHHmmss.zip`）
- 每个 .md 含 5 个段落：模型说明 / 调用基础信息 / 入参 / 回答（含 `<div class='deepThinking'>` 区块说明）
- 必须能在 `tb_ai_token_use_record` 通过 `completionId` 反查到模型与 requestId

### C07 / C08 / C09 / C12 — HTML 生成链

- C09 `htmlProGenerateParams` → 同步返回 `ConversationVo`（含拼接好的 prompt content + GENERATING 状态），客户端拿到后**自行**直连模型
- C07 `updateHtmlStatus` → 客户端模型调用完成后回传结果状态 + `htmlSavePath`
- C08 `serviceGenerateHtml` → 服务器端走全套：返回会话信息（带 content=拼好的 prompt，但 setContent(null) 给客户端避免回显），异步 `generateHtmlExecutor` 跑模型 → OSS 上传 → 写回状态
- C12 `getHtmlStatus` → 只查 `id / htmlType / htmlStatus / htmlCreateDate / htmlSavePath / htmlCreateError` 5 个字段，并主动 setHtmlDomainName

### C10 / C16 — TempToken

返回 `AiTempTokenVo`（含 OSS / 模型直连临时凭证），由 `AiFeign#getAnalysisTempToken(modelCode)` 在 replay-third 生成。

### C11 — getHtmlSignUploadUrl

固定生成 `ai-html` 前缀、`.html` 后缀、`text/html` content-type 的 OSS 预签名 PUT URL。

### C13 / C14 / C15 / C17 — AI 纠正链

与 HTML 同构（pro → server async / client client）；客户端纠正完成回传 content 时，服务端**自动合并原 deepThinking 区块**到 content 头部。

---

## 三、DiagnosisController — AI 诊断

类路径：`replay-ai/.../controller/DiagnosisController.java`，前缀 `replay/ai/diagnosis`

| # | Method | Path | 入参 | 返回 |
|---|---|---|---|---|
| D01 | GET  | `/getHandelSuccessDiagnosis` | `sourceId / sourceType (alias videoId) / diagnosisType?` | `R<List<DiagnosisCueInfoVo>>` |
| D02 | GET  | `/handleDiagnosisByUser` | — | `R<List<DiagnosisCueInfoVo>>` |
| D03 | POST | `/saveDiagnosis` | `SaveDiagnosisBo { sourceId, cueWordsIds }` | `R<List<DiagnosisCueInfoVo>>` |
| D04 | GET  | `/isGenerateDiagnosisFile` | `sourceId, diagnosisType?` | `R<String>` "yes"/"no" |
| D05 | GET  | `/getAutoDiagnosisQuestions` | `videoId` | `R<List<DiagnosisCueInfoVo>>` |
| D06 | POST | `/conversationByCueWordsIds` | `conversationByCueWordsIdsBo` | `R<List<ConversationVo>>` |
| D07 | POST | `/getDiagnosisSignUploadUrl` | `DiagnosisSignUploadUrlBo` | `R<SignUploadUrlVo>` |
| D08 | GET  | `/getDiagnosisDownloadUrl` | `videoId, sourceType?, uploadType?` | `R<String>` 签名下载 URL |
| D09 | POST | `/updateDiagnosisCueStatus` | `DiagnosisCueBo` | `R<String>` |
| D10 | GET  | `/aiModelBySourceIdAndType` | `sourceId, sourceType, diagnosisType?` + header `webVersion?` | `R<AiModelInfoVo>` |
| D11 | POST | `/saveDiagnosisCue` | `SaveDiagnosisCueBo` | `R<String>` |
| D12 | GET  | `/listDiagnosis` | `sourceId?, sourceType?, tradeId?` + header `webVersion?` | `R<AiDiagnosisCueVo>` |
| D13 | GET  | `/listDiagnosisModel` | header `webVersion?` | `R<List<AiModelInfoVo>>` |
| D14 | POST | `/updateDiagnosisModel` | `DiagnosisModelBo` | `R<String>` |
| D15 | POST | `/updateReadStatus` | `UpdateReadStatusBo { ids, isRead? }` | `R<String>` |
| D16 | GET  | `/listUnreadDataDiagnosis` | — | `R<List<UnreadDiagnosisReportVo>>` |
| D17 | GET  | `/getDataDiagnosisStatus` | `videoId` | `R<DataDiagnosisStatusVo>` |

### D05 getAutoDiagnosisQuestions

`@Transactional` 写方法。复合工作流：

1. 主播 `isAutoDiagnosis == 1` → 复制主播默认 cue 到本视频 + 设置 model (CONTENT_DIAGNOSIS)
2. 主播 `isDataDiagnosis == 1` 且 `diagnosisGenerateNum > 已生成` 且看板存在 → 取 `cue_type == 13` 首条 → 挂 DATA_DIAGNOSIS cue + 设置 model + Redis 写入计数 key

### D07 / D08 上传 / 下载诊断报告

- 上传：服务端只生成预签名 URL，客户端直传 OSS，Key = `${prefix}/yyyy/MM/dd/${videoId}.pdf`
- 下载：服务端校验 `tb_anchor_video_detail.hasDiagnosisReport == 1` 后给签名 URL，文件名取 `dataDiagnosisOssName` 或 `diagnosisOssName`
- `uploadType` 1=内容诊断（prefix `diagnosisFile`）/ 2=数据诊断（prefix `dataDiagnosisFile`）
- `sourceType=1`（文件）不支持下载，抛 `BusinessException("当前类型每诊断报告")`（原文 typo 保留）

### D09 updateDiagnosisCueStatus

更新逻辑：
- `qaStatus = 1` → 同时写 `qaHandleTime = now`
- `qaStatus = 3` → 保留 `errorContent`
- `qaStatus = 2` 且为数据诊断 → 触发配额扣减 `anchorUrlFeign.minusDataDiagnosisGenerateNum(secUid, videoId, …)`

### D10 / D13 模型版本协商

- D10：先查 `tb_diagnosis_model` 用户偏好；没有则取 `diagnosis_ai_model_default` 系统默认；按 `webVersion` 兜底 `analysis-doubao-deep-seek-R1`；返回前 `setApiKey(null) / setEndpointId(null)` 脱敏
- D13：调 `aiModelFeign.listDiagnosisModel()`，按字典 `ai_model_min_web_version` 过滤客户端版本不达标的模型

### D11 saveDiagnosisCue

`@Transactional` 写方法（在 RseImpl 上）。同步逻辑：
- 先 saveOrUpdate `tb_diagnosis_model`（按 `(sourceId, sourceType, userId, tenantId)` 唯一）
- 再以 `selectCueWordsIdsList` 为准 diff：超出的 ids 软删（实际 `removeByIds`），保留的更新，新增的批量插入
- 视频维度新建 cue 时 `qaStatus = WAITING`；视频维度更新已存在 cue 保留原 `qaStatus`（已成功的不重跑）；主播维度新建 cue 时也置 WAITING

### D12 listDiagnosis

复合视图：
- 取字典 `diagnosis_cue_type` 决定要展示几个 cueType 分组
- 每个分组按 `cueWordsFeign.pageCueWords` 拉提示词候选
- 当前用户的诊断 cue 按 `cueWordsId` 分组，标注 `anchorSelect / videoSelect / qaStatus / errorContent`
- 视频维度未选但已存在问答（`existsCueWords`）→ 自动标 `qaStatus=2`（已生成）
- 顶部带 `modelId / modelName`（用户偏好或默认）

### D17 getDataDiagnosisStatus

数据诊断快速状态接口：取该视频最新一条 DATA_DIAGNOSIS cue，若状态 SUCCESS 则附带 conversation 中最新一条 Q 类型 content（用于直接展示报告正文）。

---

## 四、CustPromptController — 用户自定义提示词

类路径：`replay-ai/.../controller/CustPromptController.java`，前缀 `replay/ai/custPrompt`

| # | Method | Path | 入参 | 返回 |
|---|---|---|---|---|
| P01 | POST | `/privateList` | `CustPromptListBo` | `R<PageUtils<CustPromptVo>>` |
| P02 | GET  | `/info` | `id: Long` | `R<CustPromptVo>` |
| P03 | POST | `/save` | `CustPromptBo` (`@Validated(Insert.class)`) | `R<CustPromptVo>` |
| P04 | POST | `/update` | `CustPromptBo` (`@Validated(Update.class)`) | `R<String>` |
| P05 | GET  | `/delete` | `id: Long` (`@NotNull`) | `R<String>` |

### 校验（CustPromptBo）

| 字段 | 校验 |
|---|---|
| `id` | `@NotNull` (Update only) |
| `promptTitle` | `@NotNull` + `@Length(max = 10)` |
| `promptContent` | `@NotNull` + 长度 ≤ `cust_prompt_content_max_count` (systemKv, 默认 30000) |
| `promptSort` | `@Max(99) @Min(0)` |

### 写入前置门槛
- 新增时 P03：`OrderFeign.currentOrderByUserId(userId).level >= 20` → 否则 `BusinessException("请升级到企业版及以上使用")`
- 数量上限 `user_cust_prompt_num_max` (systemKv)

---

## 五、ShareLinkRecordController — 分享链接

类路径：`replay-ai/.../controller/ShareLinkRecordController.java`，前缀 `replay/ai/share`

| # | Method | Path | 入参 | 返回 |
|---|---|---|---|---|
| S01 | POST | `/list` | `ShareLinkRecordListBo` | `R<PageUtils<ShareLinkRecordListVo>>` |
| S02 | GET  | `/info` | `id: Long` | `R<ShareLinkRecordInfoVo>` |
| S03 | POST | `/update` | `ShareLinkRecordBo` | `R<String>` |
| S04 | GET  | `/delete` | `id: Long` | `R<String>` |
| S05 | POST | `/save` | `ShareLinkRecordSaveBo { ids, sourceId, sourceType, askType? }` | `R<ShareSaveVo>` |
| S06 | GET  | `/getDataTxtPutUrl` | — | `R<SignUploadUrlVo>` |

### S02 info — 含会话拼装

读 `tb_share_link_record` 后，按 `codes` (JSON List<String>) 反序列化为会话 id 列表，再调 `conversationProducer.listByIds(list)` 从 MongoDB 取会话集合，给每条 set `htmlDomainName`。

MongoDB 异常分支：`MongoSocketOpenException` / `MongoSocketReadTimeoutException` 包装为 "MongoDB连接 Timed out"，其他异常包装为 "获取聊天记录失败"。

### S05 save 返回 ShareSaveVo

包含 `host`（取自 systemKv `client_front_domain_name`），客户端拼接 `host + path + id` 得到完整分享链接。

---

## 六、AiController — AI 提示词查询

类路径：`replay-ai/.../controller/AiController.java`，前缀 `replay/ai/aiRelated`

| # | Method | Path | 入参 | 返回 |
|---|---|---|---|---|
| A01 | GET  | `/getAiPromptWord` | `cueWordsId: Long, cueWordsType: Integer?` | `R<String>`（AES 加密） |
| A02 | GET  | `/getAiPromptWord2` | `cueWordsId, cueWordsType, lastConversationId?` | `R<AiPromptWordVo>` |

`AiPromptWordVo`:
- `prompt: String` —— AES 加密的提示词文本
- `reason: String` —— 占位符 `#{reason}` 锚定上一次对话提取的内容
- `optimizeActions: Integer` —— 上次对话包含 `的优化目的：` / `的优化动作：` 则置 1

---

## 七、错误码与异常

本模块未定义独有错误码，统一使用：

| 异常 | 触发场景 |
|---|---|
| `BusinessException`（com.jiuyu.replay.generic.vo.common.exception） | "提示词类型未知" / "请升级到企业版及以上使用" / "html ai 模型未配置" / "没有找到对应的会话" / "正在纠错中，请勿重复操作" 等 |
| `RRException`（com.jiuyu.replay.common.utils） | `RRException.isNotEmpty(obj, msg)` / `RRException.create(msg)` 通用断言失败 |
| `RuntimeException` | CustPromptRseImpl 中 "自定义提示词数量已达到上限"（**非 BusinessException，疑似漏改**） |

全局返回包装 `R.error(msg)`（HTTP 200，`code != 0`），见 `DiagnosisController#getHandelSuccessDiagnosis` 中"缺少来源id"分支。

---

## 八、对内 Feign（被其他模块调用）

实现位于 `replay-ai/.../api/`，接口定义在 `replay-generic/.../feign/ai/`：

| Feign | 方法 | 实现 |
|---|---|---|
| `ConversationFeign` | `saveConversationData(List<ConversationBo>)` | `ConversationApi#saveConversationData` → `ConversationRse#saveAll` |
| `DiagnosisCueFeign` | `listBySourceIds(sourceIds, sourceType, userId, tenantId)` | `DiagnosisCueApi#listBySourceIds` → `DiagnosisCueBll#listBySourceIds` |

注意：`ConversationApi#saveConversationData` 直接走 `conversationRse.saveAll`，**不走 `ConversationBll#saveConversationData` 的副作用链**（即跨模块调用不会自动更新 `hasDiagnosisReport`）。
