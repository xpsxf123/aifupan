<!-- module: activity -->
<!-- area: api -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-activity/.../controller/, replay-api/.../controller/agent/ -->

# Activity API -- 接口契约

> 邀请活动相关接口分布在两个位置：
> 1. **replay-activity 模块**（`/replay/activity/clientinviteactivity`）-- 管理端 HTTP REST + 内部 Feign
> 2. **replay-api 模块 agent 包**（`/replay/clientinvite*`）-- 代码保留但 `@RestController` 被注释，未上线
>
> 所有接口默认返回 `R<T>`，分页用 `PageUtils<T>`，feign 接口无鉴权。

---

## 一、HTTP REST（管理端 -- replay-activity 模块）

控制器：`ClientInviteActivityController`（`@RestController`, `@RequestMapping("replay/activity/clientinviteactivity")`, `@Tag(name = "邀请活动")`）

### POST /replay/activity/clientinviteactivity/list
- **Summary:** 邀请活动分页列表
- **Auth:** 全局登录态
- **Request body:** `ClientInviteActivityListBo`
  | Field | Type | Required | Default | Meaning |
  |---|---|---|---|---|
  | keyword | String | 否 | -- | 模糊搜索（当前匹配 name 字段，与实体 activityName 列名不一致） |
  | page | Integer | 否 | 1 | 当前页 |
  | limit | Integer | 否 | 10 | 每页记录数 |
- **Response `data`:** `PageUtils<ClientInviteActivityListVo>` -- 各字段见下 `ClientInviteActivityVo` 基类

### GET /replay/activity/clientinviteactivity/info
- **Summary:** 邀请活动详情
- **Auth:** 全局登录态
- **Request params:**
  | Field | Type | Required | Meaning |
  |---|---|---|---|
  | id | Long | 是 | 邀请活动 ID |
- **Response `data`:** `ClientInviteActivityInfoVo`（含 `progressList`，仅含启用中的进度阶梯）

### POST /replay/activity/clientinviteactivity/save
- **Summary:** 新增邀请活动
- **Auth:** 全局登录态
- **Request body:** `ClientInviteActivityBo`（字段见下）
- **Response `data`:** `R<String>`（"添加成功"）
- **事务:** `@Transactional(rollbackFor = Exception.class)` 在 Bll 层；同事务内级联保存 activity + progress + reward

### POST /replay/activity/clientinviteactivity/update
- **Summary:** 修改邀请活动
- **Auth:** 全局登录态
- **Request body:** `ClientInviteActivityBo`（含 `id` + 全部字段）
- **Response `data`:** `R<String>`（"修改成功"）
- **事务:** `@Transactional`；流程：更新活动主体 -> `disableOld`（将旧 progress 全置 status=0）-> 批量新增新阶梯

### GET /replay/activity/clientinviteactivity/delete
- **Summary:** 删除邀请活动
- **Auth:** 全局登录态
- **Request params:**
  | Field | Type | Required | Meaning |
  |---|---|---|---|
  | id | Long | 是 | 邀请活动 ID |
- **Response `data`:** `R<String>`（"删除成功"）
- **注意:** 物理删除（`removeById`），未级联清理 progress/reward/user_invite

### GET /replay/activity/clientinviteactivity/infoByClient
- **Summary:** 客户端拉取当前默认激活活动
- **Auth:** 全局登录态（客户端用户）
- **Request params:** 无
- **Response `data`:** `ClientInviteActivityInfoVo`（含 `progressList`，已富化展示字段）
- **业务说明:** 走 `activity.client-default-invite-activity-id` 配置的默认活动 ID；仅返回激活态活动；只列邀请人进度（type=1）
- **错误返回:** code=9001 (TIP_CUSTOM)，msg="未配置邀请活动"

---

### Request: `ClientInviteActivityBo`
| Field | Type | Required | Validation | Meaning |
|---|---|---|---|---|
| id | Long | save 时后端生成 / update 时必填 | -- | 雪花 ID |
| agentId | Long | 是 | -- | 代理商 ID |
| activityName | String | 是 | -- | 邀请活动名称 |
| activityStartTime | Date | 是 | -- | 活动开始时间 |
| activityEndTime | Date | 是 | -- | 活动结束时间 |
| activityStatus | Integer | 是 | `0 \| 1` | 0 未启用 / 1 启用中 |
| clientInviteProgressBoList | List\<ClientInviteProgressBo\> | 否 | -- | 进度和进度奖励集合（级联保存） |

