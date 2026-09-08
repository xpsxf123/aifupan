# getVideoRoi 接口变更说明

> 变更日期：2026-07-22
> 接口路径：`GET /replay/openapi/v2000/getVideoRoi?videoId={videoId}`
> Controller：`ClientOpenApi2000#getVideoRoi`

---

## 一、变更概要

| 序号 | 变更项 | 说明 |
|---|---|---|
| 1 | 新增字段 | `jlbyAuthStatus`、`qcAuthStatus`、`hasData` |
| 2 | 公式修正 | `netTransactionRoi` 公式从 `消耗 ÷ 净成交` 修正为 `净成交 ÷ 消耗` |
| 3 | 不再返回 null | data 不再可能为 null，无数据时返回 `hasData=false` 的对象 |

---

## 二、变更前返回值（旧版）

### 2.1 有数据时

```json
{
  "code": 0,
  "msg": "",
  "data": {
    "launchRoiAmount": 1234.56,
    "salesAmount": 50000,
    "overallCostRoi": 40.5,
    "netTransactionAmount": 48765.44,
    "netTransactionRoi": 0
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| launchRoiAmount | double | 投放消耗（元）|
| salesAmount | int | 销售额（元）|
| overallCostRoi | double | 整体支付ROI = 销售额 ÷ 投放消耗 |
| netTransactionAmount | double | 净成交金额 = 销售额 − 退款金额 |
| netTransactionRoi | **int（实际值，类型为整数）** | **错误公式**：消耗 ÷ 净成交，取 intValue() 只保留整数 |

### 2.2 无数据时

```json
{
  "code": 0,
  "msg": "",
  "data": null
}
```

> `data` 直接返回 `null`，前端需自行判空。

---

## 三、变更后返回值（新版）

### 3.1 有数据 + 有授权

```json
{
  "code": 0,
  "msg": "",
  "data": {
    "launchRoiAmount": 1234.56,
    "salesAmount": 50000,
    "overallCostRoi": 40.5,
    "netTransactionAmount": 48765.44,
    "netTransactionRoi": 39.51,
    "jlbyAuthStatus": 1,
    "qcAuthStatus": 1,
    "hasData": true
  }
}
```

### 新版全字段说明

| 字段 | 类型 | 可为 null | 是否新增 | 说明 |
|---|---|---|---|---|
| `launchRoiAmount` | Double | 是 | 否 | 投放消耗（元） |
| `salesAmount` | Integer | 是 | 否 | 销售额（元） |
| `overallCostRoi` | Double | 是 | 否 | 整体支付ROI = 销售额 ÷ 投放消耗 |
| `netTransactionAmount` | Double | 是 | 否 | 净成交金额 = 销售额 − 退款金额 |
| `netTransactionRoi` | Double | 是 | 否 | 净成交ROI = 净成交金额 ÷ 投放消耗（公式已修正，类型从 int→Double） |
| `jlbyAuthStatus` | Integer | 是 | **是** | 巨量百应授权状态：0=未授权 1=已授权 2=授权过期 3=授权失败 4=授权中 5=抖音号不匹配；null=无法获取 |
| `qcAuthStatus` | Integer | 是 | **是** | 千川授权状态：0=未授权 1=已授权 2=授权过期 3=授权失败 4=授权中 5=抖音号不匹配；null=无法获取 |
| `hasData` | Boolean | 否 | **是** | 是否有 ROI 数据 |

### 3.2 无数据 + 有授权

```json
{
  "code": 0,
  "msg": "",
  "data": {
    "launchRoiAmount": null,
    "salesAmount": null,
    "overallCostRoi": null,
    "netTransactionAmount": null,
    "netTransactionRoi": null,
    "jlbyAuthStatus": 1,
    "qcAuthStatus": 0,
    "hasData": false
  }
}
```

### 3.3 无数据 + 无授权

```json
{
  "code": 0,
  "msg": "",
  "data": {
    "launchRoiAmount": null,
    "salesAmount": null,
    "overallCostRoi": null,
    "netTransactionAmount": null,
    "netTransactionRoi": null,
    "jlbyAuthStatus": null,
    "qcAuthStatus": null,
    "hasData": false
  }
}
```

---

## 四、新增字段详情

| 字段 | 类型 | 可为 null | 说明 |
|---|---|---|---|
| `jlbyAuthStatus` | Integer | 是 | 巨量百应授权状态，来源 `tb_anchor_url_user.auth_jlby_status`。0=未授权 1=已授权 2=授权过期 3=授权失败 4=授权中 5=授权抖音号不匹配；null=无法获取（视频未找到或主播未绑定用户） |
| `qcAuthStatus` | Integer | 是 | 千川授权状态，来源 `tb_anchor_url_user.auth_qc_status`。0=未授权 1=已授权 2=授权过期 3=授权失败 4=授权中 5=授权抖音号不匹配；null=无法获取（视频未找到或主播未绑定用户） |
| `hasData` | Boolean | 否 | 是否有 ROI 数据（段落表或 confuse 表有一方命中即为 true） |

---

## 五、前端迁移要点

1. **`data != null` 判断改为 `data.hasData` 判断**：旧版用 `if (data)` 判断有数据，新版改为 `if (data.hasData)`。`data` 本身永不为 null。

2. **`netTransactionRoi` 值会变**：旧版公式错误导致 ROI 值几乎都是 0。新版公式正确，值会是正常的小数（如 `39.51`），且类型从 int 变为 double。

3. **未授权 ≠ 无数据**：`hasData=false` 仅表示暂无 ROI 统计数据。`jlbyAuthStatus`/`qcAuthStatus` 为 null 或不等于 1 说明授权问题，前端可据此引导用户去授权后再拉取数据。

4. **auth 字段可能为 null**：当 `videoId` 对应的视频记录不存在，或主播未绑定到用户时，授权字段为 null。前端展钧需兜底处理。
