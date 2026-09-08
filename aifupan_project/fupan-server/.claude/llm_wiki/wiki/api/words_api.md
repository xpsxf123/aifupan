# Words API -- replay-api 管理端接口契约

> replay-api 模块下 `controller/words/` 全部 39 个 Controller 的完整 API 表面。所有接口统一返回 `R<T>`，分页使用 `PageUtils<T>`，须通过 token 认证。

---

## 一、主播管理

### AnchorUrlController -- `/replay/anchorurl`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| GET | `/getAiTrade` | 获取Ai推荐的行业 | `?secUid` (String) | `R<String>` |
| GET | `/sendSwitchAnchorMsg` | 发送上下播主播消息 | `?anchorUrlName` (String, 必填) + `type` (Integer, 必填, 0上播/1下播) | `R<String>` |
| POST | `/clientAnchorRecordList` | 客户端获取AI复盘主播列表 | `ClientAnchorListBo` | `R<PageUtils<AnchorRecordListVo>>` |
| POST | `/clientTenantAnchorList` | 客户端获取云空间主播列表 | -- | `R<List<AnchorUrlUserVo>>` |
| POST | `/topAnchor` | 置顶主播 | `TopAnchorBo` | `R<String>` |
| GET | `/getUserAnchorBySecUid` | 根据secUid获取用户主播信息（同租户） | `?secUid` (String, 必填) | `R<AnchorUrlUserVo>` |
| GET | `/getCurrUserAnchorBySecUid` | 根据secUid获取用户主播信息 | `?secUid` (String, 必填) | `R<AnchorUrlUserVo>` |
| GET | `/clientAnchorList` | 客户端获取主播列表 | -- | `R<List<AnchorUrlUserVo>>` |
| POST | `/listAnchorYesterdayRecord` | 根据主播secuid集合获取昨日录制场次 | `List<String>` (secUid列表) | `R<List<AnchorYesterdayRecordVo>>` |
| POST | `/listByUniques` | 根据唯一标识集合获取主播 | `List<String>` (secUid/homeUrl/liveUrl) | `R<List<AnchorUrlInfoVo>>` |
| POST | `/bindUserAnchor` | 绑定主播和用户的关系 | `UserAnchorBo` | `R<String>` |
| POST | `/updateUserAnchor` | 修改用户绑定的主播信息 | `AnchorUrlUserBo` | `R<String>` |
| POST | `/addOrUpdateAnchor` | 添加或修改用户的主播信息 | `AddOrUpdateAnchorBo` | `R<String>` |
| POST | `/saveOrUpdateAnchor` | 添加或修改主播信息 | `AnchorUrlBo` | `R<String>` |
| POST | `/userAddAnchorRecord` | 根据userId获取当前用户添加的主播信息 | `AnchorUrlUserBo` | `R<PageUtils<AnchorUrlVo>>` |
| POST | `/selectAnchorByUserId` | 根据user_id查询用户关联的主播 | `AnchorUrlUserBo` | `R<PageUtils<AnchorUrlVo>>` |
| POST | `/removeAnchorWhite` | 删除主播白名单 | `List<AnchorUrlWhiteBo>` | `R<String>` |
| GET | `/selectUserByAnchorWhite` | 查询主播所属用户信息（白名单） | `?secUid` (String) | `R<List<UserVo>>` |
| GET | `/saveAnchorInUserWhite` | 添加主播至用户白名单 | `?userId` (Long) + `secUid` (String) | `R<String>` |
| GET | `/infoBySecUid` | 主播url信息 | `?secUid` + `liveUrl` + `homeUrl` | `R<AnchorUrlInfoVo>` |
| GET | `/infoBySecUidOne` | 主播url信息（仅secUid） | `?secUid` (String) | `R<AnchorUrlInfoVo>` |
| POST | `/save` | 新增主播url | `AnchorUrlBo` | `R<String>` |
| POST | `/saveBatch` | 批量保存 | `List<AnchorUrlBo>` | `R<String>` |
| POST | `/listBySecUids` | 根据主播唯一标识集合获取主播 | `List<String>` | `R<List<AnchorUrlInfoVo>>` |
| POST | `/listLiveBySecUids` | 根据secUid集合获取有live地址的主播 | `List<String>` | `R<List<AnchorUrlInfoVo>>` |
| GET | `/listByUserToken` | 根据userToken获取主播列表 | -- | `R<List<AnchorClientVo>>` |
| POST | `/saveBatchs` | 新增用户与主播绑定 | `List<AnchorUrlUserBo>` | `R<String>` |
| GET | `/deletBysecuid` | 删除用户与主播绑定关系 | `?secUid` (String, 必填) | `void` |
| POST | `/seletByUserId` | 服务端获取用户所绑定主播列表 | `AnchorUrlPegBo` | `R<PageUtils<AnchorUrlVo>>` |
| POST | `/seletAnchorUrl` | 服务端获取主播列表 | `AnchorUrlPegBo` | `R<PageUtils<AnchorUrlVo>>` |
| POST | `/seletBysecUidAnchorUrlWhite` | 客户查询用户是否有录制该主播 | `List<String>` | `R<List<String>>` |
| GET | `/seletBySerId` | 客户根据secUid查询是否有录制 | `?secUidS` (String) | `R<Boolean>` |
| POST | `/saveAnchorUrlWhite` | 主播保存白名单 | `AnchorUrlWhiteBo` | `R<String>` |
| POST | `/removeAnchorUrlWhite` | 服务端主播列表删除用户白名单 | `AnchorUrlWhiteBo` | `R<String>` |
| POST | `/seletUidAnchorUrlWhite` | 服务端查询主播的白名单 | `AnchorUrlWhiteListBo` | `R<PageUtils<UserListVo>>` |
| GET | `/openMonitoringPosition` | 新添加主播后自动打开监控位 | `?secUid` (String) | `R<OpenMonitoringPositionVo>` |
| GET | `/generatedAnchorKeywords` | 生成关键词 | `?secUid` (String) | `R<String>` |

