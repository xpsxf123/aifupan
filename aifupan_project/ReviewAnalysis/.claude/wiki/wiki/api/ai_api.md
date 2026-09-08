# AI 对话 API

## AiRelatedController

路由前缀: `api/aiRelated`

---

### POST /api/aiRelated/ask

AI 问答 (SSE 流式返回)。

**请求体 (JSON)** — `AskRequestDto`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type | int | 是 | 助手类型: 0=运营助手, 1=违规助手 |
| content | string | 是 | 提问的问题 |
| realContent | string | 否 | 问 AI 的真实问题 |
| sourceId | string | 是 | 来源 ID |
| sourceType | int | 是 | 数据类型: 0=视频, 1=文件, 2=对比分析 |
| lastConversationId | string | 否 | 上一次对话 ID |
| cueWordsId | string | 否 | 提示词 ID |
| cueWordsType | int? | 否 | 提示词类型: 0=系统, 1=用户 |
| identity | string | 否 | AI 身份 |
| paragraphCode | string | 否 | 段落 code (全文为0, 运营助手使用) |
| paragraphContent | string | 否 | 段落内容 (违规助手使用) |
| reasonViolation | string | 否 | 违规原因 (违规助手使用) |
| additionalList | List\<string\> | 否 | 额外要求列表 |
| paramsDescribe | string | 否 | 文本内参数说明 |
| aiModel | int | 否 | AI 模型: 0=doubai1.5-pro-32K, 1=deepseek-r1 (默认0) |
| useModelType | int | 否 | 使用方式: 0=上下文缓存对话, 1=对话 (默认1) |
| videoTimeOneList | List\<long\> | 否 | 视频一时间 [开始ms, 结束ms] |
| videoTimeTwoList | List\<long\> | 否 | 视频二时间 [开始ms, 结束ms] |
| uploadScreenshot | int? | 否 | 是否上传数据截图: 0=否, 1=是 (默认1) |
| uploadBoard | int? | 否 | 是否上传看板: 0=否, 1=是 (默认1) |
| singleMaxNum | int | 否 | 提示词字数限制 (默认-1不限) |
| thinkingType | string | 否 | 是否开启深度思考模式 (默认开启) |
| otherObj | Dictionary\<string, Object\> | 否 | 其他参数 |
| token | AiTempTokenDto | 否 | 临时 Token |

`AiTempTokenDto` 嵌套对象:

| 字段 | 类型 | 说明 |
|---|---|---|
| token | string | Token 值 |
| model | string[] | 模型列表 |
| modelId | string | 模型 ID |
| modelName | string[] | 模型名称 |
| modelCode | string | 模型代码 |
| useModelWay | int | 使用方式 (默认1) |
| mode | string | 模式 (默认 "session") |
| maxSendMessageLength | int? | 最大发送消息长度 |
| outWordNum | int? | 输出字数 |

**响应** — `AskResponseDto`:

| 字段 | 类型 | 说明 |
|---|---|---|
| property | PropertyDto | 资产信息 |
| problem | ConversationDto | 问题记录 |
| answer | ConversationDto | 回答记录 |
| checkAiContent | bool | 是否检查 AI 内容 |

`PropertyDto`:

| 字段 | 类型 | 说明 |
|---|---|---|
| currerntUseNum | int | 当前问答使用次数 |
| surplusNum | int | 剩余资产 |

`ConversationDto` 完整字段:

| 字段 | 类型 | 说明 |
|---|---|---|
| id | string | 对话 ID |
| sourceId | string | 来源 ID |
| sourceType | int? | 来源类型 |
| askType | int? | 提问类型 |
| code | string | 对话 code |
| completionId | string | 补全 ID |
| cueWordsId | string | 提示词 ID |
| contextId | string | 上下文 ID |
| type | string | Q=问, A=答 |
| content | string | 内容 |
| realContent | string | 真实内容 |
| giveStatuc | int | 点赞状态 (默认-1) |
| createDate | string | 创建时间 |
| lastConversationId | string | 上一对话 ID |
| optimizeText | string | 优化文本 |
| questionType | int? | 问题类型 (默认0) |

