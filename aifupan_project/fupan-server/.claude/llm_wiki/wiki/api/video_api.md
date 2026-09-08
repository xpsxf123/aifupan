<!-- module: video -->
<!-- area: api -->
<!-- persistence: MySQL (MyBatis-Plus) + MongoDB (single collection) -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-video/src/main/java/com/jiuyu/replay/video/project/controller/ -->

# Video API — 接口契约

> replay-video 模块完整 API 表面。**全部 5 个 Controller 都在 `replay-video` 模块内**，replay-api 模块**未发现** `controller/video/` 目录（区别于 words 域的"双模块 Controller"模式）。

---

## 一、Controller 总览

Base package：`com.jiuyu.replay.video.project.controller`

| Controller | Base Path | 用途 |
|------------|-----------|------|
| `VideoExtractController` | `/replay/video/extract` | 短视频文案提取状态机 + 历史查询 |
| `VideoInfluencerController` | `/replay/video/influencer` | 达人搜索 / 订阅 / 同步 |
| `VideoHotSearchController` | `/replay/video/hotSearch` | 爆款搜索 / 订阅 |
| `VideoHotSearchEmailAccountController` | `/replay/video/hotSearch/email` | 爆款邮箱账号池管理 |
| `VideoGroupManagementController` | `/replay/video/group` | 通用订阅分组（达人+爆款复用）|

---

## 二、VideoExtractController — 视频文案提取

### 状态机接口

| Method | Path | 说明 |
|--------|------|------|
| POST | `/replay/video/extract/urlOrLocal` | **核心状态机接口**：根据 `extractStatus` 路由四种状态分支处理 |

**请求体**：`VideoExtractBo`
**状态分支**（参 `ExtractStatusEnum`）：
- `extractStatus=1` PENDING：首次提交，必填 `videoTitle / videoUrl / sourceType`
- `extractStatus=2` PROCESSING：上传完成，必填 `id / videoHash / coverUrl`
- `extractStatus=3` COMPLETED：文案提交，必填 `id / extractContent`
- `extractStatus=4` FAILED：失败上报，必填 `id / extractErrorReason`

**响应**：`R<VideoExtractVo>`
**事务**：`@Transactional(rollbackFor = Exception.class)` on `VideoExtractProducer#extractFromUrlAndLocal`

### 历史 / 列表

| Method | Path | 说明 |
|--------|------|------|
| POST | `/replay/video/extract/history` | 分页查询历史提取记录（支持时间范围 / 视频标题 / 操作人筛选） |
| DELETE | `/replay/video/extract/batch` | 批量删除提取记录（最多 1000 条/次，逻辑删除） |
| GET | `/replay/video/extract/content/{id}` | 通过用户视频关联 ID 查询 MongoDB 文案 |
| GET | `/replay/video/extract/pendingQueue` | 客户端获取 PENDING + PROCESSING 待处理队列 |
| GET | `/replay/video/extract/operationUsers` | 查询租户下有过提取操作的用户列表（用于筛选）|
| GET | `/replay/video/extract/getCoverImgPutUrl?suffix=` | 获取封面图 OSS 预签名 PUT 链接 |
| POST | `/replay/video/extract/batchCreateExtract` | 批量创建（来自达人列表 / 爆款搜索的视频）|
| GET | `/replay/video/extract/reuse?userVideoId=` | 失败后重新提取（仅 FAILED → PENDING） |
| GET | `/replay/video/extract/getVideoInfoByVideoId?videoId=` | 获取视频基础信息 |
| GET | `/replay/video/extract/syncUserShortVideoProperty` | 客户端：重新统计用户短视频套餐资产 |
| GET | `/replay/video/extract/getExtractPrompt` | 获取文案优化的提示词模板 |

### 关键响应类型

- `R<VideoExtractVo>` — 提取任务 VO
- `R<PageUtils<VideoExtractQueryVo>>` — 分页历史记录
- `R<VideoContentExtractVo>` — MongoDB 文案内容
- `R<List<OperationUserVo>>` — 操作人列表
- `R<SignUploadUrlVo>` — OSS 预签名

---

## 三、VideoInfluencerController — 达人

### 搜索 / 同步

