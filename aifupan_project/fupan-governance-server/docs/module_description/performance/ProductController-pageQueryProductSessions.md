# 商品关联直播场次分页查询

## 接口信息

| 项目 | 说明 |
|-----|------|
| 接口路径 | `POST /api/governance/performance/product/sessions` |
| 控制器 | ProductController |
| 方法 | pageQueryProductSessions |
| 认证方式 | @RequiredLogin（登录令牌） |

## 接口描述

查询指定商品在时间范围内关联的直播场次列表，用于商品排行弹窗展示。
返回每个场次的主播信息、场次基础数据、商品销售数据（含退款金额）。

## 请求参数

### ProductSessionRequest

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| productId | String | 是 | 商品ID |
| startDate | LocalDate | 是 | 开始日期（含） |
| endDate | LocalDate | 是 | 结束日期（含） |
| sortBy | String | 否 | 排序字段，可选值：`startTime`、`duration`、`quantity`、`salesAmount`、`viewCount`、`refundAmount`，默认 `startTime` |
| sortOrder | String | 否 | 排序方向：`asc` / `desc`，默认 `desc` |
| page | Integer | 否 | 页码，默认 1 |
| limit | Integer | 否 | 每页条数，默认 10 |

> `tenantId` 由服务端从登录用户上下文中自动注入，无需前端传递。

## 业务逻辑

1. 从登录用户上下文中获取 `tenantId` 并注入请求对象
2. 对 `sortBy` 进行白名单校验，非法值回退为默认值 `startTime`
3. 对 `sortOrder` 进行白名单校验，非法值回退为 `desc`
4. 分页查询 `session_product` 表，关联 `live_session`、`live_room` 表
5. 按日期区间过滤（`DATE(ls.start_time) BETWEEN startDate AND endDate`）
6. 返回空数据时直接返回空分页对象

## 响应

### ProductSessionResponse

| 字段 | 类型 | 说明 |
|-----|------|------|
| sessionId | Long | 场次ID |
| anchorAvatar | String | 主播头像URL |
| anchorName | String | 主播名称 |
| startTime | LocalDateTime | 开播时间 |
| duration | Integer | 场次时长（秒），null时返回0 |
| quantity | Integer | 商品销量，null时返回0 |
| salesAmount | BigDecimal | 销售额（元），null时返回0 |
| refundAmount | BigDecimal | 退款金额（元），null时返回0 |
| viewCount | Integer | 场观，null时返回0 |

### 响应示例

```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "sessionId": 10001,
        "anchorAvatar": "https://example.com/avatar.jpg",
        "anchorName": "主播A",
        "startTime": "2024-07-01T20:00:00",
        "duration": 7200,
        "quantity": 150,
        "salesAmount": 29850.00,
        "refundAmount": 1500.00,
        "viewCount": 32000
      }
    ],
    "totalPages": 3,
    "pageSize": 10,
    "pageNum": 1,
    "total": 25
  }
}
```

## 关联表

| 表名 | 说明 |
|-----|------|
| session_product | 场次商品关联表，含销量、销售额、退款金额 |
| live_session | 直播场次表，含开播时间、时长、场观 |
| live_room | 直播间表，含主播头像、主播名称 |

## 请求示例

```json
POST /api/governance/performance/product/sessions

{
  "productId": "product_001",
  "startDate": "2024-07-01",
  "endDate": "2024-07-31",
  "sortBy": "salesAmount",
  "sortOrder": "desc",
  "page": 1,
  "limit": 10
}
```
