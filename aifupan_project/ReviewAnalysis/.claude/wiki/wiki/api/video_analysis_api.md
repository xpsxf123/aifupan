# 视频分析 API

## AnchorVideoController

路由前缀: `api/anchorvideo`

---

### GET /api/anchorvideo/createanalysis

生成视频分析。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |
| token | string | 是 | 认证 Token |
| tradeId | string | 是 | 行业 ID |

**业务规则**: 校验本地时间 → 检查已有分析 → 验证文件存在 → 检查分析余额 → 启动异步分析线程

**响应**: `string` (错误信息或空)

**示例**:
```http
GET /api/anchorvideo/createanalysis?videoId=v001&token=xxx&tradeId=100
```

```json
// 响应 (成功时为空字符串，失败时为错误信息)
""
```

---

### GET /api/anchorvideo/reanalysis (清除旧数据)。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |
| token | string | 是 | 认证 Token |
| tradeId | string | 是 | 行业 ID |

**响应**: `string`

---

### GET /api/anchorvideo/reanalysisbytrade

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |
| tradeId | string | 是 | 行业 ID |
| platformType | string | 是 | 平台类型 |

**响应** — `SentenceMarkDto` (完整字段见本文档底部 "公共响应模型" 章节)

---

### GET /api/anchorvideo/intelligentAnalysis

文案提取转智能分析。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |

**响应**: 无返回体 (void)

---

### GET /api/anchorvideo/cancelVideoAnalysis

取消视频分析。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |

**响应**: 无返回体 (void)

---

### GET /api/anchorvideo/stopAutoAnalysis

停止自动分析。

**响应**: 无返回体 (void)

---

### GET /api/anchorvideo/lockanalysis

---

### GET /api/anchorvideo/lockCloudAnalysis

获取云端分析详情。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |

**响应** — `SentenceMarkDto`

---

### GET /api/anchorvideo/repairVideo

修复视频。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type | int | 是 | 0=视频, 1=文件 |
| uuid | string | 是 | 视频/文件 ID |

**响应**: 无返回体 (void)

---

### GET /api/anchorvideo/cancelRepairVideo

取消修复。

**响应**: 无返回体 (void)

---

### GET /api/anchorvideo/compress

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |

**响应** — `Dictionary<string, object>`:

| 字段 | 类型 | 说明 |
|---|---|---|
| status | int | 状态 |
| message | string | 消息 |

---

### GET /api/anchorvideo/preview

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |

**响应**: `string` (播放路径)

---

### GET /api/anchorvideo/openfolder

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |

**响应**: `bool`

---

### GET /api/anchorvideo/getVideoPayUrl

获取视频播放 URL。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |

**响应**: `string` (播放 URL)

---

### GET /api/anchorvideo/shareanalysis

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |
| onlineFileUrl | string | 是 | 在线文件 URL |

**响应**: `string` (分享 URL)

---

### GET /api/anchorvideo/onlineChartData

在线图表数据 (异步)。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |

**响应**: `dynamic` (图表数据)

---

### GET /api/anchorvideo/exportVideoContent

导出视频文案 (异步)。

**请求参数 (Query)** — `ExportVideoContentVo`:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceId | string | 是 | 来源 ID |
| sourceType | int? | 否 | 来源类型 |
| type | int? | 否 | 导出类型 |

**响应**: 无返回体 (void) (触发文件保存)

---

### POST /api/anchorvideo/generateVideoContent

生成优化/自然文案。

**请求体 (JSON)** — `GenerateVideoContentBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceId | string | 是 | 来源 ID |
| sourceType | int | 是 | 来源类型 |
| type | int | 是 | 生成类型 |

**响应**: 无返回体 (void)

---

### POST /api/anchorvideo/addVideoSlice

添加视频分段。

**请求体 (JSON)** — `AddVideoSliceBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |
| videoName | string | 否 | 视频名称 |
| savePath | string | 否 | 保存路径 |
| sliceType | int? | 否 | 分段类型 |
| sliceClass | string | 否 | 分段分类 |
| startTimeMs | long | 是 | 开始时间 (毫秒) |
| endTimeMs | long | 是 | 结束时间 (毫秒) |
| isAutoUploadCloud | int? | 否 | 是否自动上传云 |
| savePathType | int? | 否 | 保存路径类型 |
| remarks | string | 否 | 备注 |
| sliceTimeType | int? | 否 | 分段时间类型 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/anchorvideo/addVideoSlice
Content-Type: application/json

