<!-- module: reward -->
<!-- area: api -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-reward/src/main/java/com/jiuyu/replay/reward/contraller/ -->

# Reward API — 接口契约

> `replay-reward` **本身**直接暴露 3 个 HTTP 接口（注意 controller 包名拼错：`contraller`）。  
> **不通过 Feign 对外服务** —— 跨模块写入是由 `replay-api` 的 RocketMQ Listener 在同一 JVM 内**注入 Bll 直接调用**完成的，详见 `reward_architecture.md`。

---

## 一、HTTP 接口

所有路径前缀：`replay/reward/clientinviterewardrecord`  
鉴权：依赖 `replay-api` 全局拦截器（用户 token / `UserFeign.getLocalUser()`）。本模块未声明独立的鉴权切面。

返回格式：`R<T>`（`com.jiuyu.replay.generic.vo.common.R`）。

### API-RW-01: 后台获取奖励列表（分组）

| 属性 | 值 |
|---|---|
| Method | POST |
| Path | `replay/reward/clientinviterewardrecord/listByBack` |
| 调用方 | 后台管理端 |
| 说明 | 按多种条件筛选已发放奖励，**按 `reward_user_id + progress_id + reward_source_user_id` 三键分组**返回一行 |

**Request Body**: `ClientInviteRewardRecordListBo` (extends `PageBo`)

| 字段 | 类型 | 必填 | 校验 | 含义 |
|---|---|---|---|---|
| `page` / `limit` | Integer | 是 | >0 | 分页 |
| `progressCode` | String | 否 | — | 进度 code，如 `REGISTER` / `DOWNLOAD` |
| `rewardStatus` | String | 否 | 0/1 | 0 待发放 / 1 已发放 |
| `inviterName` | String | 否 | — | 邀请人昵称/手机号模糊匹配，内部转换为 `inviterUserIds` |
| `inviteeName` | String | 否 | — | 被邀请人昵称/手机号模糊匹配，内部转换为 `inviteeUserIds` |
| `startTime` | String | 否 | `yyyy-MM-dd` | 奖励发放开始时间 |
| `endTime` | String | 否 | `yyyy-MM-dd` | 奖励发放结束时间 |

**Response**: `R<PageUtils<ClientInviteRewardRecordInfoVo>>`

每行 `ClientInviteRewardRecordInfoVo`（继承 `ClientInviteRewardRecordVo`）含：

| 字段 | 类型 | 含义 |
|---|---|---|
| `id` / `activityId` / `progressId` / `progressRewardId` | Long | 标识 |
| `rewardTargetType` | Integer | 0 邀请人 / 1 被邀请人 |
| `rewardUserId` / `rewardSourceUserId` | Long | 邀请人 / 被邀请人 |
| `rewardTenantId` | Long | 限制租户 |
| `progressCode` | String | 进度 code |
| `progressCodeStr` | String | 翻译为中文 label（从 `invite_user_reward_code` 字典查） |
| `rewardStatus` | Long | 0 待发放 / 1 已发放 |
| `sendDate` / `createDate` / `updateDate` | Date | 时间戳 |
| `inviterName` / `inviteeName` | String | 用户昵称 |
| `progressRewardIds` | List&lt;Long&gt; | 这一行（user×progress×source 分组）下的所有奖励规则 ID |
| `rewardList` | List&lt;String&gt; | 拼接好的奖励描述，如 `["实时分析点数100点","试用版本30天"]` |

> 后台列表查询硬编码 `reward_target_type=0`、`reward_status=1` —— **后台目前只看邀请人侧的已发放记录**。

> 已知 bug：`endTime` 拼接时实际用了 `startTime + " 23:59:59"`（见 `ClientInviteRewardRecordRseImpl#listByBack` 第 200 行）。

---

### API-RW-02: 客户端获取奖励汇总

| 属性 | 值 |
|---|---|
| Method | GET |
| Path | `replay/reward/clientinviterewardrecord/clientGetRewardSummary` |
| 调用方 | 客户端 |
| 说明 | 返回当前登录用户作为**邀请人**已获得的奖励聚合（按套餐 / 增量包合并），外加总邀请人数 |

