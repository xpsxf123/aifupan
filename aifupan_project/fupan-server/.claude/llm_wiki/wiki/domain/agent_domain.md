<!-- module: agent -->
<!-- area: domain -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-agent/, replay-api/.../controller/agent/ -->

# Agent Domain — 业务概念与词汇表

> `replay-agent` 模块核心业务概念定义。本模块承载**代理商体系（渠道 / 代理商 / 推广渠道 / 销售）**与**邀请活动体系（活动 / 进度 / 奖励 / 链接 code）**两条主线，外加**佣金分润**作为二者的交汇点。
>
> Agent 在 Explorer / Propose 阶段必须使用此术语表，避免领域漂移。

---

## 核心概念

| 概念 | 定义 | 关联概念 | 状态机 |
|------|------|----------|--------|
| **Channel (来源渠道)** | 用户来源的树形分类（父子结构），如"抖音 > 短视频 > 直播间挂车"。一个渠道最多绑定一个代理商。 | Agent | — |
| **Agent (代理商)** | 与平台签约的渠道方，分**普通代理商**与**渠道代理商**两类；持有新签佣金比例与续费佣金比例。普通代理商会同步创建后台账号。 | Channel, AgentPromotion, AgentSale, AgentCommission, InviteUrlCode | `agentStatus`: 0未启用 / 1启用中；`employeeStatus`: 0离职 / 1在职 |
| **AgentPromotion (代理商推广渠道)** | 代理商自定义的二级推广位（例如同一代理商下"小红书号 A / 抖音号 B"），可独立配海报、按钮、佣金比例。 | Agent, AgentSalePromotionChannel | `promotionStatus`: 0未启用 / 1启用中 |
| **AgentSale (代理商销售)** | 代理商组织内的销售员；可绑定多个推广渠道。 | Agent, AgentSalePromotionChannel, InviteUrlCode | `saleStatus`: 0未启用 / 1启用中 |
| **AgentPlatformSale (代理商平台销售)** | 代理商关联到平台侧 `tb_sales` 销售人员的"白名单"，用于客户分配轮询。 | Agent, [[Sales]] | — |
| **AgentSalePromotionChannel (销售-推广渠道关联)** | 代理商销售与推广渠道的多对多关系；每条关系自带 `saleUrlCode`，用于销售对外发布。 | AgentSale, AgentPromotion | — |
| **AgentCommission (佣金记录)** | 一笔订单分佣的结算记录，记录订单金额、佣金比例、佣金金额、分佣类型、分佣模式。 | Agent, AgentPromotion, AgentSale, [[Order]] | `commissionType`: 0新签 / 1续费；`commissionMode`: 0代理商 / 1推广渠道 / 2用户 |
| **InviteUrlCode (邀请链接 code)** | 10 位字母数字随机码，是代理商体系与活动体系的统一入口。`codeType` 决定它属于哪一类身份。 | Agent, AgentPromotion, AgentSale, User, ClientInviteActivity | `codeType`: 0代理商 / 1推广渠道 / 2用户 / 3代理商销售 |
| **ClientInviteActivity (客户端邀请活动)** | 平台运营定义的一期邀请活动，关联代理商，含起止时间与开关。 | Agent, ClientInviteProgress | `activityStatus`: 0未启用 / 1启用中 |
| **ClientInviteProgress (邀请进度)** | 活动下的某档进度门槛（"邀请 3 人 → 送 7 天会员"），按身份区分被邀请人/邀请人。 | ClientInviteActivity, ClientInviteProgressReward | `inviteProgressStatus`: 0停用 / 1启用中；`inviteProgressType`: 0被邀请人 / 1邀请人 |
| **ClientInviteProgressReward (进度奖励)** | 某个进度档对应的奖励内容（版本套餐或增量包），含商品类型、数量、有效期。 | ClientInviteProgress, [[CommodityType]] | `rewardType`: 0版本 / 1增量包；`validityUnit`: 0小时 / 1天 / 2月 / 3季度 / 4半年 / 5年 |
| **ClientInviteRewardRecord (邀请奖励记录)** | 一次邀请关系链上的奖励授予记录（邀请人 + 被邀请人 + 进度档）。租户隔离。 | ClientInviteProgress, [[User]] | — |
| **ClientInviteRewardRecordDetail (奖励明细)** | 单条奖励记录派生出的具体奖励落地（一条记录可能对应多条明细）。租户隔离。 | ClientInviteRewardRecord, ClientInviteProgressReward | — |

---

## 状态机定义

### Agent.agentStatus（代理商启用状态）
| 值 | 状态 | 说明 |
|----|------|------|
| 0 | DISABLED | 未启用 — 不参与分佣、不分配销售；启用前必须 `employeeStatus=1` |
| 1 | ACTIVE | 启用中 — 可分佣、可分配销售、可接收新邀请 |

切换规则（`AgentBll.updateAgentStatus`）：
- 0 → 1 前置条件：`employeeStatus == 1`（必须先把员工状态改为"在职"）
- 状态切换会**联动调整后台账号**：`agentStatus=1` → 用户 `status=0`（启用）；反之 `status=1`（禁用），通过 `UserFeign.updateAdminUserStatusByPhone`。