### AnchorCallbackController -- `/open/callback/anchor`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/channel-cancel` | 微信视频号取消授权回调 | `CancelChannelAnchorBo` (`authorizerInfoId`, `userIds`) | `R<Void>` |

---

## 二、视频管理

### AnchorVideoController -- `/replay/AnchorVideo`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/clientVideoList` | 客户端获取视频列表 | `ClientVideoListBo` | `R<PageUtils<AnchorVideoInfoVo>>` |
| POST | `/clientListVideoByVideoIds` | 客户端根据视频id集合获取视频列表 | `List<String>` (videoId) | `R<List<AnchorVideoInfoVo>>` |
| POST | `/clientDeleteVideo` | 客户端删除视频 | `List<String>` (videoId) | `R<String>` |
| GET | `/clientDeleteCloudVideo` | 客户端删除云空间视频 | `?videoId` (String, 必填) | `R<String>` |
| POST | `/clientListCloudVideo` | 客户端获取云空间视频列表 | `ClientVideoListBo` | `R<PageUtils<AnchorVideoInfoVo>>` |
| GET | `/clientGetVideoByVideoId` | 根据视频唯一标识获取视频信息 | `?videoId` (String) | `R<AnchorVideoInfoVo>` |
| POST | `/listUserVideo` | 获取用户视频列表 | `ListUserVideoByConditionBo` | `R<List<AnchorVideoInfoVo>>` |
| POST | `/updateVideoAnalysisStatus` | 修改视频的分析状态 | `UpdateVideoAnalysisStatusBo` | `R<String>` |
| POST | `/updateVideoUploadStatus` | 修改视频的上传状态 | `UpdateVideoUploadStatusBo` | `R<String>` |
| POST | `/updateVideoSizeDuration` | 批量修改视频大小时长 | `List<UpdateVideoSizeDurationBo>` | `R<String>` |
| GET | `/initVideoAndFileAnalysisStatus` | 初始化视频和文件分析状态（分析中→失败） | -- | `R<String>` |
| POST | `/saveOrUpdateVideo` | 保存或修改视频信息 | `AnchorVideoInfoBo` | `R<String>` |
| GET | `/getAnalysisInfo` | 查看视频或文件分析内容 | `?fileId` + `videoId` (均可选) | `R<OnlineAnalysisInfoVo>` |
| GET | `/getVideoListByTenantId` | 客户端根据租户id获取视频列表 | -- | `R<List<AnchorVideoInfoVo>>` |
| GET | `/infoByVideoId` | 根据视频唯一标识获取视频信息 | `?videoId` (String) | `R<AnchorVideoInfoVo>` |
| POST | `/syncVideoAnalysisToServer` | 客户端同步分析数据到服务器 | `SyncVideoAnalysisBo` | `R<String>` |
| GET | `/checkVideoAnalysisExist` | 判断服务器是否有视频分析数据 | `?videoId` + `tradeId` (Long) | `R<Boolean>` |
| GET | `/deleteOnlineVideo` | 从云点播删掉视频文件并加回容量 | `?videoId` (String) | `R<String>` |
| POST | `/videoAnalysisByUserId` | 根据user_id获取录制分析 | `AnchorVideoVO` | `R<PageUtils<AnchorVideoVO>>` |
| GET | `/videoAnalysisByVideoId` | 根据video_id获取录制分析 | `?videoId` (String) | `R<AnchorVideoVO>` |
| POST | `/selectVideoBySecUid` | 根据主播sec_uid获取已录制视频 | `AnchorVideoBo` | `R<PageUtils<AnchorVideoVO>>` |
| POST | `/saveVideoinfo` | 客户端保存录制视频信息 | `AnchorVideoInfoBo` | `R<String>` |
| GET | `/selectByuserId` | 客户端查询录制视频信息 | -- | `R<List<AnchorVideoVO>>` |
| POST | `/pageLists` | 服务端分页查询录制视频信息 | `AnchorVideoBo` | `R<PageUtils<AnchorVideoVO>>` |
| POST | `/selectByVideoId` | 服务端根据视频Id查询分析内容 | `AudioAnalysisBo` | `R<List<AudioAnalysisEntity>>` |
| GET | `/selectAnalysisByVideoId` | 根据视频ID查询视频的分析内容 | `?videoId` + `tradeId` (Long) | `R<List<SentenceMarkVo>>` |
| GET | `/selectAnalysisByFileId` | 根据文件id获取分析内容 | `?fileId` + `tradeId` (Long) | `R<List<SentenceMarkVo>>` |
| POST | `/UpdateVideo` | 客户端更新视频信息 | `AnchorVideoInfoBo` | `R<String>` |
| POST | `/saveAnchorVideoRecod` | 保存分析记录 | `List<AudioAnalysisVo>` | `R<String>` |
| POST | `/selectAnchorVideoRecod` | 服务端分析记录查询 | `AnchorVideoBo` | `R<PageUtils<AnchorVideoVO>>` |
| POST | `/removeByVoidId` | 根据List<voidId>删除 | `List<String>` | `R<String>` |
| POST | `/selectByVideoIdOrTradeId` | Id查询该视频的分析内容 | `AudioAnalysisBo` | `R<List<AudioAnalysissVO>>` |
| POST | `/listByUserId` | 服务端用户详情查询录制记录 | `AnchorVideoBo` | `R<PageUtils<AnchorVideoVO>>` |
| POST | `/selectVideoRecod` | 服务端用户详情查询分析记录 | `AnchorVideoBo` | `R<PageUtils<AnchorVideoRecodListVo>>` |
| GET | `/clearAnalysis` | 清除视频的分析数据 | `?videoId` (String) | `R<String>` |
| GET | `/startImportantBarrage` | 开始获取重要弹幕 | `?videoId` (String, @NotNull) | `R<Boolean>` |
| GET | `/getDataDiagnosisConfig` | 获取数据诊断配置 | `?sourceId` + `sourceType` + header `webVersion` | `R<DataDiagnosisConfigVo>` |
| POST | `/updateDataDiagnosisConfig` | 更新数据诊断配置 | `DataDiagnosisConfigVo` + header `webVersion` | `R<DataDiagnosisConfigVo>` |

