# Words Domain — 业务概念与词汇表

> replay-words 模块核心业务概念定义。Agent 在进行 Explorer/Propose 阶段时必须使用此术语表，避免领域漂移。

---

## 核心概念

| 概念 | 定义 | 关联概念 | 状态机 |
|------|------|----------|--------|
| **AnchorUrl (主播)** | 抖音/快手/视频号主播的唯一标识，通过 `secUid` 关联直播间 | AnchorUrlUser, AnchorVideo | — |
| **AnchorUrlUser (用户-主播绑定)** | 平台用户与主播的绑定关系，管理录制、监控、诊断等配置 | User, Tenant | — |
| **AnchorVideo (录制视频)** | 用户通过客户端录制的直播视频文件，核心分析对象 | AnchorUrl, VideoAnalysis | `VideoAnalysisEnum` |
| **UploadFile (上传文件)** | 用户上传的视频/音频/文本文件，与 AnchorVideo 并列的分析源 | FileAnalysis | `VideoAnalysisEnum` |
| **SyncContrast (对比分析)** | 两场直播/两个文件的同屏对比分析 | AnchorVideo, UploadFile | — |
| **VideoSlice (视频切片)** | 从录制视频中截取的片段，分复盘切片和短视频切片两类 | AnchorVideo | — |
| **CruxWords (关键词)** | 运营分析用的关键词库，用于匹配视频内容中的业务要点 | CruxType, Trade | — |
| **CruxType (关键词类型)** | 关键词的树形分类体系（如订单关闭、互动、其他） | CruxWords, DataModel | — |
| **SensitiveWords (敏感词)** | 平台级敏感词库，按违规等级分类（一级封号/二级严重警告/三级警告） | Trade, CruxType | `status`: 0启用/1禁用 |
| **SensitiveWordsClient (客户端敏感词)** | 用户自定义敏感词，与平台级隔离 | SensitiveWords | — |
| **WordRule (词规则)** | 基于邻近范围的词匹配规则：左边包含/右边包含/任意一边包含 | WordRuleRelevance, LexiconWord | `blackOrWhite`: 0黑名单/1白名单/2限定词 |
| **WordRuleRelevance (规则关联词)** | 与词规则绑定的具体关联词汇 | WordRule | — |
| **Lexicon (词库)** | 用户创建的自定义词汇集合，用于组织敏感词和关键词 | LexiconWord, Trade | — |
| **CueWords (提示词)** | AI 分析的提示词模板，分运营提示词/违规提示词/对比复盘提示词 | Trade, AI | `scope`: 0全文/1段落 |
| **AiCueButton (AI 按钮提示词)** | 客户端 AI 分析界面的固定提示按钮 | CueWords | — |
| **AiAnalysis (AI 分析会话)** | 一次 AI 分析的完整会话记录，含多轮对话 | VideoContent | — |
| **AiTrain (AI 训练记录)** | AI 模型训练任务的状态追踪 | AnchorVideo | `aiStatus`: 0训练中/1管理员完成/2超时完成 |
| **DataModel (数据模型/罗盘)** | 分析罗盘的数据模型定义，分通用模型和行业模型 | Trade, CruxType | `type`: 0通用/1行业 |
| **ModelCrux (模型-关键词关联)** | 数据模型中各关键词类型的占比配置 | DataModel, CruxType | — |
| **Trade (行业)** | 直播带货行业分类，树形结构（如服饰内衣 > 女装 > 连衣裙） | DataModel, AnchorUrl | `rankEnabled`: 0不生效/1生效 |
| **TradeRank (行业热榜)** | 基于相似达人数据的行业热度排名 | SimilarAnchor, Trade | `collectStatus`: 0待采集/1采集中/2已完成 |
| **SimilarAnchor (相似达人)** | 第三方平台（蝉妈妈/巨量百应）返回的行业相似主播数据 | Trade, TradeRank | — |
| **VideoDataViewing (数据看板)** | 直播数据看板，含观看人数、转化率、GMV等核心指标 | AnchorVideo, Chanmama | `DataViewingStatusEnum` |
| **VideoDataViewingConfuse (混淆数据看板)** | 数据看板的混淆/脱敏版本，用于展示给终端用户 | VideoDataViewing | `DataViewingStatusEnum` |
| **VideoDataViewingParagraph (本段看板数据)** | 视频播放当前段落对应的看板数据切片 | VideoDataViewingConfuse | — |
| **ProductDetails (商品详情)** | 直播间挂载的商品信息，含曝光/点击/成交等转化漏斗数据 | AnchorVideo | — |
| **BlessBag (福袋)** | 直播间福袋/抽奖活动数据 | AnchorVideo | — |
| **OnlineNum (实时在线人数)** | 每 5 分钟采集的直播间实时在线人数 | AnchorVideo | — |
| **TotalOnlineNum (总在线人数)** | 整场直播的累计观看人次 | AnchorVideo | — |
| **SocketCollectMessage (WebSocket 采集数据)** | 通过 WebSocket 实时采集的直播数据 | AnchorVideo | — |
| **ChanmamaSendRecord (蝉妈妈请求记录)** | 向蝉妈妈平台发送数据查询请求的记录 | VideoDataViewing | `dataStatus`: 0-3 数据校验状态 |
| **OceanEngineData (巨量百应数据)** | 巨量百应平台的实时数据 | VideoDataViewing | — |
| **DataScreenshot (数据截图)** | 数据看板的定时截图记录 | DataScreenshotConfig | `screenshotStatus`: 0未上传/1已上传/2识别中/3完成/4失败 |
| **AnalysisMark (笔记标注)** | 用户在视频分析内容上的标注/笔记 | AnchorVideo, UploadFile | — |
| **VideoMark (视频标记)** | 视频播放时间轴上的标记点 | AnchorVideo | — |
| **VideoContent (自然/优化原文)** | AI 生成的视频文案：自然原文（type=1）和优化原文（type=2），存储在 MongoDB | AnchorVideo | `generateStatus`: 0未生成/1已生成 |
| **VideoTextNotes (全文笔记)** | 用户编辑的视频复盘笔记，支持多版本历史，存储在 MongoDB | AnchorVideo | `NotesType`: 1原文笔记/2复盘小结/3分段笔记 |
| **UserAnalysisRollup (用户分析汇总)** | 用户数据分析量的日/月汇总统计 | User | — |
| **ClientAiFav (AI 收藏)** | 用户收藏的 AI 分析结果 | AiAnalysis | `favType`: 0运营助手/1违规助手 |
| **SourceStar (星标)** | 对视频/文件/对比分析添加的星标标记 | AnchorVideo, UploadFile, SyncContrast | — |
| **AiOptimizePurpose (AI 优化目的)** | 用户设定的 AI 优化目标 | AnchorVideo, UploadFile | — |
| **BasicSettings (基础设置)** | 主播/视频分析的基础配置项 | AnchorUrl | — |
| **HistoryParagraph (历史段落)** | AI 问答的历史段落缓存 | AiAnalysis | — |
| **AudioAnalysis (音频分析)** | 视频语音转文字后的分析结果 | AnchorVideo | — |
| **UploadFileAnalysis (文件分析内容)** | 上传文件的 AI 分析结果 | UploadFile | — |
| **VideoAnalysisRecord (视频分析记录)** | 视频分析结果的版本化存储记录 | AnchorVideo | — |
| **UserVideoAppeal (视频申诉)** | 用户对视频分析结果的申诉 | AnchorVideo | `status`: 0待处理/1已处理 |
| **AnchorCruxWords (主播关键词)** | 与特定主播绑定的关键词 | AnchorUrl, CruxWords | — |
| **ImportantBarrage (重要弹幕)** | AI 识别的重要弹幕数据 | AnchorVideo | `ImportantBarrageStatusEnum` |

