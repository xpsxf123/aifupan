# Words Data — 数据模型

> replay-words 模块完整数据表结构。共 60 张 MySQL 表 + 2 个 MongoDB 集合。

---

## 一、MySQL 表（MyBatis-Plus，60 张）

所有表统一使用：雪花 ID (`IdType.INPUT`)、手动软删除 (`isDeleted`)、手动时间戳 (`createDate`/`updateDate`)。

### 1. 主播域（4 张）

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_anchor_url` | AnchorUrlEntity | secUid, homeUrl, liveUrl, anchorName, anchorAvatar, platform(0:抖音/1:快手/2:视频号), anchorNumber, systemTradeId, aiCorrectTradeId | 主播基本信息 |
| `tb_anchor_url_details` | AnchorUrlDetailsEntity | sec_uid(PK), keywordStatus(0未获取/1成功/2失败), keyword | 主播补充信息，PK 为 sec_uid |
| `tb_anchor_url_user` | AnchorUrlUserEntity | anchorUrlSecUid, userId, tenantId, tradeId, 40+ 配置字段（录制/监控/上传/诊断/切片） | 用户-主播绑定，最大实体 |
| `tb_anchor_url_white` | AnchorUrlWhiteEntity | secUid, userId | 主播白名单 |

### 2. 视频域（4 张）

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_anchor_video` | AnchorVideoEntity | videoId, videoName, secUid, userId, tradeId, duration, batchNumber, analysisStatus(0-3), uploadStatus, deleteStatus(0-2), tenantId, 40+ 字段 | 录制视频核心表 |
| `tb_anchor_video_detail` | AnchorVideoDetailEntity | videoId, natureContentStatus(0-3), optimizeContentStatus(0-3), hasDiagnosisReport, importantBarrageStatus | 视频详情/生成状态 |
| `tb_anchor_video_recod` | AnchorVideoRecodEntity | userId, videoId, videoName, duration, secUid, tradeId | 视频分析历史记录 |
| `tb_video_slice` | VideoSliceEntity | userId, tenantId, sourceId, sourceType(0:视频/1:文件), sliceType, startMillisecond, endMillisecond | 视频切片 |

### 3. 文件上传域（5 张）

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_upload_file` | UploadFileEntity | fileId, fileName, fileType(0:视频/1:音频/2:文本), playUrl, tradeId, analysisStatus(0-3), userId, tenantId, fileSliceType | 上传文件 |
| `tb_upload_file_detail` | UploadFileDetailEntity | fileId, natureContentStatus(0-3), optimizeContentStatus(0-3), hasDiagnosisReport | 文件详情 |
| `tb_upload_file_analysis` | UploadFileAnalysisEntity | fileId, userId, status(0:成功/1:失败), dataJson, tradeId, paragraph, version | 文件分析结果（ID自增） |
| `tb_upload_file_analysis_record` | UploadFileAnalysisRecordEntity | userId, fileId, tradeId, storeFileName, storeFileOssKey, version, cruxWordNum, sensitiveWordNum | 文件分析版本记录 |
| `tb_upload_file_recod` | UploadFileRecodEntity | fileName, fileId, fileType, fileDuration, tradeId, userId | 文件上传记录（旧） |

### 4. AI 分析域（7 张）

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_ai_analysis` | AiAnalysisEntity | uuid, contextId, type(0:录制/1:上传), textType(0:提问/1:回答), sort, storeFileName | AI 分析会话 |
| `tb_ai_analysis_record` | AiAnalysisRecordEntity | uuid, recordType, sensitiveWordTotal, sensitiveWordMark | AI 分析统计 |
| `tb_ai_analysis_sensitive_rela` | AiAnalysisSensitiveRelaEntity | uuid, recordType, tradeId, sensitiveWord, sensitiveType(0:敏感词/1:关键词/2:白名单) | AI 分析-词关联 |
| `tb_ai_cue_button` | AiCueButtonEntity | tradeId(0=全部), resourceType(0:系统), buttonName, problem, buttonType, scope(0:全文/1:段落), scene(0:直接/1:弹窗) | AI 按钮提示 |
| `tb_ai_train` | AiTrainEntity | userId, tenantId, videoId, tradeId, aiStatus(0:训练中/1:完成/2:超时), progressRange | AI 训练任务 |
| `tb_ai_optimize_purpose` | AiOptimizePurposeEntity | sourceId, sourceType, userId, tenantId, optimizeAction, optimizePurpose | AI 优化目的 |
| `tb_history_paragraph` | HistoryParagraphEntity | tenantId, userId, alias, sourceType, sourceId, code, content | AI 问答历史 |

