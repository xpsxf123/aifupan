<!-- module: ai -->
<!-- area: data -->
<!-- persistence: MongoDB + MySQL -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-ai/src/main/java/com/jiuyu/replay/ai/entity/, replay-ai/src/main/java/com/jiuyu/replay/ai/repository/ -->

# AI Data — 数据模型

> replay-ai 模块完整持久化结构：**2 个 MongoDB Collection** + **4 张 MySQL 表**。
>
> MongoDB 用于：长文本会话流水（不可预测字段长度 + 频繁全文读写）
>
> MySQL 用于：诊断 cue 配置、模型选择、自定义提示词、分享链接（典型行式结构 + 业务 ID 列）

---

## 一、MongoDB Collections (2)

### 1. `replay_ai_conversation`

实体：`com.jiuyu.replay.ai.entity.ConversationEntity`
Repository：`ConversationRepository extends MongoRepository<ConversationEntity, String>`

**主键：** `_id` (`String`)，由 MongoDB 自动生成（ObjectId 转字符串）

**复合索引（`@CompoundIndexes`）：**

| 索引名 | 字段 | 用途 |
|---|---|---|
| `sourceId_sourceType_userId_tenantId_askType_index` | `{ sourceId: 1, sourceType: 1, userId: 1, tenantId: 1, askType: 1 }` | 用户在某来源（视频/文件/对比）下某 askType 的问答列表（C03 分页 / C04 isExist 主要查询路径） |

**单字段索引：**

| 索引 | 字段 | 用途 |
|---|---|---|
| `createTime` (DESC) | `@Indexed(direction = IndexDirection.DESCENDING)` | 列表按时间倒序 |

**字段表：**

| 字段 | 类型 | 含义 | 备注 |
|---|---|---|---|
| `_id` (id) | String | 文档主键 | MongoDB 自动生成 |
| `sourceId` | String | 来源 ID | videoId / fileId / 对比分析 ID |
| `sourceType` | Integer | 来源类型 | 0 视频 / 1 文件 / 2 对比分析 |
| `userId` | Long | 用户 ID | 租户隔离字段之一 |
| `tenantId` | Long | 租户 ID | 租户隔离字段之一 |
| `code` | String | 业务 code | 客户端透传 |
| `askType` | Integer | 助手类型 | 见 `AiEnums.askType`（0–15） |
| `completionId` | String | chat completion 唯一 ID | 关联 `tb_ai_token_use_record.requestId` |
| `cueWordsId` | Long | 提示词 ID | 系统 = `tb_cue_words.id`；用户 = `tb_cust_prompt.id` |
| `cueWordsType` | Integer | 提示词来源 | 0 系统 / 1 用户 |
| `qaCode` | String | 一问一答共享 code | 导出 / 分享聚合用 |
| `contextId` | String | 多轮会话上下文 ID | 同上下文共享 |
| `type` | String | "Q" 答 / "A" 问 | **业务命名反人类** |
| `content` | String | 内容（可能含 deepThinking 区块） | Q 类型为模型回答 |
| `realContent` | String | 真实提问 | A 类型保存原始问题；查询时主动 set null 不下发 |
| `giveStatuc` | Integer | 点赞状态 | -1 未点赞 / 0 点赞 / 1 踩 |
| `createDate` | String | 创建时间字符串 | `yyyy-MM-dd HH:mm:ss` |
| `createTime` | Long | 创建时间戳 | 毫秒；建索引按此倒序 |
| `htmlType` | Integer | HTML 生成方式 | 0 服务器 / 1 客户端 |
| `htmlStatus` | Integer | HTML 生成状态 | 0/1/2/3 |
| `htmlCreateDate` | String | HTML 生成时间 | 字符串格式同 createDate |
| `htmlSavePath` | String | HTML OSS Key | `ai-html/{snowflakeId}.html` |
| `htmlCreateError` | String | HTML 生成错误 | FAIL 时填 |
| `lastConversationId` | String | 上一次对话 ID | 用于多轮上下文 |
| `optimizeText` | String | 优化文本 | 客户端用 |
| `extraRequire` | String | 额外要求 | 客户端用 |
| `questionType` | Integer | 提问类型 | 0 正常 / 1 重新提问 |
| `aiCorrectStatus` | Integer | AI 纠正状态 | 0/1/2/3 |
| `aiCorrectType` | String | 纠正来源类型 | 0 服务器 / 1 客户端（注：实体声明为 String，BO 为 Integer，存储时 toString） |
| `aiCorrectError` | String | 纠正错误 | FAIL 时填 |
| `aiCorrectCreateTime` | Long | 纠正开始时间戳 | 毫秒；超时判定基准 |

