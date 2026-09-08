# 销售智能体（SalesCoach）联调文档 — yz 环境

> 适用对象：销售智能体（SalesCoach）侧研发 / 联调同学
> 服务端：直播复盘后端服务 replay-api（`feature/26-06-3-crm-agent`）
> 环境：yz（预发布）
> 状态：本文档随 `feature/26-06-3-crm-agent` 分支落地，最新一次更新覆盖 **客户行为信号查询接口（试用期使用深度）**

---

## 1. 环境信息

| 项 | yz 环境 |
|---|---|
| API Base URL | `https://api-yz.aifupan.com.cn` |
| 通信协议 | HTTPS |
| Content-Type | `application/json;charset=UTF-8` |
| 时区 | `GMT+8`（Asia/Shanghai） |
| 时间格式 | `yyyy-MM-dd HH:mm:ss`（除特别说明） |
| 服务端口 | 6606（由网关转发，调用方无需关心） |

> 生产环境请勿使用本文档凭证；正式上线前会在专用对接群下发 prod 凭证。

---

## 2. 鉴权机制

CRM 集成接口统一走 **API-Key 鉴权**（请求头），**不需要签名**（CrmIntegrationController 未启用 `@FeatureSignature`，签名拦截器只校验 `signature.include-paths` 中的路径，本批接口不在其中）。

### 2.1 请求头

| Header | 必填 | yz 环境取值 | 说明 |
|---|---|---|---|
| `x-jiuyu-client-id` | 是 | `salescoach-agent` | 销售智能体应用 ID |
| `api-key` | 是 | `3f834d2c-3f4a-4b91-93a4-8a6ea569a1d0` | yz 环境 API-Key |
| `Content-Type` | 是 | `application/json;charset=UTF-8` | POST 接口必带 |

> 鉴权失败返回 HTTP 200，但 body 形如 `{"code":403,"message":"警告：请提供有效的api-key"}`。

### 2.2 校验流程（服务端侧）

1. 路由命中 `@APIKey` 注解（CrmIntegrationController 类级）。
2. 取 `x-jiuyu-client-id` 作为 appId，取 `api-key` 作为 secret。
3. 与 yz `application-yz.yml > jiuyu.oauth.client.api-key-secret` 中的 `salescoach-agent` 对应密钥逐字符比对。
4. 不一致 / 缺失 → 403。

---

## 3. 通用约定

### 3.1 统一返回结构 R\<T\>

```json
{
  "code": 0,
  "msg": "success",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | int | 0 表示成功；非 0 表示业务/鉴权失败 |
| `msg` | string | 业务消息 / 错误描述 |
| `data` | object/array/null | 业务数据体，每个接口的结构见各节 |

### 3.2 常见业务错误码

| code | msg | 含义 | 触发条件 |
|---|---|---|---|
| `0` | `success` | 成功 | 正常 |
| `403` | `警告：请提供有效的api-key` | 鉴权失败 | header 缺失或不匹配 |
| 非 0 | `phones不能为空` | 入参缺失 | phones 列表为空 |
| 非 0 | `phones数量不能超过50` | 批量超限 | 入参 phones > 50 |
| 非 0 | `phone不能为空` | 单手机号入参缺失 | 路径/body 中 phone 为空 |
| 非 0 | `CUSTOMER_NOT_FOUND` | 客户未命中 | 该手机号在 fupan 系统中不存在（写入类接口） |
| 非 0 | `leadType不合法` | 客户类型枚举错误 | 见 §4.3 leadType 枚举 |
| 非 0 | `phone/eventId/profileId/profileJson/updatedAt/source不能为空` | 字段缺失 | 必填字段未传 |

### 3.3 批量入参规则

- 单次批量入参 `phones` 最多 **50** 条。
- 入参 `phones` 服务端会做 `trim + 去空 + 去重`，**返回顺序与归一化后入参顺序一致**。
- 重复手机号去重后只返回 1 条。

### 3.4 查询类接口的"未命中"语义（重要）

**批量聚合查询 / 行为信号查询** 接口：未命中客户的手机号，**不会**让整个请求失败，而是**逐条降级**：
- `exists = false`
- 计数字段填 0，布尔字段填 `false`，字符串字段为 `null`
- `batchAggregateQuery` 中还会在该条 item 上带 `missingReason = "CUSTOMER_NOT_FOUND"`

**写入类接口（profileSync / aiProfile.upsert / stageEvent）**：未命中客户 → 整个请求返回 `R.error("CUSTOMER_NOT_FOUND")`，不写库。

---

## 4. 接口清单

> 所有接口均以 yz Base URL 为前缀：`https://api-yz.aifupan.com.cn`