### Agent.agentType（代理商类型）
| 值 | 类型 | 说明 |
|----|------|------|
| 0 | NORMAL | 普通代理商 — 创建/更新时**同步创建/同步更新**后台账号（`UserFeign.saveOrUpdateAgentUser`），并写入角色（取自 `system_kv.agent_user_role_id`） |
| 1 | CHANNEL | 渠道代理商 — 仅作渠道方台账，不开通后台账号 |

不允许跨类型修改（`AgentBll.update` 校验 `agentType` 不可变）。

### Agent.employeeStatus（员工状态）
| 值 | 状态 | 联动 |
|----|------|------|
| 0 | RESIGNED | 离职 — 若为普通代理商，联动把代理商下所有销售置为离职（`SalesFeign.updateEmployeeStatusByAgentId`） |
| 1 | ACTIVE | 在职 |

### AgentCommission.commissionType（分佣类型）
| 值 | 类型 | 说明 |
|----|------|------|
| 0 | NEW_SIGN | 新签佣金 — 用 `Agent.commissionRate` 或 `AgentPromotion.commissionRate` |
| 1 | RENEWAL | 续费佣金 — 用 `Agent.renewalCommissionRate` 或 `AgentPromotion.renewalCommissionRate`；仅普通代理商生效，且要满足"断约窗口 + 首次续费"规则（见下方业务规则） |

### AgentCommission.commissionMode（分佣模式 / 镜像 `InviteUrlCode.codeType`）
| 值 | 模式 | 说明 |
|----|------|------|
| 0 | AGENT | 代理商分佣（默认；当前 `commissionAllocation` 强制改写为 0） |
| 1 | PROMOTION | 推广渠道分佣 |
| 2 | USER | 用户分佣（当前 `commissionAllocation` 抛 "当前方法不适用"） |
| 3 | AGENT_SALE | 代理商销售（走推广渠道相同分支） |

### InviteUrlCode.codeType（邀请码类型）
| 值 | 类型 | 主要字段 |
|----|------|----------|
| 0 | AGENT_LINK | 代理商链接 — 携带 `agentId` |
| 1 | PROMOTION_LINK | 推广渠道链接 — 携带 `agentId + promotionId` |
| 2 | USER_LINK | 用户邀请链接 — 携带 `agentId + userId(+subUserId) + activityId + tenantId` |
| 3 | AGENT_SALE_LINK | 代理商销售链接 — 携带 `agentId + promotionId + agentSaleId` |

### ClientInviteProgress.inviteProgressType（邀请进度身份）
| 值 | 身份 | 说明 |
|----|------|------|
| 0 | INVITEE | 被邀请人侧门槛（"被邀请成功后送你 X"） |
| 1 | INVITER | 邀请人侧门槛（"邀请 N 人后送你 X"） |

### ClientInviteProgressReward.validityUnit（有效期单位）
| 值 | 单位 |
|----|------|
| 0 | 小时 |
| 1 | 天 |
| 2 | 月 |
| 3 | 季度 |
| 4 | 半年 |
| 5 | 年 |

---

## 关键业务规则

### 1. 邀请码生成
- 字符集：`[A-Za-z0-9]`，长度 10
- 唯一性：服务端循环重试直到 `tb_invite_url_code.url_code` 不冲突（`InviteUrlCodeProducerImpl#createUrlCode`）
- 用户类型邀请码生成路径加分布式锁：`LockKeyPrefix.USER.getLockKey("inviteUrlCode:" + userId)`（Redisson）

### 2. 代理商创建/修改的强校验
- `commissionRate` 与 `renewalCommissionRate` 必须在 `[0, 1]` 区间内
- `channelId` 不可重复（"当前渠道已关联代理商"）
- 普通代理商：
  - `contactPhone` 不可重复（自身代理商表内 + `tb_user` 后台账号表内 `userType=1`）
  - 自动生成后台账号（用户名 = 雪花 ID，初始密码随机 6 位）
  - 自动绑定 `agent_user_role_id` 角色
- 不允许跨 `agentType` 修改
- 代理商名称随 `Channel.channelName` 同步刷新（`agentName = channelInfo.channelName`）

### 3. 佣金分配规则（`AgentCommissionProducerImpl#commissionAllocation`）
入参：`{inviteUrlCode, orderId, orderTotalMoney, userId, commissionType, remarks}`

流程：
1. 通过 `inviteUrlCode` 查 `InviteUrlCode`，无 → 不分佣。
2. 当前实现**强制把 `codeType` 改写为 0**（"现在都是代理商分佣"），即一律走代理商分支：
   - 代理商不存在 / `agentStatus=0` → 不分佣
   - `commissionType=0`（新签）→ 用 `agent.commissionRate`
   - `commissionType=1`（续费）→ 用 `agent.renewalCommissionRate`，且必须满足：
     - 代理商 `agentType=0`（普通）且 `agentStatus=1`（启用）
     - 同一 `agentId + userId + commissionType=1` 在 "断约窗口" 内不存在续费记录
       - 窗口 = `system_kv.new_renewal_interval`（默认 90）+ `system_kv.break_agreement_num`（默认 180）天
