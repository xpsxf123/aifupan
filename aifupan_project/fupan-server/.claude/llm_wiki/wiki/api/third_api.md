<!-- module: third -->
<!-- area: api -->
<!-- generated-by: field-level-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-api/src/main/java/com/jiuyu/replay/api/controller/third/, replay-third/src/main/java/com/jiuyu/replay/third/controller/ -->

# Third API -- 接口契约 (field-level)

> replay-third 模块完整 API 表面。第三方服务集成层，封装 AI 模型、支付、短信、存储、数据平台等。HTTP Controller 分布在 replay-api (7 个) 和 replay-third (2 个)；Feign 定义在 replay-generic (8 个)，实现均在 replay-third/api/。

所有 Controller 默认返回 `R<T>`，标 `@CrossOrigin` / `@Tag(...)`，鉴权走全局 AOP 拦截。

---

## 一、HTTP Controller -- 管理端 API

### 1. 七牛 OSS -- QiNiuOssController

`@RequestMapping("replay/qiniu")` `@CrossOrigin`

#### GET /replay/qiniu/getUploadToken
- **Summary:** 获取上传文件的临时凭证
- **Auth:** token required
- **Response `data`:** `String` -- 七牛上传 Token，前端直传七牛

---

### 2. 腾讯 VOD -- TencentVodController

`@RequestMapping("replay/vod")` `@CrossOrigin`

#### GET /replay/vod/getVodUploadSign
- **Summary:** 获取vod上传签名
- **Auth:** token required
- **Response `data`:** `String` -- 腾讯云点播上传签名（当前用户，有效期 2 天）

---

### 3. 语音识别 ASR -- AudioDiscernController

`@RequestMapping("/replay/audio")` `@CrossOrigin`

#### GET /replay/audio/checkSurplus
- **Summary:** 查询是否还有调用语音识别接口的余量
- **Auth:** token required
- **Response `data` fields (CheckSurplusVo):**
  | Field | Type | Meaning |
  |---|---|---|
  | id | Long | ID，用于加回余量 |
  | secretId | String | secretId，用于加回余量 |

#### GET /replay/audio/addSurplus
- **Summary:** 通知加回语音识别接口的余量
- **Auth:** token required
- **Request params:**
  | Field | Type | Required | Default | Meaning |
  |---|---|---|---|---|
  | id | Long | yes | - | 补回余量的 id |
  | secretId | String | yes | - | 补回余量的 secretId |
- **Response `data`:** `String`

#### GET /replay/audio/getTempToken
- **Summary:** 获取一句话语音识别接口临时调用凭证
- **Auth:** token required
- **Request params:**
  | Field | Type | Required | Default | Meaning |
  |---|---|---|---|---|
  | secretId | String | no | - | 语音识别 secretId |
- **Response `data` fields (TencentTempTokenVo):**
  | Field | Type | Meaning |
  |---|---|---|
  | token | String | 临时 token |
  | tempSecretId | String | 临时 SecretId |
  | tempSecretKey | String | 临时 SecretKey |
  | savePrefix | String | 保存前缀 |

#### GET /replay/audio/getRecTempToken
- **Summary:** 获取长音频识别接口临时调用凭证
- **Auth:** token required
- **Response `data`:** `TencentTempTokenVo` (同上)

#### GET /replay/audio/record
- **Summary:** 记录QPS访问（实现已被注释，仅占位）
- **Auth:** token required
- **Response `data`:** `String`

错误码: 7001 TIP_CUSTOM / 7002 NOT_QPS / 7003 NOT_BALANCE

---

### 4. AI 模型管理 -- AiModelController

`@RequestMapping("replay/aimodel")` `@CrossOrigin` `@Tag(name="AI模型配置表")`

#### POST /replay/aimodel/list
- **Summary:** AI模型配置表列表
- **Auth:** token required
- **Request fields (AiModelListBo extends PageBo):**
  | Field | Type | Required | Validation | Meaning |
  |---|---|---|---|---|
  | keyword | String | no | - | 模糊搜索查询条件 |
  | resourceType | String | no | - | 模型厂商过滤 |
  | useType | Integer | no | - | 使用方式 0分析内容,1数据截图视觉理解 |
  | contextSize | String | no | - | 上下文大小过滤 |
  | page | Integer | no | default=1 | 当前页 |
  | limit | Integer | no | default=10 | 每页记录数 |