**典型查询模式（来自 `ConversationRseImpl`）：**

- 分页：`sourceId + sourceType + userId + tenantId + askType` （走复合索引） + `Sort createTime DESC, type ASC`
- isExist：`sourceId + sourceType + userId + tenantId + askType`，仅 `exists` 操作
- existsCueWords：`sourceId + sourceType + userId + tenantId + askType + cueWordsId in ()`，只取 `cueWordsId` 字段
- conversationByCueWordsIds：Aggregation pipeline `match → sort → group by (cueWordsId, type) first(...)`
- getById：`_id` 精确匹配
- getHtmlStatus / getCorrectStatus：`_id in (...)` + `fields().include(...)` 投影
- updateMulti：批量改 `htmlStatus / aiCorrectStatus`（超时清扫、用户登出清扫）

---

### 2. `replay_ai_conversation_html`

实体：`com.jiuyu.replay.ai.entity.ConversationHtmlEntity`
当前 `ConversationHtmlRseImpl` 为空壳类（无方法）；`ConversationHtmlService` 接口存在但实现未在扫描范围内（疑似仍在开发或将弃用）。代码中实际 HTML 内容直接通过 `htmlSavePath` 存 OSS，本 collection **未被任何写入路径使用**，仅作为预留。

**复合索引：**

| 索引名 | 字段 | 用途 |
|---|---|---|
| `conversationId_isDelected_index` | `{ conversationId: 1, isDelected: 0 }` | 预留：按会话 ID 查未删 HTML 内容 |

**字段表：**

| 字段 | 类型 | 含义 | 备注 |
|---|---|---|---|
| `_id` (id) | String | 文档主键 | MongoDB 自动生成 |
| `conversationId` | String | 关联 `replay_ai_conversation._id` | |
| `htmlContent` | String | HTML 全文 | 预留，当前未使用（HTML 实际上 OSS） |
| `htmlType` | Integer | 生成方式 | 0 服务器 / 1 客户端 |
| `createDate` | String | 创建时间字符串 | |
| `isDelected` | Integer | 删除标记 | 默认 0；字段名拼写为 `isDelected`（**项目历史 typo**，按代码原样） |

---

## 二、MySQL 表 (4)

所有 MySQL 表统一规范：
- 雪花 ID `@TableId(type = IdType.INPUT)`，应用层通过 `SnowflakeManager.nextValue()` 生成
- **手动**软删除 `isDeleted` （**禁用** `@TableLogic`）
- **手动**时间戳 `createDate / updateDate`（**注：本模块用 `java.util.Date`**，与 words 模块的 `LocalDateTime` 不一致 — 见 architecture ADR-005）
- 租户隔离 `tenantId + userId`

### 1. `tb_diagnosis_cue` — AI 诊断提示词配置

实体：`DiagnosisCueEntity`，Dao：`DiagnosisCueDao`，Service：`DiagnosisCueServiceImpl`

| 字段 | 类型 | 含义 | 备注 |
|---|---|---|---|
| `id` | BIGINT | 雪花 ID | PK |
| `source_id` | VARCHAR | 来源 ID | secUid（主播）或 videoId（视频） |
| `source_type` | TINYINT | 0 主播 / 1 视频 | |
| `trade_id` | BIGINT | 行业 ID | |
| `cue_words_id` | BIGINT | 提示词 ID | 关联 `tb_cue_words` |
| `qa_status` | TINYINT | 0 待 / 1 中 / 2 成功 / 3 失败 | 状态机 |
| `qa_handle_time` | DATETIME | 最新分析时间 | 进入"生成中"时写；超时判定基准 |
| `error_content` | TEXT | 错误内容 | FAIL 填 |
| `is_selected` | TINYINT | 0 未选 / 1 已选 | |
| `diagnosis_type` | TINYINT | 0 内容诊断 / 1 数据诊断 | |
| `select_data_screenshot` | TINYINT | 是否选数据截图 | 0 / 1 |
| `select_board` | TINYINT | 是否选数据看板 | 0 / 1 |
| `user_id` | BIGINT | 用户 ID | 隔离 |
| `tenant_id` | BIGINT | 租户 ID | 隔离 |
| `update_user_id` | BIGINT | 更新人 | |
| `update_date` | DATETIME | 更新时间 | 手动赋值 |
| `create_user_id` | BIGINT | 创建人 | |
| `create_date` | DATETIME | 创建时间 | 手动赋值 |
| `is_read` | TINYINT | 0 未读 / 1 已读 | 数据诊断未读 inbox 用 |
| `is_deleted` | TINYINT | 0 / 1 | 手动软删 |