---

## 三、词语规则管理

### WordRuleController -- `/words/wordrule`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 词语匹配规则列表（分页） | `WordRuleListBo` | `R<PageUtils<WordRuleListVo>>` |
| GET | `/info` | 词语匹配规则信息 | `?id` (Long, 必填) | `R<WordRuleInfoVo>` |
| POST | `/save` | 新增词语匹配规则 | `WordRuleBo` | `R<String>` |
| POST | `/update` | 修改词语匹配规则 | `WordRuleBo` | `R<String>` |
| GET | `/delete` | 删除词语匹配规则 | `?id` (Long, 必填) | `R<String>` |

### WordRuleRelevanceController -- `/words/wordrulerelevance`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 规则关联词列表（分页） | `WordRuleRelevanceListBo` | `R<PageUtils<WordRuleRelevanceListVo>>` |
| GET | `/info` | 规则关联词信息 | `?id` (Long, 必填) | `R<WordRuleRelevanceInfoVo>` |
| POST | `/save` | 新增规则关联词 | `WordRuleRelevanceBo` | `R<String>` |
| POST | `/update` | 修改规则关联词 | `WordRuleRelevanceBo` | `R<String>` |
| GET | `/delete` | 删除规则关联词 | `?id` (Long, 必填) | `R<String>` |

### SensitiveWordsController -- `/replay/sensitivewords`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/importExcel` | 敏感词导入 | multipart `file` (excel) | `R<List<String>>` |
| POST | `/wordsMarkReAnalysis` | 二次分析 | `WordsMarkReAnalysisBo` | `R<List<SentenceMarkVo>>` |
| POST | `/wordsMark` | 文字关键词/敏感词标识 | `List<WordsMarkBo>` | `R<List<SentenceMarkVo>>` |
| POST | `/wordsMarkByText` | 文本关键词/敏感词标识 | `WordsMarkBo` | `R<SentenceMarkVo>` |
| POST | `/list` | 敏感词列表（分页） | `SensitiveWordsListBo` | `R<PageUtils<SensitiveWordsListVo>>` |
| GET | `/info` | 敏感词信息 | `?id` (Long, 必填) | `R<SensitiveWordsInfoVo>` |
| POST | `/save` | 新增敏感词 | `SensitiveWordsBo` | `R<List<String>>` |
| POST | `/saveBatch` | 批量新增敏感词 | `SensitiveWordsBatchBo` | `R<List<String>>` |
| POST | `/update` | 修改敏感词 | `SensitiveWordsBo` | `R<String>` |
| GET | `/delete` | 删除敏感词 | `?id` (Long, 必填) | `R<String>` |