- **Response `data`:** `PageUtils<AiModelListVo>`，list 项字段 (AiModelVo):
  | Field | Type | Meaning |
  |---|---|---|
  | id | Long | ID |
  | modelCode | String | 模型编码 |
  | resourceType | Integer | 来源类型 0:豆包, 1:通义, 2:DeepSeek |
  | useType | Integer | 使用方式 0:分析内容, 1:视觉理解 |
  | modelName | String | 模型名称 |
  | endpointId | String | 模型端点 ID |
  | apiKey | String | 接口 apiKey |
  | outSize | Integer | 输出数据流大小(kb) |
  | inputSize | Integer | 输入数据流大小(kb) |
  | contextSize | Integer | 缓存上下文大小(kb) |
  | wordsNum | Integer | 限制使用字数 |
  | outWordNum | Integer | 推荐输出字数 |
  | sort | Integer | 排序 |
  | remarks | String | 描述 |
  | consumeMultiple | Double | AI 算力消费倍数 |
  | createDate | Date | 创建时间 |
  | updateDate | Date | 最后修改时间 |
  | isDeleted | Integer | 是否已删除 |

#### GET /replay/aimodel/info
- **Summary:** AI模型配置表信息
- **Auth:** token required
- **Request params:**
  | Field | Type | Required | Meaning |
  |---|---|---|---|
  | id | Long | yes | AI模型配置表 id |
- **Response `data`:** `AiModelInfoVo extends AiModelVo`，额外字段:
  | Field | Type | Meaning |
  |---|---|---|
  | aiModel | Integer | ai 模型整数值（字典 client_ai_model），给客户端使用 |

#### POST /replay/aimodel/save
- **Summary:** 新增AI模型配置表
- **Auth:** token required
- **Request fields (AiModelBo):** 同 AiModelVo 全部字段（不含 id，由服务端生成），额外:
  | Field | Type | Required | Meaning |
  |---|---|---|---|
  | consumeMultiple | Double | no | AI 算力消费倍数 |
- **Response `data`:** `String` "添加成功"

#### POST /replay/aimodel/update
- **Summary:** 修改AI模型配置表
- **Auth:** token required
- **Request fields:** `AiModelBo` (同 save，必须带 id)
- **Response `data`:** `String` "修改成功"

#### GET /replay/aimodel/delete
- **Summary:** 删除AI模型配置表
- **Auth:** token required
- **Request params:** id (Long, required)
- **Response `data`:** `String` "删除成功"

---

### 5. 代理 IP 管理 -- ProxyIpController

`@RequestMapping("replay/proxyip")` `@CrossOrigin` `@Tag(name="代理ip提取")`

#### POST /replay/proxyip/list
- **Summary:** 代理ip提取列表
- **Auth:** token required
- **Request fields (ProxyIpListBo extends PageBo):**
  | Field | Type | Required | Meaning |
  |---|---|---|---|
  | keyword | String | no | 模糊搜索 |
  | page | Integer | no | 当前页, default=1 |
  | limit | Integer | no | 每页记录数, default=10 |
- **Response `data`:** `PageUtils<ProxyIpListVo>`，list 项字段 (ProxyIpVo):
  | Field | Type | Meaning |
  |---|---|---|
  | id | Long | ID |
  | extractUrl | String | 提取 IP 的 URL |
  | totalNum | Integer | 总 IP 数 |
  | remainingNum | Integer | 剩余 IP 数 |
  | ipEffectiveTime | Integer | 有效时长（分钟） |
  | activeStatus | Integer | 激活状态 0:未激活, 1:已激活 |
  | proxyUsername | String | 代理 IP 账号 |
  | proxyPassword | String | 代理 IP 密码 |
  | validityType | Integer | 时效类型 0:短效, 1:长效 |
  | dayUseNum | Integer | 日使用次数 |
  | createDate | Date | 创建时间 |
  | updateDate | Date | 最后修改时间 |
  | isDeleted | Integer | 是否已删除 |

#### GET /replay/proxyip/info
- **Summary:** 代理ip提取信息
- **Request params:** id (Long, required)
- **Response `data`:** `ProxyIpInfoVo` (同 ProxyIpVo 字段)

#### POST /replay/proxyip/save
- **Summary:** 新增代理ip提取
- **Request fields (ProxyIpBo):** 同 ProxyIpVo 字段（不含 id）
- **Response `data`:** `String`