{
  "videoId": "v001",
  "videoName": "精彩片段",
  "sliceType": 1,
  "startTimeMs": 60000,
  "endTimeMs": 180000,
  "remarks": "高光时刻"
}
```

---

### POST /api/anchorvideo/queryDanMuExport

导出弹幕数据。

**请求体 (JSON)**: `dynamic` (动态参数)

**响应**: 无返回体 (void) (触发文件保存)

---

### POST /api/anchorvideo/renameVideo

重命名视频 (异步)。

**请求体 (JSON)** — `RenameVideoBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 视频 ID |
| newVideoName | string | 是 | 新视频名称 |

**响应**: 无返回体 (void)

---

### POST /api/anchorvideo/updateDataDiagnosisConfig

更新数据诊断配置。

**请求体 (JSON)** — `DataDiagnosisConfigVo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sourceId | string | 是 | 来源 ID |
| sourceType | int? | 否 | 来源类型 |
| tradeId | string | 否 | 行业 ID |
| modelId | string | 否 | AI 模型 ID |
| modelName | string | 否 | AI 模型名称 |
| hasDataScreenshot | int? | 否 | 是否有数据截图 |
| hasBoard | int? | 否 | 是否有看板 |
| selectDataScreenshot | int? | 否 | 选择数据截图 |
| selectBoard | int? | 否 | 选择看板 |
| cueWordsId | string | 否 | 提示词 ID |
| newCueWordsId | string | 否 | 新提示词 ID |
| qaStatus | int? | 否 | 问答状态 |
| errorContent | string | 否 | 错误内容 |
| basicSettingsVo | BasicSettingsVo | 否 | 基础设置 |

**响应**: `DataDiagnosisConfigVo` (更新后的配置)

---

### POST /api/anchorvideo/deletebyids

**请求体 (JSON)**: `List<string>` (视频 ID 列表)

**业务规则**: 阻止删除正在分析中的视频。

**响应**: `string` (错误信息或空)

**示例**:
```http
POST /api/anchorvideo/deletebyids
Content-Type: application/json

["v001", "v002", "v003"]
```

```json
// 响应
""
```

---

### POST /api/anchorvideo/deleteLocalVideoByIds

仅删除本地视频文件。

**请求体 (JSON)**: `List<string>` (视频 ID 列表)

**响应**: `string`

---

## 公共响应模型 — SentenceMarkDto

视频分析详情的核心返回结构。

| 字段 | 类型 | 说明 |
|---|---|---|
| anchorInfo | AnchorInfo | 主播信息 |
| videoInfo | AnchorVideo | 视频信息 |
| uploadFile | UploadFile | 上传文件信息 |
| playUrl | string | 播放 URL |
| audioaAlyses | List\<AudioaAlysis\> | 音频分析列表 |
| fileAudioaAlyses | List\<UploadFileAlysis\> | 文件音频分析列表 |
| onlineNumList | List\<OnlineNum\> | 在线人数列表 |
| barrageDataList | List\<BarrageData\> | 弹幕数据列表 |
| totalBarrageNum | int? | 弹幕总数 |
| cruxTypeList | List\<AnalysisResultCruxTypeVo\> | 关键类型列表 |
| wordsCollect | List\<WordsMarkVo\> | 词汇标注集合 |
| wordsTabList | List\<WordsTabVo\> | 词汇标签列表 |
| interactionPercent | double? | 互动率 |
| purchaseCountStart | int? | 购买数起始 |
| purchaseCountEnd | int? | 购买数结束 |
| purchaseCount | int? | 购买数 |
| totalWatchNum | int? | 总观看数 |
| dataSourceType | int? | 数据来源类型 |
| suggestTrade | int? | 推荐行业 |
| uvValueStart | double? | UV 值起始 |
| uvValueEnd | double? | UV 值结束 |
| volumeStart | double? | 成交额起始 |
| volumeEnd | double? | 成交额结束 |
| juLiangDataList | List\<OceanEngineDataDto\> | 巨量数据列表 |
| tradeInfo | TradeVo | 行业信息 |

---

## ContrastController

路由前缀: `api/contrast`

---

### POST /api/contrast/savecontrast

保存视频对比任务。

**请求体 (JSON)** — `VideoContrast`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| ContrastId | string | 否 | 对比 ID (自动生成) |
| VideoOneId | string | 是 | 视频一 ID |
| VideoTwoId | string | 是 | 视频二 ID |
| AnchorOneId | string | 否 | 主播一 ID |
| AnchorTwoId | string | 否 | 主播二 ID |
| FileOneId | string | 否 | 文件一 ID |
| FileTwoId | string | 否 | 文件二 ID |
| contrastType | int | 否 | 对比类型 |
| syncScene | int? | 否 | 同步场景 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/contrast/savecontrast
Content-Type: application/json

{
  "VideoOneId": "v001",
  "VideoTwoId": "v002",
  "AnchorOneId": "a001",
  "AnchorTwoId": "a002",
  "contrastType": 0
}
```