### SensitiveWordsClientController -- `/replay/sensitivewordsClient`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 客户端自定义词语列表（分页） | `SensitiveWordsClientListBo` | `R<PageUtils<SensitiveWordsClientListVo>>` |
| GET | `/info` | 获取客户端自定义词语信息 | `?id` (Long, 必填) | `R<SensitiveWordsClientInfoVo>` |
| POST | `/save` | 新增客户端自定义词语 | `SensitiveWordsClientBo` | `R<List<String>>` |
| POST | `/update` | 修改客户端自定义词语 | `SensitiveWordsClientBo` | `R<String>` |
| GET | `/delete` | 删除客户端自定义词语 | `?id` (Long, 必填) | `R<String>` |

### LexiconController -- `/replay/lexicon`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/getWordsList` | 获取词库的词语列表 | `LexiconWordListBo` | `R<PageUtils<SensitiveWordsClientListVo>>` |
| POST | `/list` | 词库列表（分页） | `LexiconListBo` | `R<PageUtils<LexiconListVo>>` |
| GET | `/info` | 词库信息 | `?id` (Long, 必填) | `R<LexiconInfoVo>` |
| POST | `/save` | 新增词库 | `LexiconBo` | `R<String>` |
| POST | `/update` | 修改词库 | `LexiconBo` | `R<String>` |
| GET | `/delete` | 删除词库 | `?id` (Long, 必填) | `R<String>` |

### LexiconWordController -- `/words/lexiconword`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 词库-词语关联列表（分页） | `LexiconWordListBo` | `R<PageUtils<LexiconWordListVo>>` |
| GET | `/info` | 词库-词语关联信息 | `?id` (Long, 必填) | `R<LexiconWordInfoVo>` |
| POST | `/save` | 新增词库-词语关联 | `LexiconWordBo` | `R<String>` |
| POST | `/update` | 修改词库-词语关联 | `LexiconWordBo` | `R<String>` |
| GET | `/delete` | 删除词库-词语关联 | `?id` (Long, 必填) | `R<String>` |

---

## 四、关键词与类型管理

### CruxWordsController -- `/replay/cruxwords`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 关键词列表（分页） | `CruxWordsListBo` | `R<PageUtils<CruxWordsListVo>>` |
| GET | `/info` | 关键词信息 | `?id` (Long, 必填) | `R<CruxWordsInfoVo>` |
| POST | `/save` | 新增关键词 | `CruxWordsBo` | `R<String>` |
| POST | `/saveBatch` | 批量新增关键词 | `CruxWordsBatchBo` | `R<String>` |
| POST | `/update` | 修改关键词 | `CruxWordsBo` | `R<String>` |
| GET | `/delete` | 删除关键词 | `?id` (Long, 必填) | `R<String>` |

### CruxTypeController -- `/replay/cruxtype`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| GET | `/listTree` | 关键词类型列表（树形结构） | `?childrenNotNull` (Integer, 0=null/1=空集合) | `R<List<CruxTypeTreeVo>>` |
| GET | `/getShowCompassList` | 获取在数据罗盘展示的关键词类型列表 | -- | `R<List<CruxTypeVo>>` |
| POST | `/list` | 关键词类型列表（分页） | `CruxTypeListBo` | `R<PageUtils<CruxTypeListVo>>` |
| GET | `/info` | 关键词类型信息 | `?id` (Long, 必填) | `R<CruxTypeInfoVo>` |
| POST | `/save` | 新增关键词类型 | `CruxTypeBo` | `R<String>` |
| POST | `/update` | 修改关键词类型 | `CruxTypeBo` | `R<String>` |
| GET | `/delete` | 删除关键词类型 | `?id` (Long, 必填) | `R<String>` |

### TradeController -- `/replay/trade`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/listByTenantAnchor` | 根据用户租户主播返回行业列表 | -- | `R<List<AnchorTradeListVo>>` |
| POST | `/listByAnchor` | 根据用户添加的主播返回行业列表 | -- | `R<List<AnchorTradeListVo>>` |
| GET | `/listTree` | 获取行业列表（树形结构） | `?childrenNotNull` (Integer) | `R<List<TradeTreeVo>>` |
| GET | `/listSimpleTree` | 获取行业列表（树形结构-简化版） | `?childrenNotNull` (Integer) | `R<List<TradeSimpleTreeVo>>` |
| POST | `/list` | 行业列表（分页） | `TradeListBo` | `R<PageUtils<TradeListVo>>` |
| GET | `/info` | 行业信息 | `?id` (Long, 必填) | `R<TradeInfoVo>` |
| POST | `/save` | 新增行业 | `TradeBo` | `R<String>` |
| POST | `/update` | 修改行业 | `TradeBo` | `R<String>` |
| GET | `/delete` | 删除行业 | `?id` (Long, 必填) | `R<String>` |
| GET | `/deleteTradeModel` | 删除行业模型 | `?tradeId` (Long) + `modelId` (Long, 必填) | `R<String>` |

