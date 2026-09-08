<!-- module: third -->
<!-- area: domain -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-third/, replay-api/.../controller/third/ -->

# Third Domain — 业务概念与词汇表

> `replay-third` 模块封装所有第三方平台接入：支付（微信 / 支付宝）、对象存储（七牛 / 腾讯 VOD）、AI 大模型（豆包 / 通义 / DeepSeek）、短信（阿里云 / 联麓）、阿里云表格存储（TableStore，弹幕）、第三方数据平台（蝉妈妈）、代理 IP（神龙）、钉钉告警、企业后台（governance）。本模块只负责对外协议适配；业务订单、用户、视频等核心数据由调用方（replay-order / replay-words / replay-power）持有。

---

## 一、模块定位

| 维度 | 说明 |
|---|---|
| 角色 | 协议适配层（Anti-Corruption Layer） |
| 上游调用者 | replay-api（直接 HTTP）、replay-order / replay-words / replay-power / replay-ai 通过 Feign（在 replay-generic 定义） |
| 下游平台 | 微信支付 v3、支付宝 SDK、七牛 OSS、阿里云短信、联麓短信、阿里云 TableStore、火山方舟（豆包 Ark）、阿里 DashScope（通义千问）、DeepSeek、蝉妈妈、神龙代理、钉钉 Webhook、企业后台 governance |
| 持有数据 | 仅本模块自有：AI 模型配置、代理 IP 池、弹幕表（TableStore）、点赞问答文件记录、QPS 日志、蝉妈妈账号 |

---

## 二、子域拆解

### 2.1 支付域（Payment）

| 概念 | 定义 | 主要类 |
|---|---|---|
| Native Pay | 二维码扫码支付，下单后返回 `code_url` 给前端生成二维码 | `WeChatPay#nativePay`, `Alipay#nativePay` |
| 商户订单号（outTradeNo） | 复用 jiuyu 内部订单 ID（`orderId.toString()`） | 调用方传入 |
| 支付二维码缓存 | 同一订单二次发起支付直接返回上次 `code_url`，TTL = `common.order-timeout-minutes` | Redis: `WeChatPayProperties.codeUrlRedisKey` / `AliPayProperties.codeUrlRedisKey` + orderId |
| 支付回调（异步通知） | 微信 / 支付宝主动 POST 通知支付结果，校验签名后业务方处理 | `PayBll#wechatPayCallbackHandle`（验签解密）|
| 查询订单状态 | 主动拉模式，向第三方查最新支付状态 | `WeChatPay#queryOrderStatus`, `Alipay#queryOrderStatus` |
| 关闭订单 | 主动调用第三方关闭交易 | `WeChatPay#closeOrder`, `Alipay#closeOrder` |

> ⚠️ `PayBll` / `WeChatPay` / `WeChatPayRSAConfig` 当前类注解被 `//@Component` 注释，**支付模块在当前版本未被 Spring 装配**（实际接入路径以 `replay-order` 内的支付实现为准）。本文件仍记录其支付状态码语义，因为 `QueryOrderStatus` 是 Feign / VO 契约一部分。

### 2.2 存储域（Storage / Upload）

| 概念 | 定义 | 主要类 |
|---|---|---|
| Qiniu Upload Token | 七牛云上传临时凭证，前端直传 OSS 用 | `QiNiuOssUtils#getTempToken` |
| Qiniu Download URL | 带签名带过期时间（默认 3 天）的私有空间下载链接 | `QiNiuOssUtils#getDownloadUrl` |
| VOD Upload Sign | 腾讯云点播上传签名（由 `replay-common.TencentVodUtils` 生成，replay-third 仅 Controller 暴露） | `TencentVodController` |
| COS 文件记录 | 点赞问答上传的文件元数据落库（contextId / fileName / askCount / thumbState） | `CosThumbsFileEntity` |
| TableStore Barrage | 弹幕数据存储于阿里云 TableStore（NoSQL），不在 MySQL | `BarrageBo`, `TableStoreBll` |
| TableStore AnchorUser | 主播侧粉丝快照（新老用户判定 + 等级范围） | `AnchorUserBo` |