#### POST /replay/proxyip/update
- **Summary:** 修改代理ip提取
- **Request fields:** `ProxyIpBo` (同 save，必须带 id)
- **Response `data`:** `String`

#### GET /replay/proxyip/delete
- **Summary:** 删除代理ip提取
- **Request params:** id (Long, required)
- **Response `data`:** `String`

---

### 6. 代理 IP 提取记录 -- ProxyIpRecordController

`@RequestMapping("replay/proxyiprecord")` `@CrossOrigin` `@Tag(name="代理ip提取记录")`

#### POST /replay/proxyiprecord/list
- **Summary:** 代理ip提取记录列表
- **Auth:** token required
- **Request fields (ProxyIpRecordListBo extends PageBo):**
  | Field | Type | Required | Meaning |
  |---|---|---|---|
  | keyword | String | no | 模糊搜索 |
  | userIds | List\<Long\> | no | 用户 ID 集合 |
  | page | Integer | no | 当前页, default=1 |
  | limit | Integer | no | 每页记录数, default=10 |
- **Response `data`:** `PageUtils<ProxyIpRecordListVo>`，list 项字段 (ProxyIpRecordVo + userNickName):
  | Field | Type | Meaning |
  |---|---|---|
  | id | Long | ID |
  | userId | Long | 用户 ID |
  | tenantId | Long | 租户 ID |
  | proxyId | Long | 代理 ID |
  | ipStr | String | IP 地址 |
  | portStr | String | 端口号 |
  | extractDate | Date | 提取日期 |
  | validityDate | Date | 有效期 |
  | ipEffectiveTime | Integer | IP 有效时长（分钟） |
  | proxyUsername | String | 代理 IP 账号 |
  | proxyPassword | String | 代理 IP 密码 |
  | validityType | Integer | 时效类型 0:短效, 1:长效 |
  | createDate | Date | 创建时间 |
  | updateDate | Date | 最后修改时间 |
  | isDeleted | Integer | 是否已删除 |
  | userNickName | String | 用户名称（列表项额外字段） |

#### GET /replay/proxyiprecord/info
- **Summary:** 代理ip提取记录信息
- **Request params:** id (Long, required)
- **Response `data` fields (ProxyIpRecordInfoVo extends ProxyIpRecordVo):**
  | Field | Type | Meaning |
  |---|---|---|
  | expireTime | Long | IP 过期的时间戳（额外字段） |

#### POST /replay/proxyiprecord/save
- **Summary:** 新增代理ip提取记录
- **Request fields (ProxyIpRecordBo):**
  | Field | Type | Required | Meaning |
  |---|---|---|---|
  | id | Long | no | ID（服务端生成） |
  | userId | Long | yes | 用户 ID |
  | tenantId | Long | yes | 租户 ID |
  | proxyId | Long | yes | 代理 ID |
  | ipStr | String | yes | IP 地址 |
  | portStr | String | yes | 端口号 |
  | extractDate | Date | yes | 提取日期 |
  | validityDate | Date | no | 有效期 |
  | ipEffectiveTime | Integer | no | IP 有效时长（分钟） |
  | isDeleted | Integer | no | 是否已删除 |
- **Response `data`:** `String`

#### POST /replay/proxyiprecord/update
- **Summary:** 修改代理ip提取记录
- **Request fields:** `ProxyIpRecordBo` (必须带 id)
- **Response `data`:** `String`

#### GET /replay/proxyiprecord/delete
- **Summary:** 删除代理ip提取记录
- **Request params:** id (Long, required)
- **Response `data`:** `String`

---

### 7. COS 点赞问答文件 -- CosThumbsFileController

`@RequestMapping("replay/costhumbsfile")` `@CrossOrigin` `@Tag(name="点赞问答文件上传cos记录表")`

#### POST /replay/costhumbsfile/list
- **Summary:** 点赞问答文件上传cos记录表列表
- **Auth:** token required
- **Request fields (CosThumbsFileListBo extends PageBo):**
  | Field | Type | Required | Meaning |
  |---|---|---|---|
  | keyword | String | no | 模糊搜索 |
  | page | Integer | no | 当前页, default=1 |
  | limit | Integer | no | 每页记录数, default=10 |