### Response: `ClientInviteActivityVo` (base -- InfoVo 和 ListVo 的父类)
| Field | Type | Meaning |
|---|---|---|
| id | Long | 雪花 ID |
| agentId | Long | 代理商 ID |
| activityName | String | 邀请活动名称 |
| activityStartTime | Date | 活动开始时间 |
| activityEndTime | Date | 活动结束时间 |
| activityStatus | Integer | 0 未启用 / 1 启用中 |
| createDate | Date | 创建时间 |
| updateDate | Date | 最后修改时间 |
| isDeleted | Integer | 是否已删除 |

### Response: `ClientInviteActivityInfoVo` (extends ClientInviteActivityVo)
| Field | Type | Meaning |
|---|---|---|
| progressList | List\<ClientInviteProgressInfoVo\> | 活动进度阶梯列表（含 rewardList），仅启用中 |

---

## 二、进度与奖励子结构

### Request: `ClientInviteProgressBo`（嵌套在 `ClientInviteActivityBo.clientInviteProgressBoList` 中）
| Field | Type | Required | Meaning |
|---|---|---|---|
| inviteActivityId | Long | 是 | 关联的邀请活动 ID |
| inviteProgressType | Integer | 是 | 0 被邀请人进度 / 1 邀请人进度 |
| inviteProgressCode | String | 否 | 邀请进度 code |
| inviteProgressValue | Integer | 是 | 邀请进度值（如邀请 3 人） |
| inviteProgressTitle | String | 是 | 邀请进度标题 |
| inviteProgressRequire | String | 否 | 邀请进度要求描述 |
| inviteProgressStatus | Integer | 是 | 0 停用 / 1 启用中 |
| rewardList | List\<ClientInviteProgressRewardBo\> | 否 | 该进度的奖励规则列表 |

### Response: `ClientInviteProgressVo` (base)
| Field | Type | Meaning |
|---|---|---|
| id | Long | 雪花 ID |
| inviteActivityId | Long | 关联邀请活动 ID |
| inviteProgressType | Integer | 0 被邀请人 / 1 邀请人 |
| inviteProgressCode | String | 进度 code |
| inviteProgressValue | Integer | 进度值 |
| inviteProgressTitle | String | 进度标题 |
| inviteProgressRequire | String | 进度要求 |
| inviteProgressStatus | Integer | 0 停用 / 1 启用中 |
| createDate / updateDate / isDeleted | Date/Date/Integer | 审计字段 |

### Response: `ClientInviteProgressInfoVo` (extends ClientInviteProgressVo)
| Field | Type | Meaning |
|---|---|---|
| rewardList | List\<ClientInviteProgressRewardInfoVo\> | 该进度的奖励规则列表 |

### Request: `ClientInviteProgressRewardBo`
| Field | Type | Required | Meaning |
|---|---|---|---|
| id | Long | save 时后端生成 / update 时必填 | 雪花 ID |
| progressId | Long | 是 | 所属进度 ID |
| rewardType | Long | 是 | 0 版本 / 1 增量包 |
| packageId | Long | rewardType=0 时必填 | 版本 ID |
| packagePriceId | Long | rewardType=0 时必填 | 版本价格 ID |
| commodityTypeId | Long | rewardType=1 时必填 | 商品类型 ID |
| commodityNumber | Long | rewardType=1 时必填 | 商品数据量 |
| validityNum | Integer | rewardType=1 时必填 | 有效期数值（如 3） |
| validityUnit | Integer | rewardType=1 时必填 | 有效期单位：0时/1天/2月/3季度/4半年/5年 |

### Response: `ClientInviteProgressRewardVo` (base)
| Field | Type | Meaning |
|---|---|---|
| id | Long | 雪花 ID |
| progressId | Long | 所属进度 ID |
| rewardType | Long | 0 版本 / 1 增量包 |
| packageId | Long | 版本 ID |
| packagePriceId | Long | 版本价格 ID |
| commodityTypeId | Long | 商品类型 ID |
| commodityNumber | Long | 商品数据量 |
| validityNum | Integer | 有效期数值 |
| validityUnit | Integer | 有效期单位 0-5 |
| createDate / updateDate / isDeleted | Date/Date/Integer | 审计字段 |