### 2.3 AI 域（LLM Integration）

| 概念 | 定义 | 主要类 |
|---|---|---|
| AI Model | `tb_ai_model` 的一条配置，含 `modelCode` / `resourceType` / `endpointId` / `apiKey` / 上下文与窗口大小 | `AiModelEntity` |
| Resource Type | 模型厂商类型：`0=豆包(Ark)` / `1=通义(DashScope)` / `2=DeepSeek` | `AiModelEntity.resourceType` + `ModelFactoryUtils.getAiModel` |
| Use Type | 模型用途：`0=分析内容` / `1=数据截图视觉理解` | `AiModelEntity.useType` |
| Ark 临时 Token | 豆包通过 `volcengine-java-sdk-ark-runtime` 申请的会话级 token，C# / 前端直接调用大模型用 | `ZiJieUtils#getArkTempTokenByModel` |
| 上下文缓存（Context） | 豆包 SESSION 模式：上下文 ID 缓存 1 小时，节省 token | `ZiJieUtils#douBaoAI` |
| Chat Completion | 单轮 / 流式聊天补全 | `AiModel#chatCompletion` / `chatCompletionStream` |
| AI Token 使用记录 | 每次调用回写到 replay-order `tb_ai_token_use_rec` 用于计费 | `AiApi#toAiTokenUseRec` |
| 算力消费倍数 | `consumeMultiple` 决定不同模型按几倍算力扣费 | `AiModelEntity.consumeMultiple` |
| 模型选择字典 | `client_ai_model`（客户端选项）/ `client_ai_model_default`（默认）/ `diagnosis_ai_model_list`（诊断可选列表）/ `use_model_way` | DictData，由 `dictDataFeign` 拉取 |

### 2.4 短信域（SMS）

| 概念 | 定义 | 主要类 |
|---|---|---|
| SmsService | 统一短信抽象，对外只暴露 `send(templateId, mobile, params, providerName)` | `SmsService` / `SmsServiceImpl` |
| Sms Provider | 抽象提供商：阿里云（`ali`）/ 联麓（`lianlu`） | `AbstractSmsProvider`, `AliSmsProvider`, `LianLuSmsProvider` |
| 模板 ID（统一） | 业务层使用统一逻辑模板名（如 `verification_code` / `start_recording` / `end_recording` / `account_binding`），运行期通过字典 `unified_providers_template_code` 映射到各厂商真实 templateCode | `DictDataFeign.dictDataByLabel("unified_providers_template_code", templateId)` |
| Fallback 切换 | 默认 provider 发失败 → 按 `sms_service_providers` 字典顺序自动 fallback，递归上限 10 | `SmsServiceImpl#send`（cycleCount 限制） |
| Sms Enabled 开关 | 通过 `system_kv.sms_enabled = "1"` 控制总开关，默认 enabled | `SmsConfig#isEnabled` |
| Sms Result | 统一返回结构 `{success, code, message, requestId, count, illegalMobiles}` | `SmsResult`（在 replay-generic） |

### 2.5 第三方数据域（Chanmama 蝉妈妈）

| 概念 | 定义 | 主要类 |
|---|---|---|
| Chanmama Account | 第三方数据平台账号池，每账号有日查询次数上限 | `ChanmamaAccountEntity` |
| Chanmama Token | 5 分钟刷新一次的登录 token，存 Redis `tokenRedisKey + accountId` | `ThirdDataUtils#refreshChanmamaToken`, `ChanmamaScheduledTasks` |
| 改版查询（Revision Query） | 异步查询主播改版数据，回调 `chanmamaProperties.callbackUrl` | `ThirdDataUtils#sendRevisionQuery` |
| 相似主播查询 | 异步查询相似主播，回调机制同上 | `ThirdDataUtils#sendSimilarAnchorQuery` |
| 第三方榜单（Sales Ranking） | 按行业取销售榜单 | `ThirdDataUtils#getSalesRanking` |
| 余额耗尽（ChanmamaResponseCodeEnum） | 第三方平台返回的状态码枚举 | `ChanmamaResponseCodeEnum`（在 replay-generic） |