- **Response `data`:** `PageUtils<CosThumbsFileListVo>`，list 项字段 (CosThumbsFileVo):
  | Field | Type | Meaning |
  |---|---|---|
  | id | Long | ID |
  | tradeId | Long | 行业 ID，0 表示全行业 |
  | userId | Long | 用户 ID |
  | sourceId | String | 资源 ID |
  | sourceType | Integer | 资源类型 0:单个视频, 1:文件, 2:对比分析 |
  | fileSize | Long | 文件大小 |
  | coskey | String | COS Key |
  | contextId | String | 上下文 ID |
  | thumbState | Integer | 点赞状态 0:未点赞, 1:已点赞, 2:点踩 |
  | fileName | String | 文件名称 |
  | askCount | Integer | 问答次数 |
  | uploadDate | Date | 上传时间 |
  | resourceType | Integer | 来源类型 0:系统 |
  | platformType | Integer | 平台类型 0:全平台, 1:抖音, 2:快手, 3:微视 |
  | cosType | Integer | 会话类型 0:运营问题, 1:违规 |
  | sort | Integer | 排序 |
  | remarks | String | 描述 |
  | createDate | Date | 创建时间 |
  | updateDate | Date | 最后修改时间 |
  | isDeleted | Integer | 是否已删除 |

#### GET /replay/costhumbsfile/info
- **Summary:** 点赞问答文件上传cos记录表信息
- **Request params:** id (Long, required)
- **Response `data`:** `CosThumbsFileInfoVo` (同 CosThumbsFileVo)

#### POST /replay/costhumbsfile/save
- **Summary:** 新增点赞问答文件上传cos记录表
- **Request fields (CosThumbsFileBo):** 同 CosThumbsFileVo 字段（不含 id）
- **Response `data`:** `String`

#### POST /replay/costhumbsfile/update
- **Summary:** 修改点赞问答文件上传cos记录表
- **Request fields:** `CosThumbsFileBo` (必须带 id)
- **Response `data`:** `String`

#### GET /replay/costhumbsfile/delete
- **Summary:** 删除点赞问答文件上传cos记录表
- **Request params:** id (Long, required)
- **Response `data`:** `String`

---

## 二、模块内 HTTP Controller (replay-third)

### TableStoreController -- 弹幕数据
`@RequestMapping("replay/third/tableStore")`

| Method | Path | Param | Meaning |
|--------|------|-------|---------|
| GET | `/getBarrageDataList` | videoId (String, req) / isBlessBag (Integer, opt: null=全部,0=非福袋,1=福袋) | 弹幕统计数据 |
- **Response:** `R<List<Map<String, Object>>>`，Map 含 barrageNum(Integer)/date(String)/dateTime(Long)

### ThirdAiModelController -- AI 临时 Token
`@RequestMapping("replay/third/aiModel")` (无 @CrossOrigin)

| Method | Path | Param | Meaning |
|--------|------|-------|---------|
| GET | `/getAnalysisTempToken` | aiModel (Integer, opt) / code (String, opt) 二选一 | 临时 token 供客户端直连火山方舟 |
- **Response `R<AiTempTokenVo>`:** modelId(String)/modelCode(String)/modelDefinition(String)/useModelWay(int)/maxSendMessageLength(Integer)/outWordNum(Integer)/resourceType(Integer: 0=豆包,1=通义,2=DeepSeek)/token(String)/mode(String)/truncationStrategyType(Integer)/lastHistoryTokens(int)/rollingTokens(Integer)/contextSaveTime(Integer)/tempTokenSaveTime(Long)

---

## 三、Feign 接口

| Feign | 方法 | 说明 | 调用方 |
|-------|------|------|--------|
| AiFeign | `getAiModelByCode`, `chatCompletion` (批量/单次), `getVideoContentSection`, `getAnalysisTempToken` | AI 对话 | replay-ai, replay-words |
| AiModelFeign | `getByCode`, `info`, `listDiagnosisModel`, `getAiModel`, `getAiModelByAiModel` | 模型配置查询 | replay-ai, replay-words |
| ChanmamaFeign | `sendRevisionQuery`, `sendSimilarAnchorQuery`, `getSalesRanking` | 蝉妈妈数据 | replay-words |
| SmsServiceFeign | `send(templateId, mobile, params, providerName)` | 短信发送（阿里 + 联麓故障转移） | replay-power, 全模块 |
| TableStoreFeign | `getBarrageDataList`, `queryDanMuSearchCount(QueryDanMuBo)` | 表格存储弹幕 | replay-words |
| PropertiesFeign | `getVolcengine()` | 火山引擎配置 | 内部 |
| GovernanceLiveRoomService | `addAnchor(GovernanceAddAnchorBo)` | 通知企业端加主播 | replay-words |
| DouyinOpenFeign | `searchAnchor(keyword, isUnique)` | 搜索抖音主播 | replay-words, replay-agent |

