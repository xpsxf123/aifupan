# 主播管理 API (AnchorInfoController)

路由前缀: `api/anchorinfo`

---

## 1. 授权管理

### GET /api/anchorinfo/openAnchorJuliang

打开巨量百应页面。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/openAnchorJuliang?secUid=MS4wLjABAAAAxxx
```

---

### GET /api/anchorinfo/authorizeJuliang

巨量引擎授权，打开 CefSharp 授权窗口。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| authType | int | 是 | 0=主账号, 1=子账号 |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/authorizeJuliang?secUid=MS4wLjABAAAAxxx&authType=0
```

---

### GET /api/anchorinfo/cancelAuthorizeJuliang

取消巨量引擎授权。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/cancelAuthorizeJuliang?secUid=MS4wLjABAAAAxxx
```

---

### GET /api/anchorinfo/authorizeQianchuan

千川授权 (异步)。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| authType | int | 是 | 0=主账号, 1=子账号 |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/authorizeQianchuan?secUid=MS4wLjABAAAAxxx&authType=0
```

---

### GET /api/anchorinfo/cancelAuthorizeQianchuan

取消千川授权，停止该主播的千川数据采集轮询并清除授权状态与缓存。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**响应**: 无返回体 (void)

**执行流程**:

1. 从 `AnchorCacheManager` 查询主播缓存信息
2. 主播不存在 → 抛出 `CustomException("主播信息不存在")`
3. 将 `AnchorInfo.qianchuanAuthStatus` 置为 `0`（未授权），更新缓存
4. 调用 `QianchuanDataCollectionManager.StopPolling(secUid)` 停止千川数据采集轮询
5. 调用 `QianchuanUtils.deleteAnchorCacheVersion(secUid)` 清除主播的授权版本号
6. 记录操作日志 `"取消千川授权成功"`

**授权状态枚举** (`qianchuanAuthStatus`):

| 值 | 状态 | 说明 |
|---|---|---|
| 0 | 未授权 | 初始状态或已取消授权 |
| 1 | 已授权 | 授权成功，可正常拉取数据 |
| 2 | 授权过期 | Token/凭证失效，需重新授权 |
| 3 | 授权失败 | 授权流程中发生错误 |
| 4 | 授权中 | 正在执行授权流程 |
| 5 | 授权抖音号不匹配 | 当前抖音号与主播不匹配 |

**异常处理**:

| 异常 | 触发条件 | 行为 |
|---|---|---|
| `CustomException` | 主播不存在于缓存 | 直接抛出到调用方 |
| 其他 `Exception` | 缓存操作、轮询停止等环节失败 | 记录错误日志后 `throw` 重新抛出 |

**调用链路**:

```
Controller → AnchorBll.CancelAuthorizeQianchuan(secUid)
  ├── AnchorCacheManager.GetAnchorByIdFromCache(secUid)
  ├── anchorInfo.qianchuanAuthStatus = 0
  ├── AnchorCacheManager.SetAnchorCache(anchorInfo)
  ├── QianchuanDataCollectionManager.StopPolling(secUid)
  │     └── _pollers.TryRemove + poller.Stop()
  └── QianchuanUtils.deleteAnchorCacheVersion(secUid)
        └── Config/qianchuan-{userId}-{tenantId}.txt → Remove(secUid)
```

**关联接口**:

| 接口 | 关系 |
|---|---|
| `/authorizeQianchuan` | 授权千川，authStatus 从 0 → 4 → 1 |
| `/authorizeJuliang` | 巨量授权（千川依赖巨量，取消巨量时需一并处理千川） |

**示例**:
```http
GET /api/anchorinfo/cancelAuthorizeQianchuan?secUid=MS4wLjABAAAAxxx
```

---

### GET /api/anchorinfo/authorizeLife

来客授权。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| authType | int | 是 | 0=主账号, 1=子账号 |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/authorizeLife?secUid=MS4wLjABAAAAxxx&authType=0
```

---

### GET /api/anchorinfo/cancelAuthorizeLife

取消来客授权。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/cancelAuthorizeLife?secUid=MS4wLjABAAAAxxx
```

---

### GET /api/anchorinfo/authorizeEnterprise

企业号授权。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| authType | int | 是 | 0=主账号, 1=子账号 |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/authorizeEnterprise?secUid=MS4wLjABAAAAxxx&authType=0
```

---

### GET /api/anchorinfo/cancelAuthorizeEnterprise

取消企业号授权。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/cancelAuthorizeEnterprise?secUid=MS4wLjABAAAAxxx
```

