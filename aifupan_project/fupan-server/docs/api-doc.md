# 接口文档

## words 模块 - 主播管理

### 添加或修改用户的主播信息

- **请求方式**：POST
- **URL**：`/replay/anchorurl/addOrUpdateAnchor`
- **描述**：添加或修改用户的主播信息，包含主播基础信息和用户维度的录制配置

**请求参数**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| secUid | String | 是 | 主播唯一标识 |
| homeUrl | String | 否 | 主页url |
| liveUrl | String | 否 | 直播间url |
| anchorName | String | 否 | 主播名称 |
| anchorAvatar | String | 否 | 主播头像 |
| platform | Integer | 否 | 平台类型 0：抖音 1：快手 2：视频号 |
| anchorNumber | String | 否 | 主播抖音号 |
| isAutoRecord | Integer | 否 | 在线时,是否自动录制分析 0：否 1：是 |
| recordTime | String | 否 | 录制时间，如：06:00:00-19:00:00 |
| recordDefinition | Integer | 否 | 录制的清晰度 -1：跟随系统 0标清 1高清 2超清 3蓝光 |
| recordLimitType | Integer | 否 | 录制形式 -1：跟随系统 0：无限制录制 1：限制时长录制 2：时长分段录制 |
| recordLimitValue | Integer | 否 | 录制的时长，单位：分钟 |
| isScheduleRecord | Integer | 否 | 是否按排班录制 0：否 1：是 |
| recordTimeMode | Integer | 否 | 录制时间模式 0：按视频时长录制 1：按北京时间录制（默认） 2：按时间点分段录制 |
| segmentTimePoints | String | 否 | 时间点分段录制的时间点列表，格式："09:30,10:40,11:20"，多个时间点用逗号分隔 |
| isBarrageMonitoring | Integer | 否 | 是否开启弹幕监控 0：否 1：是 |
| isAutoUploadCloud | Integer | 否 | 是否自动上传到云空间 0：否 1：是 |
| isAutoAnalysis | Integer | 否 | 是否自动分析视频 0：否 1：是 |
| isDataViewing | Integer | 否 | 是否开启数据看板 0：否 1：是 |
| isDataDiagnosis | Integer | 否 | 是否开启数据诊断 0：否 1：是 |
| smsTip | Integer | 否 | 上下播短信提醒 0：不提醒 1：上播提醒 2：下播提醒 3：上下播提醒 |
| authJlbyStatus | Integer | 否 | 授权巨量百应状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配 |
| authQcStatus | Integer | 否 | 千川授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配 |
| authLifeStatus | Integer | 否 | 授权来客状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配 |

> **说明**：`authJlbyStatusTime` / `authQcStatusTime` / `authLifeStatusTime` 由后端自动维护，当传入对应状态字段时自动记录当前时间，前端无需传入。

**响应参数**（data 字段）：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| - | String | 成功时返回 "添加成功" |

**请求示例**：

```json
{
  "secUid": "test-sec-uid-001",
  "homeUrl": "https://www.douyin.com/user/test-sec-uid-001",
  "liveUrl": "https://live.douyin.com/123456",
  "anchorName": "测试主播",
  "anchorAvatar": "https://example.com/avatar.jpg",
  "platform": 0,
  "isAutoRecord": 1,
  "recordTime": "08:00:00-18:00:00",
  "isScheduleRecord": 1,
  "recordTimeMode": 2,
  "segmentTimePoints": "09:30,10:40,11:20",
  "authJlbyStatus": 1,
  "authQcStatus": 1
}
```

**响应示例**：

```json
{
  "code": 0,
  "msg": "success",
  "data": "添加成功"
}
```

---

### 更新用户主播信息

- **请求方式**：POST
- **URL**：`/replay/words/anchorUrl/updateUserAnchorInfo`
- **描述**：更新用户维度的主播配置信息，如录制设置、弹幕监控、排班录制、授权状态等

**请求参数**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| secUid | String | 是 | 主播唯一标识 |
| isAutoRecord | Integer | 否 | 在线时,是否自动录制分析 0：否 1：是 |
| isRemoveRecord | Integer | 否 | 是否从录制列表移除 0：否 1：是 2：已从恢复列表删除 |
| isBarrageMonitoring | Integer | 否 | 是否开启弹幕监控 0：否 1：是 |
| isAutoUploadCloud | Integer | 否 | 是否自动上传到云空间 0：否 1：是 |
| isTop | Integer | 否 | 是否置顶 0：否 1：是 |
| isAutoDiagnosis | Integer | 否 | 是否自动诊断 0：否 1：是 |
| recordTime | String | 否 | 录制时间，如：06:00:00-19:00:00 |
| recordDefinition | Integer | 否 | 录制的清晰度 -1：跟随系统 0标清 1高清 2超清 3蓝光 |
| recordLimitType | Integer | 否 | 录制形式 -1：跟随系统 0：无限制录制 1：限制时长录制 2：时长分段录制 |
| recordLimitValue | Integer | 否 | 录制的时长，单位：分钟 |
| isScheduleRecord | Integer | 否 | 是否按排班录制 0：否 1：是 |
| recordTimeMode | Integer | 否 | 录制时间模式 0：按视频时长录制 1：按北京时间录制（默认） 2：按时间点分段录制 |
| segmentTimePoints | String | 否 | 时间点分段录制的时间点列表，格式："09:30,10:40,11:20"，多个时间点用逗号分隔 |
| isAutoAnalysis | Integer | 否 | 是否自动分析视频 0：否 1：是 |
| isDataViewing | Integer | 否 | 是否开启数据看板 0：否 1：是 |
| smsTip | Integer | 否 | 上下播短信提醒 0：不提醒 1：上播提醒 2：下播提醒 3：上下播提醒 |
| remarksName | String | 否 | 主播备注名称 |
| accountType | Integer | 否 | 账号归属类型 0：自由账号 1：同行账号 |
| authJlbyStatus | Integer | 否 | 授权巨量百应状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配 |
| authQcStatus | Integer | 否 | 千川授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配 |
| authLifeStatus | Integer | 否 | 授权来客状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配 |

