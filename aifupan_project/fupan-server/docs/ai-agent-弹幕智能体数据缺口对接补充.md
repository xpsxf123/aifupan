# 弹幕智能体「缺数据」对接补充文档（字段级）

> 版本：v1.0　｜　对照来源：`docs/爱复盘弹幕智能体 修改建议0527.pdf` + 0527 修改建议代码对照
> 面向对象：弹幕智能体（前端 / AI pipeline）研发
> 说明：本文**只回答一件事** —— 你们 0527 报告里列为「未解决 / 缺字段」的诉求，我方 `AiAgentReplay` 开放接口现在能提供哪些数据、精确到字段。
> 完整接口参数 / 分页 / 鉴权总览见 `docs/ai-agent-replay-openapi-delivery.md`，本文不重复，只做**缺口 → 接口字段**的映射。

---

## 0. 一分钟结论

| 你们报告里的缺口 | 我方能给到什么程度 | 承载接口 |
|---|---|---|
| 真实曝光数 / 进入率 / 流出 | **部分**：进场 / 离场 / 在线人数**时序**全给；绝对曝光数不给，只有「曝光-观看率」 | `video/online-curve`、`video/dashboard` |
| 用户平均停留时长（换词） | **完全**：整场 + 分段 `averageResidenceTime`（秒） | `video/dashboard` |
| 弹幕新粉活跃时长 | **给原料不给成品**：`isNew + recordDate + nickName` 自算 | `video/barrages` |
| 弹幕原文全量 / 聚合 / 去重计数 | **完全**：全量弹幕原文 + 昵称 + 时间戳 + 发送次数，游标可拉全 | `video/barrages` |
| 福袋过滤 | **完全**：我方已识别，直接给 `isBlessBag` 布尔标记 + 福袋事件列表 | `video/barrages`、`video/dashboard` |
| 多场情绪汇总 / 剔除低在线场次 | **给原料**：每场 `averageOnlineNum / maxOnlineNum` 做阈值，弹幕原文做加权 | `video/list` + 上述明细 |
| 纯复盘片段分析 | **部分**：切片类型可筛「复盘切片」；一场内实盘/复盘话题级标注不提供 | `video/list`、`video/audio-paragraphs` |
| 行业敏感词自检 | **完全**（额外能力）：按行业父链取词，输出话术前合规过滤 | `anchor/sensitive-words` |

> 关键澄清：**福袋识别、新用户标记、进场/离场人数，我方原始数据里已经有**。你们 0527 报告中「代码里没有」的部分，多数是你们本地 SQLite demo 没有这些字段，而不是我方接口给不出——直接对接下面接口即可拿到。

---

## 1. 通用前提（只列与本文相关的）

- 全部 `POST` + JSON，HTTP 恒 200，看响应体 `code`（`0` 成功）。
- 请求头鉴权：`x-jiuyu-client-id` + `api-key`（联调群下发）。
- 每个请求体必带身份三件套：`userId`(long) / `tenantId`(long) / `userType`(int：0 主账号 /1 管理员 /2 子账号)。
- **时间戳字段统一为毫秒 `long`**（如 `recordDate`、曲线 `dateTime`、福袋 `relativeTime`）；格式化时间字段为字符串 `yyyy-MM-dd HH:mm:ss`。
- 列表接口游标分页：回传上页 `nextCursor`，直到 `hasMore=false`。

---

## 2. 缺口逐条 → 接口字段映射

### 2.1 真实曝光数 / 进入率 / 流出（0527 编号 1）

**诉求**：整场流速失真，改为分均流量流速 / 曝光数 / 对应进入率。

**我方提供**：进场、离场、在线人数**逐点时序**，可自行按分钟聚合算「分均净流速 / 进入 / 流出」。绝对曝光数无法提供，只有曝光-观看率。

接口 `POST /internal/ai-agent/video/online-curve` → `data`(OnlineCurveVo)：

| 字段 | 类型 | 含义 | 对你们的用途 |
|---|---|---|---|
| `totalViewersNum` | int | 累计观看人数 | 观看基数 |
| `maxOnlineNum` | int | 最大在线人数 | 峰值 |
| `onlineDataList[]` | 折线点 | 在线人数时序 | 净流速基线 |
| `approachDataList[]` | 折线点 | **进场人数**时序 | 进入口径（真实进入人数，非代理） |
| `exitPeopleDataList[]` | 折线点 | **离场人数**时序 | 流出口径 |
| `barrageDataList[]` | 折线点 | 弹幕数时序 | 互动密度 |

折线点结构（`CurvePointVo`）：

| 字段 | 类型 | 含义 |
|---|---|---|
| `dateTime` | long | 时间戳（毫秒） |
| `valueNum` | int | 该时点数值 |