---

## 五、数据罗盘与看板

### DataModelController -- `/replay/datamodel`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 罗盘数据模型列表（分页） | `DataModelListBo` | `R<PageUtils<DataModelListVo>>` |
| GET | `/info` | 罗盘数据模型信息 | `?id` (Long, 必填) | `R<DataModelInfoVo>` |
| POST | `/save` | 新增罗盘数据模型 | `DataModelBo` | `R<String>` |
| POST | `/update` | 修改罗盘数据模型 | `DataModelBo` | `R<String>` |
| GET | `/delete` | 删除罗盘数据模型 | `?id` (Long, 必填) | `R<String>` |
| POST | `/saveDataModel` | 新增通用模型及其关键词类型 | `DataModelSaveBo` | `R<String>` |
| POST | `/modelCruxTypeList` | 查询模型及其下关键词类型 | `DataModelListBo` | `R<PageUtils<DataModelSaveBo>>` |
| POST | `/updateDataModel` | 修改通用模型及其关键词类型 | `DataModelSaveBo` | `R<String>` |
| GET | `/deleteDataModel` | 删除通用模型及其关键词类型 | `?id` (Long, 必填) | `R<String>` |
| GET | `/infoDataModel` | 根据ID获取模型及其关键词类型 | `?id` (Long, 必填) | `R<DataModelSaveBo>` |

### ModelCruxController -- `/replay/modelcrux`

标准 CRUD: POST `/list`, GET `/info`, POST `/save`, POST `/update`, GET `/delete`。模型-关键词类型关联表。

### VideoDataViewingController -- `/replay/videodataviewing`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/callback` | 第三方数据平台查询数据回调 | `VideoDataViewingCallbackBo` | `R<String>` |
| GET | `/createDataViewing` | 生成视频场次的看盘数据 | `?videoId` (String, 必填) + `isAuto` (Integer, 0/1) + `anchorOnlineStatus` (Integer, 0/1) | `R<String>` |
| GET | `/infoByVideoId` | 根据视频id获取看盘数据 | `?videoId` (String, 必填) | `R<VideoDataViewingConfuseInfoVo>` |
| GET | `/infoByContrastId` | 根据对比id获取看盘数据 | `?contrastId` (String, 必填) | `R<VideoDataViewingContrastVo>` |
| POST | `/list` | 视频看盘数据列表（分页） | `VideoDataViewingListBo` | `R<PageUtils<VideoDataViewingListVo>>` |
| GET | `/info` | 视频看盘数据信息 | `?id` (Long, 必填) | `R<VideoDataViewingInfoVo>` |
| POST | `/save` | 新增视频看盘数据 | `VideoDataViewingBo` | `R<String>` |
| POST | `/update` | 修改视频看盘数据 | `VideoDataViewingBo` | `R<String>` |
| GET | `/delete` | 删除视频看盘数据 | `?id` (Long, 必填) | `R<String>` |

### VideoDataViewingConfuseController -- `/replay/videodataviewingconfuse`

标准 CRUD: POST `/list`, GET `/info`, POST `/save`, POST `/update`, GET `/delete`。视频看盘混淆数据。

### VideoDataViewingRatioController -- `/replay/videodataviewingratio`

标准 CRUD: POST `/list`, GET `/info`, POST `/save`, POST `/update`, GET `/delete`。数据看盘比例配置。

---

## 六、AI 分析与训练

### AiAnalysisController -- `/replay/aianalysis`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/aiAnalysis` | AI分析内容 | `AiAnalysisBo` | `R<List<String>>` |
| GET | `/listAiAnalysisByUuid` | 根据uuid查询所有AI分析记录 | `?uuid` (String, 必填) | `R<List<List<String>>>` |
| GET | `/analysisStatusByUuid` | 判断是否有正在进行的AI分析 | `?uuid` (String, 必填) + `modelId` (String, 可选) | `R<String>` |
| GET | `/getAllSessionId` | 从redis获取所有sessionId | -- | `R<Set<String>>` |
| POST | `/list` | AI分析表列表（分页） | `AiAnalysisListBo` | `R<PageUtils<AiAnalysisListVo>>` |
| GET | `/info` | AI分析表信息 | `?id` (Long, 必填) | `R<AiAnalysisInfoVo>` |
| POST | `/save` | 新增AI分析表 | `AiAnalysisBo` | `R<String>` |
| POST | `/update` | 修改AI分析表 | `AiAnalysisBo` | `R<String>` |
| GET | `/delete` | 删除AI分析表 | `?id` (Long, 必填) | `R<String>` |

### AiAnalysisRecordController -- `/words/aianalysisrecord`

标准 CRUD: POST `/list`, GET `/info`, POST `/save`, POST `/update`, GET `/delete`。AI分析关键词统计。

### AiAnalysisSensitiveRelaController -- `/words/aianalysissensitiverela`

标准 CRUD: POST `/list`, GET `/info`, POST `/save`, POST `/update`, GET `/delete`。AI分析关键词与记录关联。