### 2.6 代理 IP 域（Shenlong 神龙）

| 概念 | 定义 | 主要类 |
|---|---|---|
| Proxy IP | `tb_proxy_ip` 一条记录 = 一条神龙代理提取链接，含账号、密码、剩余数量、有效期、激活状态、时效类型（短效 / 长效） | `ProxyIpEntity` |
| Proxy IP Record | `tb_proxy_ip_record` 一条 = 一次真实分配给用户的 IP，含 `extractDate` / `validityDate` / `ipEffectiveTime` | `ProxyIpRecordEntity` |
| User 提取限流 | 按用户当日已用次数 vs `proxyIp.dayUseNum` 判定 | `ProxyIpBll#getProxyIp` |
| 钉钉告警 | 短效剩余 10000/5000/2000、长效剩余 5000/3000/1000 触发预警 | `ProxyIpBll#dingDingWarn` |
| Redis 缓存 | `shenlong.proxy-ip-user-redis-key-prefix + validityType + userId`，TTL = 有效期 - 1 分钟 | `ProxyIpBll#getProxyIp` |
| 分布式锁 | 整个分配过程加 `@CustomRedissonLock(key = "'get_proxy_ip_lock'")` | `ProxyIpBll#getProxyIp` |

### 2.7 语音识别配额域（Tencent ASR）

| 概念 | 定义 | 主要类 |
|---|---|---|
| QPS 余量 | 每个腾讯 secretId 的并发额度，存 Redis；初始化时由 `ThirdStartInit` 注入 | `ThirdStartInit#init` |
| QPS 借用 / 归还 | 借出 token 前 -1，调用完成后 +1（`addSurplus`）；过期补回逻辑当前被注释 | `AudioDiscernController#addSurplus`, `RedisTimeoutThirdListener`（已注释停用） |
| Sentence Token | 一句话识别（`asr:SentenceRecognition`）临时凭证 | `AudioDiscernController#getTempToken` |
| Rec Token | 长音频识别（`asr:CreateRecTask`）临时凭证 | `AudioDiscernController#getRecTempToken` |

### 2.8 弹幕域（TableStore）

| 概念 | 定义 | 主要类 |
|---|---|---|
| Barrage | 单条弹幕，主键复合：`(tenantId, userId, batchNumber, msgId)`；含发送人昵称、等级、粉丝团等级、内容、记录时间、是否福袋 | `BarrageBo`, `barrageTableName` |
| Anchor-User Snapshot | (tenantId, secUid, nickName) 维度的"是否新粉"快照，决定弹幕里 `isNew` 字段 | `AnchorUserBo`, `anchorUserTableName` |
| 二级索引查询 | 通过主键范围 + 过滤条件查询（`queryDanMuData`） | `TableStoreBll#queryDanMuData` |
| 多元索引 SQL 查询 | TableStore 的 SQL 接口，按昵称/内容/等级等过滤 | `TableStoreBll#queryDanMuSearchData` |
| 弹幕埋点 | 把弹幕按 audioAnalysis 段落时间窗聚合成 `barrageNum` | `TableStoreBll#getBarrageDataList` |
| 昵称脱敏 | 抖音 `x***y` 形式昵称（正则 `.\\*{3,5}`）按 `nickName + "$" + level` 维度去重 | `TableStoreBll#isDesensitization` |

### 2.9 钉钉告警域

| 概念 | 定义 | 主要类 |
|---|---|---|
| DingTalk Webhook | 钉钉机器人 Webhook URL；非 `prod` profile 跳过推送 | `DingDingUtils` |
| Text Message / Markdown Message | 两种推送格式 | `sendTextMessage` / `sendMarkdownMessage` |

### 2.10 企业后台域（Governance）

