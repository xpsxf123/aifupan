# CRM Controller/Service/Mapper 重构设计

**目标范围：**仅 CRM 集成（Spec A-D）相关代码，不做全仓分层改造。

## 1. 目标

将当前 CRM 集成实现统一收敛为三层结构：

- Controller：入口参数校验 + 鉴权注解/拦截器复用 + 入口日志
- Service：业务逻辑、聚合、字段映射、幂等、出站调用、关键日志
- Mapper：数据库读写（MyBatis/MyBatis-Plus）

同时对齐 `VideoTextNotesService` 的注释风格：

- 类/方法使用 Javadoc，明确职责与 `@param/@return`
- 主题实体类、入参、出参的字段补齐属性注释（以 `@Schema(description=...)` 为主）
- 日志统一前缀、关键字段齐全、手机号脱敏

## 2. 重构对象

### 2.1 replay-api（CRM 入站 + 订单事件监听）

**保留：**
- `CrmIntegrationController`（Controller 层）
- `CrmOrderChangedEventListener`（事件监听器，归为 Service 调用方）

**迁移/替换：**
- 将 `CrmIntegrationLogic/CrmIntegrationLogicImpl` 收敛为 `CrmIntegrationService/CrmIntegrationServiceImpl`
- 将 `CrmOrderEventPushBll` 收敛为 `CrmOrderEventPushService`

### 2.2 replay-power（CRM 落库）

**保留：**
- `CrmAiProfileService/Impl`、`CrmStageEventService/Impl`（作为 Service 层）

**迁移/替换：**
- 将 CRM 专用的 `Bll/Producer`（如 `CrmAiProfileBll/Producer`、`CrmStageEventBll/Producer`）收敛进 `Service`（新增明确业务方法）
- 将 CRM 专用 `Dao` 命名收敛为 `Mapper`（仅限 CRM 新增表的 mapper）
- 将 `tb_user_details` 的 CRM 写回逻辑从 `Producer/Bll` 收敛到 `UserDetailsService`（新增 CRM 专用写入方法）

### 2.3 replay-order（订单完成事件发布）

**迁移/替换：**
- 将 `CrmOrderEventBll` 收敛为 `CrmOrderEventService`
- `OrderBll.successOrder()` 仍作为唯一触发点发布 Spring 事件

## 3. API 与数据流

### 3.1 CRM 入站（智能体 → CRM）

- Controller 接收请求，记录入口日志，转发到 Service
- Service：
  - 归一化 phone
  - 查用户、查销售、查订单
  - 写 `tb_user_details`、`tb_crm_ai_profile`、`tb_crm_stage_event`
  - 阶段事件默认只入库；命中白名单才受控刷新 `tb_user_business` 快照

### 3.2 CRM 出站（CRM → 智能体）

- `OrderBll.successOrder()` 发布 `CrmOrderChangedEvent`
- `CrmOrderChangedEventListener` 聚合 `user/sales/order`，调用 `CrmOrderEventPushService` 出站
- `CrmOrderEventPushService` 使用 `HttpUtils` 带 `X-Api-Key` 调用智能体侧 `POST /api/internal/agent/order/event`

## 4. 日志规范

- 统一前缀：
  - CRM 入站：`[CRM-INTEGRATION]`
  - CRM 出站：`[CRM-ORDER-EVENT]`
- phone 统一使用脱敏输出：`CommonUtils.maskPhone(phone)`
- 每条关键日志至少包含：
  - 入站：`phone` + (`profileId`/`eventId`/`stageCode` 之一)
  - 出站：`eventId/eventType/userId/orderId`（必要时补 `salesId`）

## 5. 兼容与风险控制

- 不改变现有接口 URL、请求/响应字段命名（只调整内部实现分层）
- 不引入 Outbox 表，不引入异步队列（按原设计仅 Spring 事件 + 日志）
- 由于仓库当前 Maven 依赖存在 403/历史编译阻塞，本次以 IDE 诊断为主；待环境恢复后补跑回归