**示例**:
```http
POST /api/aiRelated/ask
Content-Type: application/json

{
  "type": 0,
  "content": "这场直播的转化率如何？",
  "sourceId": "video_001",
  "sourceType": 0,
  "aiModel": 0,
  "useModelType": 1,
  "uploadScreenshot": 1
}
```

```json
// 响应
{
  "property": {
    "currerntUseNum": 15,
    "surplusNum": 985
  },
  "problem": {
    "id": "conv_001",
    "type": "Q",
    "content": "这场直播的转化率如何？",
    "sourceId": "video_001",
    "sourceType": 0,
    "createDate": "2026-04-23 10:30:00"
  },
  "answer": {
    "id": "conv_002",
    "type": "A",
    "content": "根据分析，本场直播转化率为3.2%...",
    "sourceId": "video_001",
    "sourceType": 0,
    "createDate": "2026-04-23 10:30:05"
  },
  "checkAiContent": false
}
```

---

### POST /api/aiRelated/addHistoryParagraph

添加历史段落。

**请求体 (JSON)** — `HistoryParagraphBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type | int | 是 | 助手类型: 0=运营助手, 1=违规助手 |
| sourceId | string | 是 | 来源 ID |
| sourceType | int | 是 | 数据类型: 0=视频, 1=文件, 2=对比分析 |
| alias | string | 否 | 别名 |
| content | string | 是 | 段落内容 |

**响应** — `HistoryParagraphVo`:

| 字段 | 类型 | 说明 |
|---|---|---|
| type | int | 助手类型 |
| code | string | 历史段落 code |
| alias | string | 别名 |
| content | string | 段落内容 |
| sourceId | string | 来源 ID |
| sourceType | int | 数据类型 |
| createDate | string | 创建时间 |

**示例**:
```http
POST /api/aiRelated/addHistoryParagraph
Content-Type: application/json

{
  "type": 0,
  "sourceId": "video_001",
  "sourceType": 0,
  "alias": "开场白",
  "content": "大家好，欢迎来到直播间..."
}
```

```json
// 响应
{
  "type": 0,
  "code": "hp_20260423_001",
  "alias": "开场白",
  "content": "大家好，欢迎来到直播间...",
  "sourceId": "video_001",
  "sourceType": 0,
  "createDate": "2026-04-23 10:30:00"
}
```

---

### POST /api/aiRelated/historyParagraphList

查询历史段落列表。

**请求体 (JSON)** — `historyParagraphListBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceId | string | 是 | 资源 ID |
| sourceType | int | 是 | 资源类型 |
| type | int | 是 | 助手类型 |

**响应** — `List<HistoryParagraphVo>` (字段同 addHistoryParagraph 响应)

**示例**:
```http
POST /api/aiRelated/historyParagraphList
Content-Type: application/json

{
  "sourceId": "video_001",
  "sourceType": 0,
  "type": 0
}
```

```json
// 响应
[
  {
    "type": 0,
    "code": "hp_20260423_001",
    "alias": "开场白",
    "content": "大家好，欢迎来到直播间...",
    "sourceId": "video_001",
    "sourceType": 0,
    "createDate": "2026-04-23 10:30:00"
  }
]
```

---

### POST /api/aiRelated/deleteHistoryParagraph

删除历史段落。

**请求体 (JSON)** — `HistoryParagraphVo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| code | string | 是 | 历史段落 code |
| sourceId | string | 是 | 来源 ID |
| sourceType | int | 是 | 数据类型 |
| type | int | 是 | 助手类型 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/aiRelated/deleteHistoryParagraph
Content-Type: application/json

{
  "code": "hp_20260423_001",
  "sourceId": "video_001",
  "sourceType": 0,
  "type": 0
}
```

---

### POST /api/aiRelated/addStructure

添加结构化 AI 数据。

**请求体 (JSON)** — `StructureReqBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| contentList | List\<ContentObj\> | 是 | 保存内容列表 |
| sourceId | string | 是 | 来源 ID |
| sourceType | int | 是 | 数据类型: 0=视频, 1=文件, 2=对比分析 |
| type | int | 是 | 助手类型: 0=运营助手, 1=违规助手 |

`ContentObj`:

| 字段 | 类型 | 说明 |
|---|---|---|
| code | string | 内容 code |
| content | string | 内容文本 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/aiRelated/addStructure
Content-Type: application/json

{
  "sourceId": "video_001",
  "sourceType": 0,
  "type": 0,
  "contentList": [
    { "code": "summary", "content": "本场直播总结..." },
    { "code": "highlight", "content": "亮点分析..." }
  ]
}
```