### Response: `ClientInviteProgressRewardInfoVo` (extends base, 客户端富化)
| Field | Type | Meaning |
|---|---|---|
| packageName | String | 版本名称（rewardType=0 时有值） |
| packagePrice | Integer | 版本价格（rewardType=0 时有值） |
| packageDuration | String | 版本时长（rewardType=0 时有值） |
| commodityTypeName | String | 增量包类型名称（rewardType=1 时有值） |
| commodityTypeUnit | String | 增量包展示单位（rewardType=1 时有值） |
| commodityTypeValidity | String | 增量包有效期（rewardType=1 时有值） |

---

## 三、内部 Feign（跨模块调用 -- replay-activity 模块）

所有实现位于 `com.jiuyu.replay.activity.api.*Api`，接口定义在 `replay-generic/com.jiuyu.replay.generic.feign.activity.*`。同 JVM 调用，无鉴权。

### Feign-1 `ActivityFeign`
实现：`com.jiuyu.replay.activity.api.ActivityApi`
| 方法 | 入参 | 出参 | 业务语义 |
|---|---|---|---|
| `infoActivateById(Long id)` | `id`（可为 null，null 时取默认活动 ID） | `R<ClientInviteActivityInfoVo>` | 拿激活态活动（status=1 且时间窗口内）；非激活返回 data=null |

**调用方:** `replay-agent.InviteUrlCodeBll`, `replay-reward.ClientInviteRewardRecordBll`

### Feign-2 `UserInviteFeign`
实现：`com.jiuyu.replay.activity.api.UserInviteApi`
| 方法 | 入参 | 出参 | 业务语义 |
|---|---|---|---|
| `listByCode(String urlCode)` | 邀请链接 code | `R<List<UserInviteInfoVo>>` | 查某邀请链接绑定的有效邀请记录（status=1） |
| `getInviteUserNumberByInviteUserId(Long inviteUserId)` | 邀请人用户 ID | `Long`（计数） | 该用户通过用户链接（type=2）邀请的有效人数 |
| `judgeUserInviteEffective(Long acceptUserId)` | 被邀请人用户 ID | `UserInviteDto`（可为 null） | 判定该用户是否被用户链接邀请；非 null = 邀请奖励生效 |

**调用方:** `replay-power.UserBll`, `replay-order.UserPropertyImpl`, `replay-reward.ClientInviteRewardRecordBll`, `replay-api.SensitiveWordsLogicImpl`

### Feign-3 `ClientInviteProgressFeign`
实现：`com.jiuyu.replay.activity.api.ClientInviteProgressApi`
| 方法 | 入参 | 出参 | 业务语义 |
|---|---|---|---|
| `listClientInviteProgressByActivityIdAndInviteProgressType(Long activityId, Integer inviteProgressType)` | 活动 ID + 进度类型（0/1） | `List<ClientInviteProgressDto>` | 取活动下某类型的启用中进度阶梯 |

**调用方:** `replay-reward.ClientInviteRewardRecordBll`

### Feign-4 `ClientInviteProgressRewardFeign`
实现：`com.jiuyu.replay.activity.api.ClientInviteProgressRewardApi`
| 方法 | 入参 | 出参 | 业务语义 |
|---|---|---|---|
| `listByProgressRewardIds(Collection<Long> progressRewardIds)` | 奖励规则 ID 列表 | `R<List<ClientInviteProgressRewardInfoVo>>` | 按 ID 批量取奖励规则详情 |
| `listByProgressIds(List<Long> progressIds)` | 进度阶梯 ID 列表 | `List<ClientInviteProgressRewardDto>` | 按 progress_id 批量取奖励规则 |

**调用方:** `replay-reward.ClientInviteRewardRecordBll`

---

## 四、管理端 CRUD 接口（代码保留，未上线 -- replay-api agent 包）

> 以下接口位于 `replay-api/src/main/java/com/jiuyu/replay/api/controller/agent/`，`@RestController` 被注释（`//@RestController`），当前未对外开放 HTTP 端点。BO/VO 类位于 `replay-agent` 模块。

