# 商品关联分公司列表查询

## 接口信息

| 项目 | 说明 |
|-----|------|
| 接口路径 | `POST /api/governance/performance/product/companies` |
| 控制器 | ProductController |
| 方法 | queryProductCompanies |
| 认证方式 | @RequiredLogin（登录令牌） |

## 接口描述

查询指定商品在时间范围内关联的分公司列表及汇总销售数据，用于商品排行弹窗展示。
返回分公司维度的销量、销售额、退款金额、场观汇总，支持多字段排序。

## 请求参数

### ProductCompanyRequest

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| productId | String | 是 | 商品ID |
| startDate | LocalDate | 是 | 开始日期（含） |
| endDate | LocalDate | 是 | 结束日期（含） |
| sortBy | String | 否 | 排序字段，可选值：`salesAmount`、`quantity`、`viewCount`、`refundAmount`，默认 `salesAmount` |
| sortOrder | String | 否 | 排序方向：`asc` / `desc`，默认 `desc` |

> `tenantId` 由服务端从登录用户上下文中自动注入，无需前端传递。

## 业务逻辑

1. 从登录用户上下文中获取 `tenantId` 并注入请求对象
2. 对 `sortBy` 进行白名单校验，非法值回退为默认值 `salesAmount`
3. 对 `sortOrder` 进行白名单校验，非法值回退为 `desc`
4. 查询 `session_product` 表，按 `company_id` 分组聚合
5. 关联 `live_session` 表过滤日期区间，关联 `sub_company` 表获取分公司名称
6. `refundAmount` 以 `COALESCE(SUM(sp.refund_amount), 0)` 聚合，保证不返回 null
7. 返回空列表时直接返回空集合

## 响应

### ProductCompanyResponse

| 字段 | 类型 | 说明 |
|-----|------|------|
| companyId | Long | 分公司ID |
| companyName | String | 分公司名称 |
| quantity | Integer | 商品总销量，null时返回0 |
| salesAmount | BigDecimal | 总销售额（元），null时返回0 |
| refundAmount | BigDecimal | 总退款金额（元），null时返回0 |
| viewCount | Integer | 总场观，null时返回0 |

### 响应示例

```json
{
  "code": 200,
  "data": [
    {
      "companyId": 1001,
      "companyName": "上海分公司",
      "quantity": 520,
      "salesAmount": 98400.00,
      "refundAmount": 3200.00,
      "viewCount": 126000
    },
    {
      "companyId": 1002,
      "companyName": "北京分公司",
      "quantity": 310,
      "salesAmount": 62000.00,
      "refundAmount": 1800.00,
      "viewCount": 89000
    }
  ]
}
```

## 关联表

| 表名 | 说明 |
|-----|------|
| session_product | 场次商品关联表，含销量、销售额、退款金额，按分公司聚合 |
| live_session | 直播场次表，提供日期过滤条件和场观 |
| sub_company | 分公司信息表，提供分公司名称 |

## 请求示例

```json
POST /api/governance/performance/product/companies

{
  "productId": "product_001",
  "startDate": "2024-07-01",
  "endDate": "2024-07-31",
  "sortBy": "refundAmount",
  "sortOrder": "desc"
}
```