### AiCueButtonController -- `/replay/aicuebutton`

标准 CRUD: POST `/list`, GET `/info`, POST `/save`, POST `/update`, GET `/delete`。固定提示按钮管理。

### AiTrainController -- `/replay/aitrain`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | AI训练列表（分页） | `AiTrainListBo` | `R<PageUtils<AiTrainListVo>>` |
| GET | `/info` | AI训练信息 | `?id` (Long, 必填) | `R<AiTrainInfoVo>` |
| GET | `/completeTrain` | 后台完成训练 | `?id` (Long, 必填) | `R<String>` |
| GET | `/infoByVideoId` | 根据视频id获取AI训练信息 | `?videoId` (String, 必填) | `R<AiTrainInfoVo>` |
| POST | `/save` | 新增AI训练 | `AiTrainBo` | `R<String>` |
| POST | `/update` | 修改AI训练 | `AiTrainBo` | `R<String>` |
| GET | `/delete` | 删除AI训练 | `?id` (Long, 必填) | `R<String>` |

### ClientAiFavController -- `/replay/clientaifav`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 运营/违规收藏列表（分页） | `ClientAiFavListBo` | `R<PageUtils<ClientAiFavListVo>>` |
| GET | `/info` | 收藏信息 | `?id` (Long, 必填) | `R<ClientAiFavInfoVo>` |
| POST | `/saveOrUpdate` | 新增或修改收藏 | `ClientAiFavBo` | `R<String>` |
| POST | `/saveOrUpdateByNotExist` | 新增或修改（已删除不新增） | `ClientAiFavBo` | `R<String>` |
| POST | `/save` | 新增收藏 | `ClientAiFavBo` | `R<String>` |
| POST | `/update` | 修改收藏 | `ClientAiFavBo` | `R<String>` |
| GET | `/delete` | 删除收藏 | `?id` (Long, 必填) | `R<String>` |
| POST | `/batchDelete` | 批量删除收藏 | `List<String>` (ids) | `R<String>` |

---

## 七、诊断管理

### DiagnosisModelController -- `/words/diagnosismodel`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 诊断模型设置列表（分页） | `DiagnosisModelListBo` | `R<PageUtils<DiagnosisModelListVo>>` |
| GET | `/info` | 诊断模型设置信息 | `?id` (Long, 必填) | `R<DiagnosisModelInfoVo>` |
| POST | `/save` | 新增诊断模型设置 | `DiagnosisModelBo` | `R<String>` |
| POST | `/update` | 修改诊断模型设置 | `DiagnosisModelBo` | `R<String>` |
| POST | `/saveOrUpdate` | 添加和修改诊断模型设置（@UserLock） | `DiagnosisModelBo` | `R<String>` |
| GET | `/delete` | 删除诊断模型设置 | `?id` (Long, 必填) | `R<String>` |

### DiagnosisCueController -- `/words/diagnosiscue`

标准 CRUD: POST `/list`, GET `/info`, POST `/save`, POST `/update`, GET `/delete`。AI诊断提示词配置。

---

## 八、提示词管理

### CueWordsController -- `/replay/cuewords`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 提示词列表（分页，queryType=1时填充租户信息） | `CueWordsListBo` | `R<PageUtils<CueWordsListVo>>` |
| GET | `/info` | 提示词信息（填充租户账号手机） | `?id` (Long, 必填) | `R<CueWordsInfoVo>` |
| POST | `/save` | 新增提示词 | `CueWordsBo` | `R<String>` |
| POST | `/update` | 修改提示词 | `CueWordsBo` | `R<String>` |
| GET | `/delete` | 删除提示词 | `?id` (Long, 必填) | `R<String>` |

---

## 九、文件上传与分析