| Method | Path | 说明 |
|--------|------|------|
| POST | `/replay/video/influencer/saveSearch` | 客户端保存搜索达人数据，返回快照 ID |
| GET | `/replay/video/influencer/search?snapshotId&page&limit` | 通过快照 ID 分页查询达人 |
| GET | `/replay/video/influencer/history?page&limit` | 分页查询达人搜索历史 |
| GET | `/replay/video/influencer/getInfluencerLastSyncTime?platformType&platformUserId` | 获取达人最后同步时间 |
| GET | `/replay/video/influencer/hasInfluencerSyncVideos?platformType&platformUserId` | 判断达人是否需要同步视频 |
| PUT | `/replay/video/influencer/syncInfluencerInfo` | 同步达人数据（含其短视频列表）|

### 详情

| Method | Path | 说明 |
|--------|------|------|
| GET | `/replay/video/influencer/detail?platformType&platformUserId` | 达人详情 |
| GET | `/replay/video/influencer/detailVideos?page&limit&platformType&platformUserId&sortCode&sortSequence&videoPublishTimeType` | 达人详情视频分页（带排序）|

### 订阅

| Method | Path | 说明 |
|--------|------|------|
| POST | `/replay/video/influencer/subscribe` | 添加达人订阅，返回 `VideoUserInfluencerSubscriptionEntity` |
| GET | `/replay/video/influencer/subscriptions?nickname&industryId` | 查询订阅列表（按分组聚合） |
| PUT | `/replay/video/influencer/subscription/edit` | 编辑订阅（行业 / 分组 / 自动同步） |
| DELETE | `/replay/video/influencer/subscription/{subscriptionId}` | 删除订阅 |
| GET | `/replay/video/influencer/subscription/list?influencerId` | 客户端订阅达人列表 |
| GET | `/replay/video/influencer/admin/subscriptions/{userId}` | **后台管理**：按用户 ID 查询订阅 |

---

## 四、VideoHotSearchController — 爆款搜索

### 搜索 / 历史

| Method | Path | 说明 |
|--------|------|------|
| POST | `/replay/video/hotSearch/saveSearch` | 客户端保存搜索爆款数据，返回快照 ID（`R<Long>`）|
| GET | `/replay/video/hotSearch/search?snapshotId&page&limit&sortCode&sortSequence` | 通过快照 ID 分页搜索爆款 |
| GET | `/replay/video/hotSearch/history?page&limit` | 分页查询用户搜索历史 |
| POST | `/replay/video/hotSearch/list` | 按条件查询爆款列表 (`VideoHotSearchListQueryBo`) |
| POST | `/replay/video/hotSearch/list/subscription` | 按订阅条件查询视频列表 |
| POST | `/replay/video/hotSearch/list/example` | 按爆款示例条件查询视频列表 |

### 订阅

| Method | Path | 说明 |
|--------|------|------|
| POST | `/replay/video/hotSearch/subscription` | 添加订阅 → `R<VideoUserHotSubscriptionEntity>` |
| PUT | `/replay/video/hotSearch/subscription` | 编辑订阅 |
| DELETE | `/replay/video/hotSearch/subscription/{subscriptionId}` | 删除订阅 |
| GET | `/replay/video/hotSearch/subscriptions` | 按分组列出订阅 |
| GET | `/replay/video/hotSearch/subscription/list` | 客户端获取订阅列表 |
| POST | `/replay/video/hotSearch/syncVideoHotSearchData` | 客户端：根据订阅同步爆款数据 |
| GET | `/replay/video/hotSearch/videoList?platformType&keyword` | 按平台 + 关键字获取视频列表（≤100 字符）|
| GET | `/replay/video/hotSearch/admin/subscriptions/{userId}` | **后台管理**：按用户 ID 查询订阅 |

---

## 五、VideoHotSearchEmailAccountController — 邮箱账号池

| Method | Path | 说明 |
|--------|------|------|
| GET | `/replay/video/hotSearch/email/getAccount` | 智能分配邮箱（同城优先 + 使用时间最早），自动解析客户端 IP |
| POST | `/replay/video/hotSearch/email/reportFailure` | 上报不可用账号（`ReportFailureEmailBo`） |
| POST | `/replay/video/hotSearch/email/batchImport` (multipart) | 批量导入邮箱账号 Excel（`MultipartFile` 入参）|