### 4.1 邀请活动管理 (`/replay/clientinviteactivity`) -- ClientInviteActivityController
| Method | Path | Summary |
|---|---|---|
| POST | `/replay/clientinviteactivity/list` | 邀请活动列表 |
| GET | `/replay/clientinviteactivity/info` | 邀请活动信息 |
| POST | `/replay/clientinviteactivity/save` | 新增邀请活动 |
| POST | `/replay/clientinviteactivity/update` | 修改邀请活动 |
| GET | `/replay/clientinviteactivity/delete` | 删除邀请活动 |

> 请求/响应结构与 replay-activity 模块的同名接口一致。`ClientInviteActivityBo` 含 `agentId`、`activityName`、`activityStartTime`、`activityEndTime`、`activityStatus`、`clientInviteProgressBoList`。

### 4.2 邀请进度管理 (`/replay/clientinviteprogress`) -- ClientInviteProgressController
| Method | Path | Summary |
|---|---|---|
| POST | `/replay/clientinviteprogress/list` | 邀请进度列表 |
| GET | `/replay/clientinviteprogress/info` | 邀请进度信息 |
| POST | `/replay/clientinviteprogress/save` | 新增邀请进度 |
| POST | `/replay/clientinviteprogress/update` | 修改邀请进度 |
| GET | `/replay/clientinviteprogress/delete` | 删除邀请进度 |
| GET | `/replay/clientinviteprogress/clientGetInviteProgressAndReward` | 客户端获取邀请进度和奖励 |

> `clientGetInviteProgressAndReward` 是唯一可能被启用的客户端读接口。响应 `R<List<ClientInviteProgressInfoVo>>`，含 `rewardList`。

### 4.3 邀请进度奖励管理 (`/replay/clientinviteprogressreward`) -- ClientInviteProgressRewardController
| Method | Path | Summary |
|---|---|---|
| POST | `/replay/clientinviteprogressreward/list` | 进度奖励列表 |
| GET | `/replay/clientinviteprogressreward/info` | 进度奖励信息 |
| POST | `/replay/clientinviteprogressreward/save` | 新增进度奖励 |
| POST | `/replay/clientinviteprogressreward/update` | 修改进度奖励 |
| GET | `/replay/clientinviteprogressreward/delete` | 删除进度奖励 |

### 4.4 邀请奖励记录 (`/replay/clientinviterewardrecord`) -- ClientInviteRewardRecordController
| Method | Path | Summary |
|---|---|---|
| POST | `/replay/clientinviterewardrecord/list` | 奖励记录列表 |
| GET | `/replay/clientinviterewardrecord/info` | 奖励记录信息 |
| POST | `/replay/clientinviterewardrecord/save` | 新增奖励记录 |
| POST | `/replay/clientinviterewardrecord/update` | 修改奖励记录 |
| GET | `/replay/clientinviterewardrecord/delete` | 删除奖励记录 |

### Request/Response: `ClientInviteRewardRecordBo/Vo`
| Field | Type | Meaning |
|---|---|---|
| id | Long | 雪花 ID |
| inviteUserId | Long | 邀请人用户 ID |
| beInviteUserId | Long | 被邀请人用户 ID |
| inviteProgressId | Long | 邀请进度 ID |
| tenantId | Long | 租户 ID |
| createDate / updateDate / isDeleted | Date/Date/Integer | 审计字段 |

### 4.5 邀请奖励明细记录 (`/replay/clientinviterewardrecorddetail`) -- ClientInviteRewardRecordDetailController
| Method | Path | Summary |
|---|---|---|
| POST | `/replay/clientinviterewardrecorddetail/list` | 奖励明细列表 |
| GET | `/replay/clientinviterewardrecorddetail/info` | 奖励明细信息 |
| POST | `/replay/clientinviterewardrecorddetail/save` | 新增奖励明细 |
| POST | `/replay/clientinviterewardrecorddetail/update` | 修改奖励明细 |
| GET | `/replay/clientinviterewardrecorddetail/delete` | 删除奖励明细 |