### UploadFileController -- `/replay/UploadFile`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/clientFileList` | 客户端获取文件列表 | `ClientFileListBo` | `R<PageUtils<UploadFileInfoVo>>` |
| POST | `/clientDeleteFile` | 客户端删除文件 | `List<String>` (fileId) | `R<String>` |
| GET | `/clientGetFileByFileId` | 客户端根据fileId获取文件信息 | `?fileId` (String) | `R<UploadFileInfoVo>` |
| POST | `/saveOrUpdateFile` | 保存或修改文件信息 | `UploadFileInfoBo` | `R<String>` |
| POST | `/updateFileAnalysisStatus` | 修改文件的分析状态 | `UpdateFileAnalysisStatusBo` | `R<String>` |
| POST | `/updateFileTrade` | 修改文件的行业 | `UpdateFileTradeBo` | `R<String>` |
| POST | `/clientListFileByFileIds` | 客户端根据文件id集合获取文件列表 | `List<String>` (ids) | `R<List<UploadFileInfoVo>>` |
| POST | `/fileAnalysisByUserId` | 根据user_id查询文件上传分析 | `UploadFileBo` | `R<PageUtils<UploadFileVO>>` |
| POST | `/queryPagelist` | 分页查询复盘文件 | `UploadFileBo` | `R<PageUtils<UploadFileVO>>` |
| POST | `/saveUploadFile` | 保存复盘上传文件信息 | `UploadFileBo` | `R<String>` |
| POST | `/updateUploadFile` | 修改复盘上传文件信息 | `UploadFileBo` | `R<String>` |
| GET | `/deleltByFileId` | 根据FileId删除 | `?FileId` (String) | `void` |
| POST | `/saveuploadFileAnalysis` | 保存复盘上传文件分析内容 | `List<UploadFileAnalysisVo>` | `R<String>` |
| POST | `/saveFileAnalysis` | 保存复盘上传文件分析记录及内容 | `List<UploadFileAnalysisBo>` | `R<String>` |
| GET | `/seletByFileId` | 根据FileId查询分析内容 | `?FileId` (String) | `R<List<UploadFileAnalysisVo>>` |
| GET | `/seletList` | 客户端获取复盘上传文件列表 | -- | `R<List<UploadFileVO>>` |
| POST | `/seletContent` | 客户端获取复盘上传文件分析内容 | `UploadFileAnalysisListBo` | `R<List<UploadFileAnalysisVo>>` |
| POST | `/removeByFileId` | 根据List<FileId>删除 | `List<String>` (fileId) | `R<String>` |
| GET | `/clearAnalysis` | 清除文件的分析数据 | `?fileId` (String) | `R<String>` |

### UploadFileAnalysisRecordController -- `/words/updatefileanalysusrecord`

标准 CRUD: POST `/list`, GET `/info`, POST `/save`, POST `/update`, GET `/delete`。文件分析记录。

### VideoAnalysisRecordController -- `/words/videoanalysusrecord`

标准 CRUD: POST `/list`, GET `/info`, POST `/save`, POST `/update`, GET `/delete`。视频分析记录。

---

## 十、在线数据与福袋

### OnlineNumController -- `/replay/onlinenum`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 直播实时在线人数列表（分页） | `OnlineNumListBo` | `R<PageUtils<OnlineNumListVo>>` |
| GET | `/info` | 直播实时在线人数信息 | `?id` (Long, 必填) | `R<OnlineNumInfoVo>` |
| POST | `/saveOrUpdate` | 新增或修改（@UserLock） | `OnlineNumBo` | `R<String>` |
| POST | `/save` | 新增 | `OnlineNumBo` | `R<String>` |
| POST | `/update` | 修改 | `OnlineNumBo` | `R<String>` |
| GET | `/delete` | 删除 | `?id` (Long, 必填) | `R<String>` |

### TotalOnlineNumController -- `/replay/totalonlinenum`

标准 CRUD + POST `/saveOrUpdate` (@UserLock)。直播总观看人次。

### BlessBagController -- `/replay/blessbag`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 福袋信息列表（分页） | `BlessBagListBo` | `R<PageUtils<BlessBagListVo>>` |
| GET | `/info` | 福袋信息 | `?id` (Long, 必填) | `R<BlessBagInfoVo>` |
| GET | `/infoByVideo` | 通过video获取福袋信息 | `?videoId` (String, 必填) | `R<List<BlessBagInfoVo>>` |
| POST | `/save` | 新增福袋 | `BlessBagBo` | `R<String>` |
| POST | `/update` | 修改福袋 | `BlessBagBo` | `R<String>` |
| GET | `/delete` | 删除福袋 | `?id` (Long, 必填) | `R<String>` |

---

## 十一、对比分析

### SyncContrastController -- `/replay/synccontrast`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/clientContrastList` | 客户端获取对比列表 | `ClientContrastListBo` | `R<PageUtils<SyncContrastInfoVo>>` |
| POST | `/clientDeleteContrast` | 客户端删除对比记录 | `List<String>` (ids) | `R<String>` |
| POST | `/clientAddContrast` | 客户端添加对比 | `SyncContrastBo` | `R<String>` |
| POST | `/clientAddCloudContrast` | 客户端添加云空间对比 | `SyncContrastBo` | `R<String>` |
| GET | `/clientDeleteCloudContrast` | 客户端删除云空间对比记录 | `?contrastId` (String, 必填) | `R<String>` |
| POST | `/clientListCloudContrast` | 客户端获取云空间对比列表 | `ClientContrastListBo` | `R<PageUtils<SyncContrastInfoVo>>` |
| GET | `/clientGetContrast` | 客户端获取对比记录信息 | `?contrastId` (String, 必填) | `R<SyncContrastInfoVo>` |
| GET | `/infoByContrastId` | 根据contrastId获取对比数据 | `?contrastId` (String, 必填) | `R<SyncContrastInfoVo>` |
| POST | `/list` | 对比数据列表（分页） | `SyncContrastListBo` | `R<PageUtils<SyncContrastListVo>>` |
| GET | `/listByToken` | 获取用户的对比数据列表 | -- | `R<List<SyncContrastInfoVo>>` |
| GET | `/info` | 对比数据信息 | `?id` (Long, 必填) | `R<SyncContrastInfoVo>` |
| POST | `/save` | 新增对比数据 | `SyncContrastBo` | `R<String>` |
| POST | `/update` | 修改对比数据 | `SyncContrastBo` | `R<String>` |
| GET | `/delete` | 删除对比数据 | `?id` (Long, 必填) | `R<String>` |
| POST | `/listAllSyncContrast` | 分页获取对比记录（填充用户昵称/直播间昵称/行业） | `SyncContrastListBo` | `R<PageUtils<SyncContrastListVo>>` |
| GET | `/getContrastAnalysisInfo` | 查看对比分析内容 | `?contrastId` (String) | `R<OnlineContrastAnalysisInfoVo>` |