---

## 状态机定义

### VideoAnalysisEnum — 分析状态
| 值 | 状态 | 说明 |
|----|------|------|
| 0 | ANALYSIS_NO | 未分析 |
| 1 | ANALYSIS_ING | 分析中 |
| 2 | ANALYSIS_ED | 已分析 |
| 3 | ANALYSIS_EER | 分析失败 |

### DataViewingStatusEnum — 看板数据拉取状态
| 值 | 状态 | 说明 |
|----|------|------|
| 0 | PULLING | 正在拉取 |
| 1 | PULL_SUCCESS | 拉取成功 |
| 2 | PULL_FAIL | 拉取失败 |
| 3 | NOT_INCLUDE_ANCHOR | 未收录主播 |
| 4 | VIDEO_DURATION_SHORT | 视频未达 50 分钟 |
| 5 | PROPERTY_LACK | 资源不足 |
| 6 | ANCHOR_ONLINE | 主播未下播 |
| 7 | LIVE_IS_NULL | 直播列表为空 |
| 8 | DATA_ORGANIZE | 数据整理中 |

### NotesType — 笔记类型
| 值 | 类型 | 说明 |
|----|------|------|
| 1 | ORIGINAL_NOTES | 原文笔记 |
| 2 | REVIEW_NOTES | 复盘小结 |
| 3 | SECTION_NOTES | 分段笔记 |

### VideoSourceType — 视频来源类型
| 值 | 类型 | 说明 |
|----|------|------|
| 0 | LOCAL | 本地录制 |
| 1 | UPLOAD | 上传文件 |
| 2 | CONTRAST | 对比分析 |
