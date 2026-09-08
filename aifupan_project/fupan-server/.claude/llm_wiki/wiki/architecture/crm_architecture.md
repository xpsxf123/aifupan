# CRM Architecture — 架构决策与设计

> CRM 子系统的架构基线。CRM 不是一个独立模块，而是以 `replay-power` 为核心领域、由 `replay-api` 暴露接口、依赖 `replay-order` 提供订单信号的客户经营与销售跟进子系统。

---

## 一、模块定位

CRM 承载以下核心能力：

- **客户管理**: 客户主体（tb_user）、客户详情（tb_user_details）
- **销售管理**: 销售人员资料、轮询开关、员工状态
- **跟进管理**: 跟进明细（tb_user_remark）、跟进快照（tb_user_business）
- **统计看板**: 销售/团队维度统计（待跟进、到期、成交、意向分布）
- **智能体集成**: 对内 API Key 鉴权、AI 画像存储、阶段事件写入、订单事件出站

---

## 二、模块分层

```
┌──────────────────────────────────────────────────────┐
│  replay-api (接入层)                                   │
│  Controller: CrmIntegrationController, UserController │
│  Listener: CrmOrderChangedEventListener               │
│  Interceptor: APIKeyInterceptor                       │
├──────────────────────────────────────────────────────┤
│  replay-power (CRM 核心领域)                           │
│  Producer → Service/Dao → MySQL (tb_user, tb_user_*)  │
│  + 新增: CrmAiProfile, CrmStageEvent                  │
├──────────────────────────────────────────────────────┤
│  replay-order (订单支撑)                               │
│  Event: CrmOrderChangedEvent → Listener               │
│  Service: 订单生命周期查询                             │
├──────────────────────────────────────────────────────┤
│  replay-common / replay-generic (公共层)               │
│  APIKey, APIKeyInterceptor, OauthClientParameter      │
└──────────────────────────────────────────────────────┘
```

---

## 三、当前 CRM 核心领域模型

| 域对象 | 主表 | 关键字段 | 模块 |
|--------|------|----------|------|
| 客户主体 | `tb_user` | id, phone, status, tenantId | replay-power |
| 客户详情 | `tb_user_details` | userId, saleId, tradeId, userAmbition, leadType | replay-power |
| 销售人员 | `tb_sales` | id, name, status, pollingEnabled | replay-power |
| 跟进明细 | `tb_user_remark` | userId, salesId, content, remarkType, status, nextFolTime | replay-power |
| 跟进快照 | `tb_user_business` | userId, accordingStatus, nextFolTime, latestRemark | replay-power |
| 订单 | `tb_order` | userId, productId, status, payStatus, isTrial, expireTime | replay-order |
| AI 画像 | `tb_crm_ai_profile` | userId, profileId, profileJson, summary | replay-power |
| 阶段事件 | `tb_crm_stage_event` | userId, eventId, stageCode, confidence, factsJson | replay-power |

---

## 四、ADR 列表

### ADR-CRM-001: CRM 不独立建模块

**决策**: CRM 能力归属 `replay-power`，不另建 `replay-crm` 模块。
**原因**: 客户/销售/跟进数据已在 power 模块，避免跨模块事务和循环依赖。

### ADR-CRM-002: 明细-快照双层模型

**决策**: 跟进记录采用明细表 + 快照表双层存储。
**原因**: 列表/看板查询直接读快照，避免每次从明细取最新一条的性能问题。

### ADR-CRM-003: API Key 鉴权复用

**决策**: 集成接口统一使用 `@APIKey` + `APIKeyInterceptor`，不复用 Session/OAuth 鉴权。
**原因**: 智能体是系统级调用方，不需要用户登录态。

### ADR-CRM-004: phone → userId 换算在接口层

**决策**: 外部接口以 `phone` 作为客户匹配键，落库后统一换算为 `user_id`，新增扩展表只保留 `user_id`。
**原因**: 避免在多张表中重复维护手机号，防止多点更新风险。

### ADR-CRM-005: 订单事件出站采用 Spring Event

**决策**: 订单变更通过 `CrmOrderChangedEvent` Spring 事件异步推送至智能体。
**原因**: 解耦订单模块与集成逻辑，订单模块只发布事件不关心消费者。

### ADR-CRM-006: 新增表不冗余手机号

**决策**: `tb_crm_ai_profile` 和 `tb_crm_stage_event` 不保留 `phone` 字段，仅通过 `user_id` 关联。
**原因**: 遵循单一数据源原则，手机号只在 `tb_user` 维护。

---

## 五、关键技术约束

| 约束 | 说明 |
|------|------|
| 事务边界 | Producer 层 `@Transactional`，Bll 层做编排 |
| 权限控制 | 数据可见性以归属销售 + 管理员权限控制 |
| 快照同步 | 新增/修改/删除跟进记录必须触发快照刷新 |
| N+1 禁止 | 批量查询必须使用批量聚合查询，禁止循环查询 |
| 软删除 | 扩展表使用 `is_deleted` 逻辑删除 |