---

### GET /api/contrast/lockanalysiscontrast

获取本地对比详情 (异步)。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| contrastId | string | 是 | 对比 ID |

**响应** — `SentenceMarkContrastDto`:

| 字段 | 类型 | 说明 |
|---|---|---|
| SentenceMark1 | SentenceMarkDto | 视频一分析详情 |
| SentenceMark2 | SentenceMarkDto | 视频二分析详情 |
| VideoContrast | VideoContrast | 对比任务信息 |

---

### GET /api/contrast/lockCloudContrast

获取云端对比详情 (异步)。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| contrastId | string | 是 | 对比 ID |

**响应**: `SentenceMarkContrastDto`

---

### GET /api/contrast/shareanalysis

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| contrastId | string | 是 | 对比 ID |

**响应**: `string` (分享 URL)

---

## UploadFileController

路由前缀: `api/uploadfile`

---

### GET /api/uploadfile/checkUploadFile

打开文件选择对话框。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| fileType | int | 是 | 文件类型 |

**响应** — `Dictionary<string, object>`:

| 字段 | 类型 | 说明 |
|---|---|---|
| filePath | string | 选择的文件路径 |
| fileName | string | 文件名 |
| fileSize | long | 文件大小 |

---

### POST /api/uploadfile/commitUploadFile

异步处理并保存上传文件。

**请求体 (JSON)** — `CommitUploadFileBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| platformType | int | 是 | 平台类型 |
| fileType | int | 是 | 文件类型 |
| filePath | string | 是 | 文件路径 |
| engSerViceType | string | 否 | ASR 引擎模型 |

继承自 `BasicSettingsBaseDto` 的字段:

| 字段 | 类型 | 说明 |
|---|---|---|
| accountType | int? | 账号类型 |
| premiereDate | string | 首播日期 |
| accountStage | int? | 账号阶段 |
| livingTarget | int? | 直播目标 |
| livingModality | int? | 直播形态 |
| marketing | int? | 营销方式 |
| optimizeDirection | string | 优化方向 |
| livingMode | int? | 直播模式 |
| anchorSituation | string | 主播情况 |

**响应**: `string` (文件 ID 或错误信息)

**示例**:
```http
POST /api/uploadfile/commitUploadFile
Content-Type: application/json

{
  "platformType": 0,
  "fileType": 0,
  "filePath": "C:\\Videos\\test.mp4",
  "engSerViceType": "16k_zh"
}
```

```json
// 响应
"file_001"
```

---

### POST /api/uploadfile/uploadTxtFileByWord

直接上传文本内容。

**请求体 (JSON)** — `UploadTxtFileByWordBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| platformType | int | 是 | 平台类型 |
| content | string | 是 | 文本内容 |

继承自 `BasicSettingsBaseDto`。

**响应**: `string`

---

### GET /api/uploadfile/createanalysis

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| fileId | string | 是 | 文件 ID |
| token | string | 是 | 认证 Token |
| tradeId | string | 是 | 行业 ID |

**响应**: `string`

---

### GET /api/uploadfile/reanalysis

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| fileId | string | 是 | 文件 ID |
| platformType | int | 是 | 平台类型 |
| token | string | 是 | 认证 Token |
| tradeId | string | 是 | 行业 ID |

**响应**: `string`

---

### GET /api/uploadfile/reanalysisbytrade

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| fileId | string | 是 | 文件 ID |
| tradeId | string | 是 | 行业 ID |
| platformType | string | 是 | 平台类型 |
| duration | int | 否 | 时长 |
| wordNum | int | 否 | 字数 |

**响应**: `SentenceMarkDto`

---

### GET /api/uploadfile/getpage

分页查询上传文件。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageIndex | int | 否 | 页码 |
| pageSize | int | 否 | 每页条数 |
| analysisStatus | int | 否 | 分析状态筛选 |
| fileName | string | 否 | 文件名搜索 |
| uploadStartDate | string | 否 | 上传开始日期 |
| uploadEndDate | string | 否 | 上传结束日期 |
| analysisStartDate | string | 否 | 分析开始日期 |
| analysisEndDate | string | 否 | 分析结束日期 |

