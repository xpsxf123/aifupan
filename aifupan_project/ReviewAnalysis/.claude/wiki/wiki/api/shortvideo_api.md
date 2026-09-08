# 短视频 API

## ShortVideoController

路由前缀: `api/shortVideo`

---

### GET /api/shortVideo/selectLocalVideo

打开本地视频选择对话框。

**请求参数**: 无

**响应**: 无返回体 (void)，触发文件选择对话框

**示例**:
```http
GET /api/shortVideo/selectLocalVideo
```

---

### POST /api/shortVideo/immediatelyLocalVideo

立即提取本地视频文案。

**请求体 (JSON)** — `ImmediatelyLocalVideoBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceType | int | 是 | 来源类型 |
| videoUrl | string | 是 | 视频 URL 或本地路径 |
| platformType | int? | 否 | 平台类型 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/shortVideo/immediatelyLocalVideo
Content-Type: application/json

{
  "sourceType": 0,
  "videoUrl": "C:\\Videos\\test.mp4",
  "platformType": 0
}
```

---

### POST /api/shortVideo/reExtract

重新提取视频文案。

**请求体 (JSON)** — `ReExtractBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long? | 是 | 记录 ID |
| sourceType | int | 是 | 来源类型 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/shortVideo/reExtract
Content-Type: application/json

{ "id": 123, "sourceType": 0 }
```

---

### POST /api/shortVideo/batchCreateExtract

批量创建文案提取任务。

**请求体 (JSON)**: `Dictionary<string, object>` (动态参数)

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/shortVideo/batchCreateExtract
Content-Type: application/json

{ "ids": ["v001", "v002"], "sourceType": 0 }
```

---

### POST /api/shortVideo/captureInfluencerInfo

搜索达人信息。

**请求体 (JSON)** — `CaptureInfluencerInfoBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| searchKeyword | string | 是 | 搜索关键词 |
| platformType | int? | 否 | 平台类型 |
| tradeId | long? | 否 | 行业 ID |
| groupId | long? | 否 | 分组 ID |
| likeUpwards | int? | 否 | 点赞数下限 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/shortVideo/captureInfluencerInfo
Content-Type: application/json

{
  "searchKeyword": "美妆达人",
  "platformType": 0,
  "tradeId": 100,
  "likeUpwards": 1000
}
```

---

### POST /api/shortVideo/addInfluencerInfo

添加达人到订阅列表。

**请求体 (JSON)** — `CaptureInfluencerInfoBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| searchKeyword | string | 是 | 搜索关键词 |
| platformType | int? | 否 | 平台类型 |
| tradeId | long? | 否 | 行业 ID |
| groupId | long? | 否 | 分组 ID |
| likeUpwards | int? | 否 | 点赞数下限 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/shortVideo/addInfluencerInfo
Content-Type: application/json

{
  "searchKeyword": "美妆达人",
  "platformType": 0,
  "tradeId": 100,
  "groupId": 1
}
```

---

### POST /api/shortVideo/syncInfluencerVideo

同步达人视频。

**请求体 (JSON)** — `SyncInfluencerVideoBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| platformAccount | string | 是 | 平台账号 |
| platformUserId | string | 是 | 平台用户 ID |
| platformType | int? | 否 | 平台类型 |
| actionType | int? | 否 | 操作类型 (默认0) |
| influencerId | long? | 否 | 达人 ID |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/shortVideo/syncInfluencerVideo
Content-Type: application/json

{
  "platformAccount": "douyin_user_001",
  "platformUserId": "12345678",
  "platformType": 0,
  "influencerId": 999
}
```

---

### POST /api/shortVideo/captureHotSearch

搜索热门视频。

**请求体 (JSON)** — `CaptureInfluencerInfoBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| searchKeyword | string | 是 | 搜索关键词 |
| platformType | int? | 否 | 平台类型 |
| tradeId | long? | 否 | 行业 ID |
| groupId | long? | 否 | 分组 ID |
| likeUpwards | int? | 否 | 点赞数下限 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/shortVideo/captureHotSearch
Content-Type: application/json

{
  "searchKeyword": "直播带货技巧",
  "platformType": 0,
  "likeUpwards": 500
}
```

---

### POST /api/shortVideo/addHotSearch

订阅热搜关键词。

**请求体 (JSON)** — `VideoHotSubscriptionAddBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | string | 是 | 热搜关键词 |
| platformType | int? | 否 | 平台类型 |
| industryId | long? | 否 | 行业 ID |
| groupId | long? | 否 | 分组 ID |
| likeCountMin | int? | 否 | 最低点赞数 |
| autoSyncEnabled | int? | 否 | 是否自动同步 |
| likeCountThreshold | int? | 否 | 点赞数阈值 |
| updateTimeCondition | int? | 否 | 更新时间条件 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/shortVideo/addHotSearch
Content-Type: application/json

{
  "keyword": "直播带货",
  "platformType": 0,
  "likeCountMin": 500,
  "autoSyncEnabled": 1
}
```

---

### POST /api/shortVideo/updateHotSearchData

手动触发热搜数据更新。

**请求体 (JSON)** — `VideoHotSubscriptionAddBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | string | 是 | 热搜关键词 |
| platformType | int? | 否 | 平台类型 |
| industryId | long? | 否 | 行业 ID |
| groupId | long? | 否 | 分组 ID |
| likeCountMin | int? | 否 | 最低点赞数 |
| autoSyncEnabled | int? | 否 | 是否自动同步 |
| likeCountThreshold | int? | 否 | 点赞数阈值 |
| updateTimeCondition | int? | 否 | 更新时间条件 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/shortVideo/updateHotSearchData
Content-Type: application/json

{
  "keyword": "直播带货",
  "platformType": 0,
  "autoSyncEnabled": 1
}
```
