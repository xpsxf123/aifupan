# 视频 ROI 与曲线数据 — 前端对接文档

---

## 一、新增接口：getVideoRoi（Java 服务端）

```
GET /openapi/v2000/getVideoRoi?videoId={videoId}
```

**请求参数**

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| videoId | Query | string | 是 | 视频 UUID |

**响应** `R<VideoRoiVo>`

| 字段 | 类型 | 单位 | 说明 |
|---|---|---|---|
| code | int | — | 0=成功 |
| msg | string | — | 描述信息 |
| data.launchRoiAmount | double | 元 | 投放消耗 |
| data.salesAmount | int | 元 | 销售额 |
| data.overallCostRoi | double | — | 整体支付ROI = 销售额 ÷ 投放消耗 |
| data.netTransactionAmount | double | 元 | 净成交金额 = 销售额 − 退款金额 |
| data.netTransactionRoi | double | — | 净成交ROI = 净成交金额 ÷ 投放消耗 |
| data.payCount | int | 单 | 成交单量 |
| data.gpm | double | 元 | 千次观看成交金额 |
| data.jlbyAuthStatus | int | — | 巨量百应授权状态：0=未授权 1=已授权 2=授权过期 3=授权失败 4=授权中 5=抖音号不匹配 |
| data.qcAuthStatus | int | — | 千川授权状态：0=未授权 1=已授权 2=授权过期 3=授权失败 4=授权中 5=抖音号不匹配 |
| data.hasData | boolean | — | 是否有ROI数据 |

**响应示例**

```json
{
  "code": 0,
  "msg": "",
  "data": {
    "launchRoiAmount": 1234.56,
    "salesAmount": 50000,
    "overallCostRoi": 40.5,
    "netTransactionAmount": 48765.44,
    "netTransactionRoi": 0.0253,
    "payCount": 120,
    "gpm": 3500.0,
    "jlbyAuthStatus": 1,
    "qcAuthStatus": 1,
    "hasData": true
  }
}
```

---

## 二、已有接口新增字段：getOnlineAnalysis（Java 服务端 — 在线曲线 + 视频详情）