---

### POST /api/aiRelated/structurePage

分页查询结构化数据。

**请求体 (JSON)** — `StructurePageBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type | int | 是 | 助手类型 |
| sourceId | string | 是 | 资源 ID |
| sourceType | int | 是 | 资源类型 |
| pageIndex | int? | 否 | 页码 (默认1) |
| pageSize | int? | 否 | 每页条数 (默认30) |

**响应** — `StructurePageVo`:

| 字段 | 类型 | 说明 |
|---|---|---|
| existPreviousPage | bool | 是否存在上一页 (默认false) |
| list | List\<dynamic\> | 前端结构数据 |
| rawObj | Dictionary\<string, ConversationDto\> | code 对应的原始对话数据 |

**示例**:
```http
POST /api/aiRelated/structurePage
Content-Type: application/json

{
  "type": 0,
  "sourceId": "video_001",
  "sourceType": 0,
  "pageIndex": 1,
  "pageSize": 30
}
```

```json
// 响应
{
  "existPreviousPage": false,
  "list": [
    { "code": "summary", "content": "本场直播总结..." }
  ],
  "rawObj": {
    "summary": {
      "id": "conv_003",
      "type": "A",
      "content": "本场直播总结...",
      "createDate": "2026-04-23 10:35:00"
    }
  }
}
```

---

### POST /api/aiRelated/likes

点赞/踩 AI 回复。

**请求体 (JSON)** — `StructureUpdateBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type | int | 是 | 助手类型: 0=运营助手, 1=违规助手 |
| sourceId | string | 是 | 来源 ID |
| sourceType | int | 是 | 数据类型 |
| code | string | 是 | 对话 code |
| giveStatuc | int | 是 | 0=点赞, 1=踩 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/aiRelated/likes
Content-Type: application/json

{
  "type": 0,
  "sourceId": "video_001",
  "sourceType": 0,
  "code": "summary",
  "giveStatuc": 0
}
```

---

### POST /api/aiRelated/getAiModel

获取当前 AI 模型。

**请求体 (JSON)** — `AiModelVo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceId | string | 是 | 来源 ID |
| sourceType | int | 是 | 数据类型: 0=视频, 1=文件, 2=对比分析 |

**响应**: `int` — 模型编号

**示例**:
```http
POST /api/aiRelated/getAiModel
Content-Type: application/json

{ "sourceId": "video_001", "sourceType": 0 }
```

```json
// 响应
0
```

---

### POST /api/aiRelated/conversationPage

分页查询对话记录。

**请求体 (JSON)** — `ConversationPageBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type | int | 是 | 助手类型 |
| sourceId | string | 是 | 资源 ID |
| sourceType | int | 是 | 资源类型 |
| page | int? | 否 | 页码 |
| limit | int? | 否 | 每页条数 |

**响应** — 动态结构:

| 字段 | 类型 | 说明 |
|---|---|---|
| existPreviousPage | bool | 是否存在上一页 |
| list | List\<ConversationDto\> | 对话记录列表 |

**示例**:
```http
POST /api/aiRelated/conversationPage
Content-Type: application/json

{
  "type": 0,
  "sourceId": "video_001",
  "sourceType": 0,
  "page": 1,
  "limit": 20
}
```

```json
// 响应
{
  "existPreviousPage": false,
  "list": [
    {
      "id": "conv_001",
      "type": "Q",
      "content": "这场直播的转化率如何？",
      "createDate": "2026-04-23 10:30:00"
    },
    {
      "id": "conv_002",
      "type": "A",
      "content": "根据分析，本场直播转化率为3.2%...",
      "createDate": "2026-04-23 10:30:05"
    }
  ]
}
```

---

### POST /api/aiRelated/getAiRecommendTrade

AI 推荐行业。

**请求体 (JSON)** — `AiRecommendTradeBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceType | int | 是 | 来源类型 |
| sourceId | string | 是 | 来源 ID |