---

### GET /api/anchorinfo/anchorLive

主播后台授权。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| authType | int | 是 | 0=主账号, 1=子账号 |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/anchorLive?secUid=MS4wLjABAAAAxxx&authType=0
```

---

### GET /api/anchorinfo/cancelAuthorizeAnchorLive

取消主播后台授权。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/cancelAuthorizeAnchorLive?secUid=MS4wLjABAAAAxxx
```

---

### GET /api/anchorinfo/authorizeDouyin

抖音授权。

**请求参数**: 无

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/authorizeDouyin
```

---

### GET /api/anchorinfo/cancelAuthorizeDouyin

取消抖音授权。

**请求参数**: 无

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/cancelAuthorizeDouyin
```

---

### GET /api/anchorinfo/authorizeWeChatChannels

微信视频号授权。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| redirectUrl | string | 是 | 授权回调 URL |

**业务规则**: 授权前检查主播配额，超出则抛出异常。

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/authorizeWeChatChannels?redirectUrl=https://example.com/callback
```

---

## 2. 主播 CRUD

### POST /api/anchorinfo/addOrUpdateAnchor

添加或更新主播信息。

**请求体 (JSON)** — `AddOrUpdateAnchorBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| homeUrl | string | 否 | 主页 URL |
| liveUrl | string | 否 | 直播 URL |
| anchorName | string | 否 | 主播名称 |
| anchorAvatar | string | 否 | 头像 URL |
| platform | int? | 否 | 平台类型 |
| anchorUserId | string | 否 | 平台用户 ID |
| webSocketId | string | 否 | WebSocket ID |
| isAutoRecord | int? | 否 | 是否自动录制 |
| isBarrageMonitoring | int? | 否 | 是否弹幕监控 |
| isAutoUploadCloud | int? | 否 | 是否自动上传云 |
| isTop | int? | 否 | 是否置顶 |
| recordTime | string | 否 | 录制时间范围, 如 `06:00:00-19:00:00` |
| smsTip | int? | 否 | 通知模式 (0无/1开播/2下播/3全部) |
| isDataViewing | int? | 否 | 是否数据看板 |
| folderName | string | 否 | 存储文件夹名 |
| remarksName | string | 否 | 备注名 |
| tradeId | string | 否 | 行业 ID |
| isAutoDiagnosis | int | 否 | 是否自动诊断 |
| diagnosisParams | DiagnosisParams | 否 | 诊断参数 |
| recordDefinition | int? | 否 | 画质 (-1跟随/0标清/1高清/2超清/3蓝光) |
| recordLimitType | int? | 否 | 录制模式 (-1跟随/0无限/1限时单段/2限时分段) |
| recordLimitValue | int? | 否 | 限制时长(分钟) |
| isAutoAnalysis | int? | 否 | 是否自动分析 |
| isDataDiagnosis | int? | 否 | 是否数据诊断 |
| dataDiagnosisParams | DiagnosisParams | 否 | 数据诊断参数 |
| engSerViceType | string | 否 | ASR 引擎模型 |
| pureRecordOnlineNum | int? | 否 | 纯录制在线人数 |
| isScheduleRecord | int? | 否 | 是否排班录制 |
| recordTimeMode | int? | 否 | 时间模式 (0按视频时长/1按北京时间/2按固定时间点) |
| segmentTimePoints | string | 否 | 分段时间点, 如 `09:30,10:40` |
| isStatisticsPerformance | int? | 否 | 是否统计绩效 |
| accountType | int? | 否 | 账号类型 (继承 BasicSettingsBaseDto) |
| premiereDate | string | 否 | 首播日期 (继承) |
| accountStage | int? | 否 | 账号阶段 (继承) |
| livingTarget | int? | 否 | 直播目标 (继承) |
| livingModality | int? | 否 | 直播形态 (继承) |
| marketing | int? | 否 | 营销方式 (继承) |
| optimizeDirection | string | 否 | 优化方向 (继承) |
| livingMode | int? | 否 | 直播模式 (继承) |
| anchorSituation | string | 否 | 主播情况 (继承) |

`DiagnosisParams` 嵌套对象:

| 字段 | 类型 | 说明 |
|---|---|---|
| modelId | string | AI 模型 ID |
| cueWordsIds | List\<string\> | 提示词 ID 列表 |

**业务规则**: 新增主播时检查配额，超出则抛出异常。

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/anchorinfo/addOrUpdateAnchor
Content-Type: application/json