| # | Method | Path | 说明 | 本分支状态 |
|---|---|---|---|---|
| 1 | POST | `/internal/crm/customer/batch-aggregate-query` | 批量聚合查询客户（含销售 + 订单） | 既有 |
| 2 | GET | `/internal/crm/order/query-by-phone` | 按手机号查询订单 | 既有 |
| 3 | POST | `/internal/crm/customer/profile-sync` | 同步客户画像字段（意向 / 客户类型） | 既有 |
| 4 | POST | `/internal/crm/ai-profile/upsert` | 写入 AI 画像快照 | 既有 |
| 5 | POST | `/internal/crm/customer/stage-event` | 回传客户阶段事件 + 触发销售跟进快照 | 既有 |
| 6 | POST | `/internal/crm/customer/behavior-signals` | **批量查询客户行为信号（试用期使用深度）** | **本分支新增** |

---

### 4.1 批量聚合查询客户信息

- **Method / Path**：`POST /internal/crm/customer/batch-aggregate-query`
- **用途**：批量取回客户基本信息 + 跟进销售 + 历史订单，给销售智能体侧做客户画像底座。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `phones` | List\<String\> | 是 | 手机号列表，1~50 条 |

**响应体（data）**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `customers` | List\<CustomerItem\> | 按入参 phones 去空/去重后顺序回填 |

**CustomerItem**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `phone` | String | 回显入参手机号 |
| `exists` | Boolean | 是否命中客户 |
| `missingReason` | String | 未命中时为 `CUSTOMER_NOT_FOUND`，否则为 `null` |
| `userId` | Long | 客户 ID（fupan 内部） |
| `customerName` | String | 客户昵称 |
| `sales` | SalesInfoVo | 跟进销售对象（含销售 ID / 昵称 / 手机号 / 二维码等，详见 Swagger） |
| `orders` | List\<OrderInfoVo\> | 历史订单列表（含订单详情 / 支付信息 / 佣金等，详见 Swagger） |

**示例**：

```bash
curl -X POST 'https://api-yz.aifupan.com.cn/internal/crm/customer/batch-aggregate-query' \
  -H 'x-jiuyu-client-id: salescoach-agent' \
  -H 'api-key: 3f834d2c-3f4a-4b91-93a4-8a6ea569a1d0' \
  -H 'Content-Type: application/json;charset=UTF-8' \
  -d '{
    "phones": ["13800138000", "13900139000"]
  }'
```

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "customers": [
      {
        "phone": "13800138000",
        "exists": true,
        "userId": 4323112373956116480,
        "customerName": "示例客户",
        "sales": { "...": "见 SalesInfoVo" },
        "orders": [ { "...": "见 OrderInfoVo" } ]
      },
      {
        "phone": "13900139000",
        "exists": false,
        "missingReason": "CUSTOMER_NOT_FOUND"
      }
    ]
  }
}
```

---

### 4.2 按手机号查询订单

- **Method / Path**：`GET /internal/crm/order/query-by-phone?phone=<phone>`
- **用途**：销售智能体侧单条客户穿透查询订单详情。

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `phone` | String | 是 | 客户手机号 |

**响应体（data）**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `phone` | String | 回显入参 |
| `exists` | Boolean | 是否命中客户 |
| `missingReason` | String | 未命中时为 `CUSTOMER_NOT_FOUND` |
| `userId` | Long | 客户 ID |
| `orders` | List\<OrderInfoVo\> | 客户订单列表 |

**示例**：

```bash
curl -X GET 'https://api-yz.aifupan.com.cn/internal/crm/order/query-by-phone?phone=13800138000' \
  -H 'x-jiuyu-client-id: salescoach-agent' \
  -H 'api-key: 3f834d2c-3f4a-4b91-93a4-8a6ea569a1d0'
