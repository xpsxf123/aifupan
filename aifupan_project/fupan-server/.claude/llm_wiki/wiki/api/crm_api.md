# CRM API — 接口契约

> CRM 模块对外开放的内部集成接口。所有接口前缀 `/internal/crm`，由 `APIKeyInterceptor` 统一鉴权。

---

## 一、鉴权

所有 `/internal/crm/**` 接口通过 `@APIKey` 注解 + `APIKeyInterceptor` 鉴权：

| Header | 必填 | 说明 |
|---|---:|---|
| x-jiuyu-client-id | 是 | 固定值：`salescoach-agent` |
| api-key | 是 | 环境对应密钥 |
| Content-Type | POST 必填 | `application/json` |

鉴权失败返回 HTTP 200，`code=403`。

---

## 二、入站接口（SalesCoach → CRM）

### API-C01: 批量聚合查询客户信息

| 属性 | 值 |
|------|-----|
| Method | POST |
| Path | `/internal/crm/customer/batch-aggregate-query` |
| 说明 | 按手机号批量查询客户聚合信息（客户主体 + 销售归属 + 订单列表） |

**请求体** (`CrmBatchAggregateQueryBo`): `phones: List<String>`

**响应** (`CrmBatchAggregateQueryVo`): `customers: List<CrmCustomerAggregateItemVo>`，每项含 `phone`, `exists`, `userId`, `customerName`, `sales`, `orders`

---

### API-C02: 按手机号查询订单

| 属性 | 值 |
|------|-----|
| Method | GET |
| Path | `/internal/crm/order/query-by-phone` |
| 说明 | 按手机号查询客户的所有订单记录 |

**请求参数**: `phone: String`

**响应** (`CrmOrderQueryByPhoneVo`): 客户订单列表

---

### API-R01: 客户画像同步

| 属性 | 值 |
|------|-----|
| Method | POST |
| Path | `/internal/crm/customer/profile-sync` |
| 说明 | 同步客户扩展信息（行业、渠道、意向度、线索类型等） |

**请求体** (`CrmProfileSyncBo`): 手机号 + 客户扩展字段

**响应** (`CrmProfileSyncVo`): 同步结果

---

### API-R02: AI 画像写入

| 属性 | 值 |
|------|-----|
| Method | POST |
| Path | `/internal/crm/ai-profile/upsert` |
| 说明 | 写入/更新智能体生成的 AI 画像全量 JSON |

**请求体** (`CrmAiProfileUpsertBo`): `phone` → 换算 `userId` 后落库 `tb_crm_ai_profile`

**响应** (`CrmAiProfileUpsertVo`): 画像 ID

---

### API-R03: 阶段事件写入

| 属性 | 值 |
|------|-----|
| Method | POST |
| Path | `/internal/crm/customer/stage-event` |
| 说明 | 智能体回传客户阶段事件（关键事实、证据、阶段判断） |

**请求体** (`CrmStageEventSaveBo`): `phone`, `eventId`, `stageCode`, `stageLabel`, `confidence`, `summary`, `factsJson`

**响应** (`CrmStageEventInfoVo`): 事件 ID

---

## 三、出站能力（CRM → SalesCoach）

<!-- Sources: archive/20260514_crm_spec_d_order_event.md (harvested 2026-05-20) -->

### 订单变化事件推送

| 属性 | 值 |
|------|-----|
| 推送方式 | Spring Boot Event → `CrmOrderChangedEventListener` → HTTP POST |
| 目标 URL | `{crm.agent.base-url}/api/internal/agent/order/event` |
| 触发时机 | 订单创建、支付成功、续费、升级、退款、到期、取消 |
| 事件类型 | `ORDER_CREATED`, `PAY_SUCCESS`, `RENEWAL`, `UPGRADE`, `INCREMENT`, `EXPIRING`, `EXPIRED`, `REFUND`, `CANCELED` |

**推送体** (`CrmOrderEventPushBo`): `phone`, `eventType`, `orderId`, `productName`, `amount`, `isTrial`, `occurredAt`

**配置项** (`@ConfigurationProperties(prefix = "crm.agent")`):

| 配置 key | 说明 | 示例值 |
|---|---|---|
| `crm.agent.base-url` | SalesCoach Agent 服务基础 URL | `https://salescoach.ifupan.com` |
| `crm.agent.api-key` | 出站鉴权密钥，从环境变量 `CRM_AGENT_API_KEY` 注入 | — |
| `crm.agent.order-event-path` | 订单事件推送路径 | `/api/internal/agent/order/event` |

---

## API 路由表

| API (Method + Path) | Summary | Auth & Identity | Version |
|---|---|---|---|
| POST `/internal/crm/customer/batch-aggregate-query` | 批量聚合查询客户 | APIKey / salescoach-agent | v2.6 |
| GET `/internal/crm/order/query-by-phone` | 按手机号查订单 | APIKey / salescoach-agent | v2.6 |
| POST `/internal/crm/customer/profile-sync` | 客户画像同步 | APIKey / salescoach-agent | v2.6 |
| POST `/internal/crm/ai-profile/upsert` | AI 画像写入 | APIKey / salescoach-agent | v2.6 |
| POST `/internal/crm/customer/stage-event` | 阶段事件写入 | APIKey / salescoach-agent | v2.6 |
| Event `/api/internal/agent/order/event` | 订单事件出站推送 | APIKey | v2.6 |
