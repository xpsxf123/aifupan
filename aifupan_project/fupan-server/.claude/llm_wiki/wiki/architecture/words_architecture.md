# Words Architecture — 架构决策与设计

> replay-words 模块的架构基线、设计决策和关键技术约束。

---

## 一、模块定位

replay-words 是直播复盘平台的**核心业务模块**，承载以下能力：

- **主播管理**: 主播 URL 采集、用户-主播绑定、平台关联
- **视频分析**: 录制视频管理、自然/优化原文生成、AI 内容分析
- **词汇引擎**: 关键词、敏感词、词规则引擎、词库管理
- **数据看板**: 直播数据采集（蝉妈妈/巨量百应）、混淆展示、截图
- **行业热榜**: 相似达人采集、行业分类、第三方榜单管理
- **对比分析**: 多场直播/文件同步对比
- **文件分析**: 上传文件的 AI 分析流水线

---

## 二、分层架构

```
┌──────────────────────────────────────────────────────┐
│  Controller (replay-words + replay-api 双模块)        │
│  - 客户端 API: replay-words/controller/               │
│  - 管理端 API: replay-api/controller/words/           │
├──────────────────────────────────────────────────────┤
│  Bll (Business Logic Layer, 49 个)                   │
│  - @Component, 薄层透传, 组合 Producer 调用           │
├──────────────────────────────────────────────────────┤
│  Producer (49 接口 + 49 实现)                          │
│  - @Service, 核心业务逻辑, @Transactional             │
│  - Entity ↔ VO/BO 转换, 雪花 ID 生成                  │
├──────────────────────┬───────────────────────────────┤
│  Service (60 对)     │  Rse (11 接口 + 11 实现)       │
│  IService<Entity>    │  跨模块服务端点                │
│  extends ServiceImpl │  自定义聚合查询                │
├──────────────────────┼───────────────────────────────┤
│  Dao (60 个)         │  MongoRepository (2 个)        │
│  BaseMapper<Entity>  │  MongoTemplate (1 个聚合服务)  │
└──────────────────────┴───────────────────────────────┘
```

### 层级职责边界

| 层 | 允许操作 | 禁止操作 |
|----|----------|----------|
| **Controller** | 参数校验、调用 Bll/Logic、返回 R<T> | 直接调用 Dao、包含业务逻辑 |
| **Bll** | 组合 Producer 调用、简单转换 | 事务注解、直接操作 Entity |
| **Producer** | 完整 CRUD、@Transactional、Entity↔VO 转换 | 跨模块调用（通过 Rse） |
| **Rse** | 跨表聚合查询、跨模块服务 | 事务管理、写操作 |
| **Service** | MyBatis-Plus 基础 CRUD | 业务逻辑 |
| **Dao** | 单表数据操作 | 业务逻辑、关联查询（通过 Rse） |

---

## 三、ADR 记录

### ADR-001: 双模块 Controller 策略
- **决策**: 客户端 API 放在 replay-words 模块，管理端 API 放在 replay-api 模块
- **原因**: 客户端接口需跨域支持且变化频繁；管理端接口需聚合多模块数据，适合放在装配层
- **影响**: 同一实体可能有两个 Controller 分别处理客户端和管理端逻辑

### ADR-002: Producer 模式替代传统 Service
- **决策**: 使用 Producer 接口层封装完整业务逻辑，而非在 ServiceImpl 中编写
- **原因**: MyBatis-Plus ServiceImpl 提供基础 CRUD，Producer 在此基础上增加事务、ID 生成、类型转换等业务语义
- **影响**: 每新增一个实体需同步创建 Producer/ProducerImpl/Bll 三层

### ADR-003: MongoDB 存储长文本内容
- **决策**: VideoContent（自然/优化原文）和 VideoTextNotes（全文笔记）使用 MongoDB
- **原因**: 文本内容长度不可控（可达数万字），不支持固定字段索引；版本化笔记需要灵活的文档结构
- **影响**: 需要维护 MySQL 和 MongoDB 双数据源，注意事务边界