> **说明**：`authJlbyStatusTime` / `authQcStatusTime` / `authLifeStatusTime` 由后端自动维护，当传入对应状态字段时自动记录当前时间，前端无需传入。

**响应参数**（data 字段）：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| - | - | 无 |

**请求示例**：

```json
{
  "secUid": "test-sec-uid-001",
  "isScheduleRecord": 1,
  "recordTimeMode": 2,
  "segmentTimePoints": "09:30,10:40,11:20",
  "recordTime": "09:00:00-21:00:00",
  "recordDefinition": 2
}
```

**响应示例**：

```json
{
  "code": 0,
  "msg": "success",
  "data": null
}
```

---

## words 模块 - 巨量百应

### 巨量数据更新接口

- **请求方式**：POST
- **URL**：`/replay/words/oceanEngineData/updateOceanEngine`
- **描述**：客户端进行巨量数据更新，支持新增和修改

**请求参数**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| videoId | String | 是 | 视频ID（最大50字符） |
| secUid | String | 否 | 主播唯一标识 |
| ossPath | String | 否 | 巨量oss存储地址 |
| batchNumber | String | 否 | 批次号 |
| isTakeProduct | Integer | 否 | 是否带货 0：否 1：是 |
| totalWatchNum | Integer | 否 | 总观看人次 |
| averageOnlineNum | Integer | 否 | 平均在线人数 |
| averageResidenceTime | Integer | 否 | 平均停留时间(秒) |
| incrementFollowerCount | Integer | 否 | 新增粉丝数 |
| convertFanRate | Double | 否 | 粉丝转化率 |
| interactionPercent | Double | 否 | 互动率 |
| volume | Integer | 否 | 销售额(单位:分) |
| purchaseCount | Integer | 否 | 销量 |
| customerUnitPrice | Double | 否 | 客单价 |
| uvValue | Double | 否 | uv价值 |
| goodsConvertRate | Double | 否 | 带货转换率 |
| gpm | Double | 否 | 千次观看成交金额（单位：分） |
| showWatchCntRatio | Double | 否 | 曝光-观看率 |
| roi | Double | 否 | ROI |
| launchRoiAmount | Double | 否 | 投放ROI金额（单位：元） |
| refundAmount | Double | 否 | 退款金额（单位：元） |
| overallCostRoi | Double | 否 | 整体消耗ROI |
| netTransactionRoi | Double | 否 | 净成交ROI |
| watchFlowList | List | 否 | 看播流量结构 |
| payFlowList | List | 否 | 成交流量结构 |
| watchUserPortrait | Object | 否 | 看播用户画像 |
| payUserPortrait | Object | 否 | 成交用户画像 |

**响应参数**（data 字段）：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| - | Long | 数据看板ID |

**请求示例**：

```json
{
  "videoId": "test-video-id-001",
  "secUid": "test-sec-uid",
  "totalWatchNum": 5000,
  "averageOnlineNum": 200,
  "averageResidenceTime": 120,
  "incrementFollowerCount": 50,
  "volume": 100000,
  "purchaseCount": 30,
  "roi": 2.5,
  "launchRoiAmount": 5000.00,
  "refundAmount": 300.50,
  "overallCostRoi": 3.25,
  "netTransactionRoi": 2.80,
  "isTakeProduct": 1
}
```

**响应示例**：

```json
{
  "code": 0,
  "msg": "success",
  "data": 1234567890123456789
}
```

---

### 巨量数据查询接口

- **请求方式**：GET
- **URL**：`/replay/words/oceanEngineData/getOceanEngine`
- **描述**：根据视频ID查询巨量数据

**请求参数**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| videoId | String | 是 | 视频ID |

**响应参数**（data 字段）：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| dataJson | String | 巨量数据JSON（包含所有指标字段） |
| ossPath | String | 已签名的oss下载地址 |
| rawOssPath | String | 原始oss存储地址 |

> **说明**：`dataJson` 为 JSON 字符串，解析后包含 videoId、secUid、batchNumber、流量结构、用户画像、各类指标数据（roi、launchRoiAmount、refundAmount、overallCostRoi、netTransactionRoi 等）。

**响应示例**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "dataJson": "{\"videoId\":\"test-video-id-001\",\"roi\":2.5,\"overallCostRoi\":3.25,\"netTransactionRoi\":2.80,...}",
    "ossPath": "https://signed-url...",
    "rawOssPath": "path/to/oss/file"
  }
}
```

---

### 获取巨量数据上传预签名链接

- **请求方式**：GET
- **URL**：`/replay/words/oceanEngineData/getSignUploadUrl`
- **描述**：获取巨量数据上传的预签名链接

**请求参数**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| videoId | String | 是 | 视频ID |

**响应参数**（data 字段）：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| uploadUrl | String | 预签名上传链接 |

**响应示例**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "uploadUrl": "https://presigned-upload-url..."
  }
}
```