### Request/Response: `ClientInviteRewardRecordDetailBo/Vo`
| Field | Type | Meaning |
|---|---|---|
| id | Long | 雪花 ID |
| userId | Long | 用户 ID |
| inviteRewardRecordId | Long | 奖励记录 ID |
| inviteProgressRewardId | Long | 进度奖励 ID |
| rewardContent | String | 奖励内容描述 |
| tenantId | Long | 租户 ID |
| createDate / updateDate / isDeleted | Date/Date/Integer | 审计字段 |

---

## 五、错误码 / 响应结构

| 场景 | code | msg | 触发 |
|---|---|---|---|
| 默认成功 | 0 | "添加成功"/"修改成功"/"删除成功"/"获取成功" | 全部 HTTP 接口正常路径 |
| 业务自定义提示 | 9001 (TIP_CUSTOM) | "未配置邀请活动" | `/infoByClient` 无默认活动或活动非激活态时 |

**响应包装:** 所有 HTTP/Feign 返回经 `R.ok(...)` / `R.error(code, msg)` 包装（来自 `replay-generic`）。

---

## API 路由表

| API (Method + Path) | Summary | Auth | 来源 |
|---|---|---|---|
| POST `/replay/activity/clientinviteactivity/list` | 邀请活动列表（分页） | 全局登录态 | replay-activity |
| GET `/replay/activity/clientinviteactivity/info` | 邀请活动详情（含 progress+reward） | 全局登录态 | replay-activity |
| POST `/replay/activity/clientinviteactivity/save` | 新增邀请活动（级联 progress+reward） | 全局登录态 | replay-activity |
| POST `/replay/activity/clientinviteactivity/update` | 修改邀请活动（替换式） | 全局登录态 | replay-activity |
| GET `/replay/activity/clientinviteactivity/delete` | 删除邀请活动 | 全局登录态 | replay-activity |
| GET `/replay/activity/clientinviteactivity/infoByClient` | 客户端拉默认激活活动 + 富化展示 | 全局登录态 | replay-activity |
| Feign `ActivityFeign#infoActivateById` | 取激活态活动 | 同 JVM | replay-activity |
| Feign `UserInviteFeign#listByCode` | 按邀请码查邀请记录 | 同 JVM | replay-activity |
| Feign `UserInviteFeign#getInviteUserNumberByInviteUserId` | 邀请人计数（type=2, status=1） | 同 JVM | replay-activity |
| Feign `UserInviteFeign#judgeUserInviteEffective` | 判定被邀请人邀请关系生效 | 同 JVM | replay-activity |
| Feign `ClientInviteProgressFeign#list*` | 按活动+类型拉进度阶梯 | 同 JVM | replay-activity |
| Feign `ClientInviteProgressRewardFeign#listByProgressRewardIds` | 按规则 ID 拉奖励规则 | 同 JVM | replay-activity |
| Feign `ClientInviteProgressRewardFeign#listByProgressIds` | 按进度 ID 拉奖励规则 | 同 JVM | replay-activity |
| (保留) POST `/replay/clientinviteactivity/*` | 5 个端点（list/info/save/update/delete） | -- | replay-api（未上线） |
| (保留) POST/GET `/replay/clientinviteprogress/*` | 6 个端点（含 clientGetInviteProgressAndReward） | -- | replay-api（未上线） |
| (保留) POST/GET `/replay/clientinviteprogressreward/*` | 5 个端点 | -- | replay-api（未上线） |
| (保留) POST/GET `/replay/clientinviterewardrecord/*` | 5 个端点 | -- | replay-api（未上线） |
| (保留) POST/GET `/replay/clientinviterewardrecorddetail/*` | 5 个端点 | -- | replay-api（未上线） |

---

## 备注（实现细节 / 偏离规范处）

- DI 全部使用 `@Resource`（jakarta.annotation.Resource），未遵循 CLAUDE.md 构造器注入约束。
- `Controller` 未声明 `@CrossOrigin`，依赖全局跨域配置。
- 列表查询 `keyword` 仅 `LIKE name`，但实体字段是 `activityName`，列名疑似拼写错误。
- `/delete` 用 GET 接受 id，接口语义不规范（GET 应幂等读操作），且无 `@NoRepeatSubmit`。
- replay-api agent 包下的活动控制器全量 `//@RestController` 注释，未暴露 HTTP，保留供后续启用。
