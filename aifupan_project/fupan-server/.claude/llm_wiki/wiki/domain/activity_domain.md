<!-- module: activity -->
<!-- area: domain -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-activity/ -->

# Activity Domain — 业务概念与词汇表

> replay-activity 模块负责**邀请活动配置**与**用户-邀请关系记录**。
> 模块只**定义**奖励规则（活动 / 进度阶梯 / 奖励规则）与**记录**邀请关系；
> 实际**奖励计算和发放**在 `replay-reward` 模块（消费 MQ + 调用 `replay-order` 下单）。

---

## 模块定位

| 模块 | 职责 | 与 activity 的关系 |
|---|---|---|
| **replay-activity** | 活动定义 + 进度阶梯定义 + 奖励规则定义 + 用户邀请关系记录 | 本模块 |
| replay-agent | 邀请链接 / 邀请码（`tb_invite_url_code`）；通过 `ActivityFeign.infoActivateById` 拿当前活动 | 上游：分发链接 |
| replay-reward | 奖励记录（`tb_client_invite_reward_record`）+ 奖励发放编排；通过 `UserInviteFeign` / `ClientInviteProgressFeign` / `ClientInviteProgressRewardFeign` 读规则 | 下游：消费规则 |
| replay-order | 实际下单（`OrderFeign.addActivityOrder`）：把版本/增量包以"活动订单"形式发到目标用户 | 下游：执行发放 |

---

## 核心概念

| 概念 | 定义 | 关联概念 | 状态机 |
|------|------|----------|--------|
| **InviteActivity (邀请活动)** | 一段时间内 + 一个代理商下的邀请奖励活动配置，含活动名称、起止时间、启用状态 | Agent, InviteProgress | `activityStatus`: 0未启用 / 1启用中 |
| **InviteProgress (邀请进度 / 阶梯门槛)** | 一个活动下的进度阶梯节点（例如"邀请 3 人""邀请 10 人"），区分被邀请人进度 / 邀请人进度 | InviteActivity, InviteProgressReward | `inviteProgressStatus`: 0停用 / 1启用 |
| **InviteProgressReward (进度奖励规则)** | 达到某个进度节点后发放的奖励规则（版本套餐 或 增量包），含有效期单位/数量 | InviteProgress, Package, CommodityType | — |
| **UserInvite (用户-邀请关联)** | 谁邀请了谁的关系记录，含邀请类型 / 邀请码 / 邀请状态 | User (replay-power), InviteUrlCode (replay-agent) | `inviteStatus`: 1有效 / 0无效 |

> 注：本模块**不存奖励发放记录**。奖励记录、发放状态、领取流水在 `replay-reward.tb_client_invite_reward_record`。

---

## 字段值表（枚举语义）

### 活动状态 (`tb_client_invite_activity.activity_status`)
- `0` 未启用 — 配置完成但不向客户端展示
- `1` 启用中 — 同时还需满足 `activity_start_time < now < activity_end_time` 才算"激活态"

> "激活态" 判定见 `ClientInviteActivityRseImpl#infoByActivate`：
> ```
> activity_status = 1 AND activity_start_time < now AND activity_end_time > now
> ```

### 进度类型 (`tb_client_invite_progress.invite_progress_type`)
- `0` 被邀请人进度（passive） — 被邀请人达成条件可领的奖励阶梯
- `1` 邀请人进度（active） — 邀请人达成条件可领的奖励阶梯

### 进度启用状态 (`tb_client_invite_progress.invite_progress_status`)
- `0` 停用 — 修改活动时旧进度会被批量置为 0（见下方"活动版本切换"）
- `1` 启用中 — 当前有效阶梯

### 奖励类型 (`tb_client_invite_progress_reward.reward_type`)
- `0` 版本（套餐）— 使用 `package_id` + `package_price_id`，`commodity_type_id` / `commodity_number` 必为 null
- `1` 增量包 — 使用 `commodity_type_id` + `commodity_number` + `validity_num` + `validity_unit`，`package_id` / `package_price_id` 必为 null

> 互斥写入由 `ClientInviteActivityRseImpl#saveBatch` 强制：根据 `rewardType` 清空另一侧字段。

### 有效期单位 (`validity_unit`)
| 值 | 单位 |
|---|---|
| 0 | 小时 |
| 1 | 天 |
| 2 | 月 |
| 3 | 季度 |
| 4 | 半年 |
| 5 | 年 |

### 邀请类型 (`tb_user_invite.invite_type`)
- `0` 代理商链接
- `1` 推广渠道链接
- `2` 用户链接 ← **触发用户邀请奖励的唯一类型**（见 `UserInviteRseImpl#judgeUserInviteEffective`、`getInviteUserNumberByInviteUserId`）
- `3` 代理商销售链接

### 邀请状态 (`tb_user_invite.invite_status`)
- `1` 有效（默认新建即为 1）
- `0` 无效

---

## 核心业务规则

### 规则 1 — 活动激活态判定（三条件 AND）

```
activeActivity = status=1 ∧ start_time < now ∧ end_time > now
```

只有"激活态"活动会向客户端展示（`ClientInviteActivityController#infoByClient`）。
非激活活动可被后台编辑/预览，但不参与奖励发放。

### 规则 2 — 活动只有一个"默认"

平台默认活动 ID 由配置项 `activity.client-default-invite-activity-id` 注入（见 `ActivityProperties`）。
所有下游（agent / reward）调用 `activityFeign.infoActivateById(null)` 时自动走默认活动。

### 规则 3 — 修改活动 = 全量替换进度阶梯

`ClientInviteActivityBll#update` 流程（事务）：
1. 更新活动主体
2. **将旧的所有 InviteProgress `status` 改为 0** (`disableOld`)
3. 重新批量插入新阶梯