**Request**: 无 query / body 参数。当前用户由 `UserFeign.getLocalUser()` 取出；子账号 → 自动改取主账号 ID。

**Response**: `R<List<RewardSummaryVo>>`

| 字段 | 类型 | 含义 |
|---|---|---|
| `rewardLabel` | String | "总邀请人数" / 套餐名称 / 增量包名称 |
| `rewardNum` | Long | 数量（套餐：合计有效时长数值；增量包：合计数量） |
| `rewardUnit` | String | 单位（"人" / "天/月/年" / 增量包单位） |

**列表第一项固定为**：`{rewardLabel:"总邀请人数", rewardNum: <inviteNumber>, rewardUnit:"人"}`，
后续是按 packageId 分组的套餐时长合计 + 按 commodityTypeId 分组的增量包数量合计。

**返回空列表的短路条件**：当前活动不存在/已过期 → `[]`；当前用户未生成邀请码 → `[]`；用户没有有效邀请 → `[]`；用户没有已发放奖励 → `[]`。

---

### API-RW-03: 客户端获取邀请奖励列表

| 属性 | 值 |
|---|---|
| Method | POST |
| Path | `replay/reward/clientinviterewardrecord/clientGetUserRewardList` |
| 调用方 | 客户端 |
| 说明 | 分页返回当前用户作为邀请人收到的奖励明细，**按 (progress_id, reward_source_user_id) 分组**为一行（一个被邀请人在某个进度上的所有奖励聚合为一条） |

**Request Body**: `UserRewardBo` (extends `PageBo`, 只继承分页字段，无业务字段)

**Response**: `R<PageUtils<ClientUserRewardRecordVo>>`

| 字段 | 类型 | 含义 |
|---|---|---|
| `userNickName` | String | 被邀请人昵称；未知 → `"未知"`（占位） |
| `phone` | String | 被邀请人手机号，经 `DesensitizedUtil.mobilePhone` 脱敏（中间 4 位 `****`） |
| `progress` | String | 进度名称（由 `InviteRewardRuleCodeEnum.getNameByCode` 翻译，如 `REGISTER` → "注册爱复盘"） |
| `rewardDetailList` | List&lt;ClientUserRewardRecordDetailVo&gt; | 该 (进度, 来源用户) 下的所有奖励明细 |
| `rewardDate` | Date | 该聚合行的最近发放时间 |

`ClientUserRewardRecordDetailVo`：

| 字段 | 类型 | 含义 |
|---|---|---|
| `rewardType` | String | 奖励名称（套餐名 / 增量包名） |
| `rewardNumber` | Long | 数量 |
| `rewardUnit` | String | 单位（`ValidityUnitEnum.getNameByCode` 转换的"天/月/年"或增量包单位） |

底层 SQL：`pageUserRewardListByRewardUserIdAndRewardTargetTypeGroupByProgressId` —— 自定义 XML，`GROUP_CONCAT(progress_reward_id SEPARATOR '_')` 拼接出 `progressRewardIdStr`，Bll 层 `split("_")` 反解出每个进度奖励 ID。

---

## 二、Feign 接口（外部暴露）

**当前为空。** `replay-reward` 没有声明任何 `@FeignClient` 服务接口。其他模块若需要调用本模块业务：

- 写路径：依赖 `replay-api` 的 `RocketMqListener` 通过 RocketMQ tag `Tag_User_Invite_Activity` 间接触发（生产者在 `replay-power`/`replay-order`/`replay-words`，发送 `UserInviteMqDto`）。
- 读路径：当前无对外读接口，业务读取统一走前述 3 个 HTTP 接口。

---

## 三、Event Listener 接口

**本模块不直接监听任何事件**。MQ 监听物理位置在 `replay-api`：

- `RocketMqListener#processUserInviteActivity`（tag = `Tag_User_Invite_Activity`）→ 注入 `ClientInviteRewardRecordBll` → 调用 `giveUserInviteAward(UserInviteMqDto)`。
- 入站消息 DTO：`UserInviteMqDto { rewardRuleCode, acceptUserId, fingerprint }`。