| 概念 | 定义 | 主要类 |
|---|---|---|
| Employee Open Bind / Unbind | 子账号绑定 / 解绑通知企业后台 | `GovernanceEmployeeServiceImpl` |
| Tenant Status | 批量查询租户状态 | `GovernanceTenantServiceImpl` |
| LiveRoom Service | 直播间相关企业后台接口 | `GovernanceLiveRoomServiceImpl` |
| HTTP 客户端 | `GovernanceHttpServer`（来自 replay-common），统一封装鉴权与 baseUrl | `replay-common.http.governance.GovernanceHttpServer` |

---

## 三、关键状态机

### 3.1 支付状态（`QueryOrderStatus.status`）

| status | 含义 | 微信 TradeState 映射 | 支付宝 tradeStatus 映射 |
|---|---|---|---|
| 0 | 支付成功 | SUCCESS | TRADE_SUCCESS |
| 1 | 已退款 | REFUND | — |
| 2 | 未支付 | NOTPAY | WAIT_BUYER_PAY |
| 3 | 已关闭 | CLOSED | TRADE_CLOSED / TRADE_FINISHED |
| 4 | 已撤销 | REVOKED | — |
| 5 | 用户支付中 | USERPAYING | — |
| 6 | 支付失败 | PAYERROR | — |

> 来源：`WeChatPay#queryOrderStatus` + `Alipay#queryOrderStatus`。注意 1（退款）仅微信路径出现；支付宝 `ACQ.TRADE_NOT_EXIST` → status=2（视作未支付）。

### 3.2 SMS 发送状态码

| code | 含义 |
|---|---|
| `0` / OK | 成功 |
| `400` | 参数错误（手机号、模板 ID、列表为空、手机号格式错误） |
| `500` | 内部异常（厂商 SDK 抛错、provider 不存在） |
| `501` | 全部 provider 都失败 |
| `502` | 系统未配置该 templateId（字典 `unified_providers_template_code` 未命中） |
| `503` | 字典命中但具体厂商 code 没配置（`getCode()` 返回的 key 在 JSON 里缺失） |

### 3.3 AI 模型分支（`resourceType`）

```
resourceType == 0 → 豆包 / 火山 Ark → DoubaoAiModelImpl
resourceType == 1 → 阿里通义 DashScope → TongyiAiModelImpl
resourceType == 2 → DeepSeek          → DeepSeekAiModelImpl
其他              → RRException("未知厂商类型")
```

### 3.4 Proxy IP 时效类型 / 钉钉预警阈值

| validityType | 时效 | 预警阈值（剩余数量） |
|---|---|---|
| 0 | 短效（一次性） | 10000 / 5000 / 2000 |
| 1 | 长效（按分钟有效期） | 5000 / 3000 / 1000 |

---

## 四、关键业务规则

### 4.1 支付幂等 & 二维码复用

- 同一 `orderId` 再次调用 `getPayCode`，命中 Redis（`codeUrlRedisKey + orderId`）→ 直接返回历史 `code_url`，不重新下单。
- 二维码 TTL = `common.order-timeout-minutes`（系统级订单超时分钟数）。
- 微信 / 支付宝**回调签名**必须验签：`PayBll#wechatPayCallbackHandle` 用 `NotificationParser` 验签解密，校验失败抛异常。

### 4.2 SMS Fallback 与限流

- 全局开关：`system_kv.sms_enabled != "1"` → 直接返回 fail（400）。
- 默认 provider：`sms_service_providers` 字典的第一项启用状态条目；无配置 → 兜底 `ali`。
- Fallback 链：按字典顺序递归切换，硬上限 `cycleCount > 10` 终止。
- 模板 ID 双层映射：业务调用方传统一 templateId（如 `verification_code`）→ 经字典 `unified_providers_template_code.value`（JSON）按 provider code（`ali` / `lianlu`）取真实厂商 templateCode。

### 4.3 AI 调用安全 & 计费

