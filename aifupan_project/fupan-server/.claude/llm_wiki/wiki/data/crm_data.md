# CRM Data — 数据模型

> CRM 子系统数据表结构。现有 6 张核心表（replay-power + replay-order）+ 2 张新增扩展表。

---

## 一、现有核心表（replay-power 域）

### 1. 客户域

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_user` | UserEntity | id, phone, password, status(0正常/1冻结), tenantId, createDate | 客户主体账号 |
| `tb_user_details` | UserDetailsEntity | userId(PK), saleId, tradeId, channel, company, userAmbition, leadType, wxName | 客户扩展信息，PK=userId |

### 2. 销售域

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_sales` | SalesEntity | id, name, phone, status, pollingEnabled, employeeId | 销售人员信息 |

### 3. 跟进域

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_user_remark` | UserRemarkEntity | id, userId, salesId, content, remarkType, accordingStatus, currentFolTime, nextFolTime, isDeleted | 跟进记录明细 |
| `tb_user_business` | UserBusinessEntity | userId(PK), accordingStatus, nextFolTime, latestRemark, salesId, updateDate | 跟进状态快照，每客户一条 |

### 4. 订单域（replay-order）

| 表名 | 实体 | 核心字段 | 说明 |
|------|------|----------|------|
| `tb_order` | OrderEntity | id, userId, productId, status, payStatus, isTrial, amount, expireTime, createDate | 订单主表 |

---

## 二、新增扩展表（CRM 智能体集成）

所有新增表统一约束：雪花 ID (`IdType.INPUT`)、手动软删除 (`isDeleted`)、手动时间戳 (`createDate`/`updateDate`)。

### 表一：AI 画像表

**表名**: `tb_crm_ai_profile`
**用途**: 存储智能体为 CRM 客户生成的完整 AI 画像 JSON

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | BIGINT | 是 | 主键，雪花 ID |
| `user_id` | BIGINT | 是 | 客户 ID，关联 tb_user.id |
| `profile_id` | VARCHAR(128) | 是 | 画像版本标识，UNIQUE |
| `source` | VARCHAR(32) | 是 | 数据来源，固定值 `agent_system` |
| `updated_at` | DATETIME | 是 | 画像业务更新时间 |
| `profile_json` | LONGTEXT | 是 | AI 画像完整 JSON |
| `summary` | VARCHAR(1000) | 否 | 画像摘要 |
| `is_deleted` | TINYINT | 是 | 逻辑删除 0未删除/1已删除 |
| `create_date` | DATETIME | 是 | 创建时间 |
| `update_date` | DATETIME | 是 | 更新时间 |

**索引**:
- `PRIMARY KEY (id)`
- `UNIQUE KEY uk_profile_id (profile_id)`
- `KEY idx_user_id (user_id)`
- `KEY idx_updated_at (updated_at)`

### 表二：阶段事件表

**表名**: `tb_crm_stage_event`
**用途**: 存储智能体回传的阶段事件（关键事实、证据、阶段判断）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | BIGINT | 是 | 主键，雪花 ID |
| `user_id` | BIGINT | 是 | 客户 ID，关联 tb_user.id |
| `event_id` | VARCHAR(128) | 是 | 事件标识，UNIQUE，幂等键 |
| `source` | VARCHAR(32) | 是 | 数据来源，固定值 `agent_system` |
| `occurred_at` | DATETIME | 是 | 事件发生时间 |
| `stage_code` | VARCHAR(64) | 否 | 阶段编码 |
| `stage_label` | VARCHAR(128) | 否 | 阶段名称 |
| `confidence` | DECIMAL(5,4) | 否 | 置信度，范围 0-1 |
| `summary` | VARCHAR(1000) | 否 | 阶段事件摘要 |
| `facts_json` | LONGTEXT | 否 | 关键事实 JSON |
| `raw_json` | LONGTEXT | 否 | 原始扩展 JSON |
| `create_date` | DATETIME | 是 | 创建时间 |

**索引**:
- `PRIMARY KEY (id)`
- `UNIQUE KEY uk_event_id (event_id)`
- `KEY idx_user_id (user_id)`
- `KEY idx_occurred_at (occurred_at)`

---

## 三、数据关联图

```
tb_user (客户主体)
  ├── tb_user_details (客户详情，1:1)
  ├── tb_user_remark (跟进明细，1:N)
  ├── tb_user_business (跟进快照，1:1)
  ├── tb_order (订单，1:N)
  ├── tb_crm_ai_profile (AI画像，1:N)
  └── tb_crm_stage_event (阶段事件，1:N)

tb_sales (销售)
  └── tb_user_details.saleId (归属关系)
  └── tb_user_remark.salesId (跟进关系)
```

---

## 四、设计原则

<!-- Sources: archive/20260514_crm_spec_c_stage_event.md, archive/2026-05-15__fix_crm_table_missing_create_id_update_id.md (harvested 2026-05-20) -->

1. **不冗余手机号**: 扩展表不保留 `phone`，通过 `user_id` 关联
2. **入参 phone → userId 换算**: 接口层接收 `phone`，查 `tb_user` 换 `userId` 后落库
3. **event_id 幂等**: 阶段事件表 `uk_event_id` 保证同一事件不会重复写入
4. **兼容现有三层架构**: Controller → Bll → Repository
5. **CRM 扩展表禁止继承 BaseEntity**: `tb_crm_ai_profile`、`tb_crm_stage_event` 的 Entity 类**不得**继承 `BaseEntity`。原因：这两张表 DDL 不含 `create_id`/`update_id` 列，继承 `BaseEntity` 会导致 MyBatis-Plus 生成含这两列的 INSERT/UPDATE 语句，运行时 HTTP 500。（来源：Fix 2026-05-15）

---

## 五、阶段事件快照刷新规则

<!-- Sources: archive/20260514_crm_spec_c_stage_event.md (harvested 2026-05-20) -->

阶段事件默认**只入库**，不刷新 `tb_user_business` 快照。只有 `stage_code` 命中以下白名单时，才受控刷新 `accordingStatus`/`accordingContent`/`accordingDate`：

| stage_code | 映射 accordingStatus | 备注 |
|---|---|---|
| `FOLLOW_UP_DONE` | 1 | 已跟进/已完成沟通 |
| `WAIT_NEXT_CONTACT` | 2 | 待下次触达 |

- `accordingContent` = `CrmStageEventBo.summary`（优先）或 `stageLabel`
- `accordingDate` = `CrmStageEventBo.occurredAt`（解析后）
- 白名单之外的 `stage_code`（含空值）→ 跳过快照刷新，`businessUpdated=false`