```

---

### 4.3 同步客户画像字段

- **Method / Path**：`POST /internal/crm/customer/profile-sync`
- **用途**：销售智能体回写客户的"成交意向 / 客户类型"到 fupan 用户明细表。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `phone` | String | 是 | 客户手机号 |
| `source` | String | 是 | 数据来源标识（如 `salescoach-agent`） |
| `updatedAt` | String | 是 | 业务更新时间（`yyyy-MM-dd HH:mm:ss`） |
| `dealIntent` | String | 否 | 成交意向（自由文本） |
| `leadType` | String | 否 | 客户类型枚举 |

**`leadType` 枚举映射**（服务端会落库为整数 `userBelongType`）：

| 入参字符串 | 落库 userBelongType |
|---|---|
| `个人` | 0 |
| `工作室` | 1 |
| `企业` / `品牌旗舰及定制` | 2 |
| 其他非空值 | 报错 `leadType不合法` |

**响应体（data）**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | Long | 命中的客户 ID |
| `phone` | String | 回显 |
| `source` | String | 回显 |
| `updatedAt` | String | 回显 |
| `userAmbition` | String | 已写入的成交意向 |
| `userBelongType` | Integer | 已写入的客户类型枚举值 |
| `updatedFields` | List\<String\> | 固定 `["userAmbition", "userBelongType"]` |

**示例**：

```bash
curl -X POST 'https://api-yz.aifupan.com.cn/internal/crm/customer/profile-sync' \
  -H 'x-jiuyu-client-id: salescoach-agent' \
  -H 'api-key: 3f834d2c-3f4a-4b91-93a4-8a6ea569a1d0' \
  -H 'Content-Type: application/json;charset=UTF-8' \
  -d '{
    "phone": "13800138000",
    "source": "salescoach-agent",
    "updatedAt": "2026-06-03 14:30:00",
    "dealIntent": "已多次询问报价，6月有签约预算",
    "leadType": "企业"
  }'