{
  "secUid": "MS4wLjABAAAAxxx",
  "anchorName": "测试主播",
  "platform": 0,
  "isAutoRecord": 1,
  "recordDefinition": 2,
  "recordLimitType": 0,
  "tradeId": "100",
  "recordTime": "06:00:00-23:00:00",
  "smsTip": 1,
  "diagnosisParams": {
    "modelId": "model_001",
    "cueWordsIds": ["cue_01", "cue_02"]
  }
}
```

---

### POST /api/anchorinfo/getpageanchor

分页查询主播列表。

**请求体 (JSON)** — `AnchorListBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageIndex | int? | 否 | 页码 |
| pageSize | int? | 否 | 每页条数 |
| anchorName | string | 否 | 主播名称 (模糊搜索) |
| recordStatus | int? | 否 | 录制状态筛选 |
| isRemoveRecord | int? | 否 | 移除状态 (0正常/1已移除) |
| tradeId | string | 否 | 行业 ID 筛选 |
| accountType | int? | 否 | 账号类型筛选 |

**响应** — `AnchorPageDto<AnchorDto>`:

| 字段 | 类型 | 说明 |
|---|---|---|
| Total | int | 总记录数 |
| PageTotal | int | 总页数 |
| CurrentLiveNum | int | 当前直播中数量 |
| CurrentRecordNum | int | 当前录制中数量 |
| DataList | List\<AnchorDto\> | 主播列表 (字段见下方 AnchorDto) |

**AnchorDto 完整字段**:

| 字段 | 类型 | 说明 |
|---|---|---|
| Id | int | 主键 |
| AnchorName | string | 主播名称 |
| AnchorAvatar | string | 头像 URL |
| AnchorPlatform | string | 平台 |
| SecUid | string | SecUid |
| HomeUrl | string | 主页 URL |
| LiveUrl | string | 直播 URL |
| AppShareUrl | string | 分享 URL |
| LiveStatus | int | 直播状态 |
| RecordStatus | int | 录制状态 |
| IsAutoRecord | int | 是否自动录制 |
| OnlineNumber | string | 在线人数 |
| BatchNumber | string | 批次号 |
| Duration | string | 时长 |
| VedioSizie | string | 视频大小 |
| StartTime | string | 开始时间 |
| CurrentRecordStartTime | string | 当前录制开始时间 |
| CurrentRecordStatus | int | 当前录制状态 |
| AddTime | string | 添加时间 |
| TradeId | string | 行业 ID |
| IsBarrageMonitoring | int | 弹幕监控 (默认0) |
| IsRemoveRecord | int | 移除状态 |
| IsAutoUploadCloud | int | 自动上传云 |
| IsTop | int | 是否置顶 |
| AddTopTime | string | 置顶时间 |
| LastRecordTime | string | 最后录制时间 |
| RecordTime | string | 录制时间范围 |
| InRecordTime | bool | 是否在录制时间内 |
| SmsTip | int | 通知模式 |
| IsDataViewing | int | 数据看板 |
| FolderName | string | 文件夹名 |
| RemarksName | string | 备注名 |
| isAutoDiagnosis | int? | 自动诊断 |
| diagnosisParams | DiagnosisParams | 诊断参数 |
| isDataDiagnosis | int? | 数据诊断 |
| dataDiagnosisParams | DiagnosisParams | 数据诊断参数 |
| juliangAuthStatus | int | 巨量授权状态 |
| qianchuanAuthStatus | int | 千川授权状态 |
| lifeAuthStatus | int | 来客授权状态 |
| enterpriseAuthStatus | int | 企业号授权状态 |
| anchorLiveAuthStatus | int | 主播后台授权状态 |
| WeChatChannelsAuthStatus | int | 微信视频号授权 (JSON key: authChannelStatus) |
| platform | int | 平台类型 |
| deleteDate | string | 删除时间 |
| recordDefinition | int? | 画质 |
| recordLimitType | int? | 录制模式 |
| recordLimitValue | int? | 限制时长 |
| isAutoAnalysis | int? | 自动分析 |
| isScheduleRecord | int? | 排班录制 |
| recordTimeMode | int? | 时间模式 |
| segmentTimePoints | string | 分段时间点 |
| isStatisticsPerformance | int? | 统计绩效 |
| AccountType | int | 账号类型 |
| engSerViceType | string | ASR 引擎模型 |
| pureRecordOnlineNum | int? | 纯录制在线人数 |
| YesterdayRecordTime | long? | 昨日录制时长 |
| YesterdayRecordList | List\<AnchorYesterdayRecordItemVo\> | 昨日录制列表 |
| YesterdayRecordNum | int | 昨日录制次数 |
| DayBeforeRecordList | List\<AnchorYesterdayRecordItemVo\> | 前日录制列表 |
| DayBeforeRecordNum | int | 前日录制次数 |