### ADR-004: 数据看板混淆策略
- **决策**: 原始看板数据 (VideoDataViewing) 和混淆数据 (VideoDataViewingConfuse) 分表存储
- **原因**: 第三方数据有保密协议，终端用户只能看到混淆后的数据范围
- **影响**: 每次数据更新需同时写入两张表，增加了数据一致性要求

### ADR-005: 手动管理时间戳和软删除
- **决策**: 不使用 MyBatis-Plus 的 `@TableLogic` 和自动填充
- **原因**: 项目早期约定，保持对数据操作的完全控制
- **影响**: 所有 CRUD 方法需手动设置 `createDate`/`updateDate`/`isDeleted`

### ADR-006: 雪花 ID 手动赋值
- **决策**: 使用 `SnowflakeManager.nextValue()` 生成 ID，`@TableId(type = IdType.INPUT)`
- **原因**: 分布式环境下保证全局唯一，且可在业务层控制 ID 生成时机
- **影响**: 所有 insert 操作需在 Producer 层显式调用 `SnowflakeManager.nextValue()`

---

## 四、定时任务

所有任务使用 XXL-JOB (`@XxlJob`) 调度。

| 任务类 | Job Handler | 调度频率 | 功能 |
|--------|-------------|----------|------|
| AnchorSystemTradeTask | `syncAiTradeToSystemTrade` | 定时 | 同步 AI 修正的行业 ID 到系统行业字段 |
| SimilarAnchorQueryTask | `authCorrelationAnchor` | 定时 | 系统主播与相似达人自动关联 |
| SimilarAnchorQueryTask | `autoSyncAnchorSystemTradeId` | 每天 | 同步系统行业 ID |
| SimilarAnchorQueryTask | `thirdPartyRankings` | 每天 | 查询第三方销售榜单 |
| SimilarAnchorQueryTask | `probeSimilarAnchors` | 每10分钟 | 发送相似达人探测请求 |
| SimilarAnchorQueryTask | `querySimilarAnchors` | 每天凌晨2点 | 查询行业相似达人 |
| ViewingScheduledTasks | `batchTrainView` | 每5分钟 | 关闭超时看板数据（>60分钟仍在 pulling 则置为失败） |
| WordsScheduledTasks | `batchTrainWords` | 每小时 | 自动完成超时 AI 训练（>1天仍在训练则置为失败） |

---

## 五、跨模块通信

replay-words 通过以下方式暴露服务给其他模块：

### 11 个 Feign API（定义在 replay-generic）
`AnchorUrlApi`, `AnchorUrlUserApi`, `AnchorVideoApi`, `AnchorVideoDetailApi`, `BlessBagApi`, `CueWordsApi`, `SensitiveWordsApi`, `TradeApi`, `VideoContentApi`, `VideoDataViewingApi`, `VideoTextNotesApi`

### 11 个 Rse 接口（定义在 replay-words 内部）
`AiOptimizePurposeRse`, `AnchorCruxWordsRse`, `AnchorRse`, `AnchorUrlUserRse`, `AnchorVideoRse`, `ContrastRse`, `SourceStarRse`, `TradeRse`, `VideoDataViewingParagraphRse`, `VideoSliceRse`, `VideoTextNotesRse`

---

## 六、关键约束

- **事务边界**: 仅 Producer 层可使用 `@Transactional`，Rse 和 Bll 层不得开启事务
- **Redis 分布式锁**: 通过 `@CustomRedissonLock` 注解在 Producer 方法上声明
- **防重复提交**: 通过 `@NoRepeatSubmit` 注解在 Controller 方法上声明
- **N+1 查询禁令**: 必须使用批量查询 + 内存拼装，禁止循环查询
- **租户隔离**: 所有查询必须包含 `tenantId` 过滤条件
- **MongoDB 无事务**: VideoContent 和 VideoTextNotes 的操作不与 MySQL 事务共享边界