- C# 客户端 / 前端**直连大模型**需通过 `getAnalysisTempToken` 获取临时 token；只豆包（resourceType=0）支持火山 Ark 临时 token；DeepSeek（resourceType=2）回退到 modelId=`aiModelInfo.id`（不暴露真 apiKey）。
- 临时 token TTL = `volcengineProperties.tempTokenSaveTime - 120 秒`（提前 2 分钟过期防边界）。
- 每次后端 `chatCompletion` 后必须 `aiTokenUseRecordFeign.saveAiTokenUseRec(...)` 落 `tb_ai_token_use_rec`（在 replay-order）做算力计费。
- 上下文缓存 TTL = 3600 秒（`ZiJieUtils.expireTime`），命中后只发提示词不发原文，省 token。

### 4.4 Proxy IP 分配规则

- 分布式锁 `'get_proxy_ip_lock'` 全局串行（避免重复扣减剩余数）。
- 用户当天已用次数 ≥ `proxyIp.dayUseNum` → 不再分配（返回 R.ok() 空）。
- 已分配的 IP 写 Redis 后，**对同一 user + validityType** 复用直到过期；`forceUpdate=true` 时主动 evict。
- 神龙返回的 IP 包格式：`{"code":200,"data":[{"ip":"...","port":...,"expire":"..."}]}`。

### 4.5 蝉妈妈 Token 刷新

- 注册为 XXL-Job `refreshChanmamaToken`，每 5 分钟由调度器触发（注释里建议的 cron 为 `0/50 0/5 * * * ?`）。
- 失败重试：`com.jiuyu.framework.concurrent.Retry`（封装在 replay-common 或 framework）。

### 4.6 TableStore 弹幕去重 & SQL 防注入

- 上传弹幕前查 `AnchorUserBo`（按 nickName + tenantId + secUid + batchNumber），决定单条弹幕 `isNew`。
- `BarrageBo.sort`：福袋弹幕 sort=0 优先；其他按上传顺序自增。
- SQL 拼接全部经 `escapeSqlValue`，转义 `' " \ \b \n \r \t \0`，但**不是用 PreparedStatement**——本质是字符串模板，调用方传值需 trust。注意：`TableStoreBll#concatSqlWhere` 是 TableStore 的 SQL 查询接口（非 MySQL），但仍需关注注入风险。

---

## 五、敏感字段清单（**禁止入日志**）

| 字段 | 配置源 | 类型 | 来源类 |
|---|---|---|---|
| `third.tencent.pay.merchantId` / `apiKey` / `merchantSerialNumber` / `privateKeyPath` | application.yml（外部配置） | 微信商户密钥 | `WeChatPayProperties` |
| `third.ali.pay.privateKey` / `appCertPath` / `alipayPublicCertPath` / `rootCertPath` | application.yml | 支付宝商户密钥与证书 | `AliPayProperties` |
| `third.qiniu.oss.accessKey` / `secretKey` | application.yml | 七牛 AK/SK | `QiNiuProperties` |
| `third.ali.msg.accessKeyId` / `accessKeySecret` | application.yml | 阿里云短信 AK/SK | `AliMsgProperties` |
| `third.lian-lu.msg[*].appId` / `appSecret` / `mchId` | application.yml | 联麓短信 App 密钥 | `LianLuProperties` |
| `third.volcengine.ark.accessKeyId` / `secretAccessKey` | application.yml | 火山方舟 AK/SK | `VolcengineProperties` |
| `third.chanmama.username` / `chanmamaPassword` | DB（`tb_chanmama_account`）+ Redis 缓存 | 第三方平台账号密码 | `ChanmamaAccountEntity` |
| `third.ali.tablestore.accessKeyId` / `accessKeySecret` | application.yml | 阿里 TableStore AK/SK | `TableStoreProperties` |
| `third.ali.ding-ding.proxy-ip-webhook-url` | application.yml | 钉钉机器人 webhook | `DingDingUtils` |
| `tb_proxy_ip.proxy_username` / `proxy_password` | DB | 神龙代理 IP 账号密码 | `ProxyIpEntity` |
| AI Model `apiKey`（在 `tb_ai_model`）| DB | 各厂商 LLM API Key | `AiModelEntity.apiKey` |
| Volcengine 静态 `apiKey`（硬编码在 `ZiJieUtils.java` 第 52 行 / `AiGeneralUtils.java` 第 45 行） | **源码硬编码（高危）** | 豆包 32k apiKey | `ZiJieUtils`, `AiGeneralUtils` |

