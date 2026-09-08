<!-- module: reward -->
<!-- area: domain -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-reward/, replay-generic/.../reward/ -->

# Reward Domain — 业务概念与词汇表

> `replay-reward` 模块只承载一件事：**记录"邀请有礼"活动下每一笔奖励的生命周期流水**（待发放 → 已发放），并按用户维度/进度维度做查询与汇总。  
> 真正发奖（开权益、加套餐、加增量包）由 `replay-order` 完成；规则与活动配置在 `replay-activity` 与 `replay-agent`。本模块是**记账与审计**层。

---

## 一、核心概念

| 概念 | 定义 | 关联概念 | 状态机 |
|---|---|---|---|
| **进度奖励记录 (ClientInviteRewardRecord)** | 一笔可发放/已发放的邀请奖励流水，绑定到 (活动, 进度, 进度奖励规则, 受奖用户, 来源用户, 租户) 六元组 | Activity, ProgressReward, User, Tenant | `rewardStatus`: 0 待发放 / 1 已发放 |
| **奖励对象类型 (rewardTargetType)** | 区分这笔奖励是发给"邀请人"还是"被邀请人" | InviteUser, PassiveUser | 枚举: 0 邀请人 / 1 被邀请人 |
| **奖励来源用户 (rewardSourceUserId)** | 这笔奖励是"因为谁"产生的 —— 邀请人记录里指向被邀请人，被邀请人记录里指向邀请人 | 用户互指对偶 | — |
| **进度 (Progress) / 进度奖励 (ProgressReward)** | 活动配置维度，定义在 `replay-activity`。一个进度可挂多条奖励规则（不同套餐 / 增量包） | Activity | — |
| **进度 code (progressCode)** | 业务语义化的进度标识，如 `REGISTER` / `DOWNLOAD` / `USE` / `AI`，用于幂等判断"该进度是否已发过" | InviteRewardRuleCodeEnum | — |
| **限制租户 (rewardTenantId)** | 这笔奖励**只在该租户内部生效**——同一用户在不同租户下属于不同的奖励隔离单元 | tenantId | — |
| **奖励汇总 (RewardSummary)** | 用户视角的奖励聚合视图：按套餐/增量包合并已发放奖励总量 + 总邀请人数 | RewardRecord, Package, CommodityType | — |
| **设备指纹 (Fingerprint)** | 防止同一台设备/同一浏览器反复触发奖励的反作弊键 —— 由 `replay-power` 的 `tb_device_fingerprint` 持久化 | UserDeviceFingerprint | — |

---

## 二、奖励规则 code 枚举 (InviteRewardRuleCodeEnum)

定义在 `replay-generic/.../activity/InviteRewardRuleCodeEnum.java`，本模块按 code 路由不同的反作弊策略。

| code | 名称 | 触发场景 | 反作弊策略 |
|---|---|---|---|
| `REGISTER` | 注册爱复盘 | 被邀请人完成注册 | 指纹首次出现才发放；指纹已绑定其他用户 → 不发 |
| `DOWNLOAD` | 下载爱复盘 | 客户端首次登录（区分 `desktop_` / `web_` 指纹） | `web_` 一律不发；`desktop_` 指纹未绑定 → 发 |
| `USE` | 使用爱复盘 | 录制/分析直播 N 次后触发 | 指纹未绑定 → 发；已绑定他人 → 不发 |
| `AI` | 使用 AI 复盘 | AI 助手消耗触发 | 与 `USE` 同策略 |

> 指纹格式硬性校验：必须以 `desktop_` 或 `web_` 开头；包含 `error` 字符串视作无效；为空直接放弃。

---

## 三、状态机

### 单条 RewardRecord 生命周期

```
[新建]                        [发放]
 ↓                              ↓
rewardStatus=0  ─────────►  rewardStatus=1
(待发放)                      (已发放)
sendDate=null              sendDate=now
```

| From | Event | To | Side-effect |
|---|---|---|---|
| 不存在 | 邀请人创建记录 | rewardStatus=0 | INSERT，`SnowflakeManager.nextValue()` 分配 ID |
| 不存在 | 被邀请人创建记录 | rewardStatus=0 | INSERT，`rewardTargetType=1` |
| rewardStatus=0 | 调用 `orderFeign.addActivityOrder` 成功 | rewardStatus=1 | `updateRewardStatusByIds` 批量更新，写 `sendDate=now` |

无"已发放 → 撤回"逆向边——撤销奖励是另一条流水，本模块当前不支持。

---

## 四、核心业务流

### 流 1：奖励记录创建（首次邀请关系建立时）

```
MQ Tag_User_Invite_Activity 到达
  → RocketMqListener#processUserInviteActivity
  → ClientInviteRewardRecordBll#giveUserInviteAward
    → 1. 校验指纹有效性（按 progressCode 分支）
    → 2. judgeUserInviteEffective —— 是否存在 (邀请人, 被邀请人) 邀请关系
    → 3. infoActivateById —— 活动是否激活
    → 4. 查 (活动, 邀请人, 被邀请人) 是否已有记录
        ├─ 没有 → createRewardRecord：拉活动下"邀请人侧/被邀请人侧"所有 progressReward
        │         → 为每条 progressReward 各 INSERT 一条 rewardStatus=0 的记录（邀请人 + 被邀请人各一条）
        └─ 已有 → 拿出来准备状态推进
```