### 5. 词汇域（12 张）

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_crux_type` | CruxTypeEntity | name, level, sort, parentId, isShowCompass, isCount | 关键词类型树 |
| `tb_crux_words` | CruxWordsEntity | userId, resourceType(0:系统/1:自定义), type, platformType, name, tradeId, similarWords | 关键词 |
| `tb_sensitive_words` | SensitiveWordsEntity | userId, resourceType, type, platformType, level(0-2), name, tradeId, similarWords, wordsType, cruxTypeId, status(0:启用/1:禁用) | 平台敏感词 |
| `tb_sensitive_words_client` | SensitiveWordsClientEntity | 同 SensitiveWordsEntity 结构 | 客户端敏感词 |
| `tb_word_rule` | WordRuleEntity | wordId, type(0:左/1:右/2:任意), rangeLength, blackOrWhite(0:黑/1:白/2:限定) | 词匹配规则 |
| `tb_word_rule_relevance` | WordRuleRelevanceEntity | ruleId, relevanceWord | 规则关联词 |
| `tb_lexicon` | LexiconEntity | userId, tradeId, name | 词库 |
| `tb_lexicon_word` | LexiconWordEntity | lexiconId, wordId, wordsType(0:敏感词/1:关键词/2:白名单) | 词库-词关联 |
| `tb_cue_words` | CueWordsEntity | tradeId, tenantId(0=系统/非0=租户私有), cueWord(短标题), problem(prompt 文本主体), cueType(askType, 0-15 中 11 种), applyTo(0=单场/1=多场聚合), scope(0=直播/1=短视频), accountType(0/1, 推断为平台账号变体), syncScene(1=默认/2=跨场对比/3=同直播间不同场次), scene(几乎全 0), resourceType(全 0,疑似死字段), sort(per-trade 顺序,跨 trade 不稳定) | 提示词（详见 [research/cue-words-askType-inventory.md](../research/20260524__cue-words-askType-inventory.md) §3 字段语义推断） |
| `tb_model_crux` | ModelCruxEntity | modelId, cruxTypeId, scale(比例) | 模型-关键词比例 |
| `tb_data_model` | DataModelEntity | name, type(0:通用/1:行业), tradeId | 数据模型 |
| `tb_anchor_crux_words` | AnchorCruxWordsEntity | keyword | 主播关键词（旧） |
| `tb_anchor_crux_words_rela` | AnchorCruxWordsRelaEntity | secUid, keywordId, userId, tenantId | 主播-关键词关联 |

### 6. 数据看板域（6 张）

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_video_data_viewing` | VideoDataViewingEntity | totalWatchNum, averageOnlineNum, incrementFollowerCount, convertFanRate, volume/purchase/customerUnitPrice/uvValue/goodsConvertRate (各含 Start/End 和 Confuse 版) | 数据看板原表 |
| `tb_video_data_viewing_confuse` | VideoDataViewingConfuseEntity | 同上指标字段 + dataStatus(0-8), dataSourceType(0:蝉妈妈/1:巨量百应), gpmStart/End, roi 系列 | 混淆数据看板 |
| `tb_video_data_viewing_paragraph` | VideoDataViewingParagraphEntity | 同上 + dataViewingConfuseId | 本段看板数据 |
| `tb_video_data_viewing_ratio` | VideoDataViewingRatioEntity | ratioName, ratioCode, ratioStart, ratioEnd | 看板比率配置 |
| `tb_data_screenshot` | DataScreenshotEntity | tenantId, userId, screenshotCode, sourceId, sourceImagesAddress, screenshotStatus(0-4) | 数据截图 |
| `tb_data_screenshot_config` | DataScreenshotConfigEntity | screenshotCode, title, sort, sourceType | 截图配置 |

### 7. 行业/热榜域（7 张）

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_trade` | TradeEntity | name, parentId, sort, rankEnabled(0:禁用/1:启用), thirdCollect, defaultGeneralModelId | 行业分类树 |
| `tb_similar_anchor` | SimilarAnchorEntity | similarScore, liveAverageUv, followerCount, liveAverageAmount, authorId, uniqueId, secUid, anchorName, accountHeat | 相似达人 |
| `tb_similar_collect` | SimilarCollectEntity (@Deprecated) | tradeId, similarAnchorId, similarSecUid | 相似采集（已废弃） |
| `tb_similar_send_record` | SimilarSendRecordEntity | userId, tenantId, anchorNumber, secUid, requestId, requestBody, responseBody, callbackBody | 相似达人请求记录 |
| `tb_hot_search_trade_ranking_list` | TbHotSearchTradeRankingList | similarAnchorId, tradeId, sourceType(1:系统/2:手动/3:第三方榜单), collectStatus(0-2), upRanking, weightScore | 行业热榜排行（ID自增） |
| `tb_trade_third_ranking` | TradeThirdRanking | parentId, layer, categoryName | 第三方榜单分类树（ID自增） |
| `tb_trade_third_ranking_relation` | TradeThirdRankingRelation | tradeId, thirdRankingId, enableCollect, dayOfWeek, lastCollectTime | 行业-第三方榜单关联（ID自增） |

### 8. 直播数据域（6 张）

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_online_num` | OnlineNumEntity | userId, secUid, batchNumber, recordDate, peopleNum, videoId | 实时在线人数 |
| `tb_total_online_num` | TotalOnlineNumEntity | userId, secUid, batchNumber, recordDate, peopleNum, videoId | 总在线人数 |
| `tb_socket_collect_message` | SocketCollectMessageEntity | userId, secUid, batchNumber, videoId, fileAddress, cosKey, totalOnlineNum, observationNum, totalBarrageNum | WebSocket 采集数据 |
| `tb_total_socket_message` | TotalSocketMessageEntity | userId, tenantId, secUid, batchNumber, totalOnlineNum, totalBulletChatNum | 整场直播汇总 |
| `tb_bless_bag` | BlessBagEntity | userId, tenantId, videoId, batchNumber, lotteryInfo, prizeCount, luckyCount, startTime, drawTime | 福袋数据 |
| `tb_product_details` | ProductDetailsEntity | batchNumber, videoId, productId, title, marketPrice, explainCnt, gpm, payAmt, payCnt, refundRate, 30+ 商品转化指标 | 商品详情 |

