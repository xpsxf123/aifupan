# CRM Controller/Service/Mapper Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 CRM 集成（Spec A-D）的内部实现统一改造为 Controller / Service / Mapper 三层结构，对齐 `VideoTextNotesService` 的注释与日志习惯；本次不新增/不完善测试用例，仅保证 IDE 诊断为 0。

**Architecture:** `replay-api` 仅保留 Controller、Listener；业务逻辑全部下沉到 `Service`。`replay-power` 侧 CRM 新表统一使用 `Mapper + Service`，把 CRM 专用 `Bll/Producer/Dao` 收敛/移除。`replay-order` 侧把 CRM 订单事件发布器收敛为 `Service` 并由 `OrderBll.successOrder()` 触发事件发布。

**Tech Stack:** Java 17, Spring Boot 3.3, Spring MVC, Spring Events, Lombok, MyBatis-Plus, Swagger `@Schema`

---

### Task 1: replay-api 收敛为 Controller → Service

**Files:**
- Create: `replay-api/src/main/java/com/jiuyu/replay/api/service/crm/CrmIntegrationService.java`
- Create: `replay-api/src/main/java/com/jiuyu/replay/api/service/crm/impl/CrmIntegrationServiceImpl.java`
- Modify: `replay-api/src/main/java/com/jiuyu/replay/api/controller/openapi/CrmIntegrationController.java`
- Delete: `replay-api/src/main/java/com/jiuyu/replay/api/logic/power/CrmIntegrationLogic.java`
- Delete: `replay-api/src/main/java/com/jiuyu/replay/api/logic/power/impl/CrmIntegrationLogicImpl.java`
- Modify: `replay-api/src/main/java/com/jiuyu/replay/api/listener/CrmOrderChangedEventListener.java`（改为依赖 Service）

- [ ] **Step 1: 新建 Service 接口（方法签名保持不变）**

```java
public interface CrmIntegrationService {

    R<CrmBatchAggregateQueryVo> batchAggregateQuery(CrmBatchAggregateQueryBo bo);

    R<CrmOrderQueryByPhoneVo> queryOrderByPhone(String phone);

    R<CrmProfileSyncVo> profileSync(CrmProfileSyncBo bo);

    R<CrmAiProfileUpsertVo> upsertAiProfile(CrmAiProfileUpsertBo bo);

    R<CrmStageEventVo> saveStageEvent(CrmStageEventBo bo);
}
```

- [ ] **Step 2: 新建 Service 实现，并把原 LogicImpl 全量迁移进去**

要求：
- 类/方法 Javadoc 对齐 `VideoTextNotesService` 风格
- 入口与成功日志保留现有前缀与手机号脱敏
- 主题实体/入参/出参字段 `@Schema(description=...)` 补齐

- [ ] **Step 3: Controller 注入从 Logic 改为 Service**

```java
private final CrmIntegrationService crmIntegrationService;
```

- [ ] **Step 4: Listener 注入从 Bll 改为 Service（出站推送 Service 由 Task3 提供）**

- [ ] **Step 5: 删除旧 Logic 接口与实现**

- [ ] **Step 6: IDE 诊断检查**

Run: `GetDiagnostics` for:
- `CrmIntegrationController.java`
- `CrmIntegrationService.java`
- `CrmIntegrationServiceImpl.java`
- `CrmOrderChangedEventListener.java`

Expected: 无新增诊断错误

---

### Task 2: replay-power 收敛为 Service → Mapper（CRM 新表）

**Files:**
- Create: `replay-power/src/main/java/com/jiuyu/replay/power/mapper/CrmAiProfileMapper.java`
- Create: `replay-power/src/main/java/com/jiuyu/replay/power/mapper/CrmStageEventMapper.java`
- Modify: `replay-power/src/main/java/com/jiuyu/replay/power/repository/service/impl/CrmAiProfileServiceImpl.java`
- Modify: `replay-power/src/main/java/com/jiuyu/replay/power/repository/service/impl/CrmStageEventServiceImpl.java`
- Delete: `replay-power/src/main/java/com/jiuyu/replay/power/repository/dao/CrmAiProfileDao.java`
- Delete: `replay-power/src/main/java/com/jiuyu/replay/power/repository/dao/CrmStageEventDao.java`
- Delete: `replay-power/src/main/java/com/jiuyu/replay/power/bll/CrmAiProfileBll.java`
- Delete: `replay-power/src/main/java/com/jiuyu/replay/power/producer/CrmAiProfileProducer.java`
- Delete: `replay-power/src/main/java/com/jiuyu/replay/power/producer/impl/CrmAiProfileProducerImpl.java`
- Delete: `replay-power/src/main/java/com/jiuyu/replay/power/bll/CrmStageEventBll.java`
- Delete: `replay-power/src/main/java/com/jiuyu/replay/power/producer/CrmStageEventProducer.java`
- Delete: `replay-power/src/main/java/com/jiuyu/replay/power/producer/impl/CrmStageEventProducerImpl.java`
- Delete: `replay-power/src/main/java/com/jiuyu/replay/power/bo/crm/CrmBusinessSnapshotUpdateBo.java`（迁移到 Service 内部类或使用现有 entity）

- [ ] **Step 1: 新建 Mapper（命名对齐“Mapper 层”）**

```java
@Mapper
public interface CrmAiProfileMapper extends BaseMapper<CrmAiProfileEntity> {}
```

```java
@Mapper
public interface CrmStageEventMapper extends BaseMapper<CrmStageEventEntity> {}
```

- [ ] **Step 2: 在 ServiceImpl 内部增加业务方法（替代 Producer/Bll）**

示例：