**典型索引（推断，DDL 未在仓库内）：**
- `idx_user_tenant_source` (`user_id, tenant_id, source_id, source_type`) — 列出本用户某来源全部 cue
- `idx_user_tenant_unread` (`user_id, tenant_id, is_read, qa_status, diagnosis_type`) — 数据诊断未读列表 `listUnreadDataDiagnosis`
- `idx_cue_words` (`cue_words_id`) — 反查谁选过该提示词

**业务唯一性**：`(source_id, source_type, cue_words_id, user_id, tenant_id, diagnosis_type)` 应用层去重，无数据库唯一约束。

### 2. `tb_diagnosis_model` — 诊断模型选择

实体：`DiagnosisModelEntity`

| 字段 | 类型 | 含义 | 备注 |
|---|---|---|---|
| `id` | BIGINT | 雪花 ID | PK |
| `source_id` | VARCHAR | secUid 或 videoId | |
| `source_type` | TINYINT | 0 主播 / 1 视频 | |
| `model_id` | BIGINT | AI 模型 ID | 关联 third 模块的 ai_model |
| `diagnosis_type` | TINYINT | 0 内容 / 1 数据 | |
| `user_id` | BIGINT | 隔离 | |
| `tenant_id` | BIGINT | 隔离 | |
| `update_user_id` / `update_date` / `create_user_id` / `create_date` / `is_deleted` | 审计字段 | | |

**业务唯一性**：`(source_id, source_type, user_id, tenant_id, diagnosis_type)` 应用层 saveOrUpdate 去重。

### 3. `tb_cust_prompt` — 用户自定义提示词

实体：`CustPromptEntity`，Dao：`CustPromptDao`

| 字段 | 类型 | 含义 | 备注 |
|---|---|---|---|
| `id` | BIGINT | 雪花 ID | PK |
| `prompt_title` | VARCHAR(10) | 标题 | `@Length(max=10)` |
| `prompt_content` | TEXT | 内容 | 长度上限受 systemKv `cust_prompt_content_max_count` 控制（默认 30000） |
| `prompt_sort` | TINYINT | 排序 | 0–99 |
| `user_id` | BIGINT | 用户 ID | 私人提示词，仅按 `userId` 隔离（无 `tenantId`） |
| `update_date` / `create_date` / `is_deleted` | 审计字段 | | |

**典型索引：** `idx_user_id` (`user_id`)，可能 `idx_user_sort` (`user_id, prompt_sort`)。

### 4. `tb_share_link_record` — 分享链接记录

实体：`ShareLinkRecordEntity`，Dao：`ShareLinkRecordDao`

| 字段 | 类型 | 含义 | 备注 |
|---|---|---|---|
| `id` | BIGINT | 雪花 ID | PK |
| `user_id` | BIGINT | 隔离 | |
| `tenant_id` | BIGINT | 隔离 | |
| `share_time` | DATETIME | 分享时间 | 用于过期清扫 |
| `codes` | TEXT | `JSONUtil.toJsonStr(qaCodeList)` | List<String> 序列化为 JSON 字符串 |
| `source_id` | VARCHAR | 来源 ID | |
| `source_type` | TINYINT | 0 视频 / 1 文件 / 2 对比 | |
| `expire_time` | DATETIME | 实际失效时间 | 由 cron 推过期天数时写入 |
| `url_status` | TINYINT | 0 正常 / 1 已失效 | |
| `create_date` / `update_date` / `is_deleted` | 审计字段 | | |

**典型索引：** `idx_share_time_status` (`share_time, url_status`) — 过期扫描；`idx_user_tenant` (`user_id, tenant_id`) — 列表。

---

## 三、关系图