**响应** — `AiRecommendTradeVo`:

| 字段 | 类型 | 说明 |
|---|---|---|
| tradeId | long | 行业 ID |
| tradeName | string | 行业名称 |

**示例**:
```http
POST /api/aiRelated/getAiRecommendTrade
Content-Type: application/json

{ "sourceType": 0, "sourceId": "video_001" }
```

```json
// 响应
{ "tradeId": 100, "tradeName": "美妆护肤" }
```

---

### GET /api/aiRelated/exportAiConfig

导出 AI 问答配置。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| qaCodes | string | 是 | 问答 code 列表 |

**响应**: 无返回体 (void)，触发文件保存对话框

**示例**:
```http
GET /api/aiRelated/exportAiConfig?qaCodes=qa_001,qa_002
```

---

### GET /api/aiRelated/generateHtml

生成 HTML 报告。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | string | 是 | 对话 ID |

**响应** — `ConversationVo`:

| 字段 | 类型 | 说明 |
|---|---|---|
| id | string | 对话 ID |
| sourceId | string | 来源 ID |
| sourceType | int? | 来源类型 |
| code | string | 对话 code |
| content | string | 内容 |
| htmlType | int? | HTML 类型 |
| htmlStatus | int? | HTML 生成状态 |
| htmlSavePath | string | HTML 保存路径 |
| htmlCreateDate | string | HTML 创建时间 |
| htmlCreateError | string | 创建错误信息 |
| htmlDomainName | string | HTML 域名 |
| giveStatuc | int? | 点赞状态 (默认-1) |

**示例**:
```http
GET /api/aiRelated/generateHtml?id=conv_001
```

```json
// 响应
{
  "id": "conv_001",
  "sourceId": "video_001",
  "sourceType": 0,
  "code": "qa_001",
  "content": "本场直播转化率分析...",
  "htmlStatus": 1,
  "htmlSavePath": "/reports/conv_001.html",
  "htmlCreateDate": "2026-04-23 11:00:00",
  "giveStatuc": -1
}
```

---

### POST /api/aiRelated/getHtmlStatus

批量获取 HTML 生成状态。

**请求体 (JSON)**: `List<string>` (对话 ID 列表)

**响应** — `List<ConversationVo>` (字段同 generateHtml 响应)

**示例**:
```http
POST /api/aiRelated/getHtmlStatus
Content-Type: application/json

["conv_001", "conv_002", "conv_003"]
```

```json
// 响应
[
  { "id": "conv_001", "htmlStatus": 1, "htmlSavePath": "/path/to/report.html" },
  { "id": "conv_002", "htmlStatus": 0 },
  { "id": "conv_003", "htmlStatus": 2, "htmlCreateError": "生成失败" }
]
```

---

### GET /api/aiRelated/generateCorrectAiContent

生成 AI 纠错内容。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | string | 是 | 对话 ID |

**响应** — `ConversationVo` (字段同 generateHtml 响应)

**示例**:
```http
GET /api/aiRelated/generateCorrectAiContent?id=conv_001
```

---

## DiagnosisController

路由前缀: `api/diagnosis`

---

### POST /api/diagnosis/saveDiagnosis

添加诊断分析任务。

**请求体 (JSON)** — `SaveDiagnosisBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |
| cueWordsId | long | 是 | 提示词 ID |

**业务规则**: 检查 `AiTokenNum` 余额，不足时抛出异常 (code: 7001, msg: "AI助手分析余量不足")。

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/diagnosis/saveDiagnosis
Content-Type: application/json

{ "videoId": "video_001", "cueWordsId": 12345 }
```

---

### POST /api/diagnosis/generateReport

生成诊断报告。

**请求体 (JSON)** — `GenerateReportVo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |
| sourceType | int? | 否 | 来源类型 (默认0) |
| fileName | string | 否 | 文件名 |
| uploadType | int? | 否 | 类型: 1=内容, 2=数据 (默认1) |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/diagnosis/generateReport
Content-Type: application/json

{
  "videoId": "video_001",
  "sourceType": 0,
  "fileName": "诊断报告_20260423",
  "uploadType": 1
}
```