> `step`（int，默认 1）控制抽样步长，越大越稀疏。

接口 `POST /internal/ai-agent/video/dashboard` → `data`(VideoDashboardVo) 中与「曝光」直接相关的**唯一**字段：

| 字段 | 类型 | 含义 | 边界 |
|---|---|---|---|
| `showWatchCntRatio` | double | 曝光-观看率 | **这是比率不是绝对曝光数**；1 表示 100% |

**结论**：`approachDataList` / `exitPeopleDataList` 已是真实进入 / 流出人数，可直接替换你们当前「在线人数差分」的代理指标；进入率可用 `进场人数 / 曝光` 计算，但**曝光只有比率没有绝对数**，绝对曝光数需平台侧另行确认能否补。

---

### 2.2 停留时长（0527 编号 2、6）

**诉求**：换词为「弹幕粉丝活跃时长」，字段数据要合理。

**我方提供**：整场 + 分段平均停留时长，单位秒。换词是你们展示层的事，数值口径以此为准。

`video/dashboard` → `data`：

| 字段 | 类型 | 含义 |
|---|---|---|
| `averageResidenceTime` | int | 整场平均停留时长（秒） |
| `paragraphs[].averageResidenceTime` | int | 分段（看盘）平均停留时长（秒） |

---

### 2.3 弹幕新粉活跃时长（0527 编号 3）—— 给原料自算

**诉求**：在停留时长外，多拆一个「弹幕新粉活跃时长」，按 `isNewUser` 分组。

**我方提供**：不直接给聚合成品，但 `video/barrages` 每条弹幕都带**新用户标记 + 用户 + 毫秒时间戳**，你们可按「同一 `nickName` 且 `isNew=true`」分组，取其首末弹幕 `recordDate` 差值即为该新粉的活跃时长。

`POST /internal/ai-agent/video/barrages` → `data.list[]`(BarrageItemVo)：

| 字段 | 类型 | 含义 | 算新粉活跃时长用途 |
|---|---|---|---|
| `isNew` | bool | **是否新用户** | 分组维度 |
| `nickName` | string | 弹幕用户昵称 | 按人聚合 key |
| `recordDate` | long | 弹幕时间戳（毫秒） | 首末差 = 活跃时长 |
| `countSendNum` | int | 该用户累计发送次数 | 活跃度权重 |
| `level` | long | 用户等级 | 可选画像 |
| `fansLevelCurrent` | long | 当前粉丝团等级 | 可选画像 |

---

### 2.4 弹幕原文全量 / 聚合 / 去重计数（0527 编号 4、9、9补充、13）

**诉求**：话题指标表除数量外要有弹幕原文聚合、小样本直接显示原文、同一用户重复同文计 1 条。

**我方提供**：`video/barrages` 游标翻页可拉**全量弹幕原文**，含用户、时间戳、发送次数——聚合 / 代表原评论 / 去重计数所需的原始素材全部具备（聚合逻辑在你们 AI 侧）。

`video/barrages` → `data.list[]`(BarrageItemVo) 与本诉求相关字段：

| 字段 | 类型 | 含义 | 对应你们的需求点 |
|---|---|---|---|
| `content` | string | **弹幕原文** | 原文全量展开 / 代表原评论 |
| `nickName` | string | 用户昵称 | 「同一人重复」判定 |
| `recordDate` | long | 时间戳（毫秒） | 「短时间窗口」判定、跳回逐字稿定位 |
| `countSendNum` | int | 发送次数 | 「同一人一模一样问题计 1 条」去重计数 |
| `isBlessBag` | bool | 是否福袋弹幕 | 聚合前先剔福袋（见 2.5） |
| `isNew` | bool | 是否新用户 | 新粉互动聚合 |
| `batchNumber` | string | 直播场次号 | 跨场归属 |

分页游标（`BarrageQueryBo`）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `videoId` | string | 必填 |
| `cursor` | long | 首页 null；回传上页 `nextCursor`（底层是页码，你们无需感知） |
| `pageSize` | int | 默认 200，上限 500 |

> 全量拉取方式：`cursor=null` 起，循环回传 `nextCursor` 直到 `hasMore=false`。

---

### 2.5 福袋过滤（0527 编号 10、D）—— 我方已识别，直接给标记

**诉求**：排除福袋评论，短时间连续发的一模一样内容判为福袋文字。

**我方提供**：**你们不用自己猜福袋规则**。我方采集侧已对弹幕打好福袋标记，逐条弹幕带 `isBlessBag`；另有福袋事件列表（时点 / 奖品 / 参与人数）。

逐条标记 —— `video/barrages` → `data.list[].isBlessBag`：