含义：进度阶梯不做"版本号"，而是**软停用旧的 + 新增新的**。历史奖励发放记录（在 reward 模块）通过 `progress_id` 指向旧阶梯仍然可追溯。

### 规则 4 — 奖励类型互斥

reward_type 决定填哪组字段，另一组必须为 null（由 saveBatch 强制清空）：

| reward_type | 必填字段 | 必为 null |
|---|---|---|
| 0 版本 | package_id, package_price_id | commodity_type_id, commodity_number |
| 1 增量包 | commodity_type_id, commodity_number | package_id, package_price_id |

### 规则 5 — 用户邀请奖励生效条件

`UserInviteFeign#judgeUserInviteEffective(acceptUserId)` 返回非空 ⟺
存在一条 `tb_user_invite`，满足：
- `passive_user_id = acceptUserId`
- `invite_status = 1`
- `invite_type = 2`（用户链接）

只有这三条同时满足，下游 reward 才会进入奖励发放流程。代理商/渠道/销售链接邀请**不进入**用户邀请奖励链路。

### 规则 6 — 邀请进度计数口径

`UserInviteFeign#getInviteUserNumberByInviteUserId(inviteUserId)`：
```
COUNT(*) WHERE invite_user_id=? AND invite_type=2 AND invite_status=1
```
即**该用户作为邀请人**，**通过用户链接**，**当前仍有效**的被邀请人数。
该数字驱动 reward 模块按 `inviteProgressValue` 阈值发放奖励。

---

## 核心工作流

### 流程 A — 后台配置活动

```
管理员 → 邀请活动表单（活动 + N 个阶梯 + 每阶梯 M 条奖励规则）
→ POST /save 或 /update（@Transactional）
→ tb_client_invite_activity ↻
→ tb_client_invite_progress 批量 ↻
→ tb_client_invite_progress_reward 批量 ↻（rewardType 字段互斥清洗）
```

### 流程 B — 客户端拉取当前活动

```
客户端登录 → GET /infoByClient
→ activityProperties.clientDefaultInviteActivityId
→ ClientInviteActivityRse#infoByActivate（status=1 ∧ 时间窗口内）
→ ClientInviteProgressRse#getInviteProgressAndReward(activityId, inviteProgressType=1)
   注：客户端只展示"邀请人进度"，按 invite_progress_value 升序
→ 调 PackageFeign / CommodityTypeFeign 补全套餐名 / 价格 / 数量单位
→ 返回 ClientInviteActivityInfoVo（含 progressList + rewardList）
```

### 流程 C — 邀请关系写入（被动方注册时）

```
新用户用邀请链接注册 → replay-power 创建 tb_user
→ replay-power 通过 UserInviteFeign（具体落点：tb_user_invite 写一条）
→ tb_user_invite { invite_user_id, passive_user_id, invite_code, invite_type, invite_status=1 }
```

(注：写入触发点在 replay-power / replay-agent，本模块只提供查询和判定接口。)

### 流程 D — 奖励发放（reward 主导，跨模块）

```
（reward 模块）MQ: UserInviteMqDto → ClientInviteRewardRecordBll#giveUserInviteAward
→ activity.UserInviteFeign#judgeUserInviteEffective(passiveUserId)  ← 入口闸门
→ activity.ActivityFeign#infoActivateById(null)                     ← 拿当前活动
→ activity.ClientInviteProgressFeign#listClientInviteProgressByActivityIdAndInviteProgressType
→ activity.ClientInviteProgressRewardFeign#listByProgressIds         ← 取规则
→ reward 落 tb_client_invite_reward_record
→ order.OrderFeign#addActivityOrder（执行发放）
→ reward 更新 reward_status=已发放
```

---

## 与其他模块的区别

| 维度 | replay-activity | replay-agent (邀请奖励) | replay-reward (奖励记录) |
|---|---|---|---|
| 持有什么 | 活动定义 + 阶梯 + 规则 + 邀请关系 | 邀请链接 / 短码 | 用户领奖流水 |
| 写入触发 | 后台运营 | 用户首次生成链接 | MQ 消费 + 进度达成 |
| 有 tenantId 吗 | 否（平台级 / agentId 维度） | 是 | 是 |
| 涉及 @Transactional | 是（活动 save/update） | 是（链接生成 + 分布式锁） | 是（奖励发放） |

---

## 角色与权限

| 角色 | 权限范围 |
|---|---|
| 平台运营 | 维护 `tb_client_invite_activity` / progress / reward 配置，启停活动 |
| 客户端用户 | 只读：通过 `/infoByClient` 看当前激活活动 + 邀请人阶梯 |
| 系统（reward 模块） | 通过 4 个 Feign 接口读规则与邀请关系（无授权校验，内部信任） |

---

## 已知约定与限制

- **无租户隔离**：activity / progress / progress_reward / user_invite 四张表均**无 `tenant_id` 字段**，邀请活动是平台级配置（按 `agent_id` 区分代理商，但客户端只用默认活动 ID）。
- **无逻辑删除过滤**：`is_deleted` 字段存在但代码用 `removeById` 物理删除（MyBatis-Plus 默认 `DELETE`，未配置 `@TableLogic`）。
- **进度阶梯软停用**：修改活动时旧 progress 不删，仅置 `invite_progress_status=0`，保留历史可追溯。
- **奖励类型互斥**：保存时强制清洗另一侧字段，避免脏数据。
- **客户端只展示邀请人进度**：`infoByClient` 硬编码 `inviteProgressType=1`，被邀请人进度（type=0）仅在 reward 模块发放时使用。