**AnchorYesterdayRecordItemVo**:

| 字段 | 类型 | 说明 |
|---|---|---|
| recordDate | string | 录制日期 |
| observationNum | string | 观看人数 |
| volumeStart | int | 成交额起始 |
| volumeEnd | int | 成交额结束 |
| videoId | string | 视频 ID |
| batchNumber | string | 批次号 |

**示例**:
```http
POST /api/anchorinfo/getpageanchor
Content-Type: application/json

{
  "pageIndex": 1,
  "pageSize": 20,
  "isRemoveRecord": 0,
  "anchorName": "测试"
}
```

```json
// 响应
{
  "Total": 50,
  "PageTotal": 3,
  "CurrentLiveNum": 5,
  "CurrentRecordNum": 3,
  "DataList": [
    {
      "Id": 1,
      "AnchorName": "测试主播",
      "AnchorAvatar": "https://xxx/avatar.jpg",
      "SecUid": "MS4wLjABAAAAxxx",
      "LiveStatus": 1,
      "RecordStatus": 1,
      "IsAutoRecord": 1,
      "OnlineNumber": "1234",
      "RecordTime": "06:00:00-23:00:00",
      "InRecordTime": true,
      "juliangAuthStatus": 1,
      "qianchuanAuthStatus": 0,
      "lifeAuthStatus": 0,
      "enterpriseAuthStatus": 0,
      "anchorLiveAuthStatus": 1,
      "authChannelStatus": 0,
      "platform": 0,
      "recordDefinition": 2,
      "recordLimitType": 0,
      "isAutoAnalysis": 1,
      "AccountType": 0,
      "YesterdayRecordList": [
        {
          "recordDate": "2026-04-22",
          "observationNum": "5600",
          "volumeStart": 1000,
          "volumeEnd": 5000,
          "videoId": "v_001",
          "batchNumber": "batch_20260422"
        }
      ],
      "YesterdayRecordNum": 1
    }
  ]
}
```

---

### GET /api/anchorinfo/getAnchorInfo

获取单个主播详情。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**响应** — `AnchorDto` (完整字段同 getpageanchor 中的 AnchorDto)

**示例**:
```http
GET /api/anchorinfo/getAnchorInfo?secUid=MS4wLjABAAAAxxx
```

```json
// 响应
{
  "Id": 1,
  "AnchorName": "测试主播",
  "AnchorAvatar": "https://xxx/avatar.jpg",
  "SecUid": "MS4wLjABAAAAxxx",
  "LiveStatus": 0,
  "RecordStatus": 0,
  "IsAutoRecord": 1,
  "platform": 0,
  "recordDefinition": 2,
  "RecordTime": "06:00:00-23:00:00",
  "juliangAuthStatus": 1,
  "qianchuanAuthStatus": 0
}
```

---

### GET /api/anchorinfo/reAddAnchor

从回收站恢复主播。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**业务规则**: 恢复前检查配额，超出则抛出异常。

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/reAddAnchor?secUid=MS4wLjABAAAAxxx
```

---

### GET /api/anchorinfo/removeanchor

移除主播 (软删除，可恢复)。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/removeanchor?secUid=MS4wLjABAAAAxxx
```

---

### GET /api/anchorinfo/fullRemoveAnchor

从回收站永久删除主播。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/fullRemoveAnchor?secUid=MS4wLjABAAAAxxx
```

---

### GET /api/anchorinfo/updateanchortrade

更新主播行业。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| tradeId | string | 是 | 行业 ID |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/updateanchortrade?secUid=MS4wLjABAAAAxxx&tradeId=100
```

---

### GET /api/anchorinfo/existAnchor

检查是否存在主播。

**请求参数**: 无

**响应**: `bool`

**示例**:
```http
GET /api/anchorinfo/existAnchor
```

```json
// 响应
true
```

---

## 3. 录制控制

### GET /api/anchorinfo/decector

启动全部主播检测录制。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| VideoType | int | 是 | 视频类型 |
| Definition | int | 是 | 画质 |

