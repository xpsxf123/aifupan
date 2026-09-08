# 视频数据接收接口

## 接口信息

| 项目 | 说明 |
|-----|------|
| 接口路径 | `POST /api/governance/performance/video/receive` |
| 控制器 | LiveVideoController |
| 方法 | receiveVideo |
| 认证方式 | @GovernanceUser |

## 接口描述

接收爱复盘系统推送的视频数据和关联商品数据。

## 请求参数

### VideoPushRequest

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| tenantId | Long | 是 | 租户ID |
| batchNumber | String | 是 | 直播批次号 |
| videoId | String | 是 | 视频唯一标识 |
| hasPerformance | Integer | 否 | 业绩数据是否存在：0-丢失业绩数据，1-完好 |
| videoOssUrl | String | 否 | 视频文件OSS地址 |
| startTime | LocalDateTime | 是 | 视频开始时间 |
| endTime | LocalDateTime | 是 | 视频结束时间 |
| cumulativeView | Integer | 否 | 场观 |
| startCumulativeSales | BigDecimal | 否 | 开始累计销售额 |
| endCumulativeSales | BigDecimal | 否 | 结束累计销售额 |
| cumulativeRefund | BigDecimal | 否 | 退款 |
| investment | BigDecimal | 否 | 投放金额 |
| secUid | String | 是 | 主播secUid |
| products | List\<VideoProductRequest\> | 否 | 商品列表 |

### VideoProductRequest

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| id | Long | 是 | 商品ID（主键，用于判断新增或更新） |
| productId | String | 是 | 商品productId（爱复盘的商品ID） |
| title | String | 是 | 商品标题 |
| imageUri | String | 否 | 商品图片URL |
| marketPrice | BigDecimal | 否 | 到手价（元） |
| productBindTime | LocalDateTime | 否 | 直播间上架时间 |
| productDownTime | LocalDateTime | 否 | 直播间下架时间 |
| explainCnt | Integer | 否 | 讲解次数 |
| productShowUcnt | Long | 否 | 商品曝光人数 |
| productClickUcnt | Long | 否 | 商品点击人数 |
| productShowClickUcntRatio | BigDecimal | 否 | 曝光-点击转化率 |
| productShowPayUcntRatio | BigDecimal | 否 | 曝光-成交转化率 |
| productClickPayUcntRatio | BigDecimal | 否 | 点击-成交转化率 |
| gpm | BigDecimal | 否 | 商品千次曝光成交金额（元） |
| payAmt | BigDecimal | 否 | 累计成交金额（元） |
| avgMaxPayAmtMin | BigDecimal | 否 | 分钟最高成交金额（元） |
| payComboCnt | Long | 否 | 累计成交件数 |
| payCnt | Long | 否 | 累计成交订单数 |
| createCnt | Long | 否 | 创建订单数 |
| createPayUcntRatio | BigDecimal | 否 | 订单支付率 |
| payDepositPreOrderCnt | Long | 否 | 预售订单数 |
| presaleDepayDeamt | BigDecimal | 否 | 预售定金金额（元） |
| payDepositPreOrderAmt | BigDecimal | 否 | 预售全款金额（元） |
| refundCnt | Long | 否 | 退款订单数 |
| realRefundAmt | BigDecimal | 否 | 退款金额（元） |
| refundRate | BigDecimal | 否 | 退款率 |

## 业务逻辑

### 视频处理逻辑

```
视频不存在？
  └─ 是 → 新增视频，状态为「待处理」
  └─ 否 → 判断当前状态
           ├─ 待处理 → 更新视频数据
           ├─ 处理中 → 打日志，跳过保存
           └─ 处理完成/失败 → 对比关键指标
                              ├─ 有变化 → 更新数据，状态改为「待处理」
                              └─ 无变化 → 打日志，跳过保存
```

### 关键指标对比字段

- 场观（cumulativeView）
- 结束销售额（endCumulativeSales）
- 退款（cumulativeRefund）
- 投放金额（investment）

### 商品处理逻辑

用传过来的 `id` 判断：
- 存在 → 更新商品数据
- 不存在 → 新增商品数据

## 响应

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

## 关联表

| 表名 | 说明 |
|-----|------|
| live_video | 视频表 |
| video_product | 视频商品表 |

## 请求示例

```json
{
  "tenantId": 1001,
  "batchNumber": "batch_123456",
  "videoId": "video_abc123",
  "hasPerformance": 1,
  "videoOssUrl": "https://oss.example.com/video.mp4",
  "startTime": "2026-03-19T10:00:00",
  "endTime": "2026-03-19T12:00:00",
  "cumulativeView": 10000,
  "startCumulativeSales": 0.00,
  "endCumulativeSales": 50000.00,
  "cumulativeRefund": 1000.00,
  "investment": 5000.00,
  "secUid": "sec_uid_123",
  "products": [
    {
      "id": 1001,
      "productId": "prod_001",
      "title": "商品A",
      "marketPrice": 99.00,
      "payAmt": 9900.00,
      "payCnt": 100
    }
  ]
}
```