| 字段 | 类型 | 含义 |
|---|---|---|
| `isBlessBag` | bool | `true`=该弹幕为福袋弹幕，聚合 / 有效弹幕统计时直接排除 |

福袋事件列表 —— `video/dashboard` → `data.blessBagList[]`(DashboardBlessBagVo)：

| 字段 | 类型 | 含义 |
|---|---|---|
| `relativeTime` | long | 相对开播毫秒（定位第几分钟开的福袋） |
| `blessBagReward` | string | 福袋奖品 |
| `candidateNum` | int | 参与人数 |

> 若你们仍想在此之上叠加「短时间窗口 + 重复度」二次判定，用 2.4 的 `content + nickName + recordDate + countSendNum` 即可，但通常直接用 `isBlessBag` 就够。

---

### 2.6 多场情绪汇总 / 剔除低在线场次（0527 编号 11、F）

**诉求**：多场汇总一眼看今日总情绪；相同情绪百分比相加；剔除平均在线人数 < 1000 的场次。

**我方提供**：情绪本身由你们 AI 聚合，我方给两类原料 —— ①每场的在线人数指标（做 <1000 剔除阈值 + 加权权重）；②每场弹幕原文（做情绪加权基数）。

先用 `POST /internal/ai-agent/video/list` 拿场次清单 → `data.list[]`(VideoItemVo)：

| 字段 | 类型 | 含义 | 用途 |
|---|---|---|---|
| `videoId` | string | 视频/场次唯一标识 | 逐场取明细 key |
| `secUid` | string | 主播标识 | 分主播汇总 |
| `anchorName` | string | 主播名称 | 展示 |
| `startTime` / `endTime` | string | 录制起止时间 | 「今天」的场次筛选 |
| `hasDashboard` | bool | 是否有数据看板 | 跳过无数据场次 |
| `hasBarrages` | bool | 是否有弹幕 | 跳过无弹幕场次 |
| `hasChartData` | bool | 是否有在线曲线 | 跳过无曲线场次 |

再对每个 `videoId` 取在线指标做「剔除 < 1000」与加权：

- `video/dashboard` → `averageOnlineNum`(int，平均在线人数)、`totalWatchNum`(int，累计观看)
- 或 `video/online-curve` → `maxOnlineNum`、`onlineDataList`（自算均值）

> 建议按你们报告里的口径：用**有效弹幕数 / 平均在线人数加权**，而非简单百分比相加；`averageOnlineNum < 1000` 的场次默认剔除。

---

### 2.7 纯复盘片段分析（0527 编号 8、E）—— 部分支持

**诉求**：一场里一半实盘一半复盘，想只分析纯复盘。

**我方提供**：
- **切片级可筛**：`video/list` 请求参数 `videoSliceType`（0 原视频 /1 复盘切片视频 /2 短视频切片视频），传 `1` 只取复盘切片场次。响应回带 `videoSliceType` + `videoSliceTypeLabel`。
- **话术级自识别原料**：`video/audio-paragraphs` 给逐分钟全文，你们 AI 可据话术区分实盘/复盘时段。
- **不提供**：一场内「实盘/复盘」话题级的现成标注字段（需你们 AI 侧或运营标时段）。

`POST /internal/ai-agent/video/audio-paragraphs` → `data[]`(AudioParagraphVo)：

| 字段 | 类型 | 含义 |
|---|---|---|
| `paragraph` | int | 段落序号（第几分钟段，从 1 起） |
| `time` | string | 该段时间范围（如 `120s - 180s`） |
| `content` | string | 该分钟段全文文本 |

`video/list` 切片相关字段（VideoItemVo）：

| 字段 | 类型 | 含义 |
|---|---|---|
| `videoSliceType` | int | 0 原视频 /1 复盘切片 /2 短视频切片 |
| `videoSliceTypeLabel` | string | 上述中文文案 |

---

### 2.8 行业敏感词自检（额外能力，0527 未列但相关）

输出话术前合规过滤可用 `POST /internal/ai-agent/anchor/sensitive-words`：按行业父链（4 级自身 + 3/2/1 级父 + 全行业）取词。

请求（SensitiveWordsQueryBo）：`tradeId`(long 必填) / `platform`(int 0 抖音 /1 快手 /2 视频号，空=不限) / `wordsType`(int 0 敏感词 /1 关键词)。

响应 `data[]`(SensitiveWordVo)：

| 字段 | 类型 | 含义 |
|---|---|---|
| `word` | string | 禁词 |
| `level` | int | 0=1级(封号) /1=2级(严重警告) /2=3级(警告)，越小越严重 |
| `levelLabel` | string | 等级中文 |
| `type` | int | 0 广告 /1 品牌 /2 国家 /3 限制词 /4 其他 |
| `typeLabel` | string | 类型中文 |
| `similarWords` | string | 相似词（`_` 分隔的变体串，兜底） |