> 配置统一前缀：`third.<provider>.<sub>`。运行时配置加载点：`@ConfigurationProperties` Bean（在 `replay-third/src/main/java/com/jiuyu/replay/third/constant/`）。
> 字典补充配置（运行期动态）：`unified_providers_template_code`、`sms_service_providers`、`client_ai_model`、`client_ai_model_default`、`diagnosis_ai_model_list`、`use_model_way`、`lianlu_template_params`（在 replay-system 字典）。

**Secret 来源汇总：**
- 大多数密钥：`application-*.yml`（外部配置），通过 `@ConfigurationProperties` 注入。
- 第三方平台账号 / AI Model apiKey：MySQL（`tb_chanmama_account`, `tb_ai_model`）。
- **历史遗留硬编码**：`ZiJieUtils.apiKey`（豆包 32k）、`AiGeneralUtils.apiKey`、`ZiJieUtils.model`（endpoint id）。建议后续重构迁移到 `tb_ai_model` 表 / 配置文件。

---

## 六、跨子域共用规则（jiuyu 项目级约束在本模块体现）

| 约束 | 在本模块体现位置 |
|---|---|
| 实体雪花 ID `IdType.INPUT` | 所有 entity 类（`AiModelEntity` / `CosThumbsFileEntity` / `ProxyIp*` / `LogAudio*` / `ChanmamaAccountEntity`） |
| 手动 `isDeleted` / `createDate` / `updateDate`（**禁用 `@TableLogic`**） | 同上 |
| Controller 返回 `R<T>` | 所有 controller（`replay-api.controller.third.*` 与 `replay-third.controller.*`） |
| Hard-coded value `RRException.create(...)` for ad-hoc errors | 多处（如 `WeChatPay#queryOrderStatus`、`ModelFactoryUtils#getAiModel`） |
| Feign 暴露在 replay-generic | `feign/third/*Feign.java` 接口；本模块的 `api/*Api.java` 是 impl |
| 跨模块调用通过 Feign | `dictDataFeign`（system）、`anchorVideoFeign`（words）、`sensitiveWordsFeign`（words）、`aiTokenUseRecordFeign`（order） |
| 时间戳手动 `new Date()` / `LocalDateTime.now()` | 实体注入时显式赋值（`ProxyIpBll`、`TableStoreBll#uploadDanMuData` 等） |
| 写方法 `@Transactional(rollbackFor = Exception.class)` | `ProxyIpBll#getProxyIp` 显式声明；其他 `*Bll` 大量缺失（历史遗留，新代码必须补） |

**注意（违反项目规范的历史代码）：**
- 全模块 DI 仍大量使用 `@Resource` 与 `@Autowired`，**未切换为构造器注入**。新增类应使用 `@AllArgsConstructor` + final 字段（部分新类如 `TableStoreController`、`ThirdAiModelController`、`AiApi` 已修正）。
- 多处 `*Bll` 缺 `@Transactional`，依赖 MyBatis-Plus 自动事务。新增写方法必须显式添加。

---

## 七、术语速查

| 术语 | 中文 | 等价说法 |
|---|---|---|
| Native Pay | 扫码支付 | code_url / QR Code Pay |
| Outbound | 出站 | 我方主动调用第三方 |
| Inbound | 入站 / 回调 | 第三方推消息给我方 |
| Idempotency | 幂等 | 同请求多次结果一致 |
| AK / SK | Access Key / Secret Key | 阿里云 / 火山 / 七牛通用术语 |
| Ark | 火山方舟 | 字节跳动 LLM 平台 |
| DashScope | 阿里灵积 | 通义系列模型入口 |
| TableStore | 阿里云表格存储 | OTS（旧称） |
| Chanmama | 蝉妈妈 | 抖音第三方数据平台 |
| Shenlong | 神龙 | 代理 IP 提供商 |
| Lianlu | 联麓 | 短信厂商 |
| Governance | 企业后台 | 内部企业管理服务 |