```

---

### 4.4 写入 AI 画像

- **Method / Path**：`POST /internal/crm/ai-profile/upsert`
- **用途**：销售智能体定期/事件驱动地把生成的 AI 画像 JSON 快照写入 fupan。
- **幂等键**：`(userId, profileId)`，重复 `profileId` 触发更新而非插入。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `phone` | String | 是 | 客户手机号 |
| `profileId` | String | 是 | 画像版本标识（由销售智能体侧生成，作幂等键） |
| `source` | String | 否 | 来源（默认空串） |
| `updatedAt` | String | 否 | 业务更新时间 |
| `profileJson` | String | 是 | AI 画像完整 JSON（**JSON 字符串**，不是嵌套对象） |
| `summary` | String | 否 | 画像摘要 |

**响应体（data）**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | Long | 客户 ID |
| `phone` | String | 回显 |
| `source` | String | 已写入的 source |
| `updatedAt` | String | 回显 |
| `profileId` | String | 已写入的 profileId |

---

### 4.5 回传客户阶段事件

- **Method / Path**：`POST /internal/crm/customer/stage-event`
- **用途**：销售智能体把客户的"跟进阶段"事件回传给 fupan；当 `stageCode` 命中白名单时同步刷新销售跟进快照。
- **幂等键**：`(userId, eventId)`。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `phone` | String | 是 | 客户手机号 |
| `eventId` | String | 是 | 事件唯一标识（销售智能体侧生成，作幂等键） |
| `source` | String | 否 | 来源 |
| `occurredAt` | String | 是 | 事件发生时间（`yyyy-MM-dd HH:mm:ss`） |
| `stageCode` | String | 否 | 阶段编码（见下方白名单） |
| `stageLabel` | String | 否 | 阶段名称 |
| `confidence` | BigDecimal | 否 | 置信度 0~1 |
| `summary` | String | 否 | 事件摘要 |
| `factsJson` | String | 否 | 关键事实 JSON 字符串 |
| `rawJson` | String | 否 | 原始扩展 JSON 字符串 |

**`stageCode` 白名单（命中则同时刷新销售跟进快照 `accordingStatus`）**：

| stageCode | accordingStatus（落库） | 含义 |
|---|---|---|
| `FOLLOW_UP_DONE` | 1 | 已完成跟进 |
| `WAIT_NEXT_CONTACT` | 2 | 待下次联系 |
| 其他/空 | 不刷新销售快照 | 仅入事件表 |

**响应体（data）**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | Long | 客户 ID |
| `phone` | String | 回显 |
| `eventId` | String | 回显 |
| `businessUpdated` | Boolean | true=本次刷新了销售跟进快照；false=仅入事件表 |

---

### 4.6 批量查询客户行为信号（试用期使用深度）**【本分支新增】**

- **Method / Path**：`POST /internal/crm/customer/behavior-signals`
- **用途**：销售智能体在"试用期使用深度"场景下，按手机号批量拉取**团队维度**的产品使用强度信号，用于评估客户健康度 / 转化优先级。
- **聚合粒度**：以手机号定位用户 → 用户的当前所属团队（租户）→ 按租户维度聚合下述 12 个指标。
- **未命中行为**：参见 §3.4。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `phones` | List\<String\> | 是 | 手机号列表，1~50 条 |

**响应体（data）**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `signals` | List\<BehaviorSignalItem\> | 按入参顺序回填 |

**BehaviorSignalItem 字段表**：

| 字段 | 类型 | 含义 | 数据口径 |
|---|---|---|---|
| `phone` | String | 回显入参 | — |
| `exists` | Boolean | 是否命中客户 | 命中 fupan 用户表 |
| `customerUnitSize` | Integer | 客户团队账号总数 | `tb_user.active_tenant_id = <tenantId>` 且未删除的 user 计数 |
| `addedAnchorCount` | Integer | 团队添加的直播间账号总数 | `tb_anchor_url_user` 中租户下未删除的主播账号计数 |
| `ownAnchorCount` | Integer | 团队"自有账号"数 | 上一指标中 `account_type = 0`（自由账号）的计数 |
| `hasUsedOpAssistant` | Boolean | 团队是否使用过运营助手 | `tb_ai_token_use_record` 中租户下 `assistant_type = 0` 的任一记录存在即 true |
| `hasUsedCompareReplay` | Boolean | 团队是否使用过对比复盘 | `tb_sync_contrast` 中租户下存在任一未删除记录即 true |
| `serviceDurationHours` | Integer | 服务时长（小时，**仅主账号**） | 团队主账号注册时间至今的小时差 |
| `aiAnalysisTimeConsumed` | Long | 团队累计 AI 语音分析时长（**秒**） | `tb_anchor_video` 租户下 `analysis_status = 2`（已完成）且未删除视频的 duration 求和 |
| `totalAiTokensUsed` | Long | 团队历史累计算力（tokens） | `tb_ai_token_use_record` 中租户下 `total_tokens` 求和（无时间过滤） |
| `recentAiTokensUsed30d` | Long | 团队近 30 个自然日算力（tokens） | 时间窗口：今日 0 点起回退 29 天，至今日 0 点起的累计 |
| `recentAiTokensUsed7d` | Long | 团队近 7 个自然日算力（tokens） | 时间窗口：今日 0 点起回退 6 天，至今日 0 点起的累计 |

> **自然日窗口口径**：以服务端时区（Asia/Shanghai）今日 0 点为窗口结束点的"含今日"自然日。例：2026-06-03 18:00 查询 `recentAiTokensUsed7d` → 起点 = 2026-05-28 00:00:00。

> **未命中 / 无租户场景**（同 §3.4）：当 `exists = false` 或用户无所属租户时，所有计数字段填 0、所有布尔字段填 `false`，但仍会回填一行。

**请求示例**：

```bash
curl -X POST 'https://api-yz.aifupan.com.cn/internal/crm/customer/behavior-signals' \
  -H 'x-jiuyu-client-id: salescoach-agent' \
  -H 'api-key: 3f834d2c-3f4a-4b91-93a4-8a6ea569a1d0' \
  -H 'Content-Type: application/json;charset=UTF-8' \
  -d '{
    "phones": ["13800138000", "13900139000"]
  }'