```
tb_diagnosis_model (1) ──┐
                         │ (sourceId, sourceType, userId, tenantId, diagnosisType) 同组协同
tb_diagnosis_cue   (N) ──┘   每个组下 N 条 cue（按 cue_words_id 区分）
                         │
                         ▼  问答执行后
replay_ai_conversation (MongoDB)
   ▲
   │ qaCode 聚合分享
tb_share_link_record (codes = JSON List<qaCode>)

tb_cust_prompt        独立私人提示词，被 ConversationEntity.cueWordsId 引用（cueWordsType=1）
```

**跨模块引用：**
- `tb_diagnosis_cue.cue_words_id` → `replay-words.tb_cue_words.id`（系统提示词）或 `tb_cust_prompt.id`（用户提示词，由 `cueWordsType` 区分；本表未独立标注 type，依赖应用层语义）
- `tb_diagnosis_model.model_id` → `replay-third.tb_ai_model.id`
- `replay_ai_conversation.completionId` → `replay-order.tb_ai_token_use_record.request_id`
- `replay_ai_conversation.sourceId` (sourceType=0) → `replay-words.tb_anchor_video.video_id`

---

## 四、租户隔离

| 表 / Collection | 隔离字段 | 强制位置 |
|---|---|---|
| `replay_ai_conversation` | `userId + tenantId` | `ConversationRseImpl` 所有 query 均显式带；`ConversationServiceImpl#page` 用 Spring Data `Example.of(probe)`，要求 entity 上 `userId/tenantId` 已填 |
| `replay_ai_conversation_html` | (未启用) | — |
| `tb_diagnosis_cue` | `userId + tenantId` | `DiagnosisCueRseImpl` 所有 LambdaQueryWrapper 都带 `eq` |
| `tb_diagnosis_model` | `userId + tenantId` | `DiagnosisModelRseImpl` 同上 |
| `tb_cust_prompt` | `userId` | 仅按 userId（私人提示词无租户概念） |
| `tb_share_link_record` | `userId + tenantId` | `ShareLinkRecordRseImpl` 同上 |

---

## 五、数据生命周期

| 数据 | 创建 | 更新 | 软删 / 过期 | 物理删除 |
|---|---|---|---|---|
| Conversation | C02 / `ConversationFeign#saveConversationData` 批量插入 | `updateLikesStatus / updateHtmlStatus / updateCorrectStatus` | **无软删字段**；用户登出后 `updateUserHtmlFail / updateUserCorrectFail` 把进行中状态改 FAIL | 不主动删除 |
| ConversationHtml | (未启用) | — | `isDelected` 字段预留 | — |
| DiagnosisCue | D03 / D05 / D11 | D09 / D11 `saveOrUpdateBatch` | `removeByIds`（**调用的是 MyBatis-Plus removeByIds，实际是物理删除，因为 `isDeleted` 字段没接 @TableLogic**） | D11 中按 diff 触发 |
| DiagnosisModel | D11 / D14 / D05 | D14 `updateDiagnosisModel` 优先 update 已有，否则新建 | `deleteBySourceId` 物理删 | D11 协同删 |
| CustPrompt | P03 | P04 | `deleteById` (物理) | P05 |
| ShareLinkRecord | S05 | S03 | `updateUrlExpire` 改 `urlStatus=1` | S04 物理删 |

**关键提示：** 模块内大量 `removeById` / `removeByIds` 看起来像软删但实际是物理删除（因为 `isDeleted` 字段没用 @TableLogic 也没在 update 中手动 set）。需要软删时必须在业务层显式 `update isDeleted = 1`。

---

## 六、OSS 存储

模块依赖 `AiOssUtils`（`replay-common`）：

| 用途 | Bucket | Key 前缀 | 文件类型 |
|---|---|---|---|
| 内容诊断 PDF | `replay-ai-data` (`bucketNameAi`) | `diagnosisFile/yyyy/MM/dd/{videoId}.pdf` | application/pdf |
| 数据诊断 PDF | `replay-ai-data` | `dataDiagnosisFile/yyyy/MM/dd/{videoId}.pdf` | application/pdf |
| HTML 输出 | `replay-images` (`bucketName`) | `ai-html/{snowflakeId}.html` | text/html |
| 分享数据文本 | `replay-ai-data` | `dataTxt/...` | text/plain |

PDF 走客户端预签名 PUT 直传（D07），下载走服务端签名 GET 给客户端 URL（D08）。

HTML 走服务端 SDK `putObjectByString` 直接上传（`AiOssUtils#uploadToString` → `ossUtils.putObjectByString`）。