**Client IP 解析顺序**：`X-Forwarded-For` → `Proxy-Client-IP` → `WL-Proxy-Client-IP` → `HTTP_CLIENT_IP` → `HTTP_X_FORWARDED_FOR` → `request.getRemoteAddr()`。多 IP 取首位。
**测试 bypass**：query / body 中传 `clientIp` 字段直接覆盖（注释里标记 TODO）。

---

## 六、VideoGroupManagementController — 订阅分组

| Method | Path | 说明 |
|--------|------|------|
| POST | `/replay/video/group/list` | 分页查询分组（`GroupQueryBo`） |
| GET | `/replay/video/group/options?groupType` | 获取分组下拉选项（含默认分组占位，`groupId = null`） |
| POST | `/replay/video/group` | 添加分组（`GroupAddBo`） |
| PUT | `/replay/video/group` | 编辑分组（`GroupEditBo`） |
| DELETE | `/replay/video/group/{groupId}` | 删除分组（分组下有成员时禁止） |
| GET | `/replay/video/group/{groupId}` | 获取分组详情（含成员数量统计） |

`groupType`：1 = 达人订阅分组，2 = 爆款订阅分组（参 `SubscriptionGroupTypeEnum`）。

---

## 七、通用约定

### 响应包装
- 所有接口返回 `R<T>`（`com.jiuyu.replay.generic.vo.common.R`）
- 错误统一通过 `BusinessException(ResponseCode...)` 抛出，由全局异常处理转换为 `R.error`

### 分页
- 使用 `PageUtils<T>` 包装（`com.jiuyu.replay.generic.utils.PageUtils`）
- 入参常规：`page`（默认 1，`@Min(1)`）+ `limit`（默认 10，`@Max(100)`）
- 部分 POST 接口分页参数封装在 QueryBo 内（`current / size` 或 `page / limit`）

### 参数校验
- `@Validated` 在 Controller 类或方法级
- `@RequestBody @Validated XxxBo`
- 路径 / 查询参数：`@NotNull / @NotBlank / @Min / @Max / @Length / @EnumValue(byteValues = {...})`
- 分组校验：`@Validated(VideoHotSearchSyncDataBo.Add.class)`、`@Validated(VideoInfluencerInfoBo.Add.class)`

### 依赖注入
- 全部 Controller 使用构造器注入（`@RequiredArgsConstructor` + `private final ...`），**未发现** `@Autowired / @Resource`（符合 CLAUDE.md §5）

### Swagger / Knife4j
- Controller 注解 `@Tag(name = "V2.5.3短视频/...")` + `@ApiSort` 排序
- 方法注解 `@Operation(summary = ..., description = ...)`（多用 markdown 文本块）

### 跨域 / 鉴权
- 当前 5 个 Controller **未声明** `@CrossOrigin`（依赖全局 CORS 配置）<待补充>
- 鉴权通过全局过滤器 / 拦截器（与 words 模块共用）+ `UserFeign#getLocalUser()` 在 Producer 内取当前用户

### 防重复提交
- 当前 5 个 Controller **未发现** `@NoRepeatSubmit` 注解
- 防重逻辑改由 Producer 层 `@CustomRedissonLock` 分布式锁实现（见 `VideoHotSearchProducer` / `VideoInfluencerProducer`）

### Feign 暴露给其他模块
- 当前 **未发现** `replay-generic/src/main/java/com/jiuyu/replay/generic/feign/video/` 目录
- replay-video 是**消费者**：调用 `UserFeign`（power）、`UserPropertyFeign`（order）、`TradeFeign`（words）、`DictDataFeign`（system）等
- 没有反向 Feign 接口暴露给其他模块（截止扫描时）

---

## 八、典型 VO / BO 命名约定

- `XxxBo` — 请求入参（`VideoExtractBo / VideoInfluencerInfoBo / VideoHotSearchBo / GroupAddBo / ReportFailureEmailBo`）
- `XxxVo` — 响应出参（`VideoExtractVo / VideoInfluencerInfoDetailVo / VideoHotSearchResultVo / HotSearchEmailAccountVo / GroupVo`）
- `XxxQueryBo` — 分页查询专用入参
- `XxxAddBo / XxxEditBo` — CRUD 差异化入参
- Bo 子包：`bo/influencer/ bo/hotsearch/ bo/group/ bo/subscription/ bo/email/`
- Vo 子包：`vo/influencer/ vo/hotsearch/ vo/group/ vo/subscription/ vo/email/ vo/video/ vo/admin/`
