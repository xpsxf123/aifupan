# getVideoRoi 接口新增字段说明

## 接口信息

| 项目 | 内容 |
|---|---|
| 路径 | `GET /replay/openapi/v2000/getVideoRoi` |
| 参数 | `videoId` (String, 必填) — 视频唯一标识 |
| 响应 | `R<VideoRoiVo>` |
| 变更 | **纯新增字段，完全向后兼容，无破坏性** |

---

## 新增字段

> 两个字段的数据来源于直播间数据看盘，在已有数据源中**已经存在**，本次仅暴露到接口返回中。

| 字段 | JSON key | 类型 | 单位 | 说明 |
|---|---|---|---|---|
| 成交单量 | `payCount` | `int` | 单 | 直播间的累计成交订单数 |
| 千次观看成交金额 | `gpm` | `double` | 元 | 每千次观看带来的成交金额（GPM = GMV per Mille） |

---

## 完整响应结构

```json
{
  "code": 0,
  "msg": "",
  "data": {
    "launchRoiAmount": 1234.56,
    "salesAmount": 50000,
    "overallCostRoi": 40.5,
    "netTransactionAmount": 48765.44,
    "netTransactionRoi": 2.53,
    "jlbyAuthStatus": 1,
    "qcAuthStatus": 1,
    "payCount": 120,
    "gpm": 3500.0,
    "hasData": true
  }
}
```

## 全字段速查表

| JSON key | 类型 | 单位 | 说明 | 新增 |
|---|---|---|---|---|
| `launchRoiAmount` | double | 元 | 投放消耗 | — |
| `salesAmount` | int | 元 | 销售额 | — |
| `overallCostRoi` | double | — | 整体支付ROI = 销售额 ÷ 投放消耗 | — |
| `netTransactionAmount` | double | 元 | 净成交金额 = 销售额 − 退款金额 | — |
| `netTransactionRoi` | double | — | 净成交ROI = 净成交金额 ÷ 投放消耗 | — |
| `jlbyAuthStatus` | int | — | 巨量百应授权状态：0=未授权 1=已授权 2=过期 3=失败 4=授权中 5=抖音号不匹配 | — |
| `qcAuthStatus` | int | — | 千川授权状态：同上枚举 | — |
| `payCount` | int | 单 | 成交单量 | ⭐ |
| `gpm` | double | 元 | 千次观看成交金额 | ⭐ |
| `hasData` | boolean | — | 是否有ROI数据，false时上述金额字段均无意义 | — |

---

## 前后端对接说明

- 两个新字段在**无数据时返回 `null`**（如 `hasData: false` 时），前端做空值兜底即可。
- 老版本客户端解析 JSON 时忽略未知字段，不受影响。
- 无须变更请求参数，仅响应体新增字段。