---

## 四、非 Feign 服务

| 接口 | 用途 |
|------|------|
| GovernanceTenantService | 获取租户状态列表 (POST /api/governance/tenant/status) |
| GovernanceEmployeeService | 绑定/解绑子账户 (POST /api/governance/employee-open/bind\|unbind) |

---

## 五、回调

| 来源 | 说明 |
|------|------|
| 微信/支付宝回调 | PayBll 验签，replay-order 接收 |
| 蝉妈妈回调 | Controller 在 replay-words |

---

## 六、错误码

| code | 含义 |
|------|------|
| 7001 | TIP_CUSTOM |
| 7002 | NOT_QPS |
| 7003 | NOT_BALANCE |

SMS: 400 参数错误 / 500 服务异常 / 501 全部发送失败 / 502 未配置模板 / 503 模板映射缺失

---

## 七、路由全表

| Method + Path | Summary | Auth | Controller |
|------|------|------|------|
| GET `/replay/qiniu/getUploadToken` | 七牛上传凭证 | Session | QiNiuOssController |
| GET `/replay/vod/getVodUploadSign` | VOD 上传签名 | Session | TencentVodController |
| GET `/replay/audio/checkSurplus` | QPS 余量 | Session | AudioDiscernController |
| GET `/replay/audio/addSurplus?id=&secretId=` | 归还 QPS | Session | AudioDiscernController |
| GET `/replay/audio/getTempToken` | 一句话识别 Token | Session | AudioDiscernController |
| GET `/replay/audio/getRecTempToken` | 长音频识别 Token | Session | AudioDiscernController |
| GET `/replay/audio/record` | QPS 埋点 | Session | AudioDiscernController |
| POST `/replay/aimodel/list` | AI 模型列表 | Session | AiModelController |
| GET `/replay/aimodel/info?id=` | AI 模型详情 | Session | AiModelController |
| POST `/replay/aimodel/save` | 新增 AI 模型 | Session | AiModelController |
| POST `/replay/aimodel/update` | 修改 AI 模型 | Session | AiModelController |
| GET `/replay/aimodel/delete?id=` | 删除 AI 模型 | Session | AiModelController |
| POST `/replay/proxyip/list` | 代理 IP 列表 | Session | ProxyIpController |
| GET `/replay/proxyip/info?id=` | 代理 IP 详情 | Session | ProxyIpController |
| POST `/replay/proxyip/save` | 新增代理 IP | Session | ProxyIpController |
| POST `/replay/proxyip/update` | 修改代理 IP | Session | ProxyIpController |
| GET `/replay/proxyip/delete?id=` | 删除代理 IP | Session | ProxyIpController |
| POST `/replay/proxyiprecord/list` | 提取记录列表 | Session | ProxyIpRecordController |
| GET `/replay/proxyiprecord/info?id=` | 提取记录详情 | Session | ProxyIpRecordController |
| POST `/replay/proxyiprecord/save` | 新增记录 | Session | ProxyIpRecordController |
| POST `/replay/proxyiprecord/update` | 修改记录 | Session | ProxyIpRecordController |
| GET `/replay/proxyiprecord/delete?id=` | 删除记录 | Session | ProxyIpRecordController |
| POST `/replay/costhumbsfile/list` | COS 文件列表 | Session | CosThumbsFileController |
| GET `/replay/costhumbsfile/info?id=` | COS 文件详情 | Session | CosThumbsFileController |
| POST `/replay/costhumbsfile/save` | 新增 COS 文件 | Session | CosThumbsFileController |
| POST `/replay/costhumbsfile/update` | 修改 COS 文件 | Session | CosThumbsFileController |
| GET `/replay/costhumbsfile/delete?id=` | 删除 COS 文件 | Session | CosThumbsFileController |
| GET `/replay/third/tableStore/getBarrageDataList` | 弹幕列表 | Session | TableStoreController |
| GET `/replay/third/aiModel/getAnalysisTempToken` | 大模型临时 Token | Session (无 CrossOrigin) | ThirdAiModelController |