```

**响应示例**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "signals": [
      {
        "phone": "13800138000",
        "exists": true,
        "customerUnitSize": 5,
        "addedAnchorCount": 12,
        "ownAnchorCount": 8,
        "hasUsedOpAssistant": true,
        "hasUsedCompareReplay": false,
        "serviceDurationHours": 240,
        "aiAnalysisTimeConsumed": 18650,
        "totalAiTokensUsed": 1284563,
        "recentAiTokensUsed30d": 845210,
        "recentAiTokensUsed7d": 132480
      },
      {
        "phone": "13900139000",
        "exists": false,
        "customerUnitSize": 0,
        "addedAnchorCount": 0,
        "ownAnchorCount": 0,
        "hasUsedOpAssistant": false,
        "hasUsedCompareReplay": false,
        "serviceDurationHours": 0,
        "aiAnalysisTimeConsumed": 0,
        "totalAiTokensUsed": 0,
        "recentAiTokensUsed30d": 0,
        "recentAiTokensUsed7d": 0
      }
    ]
  }
}
```

---

## 5. 联调 Checklist

请按顺序逐项过一遍 yz 环境对接：

- [ ] **§2 鉴权**：两个 header 都已带，appId / api-key 与本文 yz 取值一致。
- [ ] **§4.1 批量聚合查询**：用 1 个已知存在的手机号 + 1 个不存在手机号，验证 `exists` / `missingReason` 行为。
- [ ] **§4.2 按手机号查订单**：验证已知存在手机号有 `orders` 列表。
- [ ] **§4.3 画像同步**：分别用 `个人 / 工作室 / 企业` 三种 leadType 验证落库；故意传 `企业XYZ` 验证 `leadType不合法`。
- [ ] **§4.4 AI 画像写入**：同一个 `profileId` 重复调用 2 次，确认是更新而非插入（`userId` / `profileId` 回显一致）。
- [ ] **§4.5 阶段事件**：分别用 `FOLLOW_UP_DONE` / `WAIT_NEXT_CONTACT` / `OTHER_CODE` 验证 `businessUpdated` 切换。
- [ ] **§4.6 行为信号**（重点）：
  - [ ] 单条用例：用一个已知活跃团队的主账号手机号，验证 12 个字段是否非零（特别是 `customerUnitSize >= 1` / `serviceDurationHours > 0`）。
  - [ ] 子账号用例：用同团队子账号手机号，验证 `customerUnitSize` 同主账号；`serviceDurationHours` **仍按主账号注册时间算**（不是子账号自己注册时间）。
  - [ ] 时间窗口用例：对同一个团队比较 `totalAiTokensUsed` >= `recentAiTokensUsed30d` >= `recentAiTokensUsed7d`。
  - [ ] 未命中用例：用一个不存在的手机号，验证返回行的所有数值字段为 0、布尔字段为 `false`、`exists = false`。
  - [ ] 边界用例：phones = 51 条 → `phones数量不能超过50`；phones = `[""]` → `phones不能为空`。

---

## 6. 联调建议 / 注意事项

1. **批量上限就是 50**，不要拼超过 50 的请求；如果业务上有更大批量需求，请提前同步以便评估服务端是否需要扩容或加分批策略。
2. **行为信号是团队（租户）维度聚合**，调用方传单个用户的手机号都会被映射到该用户当前所属团队（`tb_user.active_tenant_id`）；若用户在子团队间切换过，结果**反映的是当前团队**，不是历史团队。
3. **行为信号目前不带缓存**，单条聚合涉及 ≥7 个下游 Producer 调用，请避免高 QPS 轮询；正式上线前若有压测需求请提前沟通。
4. **时间字段都是字符串**（`yyyy-MM-dd HH:mm:ss`，GMT+8），不是时间戳。
5. **不返回 HTTP 4xx/5xx**：鉴权失败 / 业务异常都通过 `code` 字段表达，请不要按 HTTP 状态码切分支。
6. 出现 `CUSTOMER_NOT_FOUND` 时，请勿重复重试；先确认手机号格式（`trim` 后是否为纯数字、是否带国家码前缀）。

---

## 7. 联系人 / 反馈

- 后端：fupan-server 后端组（CrmIntegrationController / CrmIntegrationServiceImpl 负责人）
- 联调反馈：请把 requestId / phone / 请求时间 / 实际响应贴到对接群。
- 文档版本：随 `feature/26-06-3-crm-agent` 最新提交（commit `eb2d9147b feat(api): 添加CRM客户行为信号查询功能`）。