```java
public CrmAiProfileInfoVo saveOrUpdateByProfileId(CrmAiProfileBo bo) { ... }
```

```java
public CrmStageEventInfoVo saveOrUpdateByEventId(CrmStageEventSaveBo bo) { ... }
```

- [ ] **Step 3: 把“受控刷新 tb_user_business 快照”逻辑放到 `CrmStageEventServiceImpl`**

要求：
- 仅白名单阶段刷新（维持当前行为）
- 方法加 Javadoc，写清楚“默认不改快照”

- [ ] **Step 4: 修改 replay-api 的 `CrmIntegrationServiceImpl` 引用点**

从 `CrmAiProfileBll/CrmStageEventBll` 改为注入对应 `Service`：
- `CrmAiProfileService`
- `CrmStageEventService`

- [ ] **Step 5: 删除 CRM 专用 Bll/Producer/Dao**

- [ ] **Step 6: IDE 诊断检查**

Run: `GetDiagnostics` for:
- `CrmAiProfileServiceImpl.java`
- `CrmStageEventServiceImpl.java`
- `CrmAiProfileMapper.java`
- `CrmStageEventMapper.java`
- `CrmIntegrationServiceImpl.java`

Expected: 无新增诊断错误

---

### Task 3: replay-power 的 tb_user_details CRM 写回收敛到 UserDetailsService

**Files:**
- Modify: `replay-power/src/main/java/com/jiuyu/replay/power/repository/service/UserDetailsService.java`
- Modify: `replay-power/src/main/java/com/jiuyu/replay/power/repository/service/impl/UserDetailsServiceImpl.java`
- Revert/Modify: `replay-power/src/main/java/com/jiuyu/replay/power/bll/UserDetailsBll.java`
- Revert/Modify: `replay-power/src/main/java/com/jiuyu/replay/power/producer/UserDetailsProducer.java`
- Revert/Modify: `replay-power/src/main/java/com/jiuyu/replay/power/producer/impl/UserDetailsProducerImpl.java`

- [ ] **Step 1: 在 UserDetailsService 增加 CRM 专用写入方法**

```java
CrmUserDetailsSyncVo saveCrmProfileFields(CrmUserDetailsSyncBo bo);
```

- [ ] **Step 2: 在 UserDetailsServiceImpl 内实现最小 upsert（复用原 ProducerImpl 的实现）**

- [ ] **Step 3: replay-api 的 `CrmIntegrationServiceImpl.profileSync()` 改为直接调用 `UserDetailsService`**

- [ ] **Step 4: 清理 `UserDetailsBll/Producer` 中本次 CRM 专用改动**

目标：
- CRM 路径不再依赖 `Bll/Producer`
- `UserDetailsBll/Producer` 回归其原有职责（不强制删除，但不再承载 CRM 专用入口）

- [ ] **Step 5: IDE 诊断检查**

Run: `GetDiagnostics` for:
- `UserDetailsService.java`
- `UserDetailsServiceImpl.java`
- `CrmIntegrationServiceImpl.java`

Expected: 无新增诊断错误

---

### Task 4: Spec-D 订单事件发布/出站收敛为 Service

**Files:**
- Create: `replay-order/src/main/java/com/jiuyu/replay/order/service/CrmOrderEventService.java`
- Create: `replay-order/src/main/java/com/jiuyu/replay/order/service/impl/CrmOrderEventServiceImpl.java`
- Delete: `replay-order/src/main/java/com/jiuyu/replay/order/bll/CrmOrderEventBll.java`
- Modify: `replay-order/src/main/java/com/jiuyu/replay/order/bll/OrderBll.java`
- Create: `replay-api/src/main/java/com/jiuyu/replay/api/service/crm/CrmOrderEventPushService.java`
- Create: `replay-api/src/main/java/com/jiuyu/replay/api/service/crm/impl/CrmOrderEventPushServiceImpl.java`
- Delete: `replay-api/src/main/java/com/jiuyu/replay/api/bll/crm/CrmOrderEventPushBll.java`
- Modify: `replay-api/src/main/java/com/jiuyu/replay/api/listener/CrmOrderChangedEventListener.java`

- [ ] **Step 1: 把订单事件发布器改为 Service**

`CrmOrderEventService.publishOrderChangedEvent(...)` 行为保持不变，仅换层与注释/日志。

- [ ] **Step 2: 把 CRM 出站推送改为 Service**

`CrmOrderEventPushService.pushOrderEvent(...)` 内部继续使用 `HttpUtils`，保留 `X-Api-Key` Header。

- [ ] **Step 3: Listener 依赖注入替换为 PushService**

- [ ] **Step 4: IDE 诊断检查**

Run: `GetDiagnostics` for:
- `OrderBll.java`
- `CrmOrderChangedEventListener.java`
- `CrmOrderEventPushServiceImpl.java`

Expected: 无新增诊断错误

---

### Task 5: 注释与日志收口（按 VideoTextNotesService 风格）

**Files:**
- Modify: 所有本次 CRM Service/Mapper/Entity/DTO 文件

- [ ] **Step 1: 类/方法 Javadoc 补齐**

要求：
- 类注释说明职责
- 方法注释写清 `@param/@return`

- [ ] **Step 2: 主题实体类、入参、出参字段注释补齐**

要求：
- DTO/VO/BO/Entity 字段补 `@Schema(description=...)`

- [ ] **Step 3: 日志口径统一**

要求：
- 统一前缀
- phone 脱敏
- 关键字段齐全（profileId/eventId/stageCode/orderId 等）

- [ ] **Step 4: 全量诊断扫描**

Run: `GetDiagnostics` (all files)
Expected: 无新增诊断错误