**前置检查**: 系统时钟精度 + 磁盘空间 ≥ 10GB

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/decector?VideoType=0&Definition=2
```

---

### GET /api/anchorinfo/stopdecector

停止全部检测。

**请求参数**: 无

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/stopdecector
```

---

### GET /api/anchorinfo/StartRecord

启动单个主播录制。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**前置检查**: 系统时钟精度

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/StartRecord?secUid=MS4wLjABAAAAxxx
```

---

### GET /api/anchorinfo/stoprecord

停止单个主播录制。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/stoprecord?secUid=MS4wLjABAAAAxxx
```

---

### GET /api/anchorinfo/openorcloseautorecord

开关自动录制。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| isAuto | int | 是 | 0=关闭, 1=开启 |

**前置检查**: 系统时钟精度

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/openorcloseautorecord?secUid=MS4wLjABAAAAxxx&isAuto=1
```

---

### GET /api/anchorinfo/openfolder

打开主播存储目录。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**响应**: `bool` — 是否成功打开

**示例**:
```http
GET /api/anchorinfo/openfolder?secUid=MS4wLjABAAAAxxx
```

```json
// 响应
true
```

---

### GET /api/anchorinfo/previewvideo

预览录制视频。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**响应**: 无返回体 (void)，触发本地播放器打开

**示例**:
```http
GET /api/anchorinfo/previewvideo?secUid=MS4wLjABAAAAxxx
```

---

### GET /api/anchorinfo/converttomp4

转换为 MP4 (当前为空实现)。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/converttomp4?secUid=MS4wLjABAAAAxxx
```

---

## 4. 数据与监控

### GET /api/anchorinfo/pullJuliang

拉取巨量数据 (异步)。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/pullJuliang?videoId=v_001
```

---

### GET /api/anchorinfo/updateBarrageMonitoring

开关弹幕监控。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| isBarrageMonitoring | int | 是 | 0=关闭, 1=开启 |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/updateBarrageMonitoring?secUid=MS4wLjABAAAAxxx&isBarrageMonitoring=1
```

---

### GET /api/anchorinfo/openOrCloseAutoUploadCloud

开关自动上传云空间。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| isAutoUploadCloud | int | 是 | 0=关闭, 1=开启 |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/openOrCloseAutoUploadCloud?secUid=MS4wLjABAAAAxxx&isAutoUploadCloud=1
```

---

### GET /api/anchorinfo/openOrCloseDataViewing

开关数据看板。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| isDataViewing | int | 是 | 0=关闭, 1=开启 |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/openOrCloseDataViewing?secUid=MS4wLjABAAAAxxx&isDataViewing=1
```

---

### GET /api/anchorinfo/updatePureRecordOnlineNum

更新纯录制在线人数阈值。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| pureRecordOnlineNum | int | 是 | 在线人数阈值 |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/updatePureRecordOnlineNum?secUid=MS4wLjABAAAAxxx&pureRecordOnlineNum=100
```

---

### POST /api/anchorinfo/updateAiPartial

更新主播 AI 配置。

**请求体 (JSON)** — `AiAnchorInfoVo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| videoId | string | 否 | 视频 ID |
| isUpdate | bool? | 否 | 是否更新 |
| sourceId | string | 否 | 来源 ID |
| sourceType | int? | 否 | 来源类型 |
| accountType | int? | 否 | 账号类型 (继承 BasicSettingsBaseDto) |
| premiereDate | string | 否 | 首播日期 (继承) |
| accountStage | int? | 否 | 账号阶段 (继承) |
| livingTarget | int? | 否 | 直播目标 (继承) |
| livingModality | int? | 否 | 直播形态 (继承) |
| marketing | int? | 否 | 营销方式 (继承) |
| optimizeDirection | string | 否 | 优化方向 (继承) |
| livingMode | int? | 否 | 直播模式 (继承) |
| anchorSituation | string | 否 | 主播情况 (继承) |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/anchorinfo/updateAiPartial
Content-Type: application/json

{
  "secUid": "MS4wLjABAAAAxxx",
  "videoId": "v_001",
  "isUpdate": true,
  "accountType": 1,
  "livingTarget": 2
}
```

---

### GET /api/anchorinfo/topAnchor

置顶/取消置顶主播。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |
| action | int | 是 | 0=取消置顶, 1=置顶 |

**响应**: 无返回体 (void)

**示例**:
```http
GET /api/anchorinfo/topAnchor?secUid=MS4wLjABAAAAxxx&action=1
```