```
GET /openapi/v2000/getOnlineAnalysis?type={0|1}&uuid={videoId}
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| type | Query | int | 是 | 0=视频，1=文件 |
| uuid | Query | string | 是 | 视频或文件 UUID |

**响应** `R<AnalysisResultCloudVo>`

### 新增字段（在 `data` 内）

| 字段 | 类型 | 单位 | 说明 |
|---|---|---|---|
| qianchuanCostDataList | `List<Map>` | 元 | 投放消耗分时曲线 |
| totalQianchuanCost | int | 元 | 投放消耗总计 |
| netTransactionRoiDataList | `List<Map>` | — | 净成交ROI分时曲线 |
| totalNetTransactionRoi | int | — | 净成交ROI总计 |

### 曲线点结构

```json
{ "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "value": 150 }
```

| key | 类型 | 说明 |
|---|---|---|
| dateTime | string | 毫秒时间戳 |
| date | string | 格式化日期时间 |
| value | int | costDataList=消耗金额(元)；roiDataList=ROI比率 |

### 新增字段示例

```json
{
  "code": 0,
  "msg": "",
  "data": {
    "qianchuanCostDataList": [
      { "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "value": 150 },
      { "dateTime": "1715616060000", "date": "2024-05-13 20:01:00", "value": 200 }
    ],
    "totalQianchuanCost": 350,
    "netTransactionRoiDataList": [
      { "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "value": 2 },
      { "dateTime": "1715616060000", "date": "2024-05-13 20:01:00", "value": 3 }
    ],
    "totalNetTransactionRoi": 2
  }
}
```

---

## 三、已有接口新增字段：getOnlineContrastAnalysis（Java 服务端 — 在线对比）

```
GET /openapi/v2000/getOnlineContrastAnalysis?contrastId={contrastId}
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| contrastId | Query | string | 是 | 对比 ID |

**响应** `R<AnalysisContractResultCloudVo>`

### 响应结构

| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | 0=成功 |
| msg | string | 描述信息 |
| data.sentenceMark1 | AnalysisResultCloudVo | 视频1/文件1 分析信息 |
| data.sentenceMark2 | AnalysisResultCloudVo | 视频2/文件2 分析信息 |
| data.videoContrast | SyncContrastInfoVoUpper | 对比元信息 |

### 新增字段

4 个曲线字段在 `data.sentenceMark1` 和 `data.sentenceMark2` 内各自独立返回，字段定义和曲线点结构与 [getOnlineAnalysis](#二已有接口新增字段getonlineanalysisjava-服务端--在线曲线--视频详情) 完全一致。

### 新增字段示例

```json
{
  "code": 0,
  "msg": "",
  "data": {
    "sentenceMark1": {
      "qianchuanCostDataList": [
        { "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "value": 150 }
      ],
      "totalQianchuanCost": 350,
      "netTransactionRoiDataList": [
        { "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "value": 2 }
      ],
      "totalNetTransactionRoi": 2
    },
    "sentenceMark2": {
      "qianchuanCostDataList": [
        { "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "value": 100 }
      ],
      "totalQianchuanCost": 200,
      "netTransactionRoiDataList": [
        { "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "value": 1 }
      ],
      "totalNetTransactionRoi": 1
    }
  }
}
```

---

## 四、已有接口新增字段：lockanalysis（C# 客户端 — 在线曲线 + 视频详情）

```
GET /anchorvideo/lockanalysis?videoId={videoId}
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| videoId | Query | string | 是 | 视频 UUID |

**返回值** `SentenceMarkDto`（直接返回对象，无外层 `code/msg/data` 封装）

### 新增字段（在顶层）

| 字段 | 类型 | 单位 | 说明 |
|---|---|---|---|
| qianchuanCostDataList | `List<BarrageData>` | 元 | 投放消耗分时曲线 |
| totalQianchuanCost | double? | 元 | 投放消耗总计 |
| netTransactionRoiDataList | `List<BarrageData>` | — | 净成交ROI分时曲线 |
| totalNetTransactionRoi | double? | — | 净成交ROI总计 |

### BarrageData 曲线点结构

```json
{ "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "barrageNum": 150 }
```

| 字段 | 类型 | 说明 |
|---|---|---|
| dateTime | string | 毫秒时间戳 |
| date | string | 格式化日期时间 |
| barrageNum | int | costDataList=消耗金额(元)；roiDataList=ROI比率 |

> `barrageNum` 是历史字段名，此处复用承载曲线数值。

### 新增字段示例

```json
{
  "qianchuanCostDataList": [
    { "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "barrageNum": 150 },
    { "dateTime": "1715616060000", "date": "2024-05-13 20:01:00", "barrageNum": 200 }
  ],
  "totalQianchuanCost": 350.0,
  "netTransactionRoiDataList": [
    { "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "barrageNum": 2 },
    { "dateTime": "1715616060000", "date": "2024-05-13 20:01:00", "barrageNum": 3 }
  ],
  "totalNetTransactionRoi": 2.0
}
```

---

## 五、已有接口新增字段：lockCloudContrast（C# 客户端 — 在线对比）

```
GET /contrast/lockCloudContrast?contrastId={contrastId}
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| contrastId | Query | string | 是 | 对比 ID |

**返回值** `SentenceMarkContrastDto`（直接返回对象，无外层封装）

### 响应结构

| 字段 | 类型 | 说明 |
|---|---|---|
| SentenceMark1 | SentenceMarkDto | 视频1/文件1 分析信息 |
| SentenceMark2 | SentenceMarkDto | 视频2/文件2 分析信息 |
| VideoContrast | VideoContrast | 对比元信息 |

### 新增字段

4 个曲线字段在 `SentenceMark1` 和 `SentenceMark2` 内各自独立返回，字段定义和曲线点结构与 [lockanalysis](#四已有接口新增字段lockanalysisc-客户端--在线曲线--视频详情) 完全一致。

### 新增字段示例

```json
{
  "SentenceMark1": {
    "qianchuanCostDataList": [
      { "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "barrageNum": 150 }
    ],
    "totalQianchuanCost": 350.0,
    "netTransactionRoiDataList": [
      { "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "barrageNum": 2 }
    ],
    "totalNetTransactionRoi": 2.0
  },
  "SentenceMark2": {
    "qianchuanCostDataList": [
      { "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "barrageNum": 100 }
    ],
    "totalQianchuanCost": 200.0,
    "netTransactionRoiDataList": [
      { "dateTime": "1715616000000", "date": "2024-05-13 20:00:00", "barrageNum": 1 }
    ],
    "totalNetTransactionRoi": 1.0
  }
}
```

---

## 六、已有接口新增字段：onlineChartData（Java 服务端 / C# 客户端）

### 6.1 Java 服务端

```
GET /openapi/v2100/onlineChartData?videoId={videoId}&step={step}
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| videoId | Query | string | 是 | 视频 UUID |
| step | Query | int | 否 | 步长，默认 1 |

**响应** `R<OnlineChartVo>`

### 新增字段（在 `data` 内）

| 字段 | 类型 | 单位 | 说明 |
|---|---|---|---|
| payComboCntDataList | `List<CurveData>` | 件 | 成交件数分时曲线 |
| payAmtDataList | `List<CurveData>` | 分 | 成交金额分时曲线 |
| fansClubJoinUcntDataList | `List<CurveData>` | 人 | 新增直播团人数分时曲线 |
| followAnchorUcntDataList | `List<CurveData>` | 人 | 新增粉丝数量分时曲线 |
| qianchuanCostDataList | `List<CurveData>` | 分 | 投放消耗分时曲线 |
| refundAmtDataList | `List<CurveData>` | 分 | 退款金额分时曲线 |
| netTransactionRoiDataList | `List<CurveData>` | — | 净成交ROI分时曲线 |

### CurveData 结构

| 字段 | 类型 | 说明 |
|---|---|---|
| dateTime | long | 自然时间戳（毫秒） |
| dateTimeNew | long | 非自然时间戳（毫秒） |
| valueNum | int | 数值（含义取决于所在 list） |

### 新增字段示例

```json
{
  "code": 0,
  "msg": "",
  "data": {
    "qianchuanCostDataList": [
      { "dateTime": 1715616000000, "dateTimeNew": 1715616000, "valueNum": 15000 },
      { "dateTime": 1715616060000, "dateTimeNew": 1715616060, "valueNum": 20000 }
    ],
    "netTransactionRoiDataList": [
      { "dateTime": 1715616000000, "dateTimeNew": 1715616000, "valueNum": 2 },
      { "dateTime": 1715616060000, "dateTimeNew": 1715616060, "valueNum": 3 }
    ],
    "payAmtDataList": [...],
    "refundAmtDataList": [...],
    "payComboCntDataList": [...],
    "fansClubJoinUcntDataList": [...],
    "followAnchorUcntDataList": [...]
  }
}
```

### 6.2 C# 客户端

```
GET /anchorvideo/onlineChartData?videoId={videoId}
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| videoId | Query | string | 是 | 视频 UUID |

**返回值**：透传 Java 服务端响应（含 `R<T>` 外层），字段结构与 Java 完全一致。

> C# 客户端接口直接代理 Java 服务端 `/openapi/v2100/onlineChartData`，返回原始 JSON。

---

## 七、Java 服务端 vs C# 客户端 差异速查

| 差异点 | Java 服务端 | C# 客户端 |
|---|---|---|
| 响应外层 | `{ code, msg, data }` | 直接返回对象 |
| 曲线点类型 | `Map` (key: dateTime/date/value) | `BarrageData` (dateTime/date/barrageNum) |
| 曲线数值字段 | `value` | `barrageNum` |
| total 字段类型 | int | double? |

---

## 八、字段含义

**投放消耗（qianchuanCost）**：千川广告投放消耗，原始单位为分，接口返回已 ÷100 转元。

**净成交 ROI（netTransactionRoi）**：

```
净成交金额 = 成交金额 − 退款金额
净成交ROI = 净成交金额 ÷ 投放消耗
（净成交金额 ≤ 0 时返回 0）
```