3. 推广渠道分支（实际未启用，留作扩展）：取 `agentPromotion.commissionRate / renewalCommissionRate`，要求 `promotionStatus=1`。
4. 用户分支（`codeType=2`）：直接抛 `RRException("当前方法不适用")`。
5. 计算 `commissionMoney = orderTotalMoney * min(commission, 1)`；上限不超过订单总金额。
6. `commissionMoney <= 0` → 不分佣。
7. 落库 `tb_agent_commission`。

### 4. 用户邀请链接获取（`InviteUrlCodeBll#getUserInviteUrlCodeInfo`）
1. 取 `UserFeign.getLocalUser` 获取当前用户；子账号（`userType=2`）用 `parentId` 作为 `userId`。
2. 取 `ActivityFeign.infoActivateById(null)` 获取**当前生效活动**，无 → "活动已结束"。
3. Redisson 锁 `LockKeyPrefix.USER.getLockKey("inviteUrlCode:" + userId)` 串行化。
4. 按 `(userId, tenantId, activityId, codeType=2)` 查已有码；有 → 返回。无 → 生成新码并落库。

### 5. 代理商平台销售轮询（`AgentPlatformSaleProducerImpl#getPollingSaleId`）
- Key：`replay:agent:sale:index:{agentId}`（`RedisAgentKeyCache.REDIS_AGENT_SALE_INDEX_KEY`）
- 自增策略：`opsForValue().increment(key, 1)`；首次设置 30 天过期；超过 10000 重置为 1
- 销售选择优先级：先取代理商销售（`salesType=AGENT`），无则降级到平台销售。
- 返回 `saleList.get(index % saleList.size())`。

### 6. 租户隔离
- 用户身份链路的所有表均带 `tenant_id`：
  - `tb_invite_url_code.tenant_id`
  - `tb_client_invite_reward_record.tenant_id`
  - `tb_client_invite_reward_record_detail.tenant_id`
- 列表查询及生成新码时必须以 `tenantId = currentUser.activeTenantId` 过滤/写入。
- 代理商体系本身（`tb_agent`、`tb_agent_sale` 等）不存 `tenantId`，属于平台级配置数据。

### 7. 邀请奖励发放
- `replay-reward` 模块的 `ClientInviteRewardRecordBll` 通过 `InviteUrlCodeFeign.inviteCodeAndPromotionName` 反查邀请码归属，决定一条奖励记录应该归到哪条邀请关系链。
- 跨模块统一走 `replay-generic` 的 Feign 接口，**禁止跨模块直连 Dao**。

---

## 核心工作流

### 代理商签约 → 上线分佣
```
新增渠道(Channel) → 创建代理商(Agent, agentType=0/1) →
  ├─ 普通代理商：联动 UserFeign 创建后台账号 + 绑定角色
  └─ 渠道代理商：仅落库
→ 生成代理商邀请码(InviteUrlCode, codeType=0)
→ 配置推广渠道(AgentPromotion) → 绑定代理商销售(AgentSale + AgentSalePromotionChannel)
→ 代理商状态置为 1(启用) → 可参与分佣
```

### 用户邀请奖励触发
```
用户 A 在生效活动期请求邀请码(getUserInviteUrlCodeInfo) →
  ├─ Redisson 锁 +
  └─ codeType=2 邀请码生成/复用
→ 用户 B 通过邀请码注册/支付 →
→ 触发 ClientInviteRewardRecord(in replay-reward via Feign 回查) →
→ 派生 ClientInviteRewardRecordDetail 发放奖励
```

### 订单分佣
```
订单支付成功(replay-order) → 携带 inviteUrlCode →
→ AgentCommissionProducer.commissionAllocation →
  ├─ 校验代理商 + 续费窗口
  └─ 计算佣金金额(≤订单金额)
→ 落库 tb_agent_commission
```

---

## 角色与权限

| 角色 | 权限范围 |
|------|----------|
| 平台运营 | 渠道与代理商台账、活动定义、佣金审计 |
| 代理商（普通） | 通过后台账号登录，查看自己代理商下的销售、推广渠道、佣金 |
| 代理商销售 | 通过销售邀请码 (`codeType=3`) 拉新 |
| 客户端用户 | 通过个人邀请码 (`codeType=2`) 邀请新用户参与活动 |

---

## 跨模块引用

`replay-agent` 仅通过 `replay-generic` 的 Feign 接口被外部消费；自身依赖 `replay-power`、`replay-activity`、`replay-common` 的 Feign：

**被外部调用（agent 对外暴露）：** `ChannelFeign`、`InviteUrlCodeFeign`（详见 `agent_architecture.md`）。

**主动调用其他模块：** `UserFeign` / `SalesFeign`（power）、`ActivityFeign`（activity）、`FileFeign`（common）。