### 流 2：奖励发放（按 progressCode 命中时把记录推到已发放）

```
拿到候选记录列表（rewardStatus=0 ∧ progressCode 匹配 ∧ tenantId 匹配）
  → buildSendRewardRecordDetail：组装成 InviteUserRewardDetailDto 列表
  → orderFeign.addActivityOrder(...)   ◀── 真正发奖
  → updateRewardStatusByIds(...)       ◀── 流转到 rewardStatus=1，写 sendDate
```

### 流 3：奖励汇总查询（客户端）

```
ClientInviteRewardRecordController#clientGetRewardSummary
  → 取当前用户（子账号 → 主账号）
  → listByRewardUserIdAndRewardTargetTypeAndRewardStatus(userId, targetType=0, rewardStatus=1)
  → 按 packageId / commodityTypeId 分组聚合
  → 返回 RewardSummaryVo[]: 总邀请人数 + 各套餐时长合计 + 各增量包数量合计
```

---

## 五、业务不变量 (Invariants)

1. **租户隔离**：`rewardTenantId` 是这笔奖励发放的有效租户域。`giveUserInviteAward` 内通过 `UserFeign.getUserTenantId(userId)` 把邀请人和被邀请人各自的 tenantId 写到自己的那条记录里——它们**可以不同**。
2. **邀请对偶**：一次有效邀请关系一定会产生两组记录——邀请人侧（`rewardTargetType=0`）+ 被邀请人侧（`rewardTargetType=1`），各自挂自己一侧的所有 progressReward。
3. **幂等 — 同进度只发一次**：发放阶段筛选条件 `progressCode == rewardRuleCode ∧ rewardStatus == 0`，命中后立即推到 `rewardStatus=1`，相同 (用户, 活动, 进度) 不会重复发放。
4. **同人不奖**：`inviteUserId == passiveUserId` 直接 short-circuit 返回 true，不产生记录。
5. **指纹必校**：MQ 消费分布式锁 key 是 `fingerprintInfo:{fingerprint}`——同一指纹同时只能处理一笔。
6. **数据来源唯一**：`replay-reward` 不主动消费 MQ —— **MQ 监听代码物理位置在 `replay-api` 的 `RocketMqListener`**，它注入 `ClientInviteRewardRecordBll` 后调用本模块业务方法（见架构文档）。
7. **不暴露 Feign**：当前 `replay-reward` 没有声明任何 Feign 接口；其他模块不能远程调用本模块业务——只有 `replay-api` 在同一 JVM 内通过 Bll 直接注入使用（见 `RocketMqListener` 和 `ClientInviteRewardRecordController`）。

---

## 六、和邻居模块的语义边界

| 邻居 | 谁负责 | 本模块的姿势 |
|---|---|---|
| `replay-activity` | 维护活动定义、进度定义、进度奖励规则 | 通过 `ActivityFeign` / `ClientInviteProgressFeign` / `ClientInviteProgressRewardFeign` 拉取配置 |
| `replay-agent` | 邀请关系、邀请码、邀请人-被邀请人绑定 | 通过 `UserInviteFeign` / `InviteUrlCodeFeign` 校验邀请关系是否有效、查邀请人数 |
| `replay-order` | 真正发权益（开套餐、加增量包） | 通过 `OrderFeign.addActivityOrder` 把"奖励 → 订单"，并发完后回写 rewardStatus=1 |
| `replay-power` | 用户身份、租户、子账号关系、设备指纹 | `UserFeign` 取用户/租户、`UserDeviceFingerprintFeign` 校验+写入指纹 |
| `replay-system` | 字典 | `DictDataFeign` 翻译 progressCode → 中文 label（仅后台列表展示用）|
| `replay-common` | MQ 工具、分布式锁、雪花 ID | `SnowflakeManager.nextValue()` 分配 ID、`DistributedLock` 按指纹串行化 |

---

## 七、历史遗留 / 风险点（不要在新代码里学）

- **旧 controller 包名拼错**：`com.jiuyu.replay.reward.contraller`（少了一个 `o`）。新代码不要照抄。
- **DI 风格不统一**：现有 Bll/Rse/Service 大量使用 `@Resource` 字段注入。**新代码必须用构造器注入**（见 `CLAUDE.md §5`）。
- **历史"通用 CRUD"接口**（`queryPage / info / save / update / deleteById`）实际未挂任何 Controller —— `ClientInviteRewardRecordLogicImpl` 是 `//@Service` 注释掉的死代码。这些 Rse 方法属于规划失败的遗留，不要在新代码里调用。
- **listByBack 的 endTime 拼接 bug**：`wrapper.le("send_date", ...getStartTime() + " 23:59:59")` —— 本应取 `getEndTime()`。读到这块代码的人请按需修复（独立任务）。
- **GROUP BY 在 MyBatis-Plus 分页里组合**：`pageUserRewardListByRewardUserIdAndRewardTargetTypeGroupByProgressId` 走自定义 XML，`GROUP_CONCAT` 拼 `progressRewardIdStr`，列表里再 `split("_")` 反解 —— 这是受限于"列表展示需要每个 user×progress 聚合一行"的现实妥协。