### 9. 对比/收藏/标注域（6 张）

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_sync_contrast` | SyncContrastEntity | userId, contrastId, contrastType(0:视频/1:文件), sliceContrastType, tradeOneId, tradeTwoId, syncScene(1-3), cloudRemarks | 对比分析 |
| `tb_source_star` | SourceStarEntity | sourceId, sourceType(0:视频/1:文件/2:对比), userId, tenantId | 星标 |
| `tb_client_ai_fav` | ClientAiFavEntity | tenantId, userId, favType(0:运营/1:违规), dataResourceType, dataResourceUuid | AI 收藏 |
| `tb_analysis_mark` | AnalysisMarkEntity | sourceId, sourceType, paraphStartNo, paraphEndNo, markContent, markNo | 笔记标注 |
| `tb_video_mark` | VideoMarkEntity | fileUuid, type, startDate, endDate, color, remarks | 视频标记 |
| `tb_video_analysis_record` | VideoAnalysisRecordEntity | userId, videoId, tradeId, storeFileName, storeFileOssKey, version, cruxWordNum, sensitiveWordNum | 视频分析版本记录 |

### 10. 其他域（4 张）

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_chanmama_send_record` | ChanmamaSendRecordEntity | userId, tenantId, videoId, requestId, anchorNumber, requestBody, responseBody, callbackBody, dataStatus(0-3), accountType | 蝉妈妈请求记录 |
| `tb_user_analysis_rollup` | UserAnalysisRollupEntity | userId, analysisSum, dayAverageAnalysis, lastAnalysis, contrastAnalysis, promotionName | 用户分析汇总 |
| `tb_user_video_appeal` | UserVideoAppealEntity | userId, nickName, appealReason, anchorVideoId, status(0:待处理/1:已处理), handleUserId, handleRemarks | 视频申诉 |
| `tb_basic_settings` | BasicSettingsEntity | sourceId, sourceType, userId, tenantId, accountType, accountStage, optimizeDirection | 基础设置 |
| `tb_audio_analysis` | AudioAnalysisEntity | videoId, userId, status(0:成功/1:失败), dataJson, tradeId, paragraph, version | 音频分析（ID自增） |

---

## 二、MongoDB 集合（2 个）

| 集合名 | 文档类 | 核心字段 | 索引 |
|--------|--------|----------|------|
| `replay_video_content` | VideoContentEntity | sourceId, content, cueWord, generateStatus, paragraph, type(1:自然/2:优化), sourceType, userId, tenantId | `{sourceId, type, userId, tenantId}` |
| `replay_video_text_notes` | VideoTextNotes | tenantId, userId, sourceId, sourceType, notesType(1:原文/2:小结/3:分段), contentHash, lastVersion, contentFilePath(@Indexed unique), editors[] | `{sourceId, sourceType, notesType}`, `{contentFilePath}` unique |

### VideoTextNotes 版本机制

- `contentFilePath` 使用 `String.format` 模式，如 `/path/to/file_v%s.html`
- `upgrade(contentHash, user)` 方法实现版本递增 + 编辑者记录
- 通过 `historyFilePaths()` 获取所有历史版本路径
- 内嵌 `Editor` 对象数组：`{userId, userName, editTime, version}`

---

## 三、数据层分层

```
Controller → Logic → Bll → Producer → Service (IService<Entity>)
                                     → Dao (BaseMapper<Entity>)
                                     → MongoRepository
```

- **DAO 层**: 60 个接口，统一继承 `BaseMapper<Entity>`，MyBatis-Plus 注解驱动，无 XML 映射文件
- **Service 层**: 60 个接口继承 `IService<Entity>` + 60 个实现继承 `ServiceImpl<Dao, Entity>`
- **MongoDB 层**: 2 个 `MongoRepository` + 1 个 `MongoTemplate` 聚合查询服务
- **ID 策略**: 所有表使用雪花 ID 手动赋值 (`SnowflakeManager.nextValue()`)，`@TableId(type = IdType.INPUT)`；5 个例外使用 `IdType.AUTO`
- **软删除**: 所有表通过 `isDeleted` 字段手动标记（非 MyBatis-Plus `@TableLogic`）
- **时间戳**: 手动 `new Date()` / `LocalDateTime.now()` 设置（非自动填充）
