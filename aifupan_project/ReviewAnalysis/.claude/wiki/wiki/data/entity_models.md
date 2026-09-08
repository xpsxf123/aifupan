# 实体模型详情

## VideoEntity (视频)

**文件**: `entity/video/VideoEntity.cs`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long? | 主键 |
| videoId | string | 视频 ID |
| videoName | string | 视频名称 |
| startTime | string | 开始时间 |
| endTime | string | 结束时间 |
| duration | string | 时长 |
| vedioSizie | string | 视频大小 |
| batchNumber | string | 批次号 (直播场次) |
| videoType | int? | 视频类型 |
| definition | int? | 画质 |
| storagePath | string | 存储路径 |
| liveUrl | string | 直播 URL |
| shareUrl | string | 分享 URL |
| playUrl | string | 播放 URL |
| sourceUrl | string | 来源 URL |
| anchorId | int? | 主播 ID |
| secUid | string | 主播 SecUid |
| liveTitle | string | 直播标题 |
| tradeId | string | 行业 ID |
| platformType | string | 平台类型 |
| isRecording | int? | 是否录制中 |
| sourceType | int? | 来源类型 |
| analysisStatus | int? | 分析状态 |
| analysisTime | string | 分析时间 |
| errorReason | string | 错误原因 |
| uploadStatus | int? | 上传状态 |
| isMark | int? | 是否标注 |
| cloudStore | int? | 云存储状态 |
| deleteStatus | int? | 删除状态 |
| type | int? | 类型 |
| recordErrorStatus | int? | 录制错误状态 |
| videoSliceType | int? | 视频分段类型 |
| videoSliceInfo | VideoSliceEntity | 分段信息 |
| sliceList | List\<VideoSliceEntity\> | 分段列表 |
| hasAiOptimizePurpose | int? | 是否有 AI 优化目的 |
| userId / tenantId | string / long? | 用户/租户 |

## VideoSliceEntity (视频分段)

**文件**: `entity/video/VideoSliceEntity.cs`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | string | 分段 ID |
| sourceId | string | 源视频 ID |
| sourceParentId | string | 父视频 ID |
| sliceType | int? | 分段类型 |
| sliceClass | string | 分段分类 |
| startMillisecond | long? | 开始毫秒 |
| startTime | string | 开始时间 |
| endMillisecond | long? | 结束毫秒 |
| endTime | string | 结束时间 |
| remarks | string | 备注 |
| sliceVideoName | string | 分段视频名 |
| savePath | string | 保存路径 |
| sliceTimeType | int? | 分段时间类型 |

## UploadFileEntity (上传文件)

**文件**: `entity/uploadFile/UploadFileEntity.cs`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long? | 主键 |
| fileName | string | 文件名 |
| fileType | int | 文件类型 |
| originalPath | string | 原始路径 |
| nowPath | string | 当前路径 |
| fileSize | long? | 文件大小 |
| fileDuration | long? | 文件时长 |
| fileId | string | 文件 ID |
| fileWordNum | int? | 文字数量 |
| tradeId | long? | 行业 ID |
| platformType | int | 平台类型 |
| analysisStatus | int? | 分析状态 |
| analysisTime | string | 分析时间 |
| errorReason | string | 错误原因 |
| uploadTime | string | 上传时间 |
| uploadStatus | int? | 上传状态 |
| isMark | int? | 是否标注 |
| cloudStore | int? | 云存储状态 |
| playUrl | string | 播放 URL |
| shareUrl | string | 分享 URL |
| fileSliceType | int? | 文件分段类型 |
| videoSliceInfo | VideoSliceEntity | 分段信息 |
| sliceList | List\<VideoSliceEntity\> | 分段列表 |
| engSerViceType | string | ASR 引擎模型 |