**响应** — `PageDto<UploadFileDto>`:

| 字段 | 类型 | 说明 |
|---|---|---|
| Total | int | 总记录数 |
| PageTotal | int | 总页数 |
| DataList | List\<UploadFileDto\> | 文件列表 |

`UploadFileDto`:

| 字段 | 类型 | 说明 |
|---|---|---|
| Id | int? | 主键 |
| FileId | string | 文件 ID |
| FileName | string | 文件名 |
| FileType | int | 文件类型 |
| OriginalPath | string | 原始路径 |
| NowPath | string | 当前路径 |
| AnalysisStatus | int? | 分析状态 |
| AnalysisTime | string | 分析时间 |
| UploadTime | string | 上传时间 |
| FileSize | string | 文件大小 |
| FileDuration | int? | 文件时长 |
| ErrorReason | string | 错误原因 |
| ShowUrl | string | 展示 URL |

**示例**:
```http
GET /api/uploadfile/getpage?pageIndex=1&pageSize=10&analysisStatus=1
```

```json
// 响应
{
  "Total": 25,
  "PageTotal": 3,
  "DataList": [
    {
      "FileId": "file_001",
      "FileName": "直播录音.mp3",
      "FileType": 1,
      "AnalysisStatus": 1,
      "UploadTime": "2026-04-23 10:00:00",
      "FileSize": "15.2MB",
      "FileDuration": 3600
    }
  ]
}
```

---

### GET /api/uploadfile/lockanalysis

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| fileId | string | 是 | 文件 ID |

**响应**: `SentenceMarkDto`

---

### GET /api/uploadfile/compress

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| fileId | string | 是 | 文件 ID |

**响应**: `Dictionary<string, object>`

---

### GET /api/uploadfile/preview

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| fileId | string | 是 | 文件 ID |

**响应**: `string` (播放路径)

---

### GET /api/uploadfile/shareanalysis

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| fileId | string | 是 | 文件 ID |
| onlineFileUrl | string | 是 | 在线文件 URL |

**响应**: `string` (分享 URL)

---

### GET /api/uploadfile/confirmusemark

确认消费标注资源。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| fileId | string | 是 | 文件 ID |
| duration | int | 是 | 时长 |
| wordNum | int | 是 | 字数 |

**响应**: 无返回体 (void)

---

### GET /api/uploadfile/checkTxtFileAnalysisProperty

检查文字分析余额。

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| fileId | string | 是 | 文件 ID |

**响应** — `CheckTxtFileAnalysisPropertyVo`:

| 字段 | 类型 | 说明 |
|---|---|---|
| fileWordNum | int | 文件字数 |
| propertyWordNum | long | 用户剩余字数配额 |
| isSufficient | int | 是否充足: 0=不足, 1=充足 |

**示例**:
```http
GET /api/uploadfile/checkTxtFileAnalysisProperty?fileId=file_001
```

```json
// 响应
{
  "fileWordNum": 5000,
  "propertyWordNum": 100000,
  "isSufficient": 1
}
```

---

### POST /api/uploadfile/deletebyids

**请求体 (JSON)**: `List<string>` (文件 ID 列表)

**响应**: `string`

---

### POST /api/uploadfile/addFileSlice

添加文件分段。

**请求体 (JSON)** — `AddFileSliceBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| videoId | string | 是 | 文件 ID |
| videoName | string | 否 | 文件名称 |
| savePath | string | 否 | 保存路径 |
| sliceType | int? | 否 | 分段类型 |
| sliceClass | string | 否 | 分段分类 |
| startTimeMs | long | 是 | 开始时间 (毫秒) |
| endTimeMs | long | 是 | 结束时间 (毫秒) |
| isAutoUploadCloud | int? | 否 | 是否自动上传云 |
| savePathType | int? | 否 | 保存路径类型 |
| remarks | string | 否 | 备注 |
| sliceTimeType | int? | 否 | 分段时间类型 |

**响应**: 无返回体 (void)

---

### POST /api/uploadfile/renameFile

重命名文件。

**请求体 (JSON)** — `RenameFileBo`:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| fileId | string | 是 | 文件 ID |
| newFileName | string | 是 | 新文件名 |

**响应**: 无返回体 (void)

**示例**:
```http
POST /api/uploadfile/renameFile
Content-Type: application/json

{ "fileId": "file_001", "newFileName": "直播录音_已分析" }
```