---

## 3. 数据看板整体汇总字段全量表（补充参考）

`video/dashboard` → `data`(VideoDashboardVo) 整体汇总段，除前文已引用字段外的全量清单，供你们对齐指标口径：

| 字段 | 类型 | 含义 |
|---|---|---|
| `videoId` | string | 视频标识 |
| `secUid` | string | 主播标识 |
| `batchNumber` | string | 直播场次号 |
| `anchorNumber` | string | 主播账号 |
| `isTakeProduct` | int | 是否带货 0 否 /1 是 |
| `totalWatchNum` | int | 累计观看人数 |
| `averageOnlineNum` | int | 平均在线人数 |
| `averageResidenceTime` | int | 平均停留时长（秒） |
| `incrementFollowerCount` | int | 新增粉丝数 |
| `convertFanRate` | double | 转粉率 |
| `interactionPercent` | double | 互动率 |
| `volume` | int | 销售额 |
| `purchaseCount` | int | 销量 |
| `customerUnitPrice` | double | 客单价 |
| `uvValue` | double | UV 价值 |
| `goodsConvertRate` | double | 带货转化率 |
| `showWatchCntRatio` | double | 曝光-观看率 |
| `roi` / `launchRoiAmount` / `refundAmount` / `overallCostRoi` / `netTransactionRoi` | double | ROI 系列 |
| `totalBarrageNum` | int | 弹幕总数（实时采集补充） |
| `watchFlowList[]` | 流量结构 | 看播流量结构（`channelName` 来源 + `ratio` 占比 + `subFlow` 子结构） |
| `payFlowList[]` | 流量结构 | 成交流量结构（同上） |
| `onlineDataList[]` / `approachDataList[]` / `exitPeopleDataList[]` / `payComboCntDataList[]` / `payAmtDataList[]` / `followAnchorUcntDataList[]` | 折线点 | 在线/进场/离场/成交/成交额/新增粉丝时序（**降采样约 5 点**，全量走 `online-curve`） |
| `blessBagList[]` | 福袋 | 福袋事件（见 2.5） |
| `paragraphs[]` | 分段明细 | 分段看盘数据（`DashboardParagraphVo`，含各指标 start/end 区间） |

> 注意：`dashboard` 的折线是**降采样压缩版（约 5 点）**，只用于看板概览；需要完整逐点时序请调 `video/online-curve`。

---

## 4. 明确「我方给不了」的项（避免反复确认）

| 诉求 | 现状 | 出路 |
|---|---|---|
| 绝对曝光数（分均曝光基数） | 只有 `showWatchCntRatio` 曝光-观看率 | 需平台侧确认导出 JSON 是否含曝光绝对值 |
| 一场内「实盘/复盘」话题级标注 | 无现成字段 | 你们 AI 按话术识别，或运营手动标时段 |
| 弹幕聚合标题 / 情绪分类结果 | 我方只给原始弹幕，不给聚合成品 | 聚合与情绪在你们 AI pipeline |
| 新粉活跃时长成品值 | 只给 `isNew + recordDate + nickName` 原料 | 你们侧按人分组自算 |

---

## 5. 接口速查

| 缺口 | 接口 | 关键字段 |
|---|---|---|
| 进入/流出/在线时序 | `POST /internal/ai-agent/video/online-curve` | `approachDataList` `exitPeopleDataList` `onlineDataList` |
| 停留时长 | `POST /internal/ai-agent/video/dashboard` | `averageResidenceTime` |
| 新粉活跃时长原料 | `POST /internal/ai-agent/video/barrages` | `isNew` `nickName` `recordDate` |
| 弹幕原文全量/去重 | `POST /internal/ai-agent/video/barrages` | `content` `nickName` `countSendNum` |
| 福袋过滤 | `POST /internal/ai-agent/video/barrages` + `.../video/dashboard` | `isBlessBag` / `blessBagList` |
| 多场汇总原料 | `POST /internal/ai-agent/video/list` (+ dashboard) | `videoId` `averageOnlineNum` `hasBarrages` |
| 纯复盘筛选 | `POST /internal/ai-agent/video/list` | 入参 `videoSliceType=1` |
| 逐分钟全文 | `POST /internal/ai-agent/video/audio-paragraphs` | `paragraph` `time` `content` |
| 行业敏感词 | `POST /internal/ai-agent/anchor/sensitive-words` | `word` `level` `type` |

> 完整请求参数、分页、鉴权、错误码见 `docs/ai-agent-replay-openapi-delivery.md`。本文仅覆盖 0527 缺口相关字段。