---

## 十二、其他管理

### ChanmamaSendRecordController -- `/replay/chanmamasendrecord`

标准 CRUD: POST `/list`, GET `/info`, POST `/save`, POST `/update`, GET `/delete`。第三方数据平台发送记录。

### DataScreenshotController -- `/replay/dataScreenshot`

空 Controller（暂无实现接口）。

### DataScreenshotConfigController -- `/replay/dataScreenshotConfig`

标准 CRUD: POST `/list`, GET `/info`, POST `/save`, POST `/update`, GET `/delete`。数据截图配置。

### VideoMarkController -- `/replay/videomark`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 视频标记列表（分页） | `VideoMarkListBo` | `R<PageUtils<VideoMarkListVo>>` |
| GET | `/listByUUID` | 根据文件uuid获取标记列表 | `?uuid` (String, 必填) | `R<List<VideoMarkListVo>>` |
| GET | `/info` | 视频标记信息 | `?id` (Long, 必填) | `R<VideoMarkInfoVo>` |
| POST | `/save` | 新增视频标记 | `VideoMarkBo` | `R<String>` |
| POST | `/update` | 修改视频标记 | `VideoMarkBo` | `R<String>` |
| GET | `/delete` | 删除视频标记 | `?id` (Long, 必填) | `R<String>` |

### UserVideoAppealController -- `/replay/uservideoappeal`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 用户视频申述列表（分页） | `UserVideoAppealListBo` | `R<PageUtils<UserVideoAppealListVo>>` |
| GET | `/info` | 用户视频申述信息 | `?id` (Long, 必填) | `R<UserVideoAppealInfoVo>` |
| POST | `/save` | 新增申述 | `UserVideoAppealBo` | `R<String>` |
| POST | `/update` | 修改申述 | `UserVideoAppealBo` | `R<String>` |
| GET | `/delete` | 删除申述 | `?id` (Long, 必填) | `R<String>` |
| POST | `/handleAppeal` | 处理申述 | `UserVideoAppealBo` | `R<String>` |

### UserAnalysisRollupController -- `/words/useranalysisrollup`

| Method | Path | Summary | Request | Resp data |
|--------|------|---------|---------|-----------|
| POST | `/list` | 用户分析汇总列表（分页） | `UserAnalysisRollupListBo` | `R<PageUtils<UserAnalysisRollupListVo>>` |
| GET | `/info` | 用户分析汇总信息 | `?id` (Long, 必填) | `R<UserAnalysisRollupInfoVo>` |
| POST | `/save` | 新增 | `UserAnalysisRollupBo` | `R<String>` |
| POST | `/update` | 修改 | `UserAnalysisRollupBo` | `R<String>` |
| GET | `/delete` | 删除 | `?id` (Long, 必填) | `R<String>` |
| GET | `/timingUpdateData` | 定时更新用户分析汇总数据 | -- | `void` |

---

## 通用约定

- **统一响应**: 所有接口返回 `R<T>` 包装 (`com.jiuyu.replay.generic.vo.common.R`)
- **分页**: 使用 `PageUtils<T>` 包装，入参 `page`(默认1) + `limit`(默认10)
- **认证**: 通过 `SignatureAuthFilter` + `LoginInterceptor` 双重校验（AnchorCallbackController 除外，其路径为 `/open/callback/anchor`）
- **API 文档**: Knife4j (Swagger 3)，访问 `/doc.html`
- **跨域**: 大部分 Controller 标注 `@CrossOrigin`
- **标准 CRUD 模式（含分页）**: POST `/list` (ListBo body) / GET `/info?id=` / POST `/save` (Bo body) / POST `/update` (Bo body) / GET `/delete?id=`
- **BO/VO 类型**: 位于 `replay-words` 模块 `com.jiuyu.replay.words.bo.*` / `vo.*` 和 `replay-generic` 模块 `com.jiuyu.replay.generic.vo.words.*` / `bo.words.*`
- **幂等控制**: `OnlineNumController#saveOrUpdate` 和 `TotalOnlineNumController#saveOrUpdate` 使用 `@UserLock`; `DiagnosisModelController#saveOrUpdate` 使用 `@UserLock`
