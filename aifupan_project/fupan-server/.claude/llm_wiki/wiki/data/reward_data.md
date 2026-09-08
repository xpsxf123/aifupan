<!-- module: reward -->
<!-- area: data -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-reward/.../entity/ -->

# Reward Data (邀请奖励流水表)

## 一、表清单

| Table | 用途 | 引用频率 |
|---|---|---|
| `tb_client_invite_reward_record` | 邀请奖励发放流水（双方对称记录） | 核心表 |

本模块**仅 1 张物理表**。`ClientInviteRewardRecordGroupEntity` 是逻辑封装，extends 物理实体仅多一个 `progressRewardIdStr`（用于 SQL `GROUP_CONCAT` 结果的承载），无对应表。

## 二、tb_client_invite_reward_record

来源实体 `ClientInviteRewardRecordEntity`。MyBatis-Plus 注解 `@TableName("tb_client_invite_reward_record")`，主键 `@TableId(type = IdType.INPUT)`（雪花 ID）。

### 字段定义

| 列名 (DB) | 字段 (Entity) | 类型 | 含义 |
|---|---|---|---|
| `id` | `id` | BIGINT NOT NULL | 主键 (SnowflakeManager.nextValue) |
| `activity_id` | `activityId` | BIGINT | 活动 ID，关联 [[activity_data]] tb_client_invite_activity |
| `progress_id` | `progressId` | BIGINT | 进度 ID，关联 tb_client_invite_progress |
| `progress_reward_id` | `progressRewardId` | BIGINT | 进度奖励 ID，关联 tb_client_invite_progress_reward（一条记录对应一份具体奖励项） |
| `reward_target_type` | `rewardTargetType` | TINYINT | `0`=邀请人；`1`=被邀请人 |
| `reward_user_id` | `rewardUserId` | BIGINT | 被发奖用户 ID |
| `reward_source_user_id` | `rewardSourceUserId` | BIGINT | 来源用户 ID（对方） |
| `reward_tenant_id` | `rewardTenantId` | BIGINT | 限制奖励发到哪个租户 |
| `progress_code` | `progressCode` | VARCHAR | 进度码（register/download/use/ai） |
| `reward_status` | `rewardStatus` | BIGINT | `0`=待发放；`1`=已发放（**注意**：声明为 Long 而非 Integer/TinyInt） |
| `send_date` | `sendDate` | DATETIME | 实际发奖时间，仅当 `rewardStatus=1` |
| `create_date` | `createDate` | DATETIME NOT NULL | 创建时间（**手动 `new Date()`** 而非数据库默认值） |
| `update_date` | `updateDate` | DATETIME NOT NULL | 修改时间（同上） |
| `is_deleted` | `isDeleted` | TINYINT NOT NULL DEFAULT 0 | 软删除标记（**手动**，**不**用 `@TableLogic`） |

### 推断 DDL

```sql
CREATE TABLE tb_client_invite_reward_record (
  id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  progress_id BIGINT NOT NULL,
  progress_reward_id BIGINT NOT NULL,
  reward_target_type TINYINT NOT NULL COMMENT '0=邀请人 1=被邀请人',
  reward_user_id BIGINT NOT NULL,
  reward_source_user_id BIGINT NOT NULL,
  reward_tenant_id BIGINT NOT NULL,
  progress_code VARCHAR(64) NOT NULL,
  reward_status BIGINT NOT NULL DEFAULT 0 COMMENT '0=待发放 1=已发放',
  send_date DATETIME NULL,
  create_date DATETIME NOT NULL,
  update_date DATETIME NOT NULL,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_reward_user (reward_user_id, reward_target_type, reward_status),
  KEY idx_source_user (reward_source_user_id),
  KEY idx_activity_pair (activity_id, reward_target_type, reward_user_id, reward_source_user_id),
  KEY idx_send_date (send_date)
);
```

⚠️ DDL 为根据查询模式**推断**，未在源码确认。实际索引以 SQL 慢日志或 `SHOW INDEX` 为准。

### 关键查询模式（含索引使用提示）

| 查询方法 | 等值条件 | 排序 | 期望索引 |
|---|---|---|---|
| `listByRewardUserIdAndRewardTargetTypeAndRewardStatus` | `reward_user_id` + `reward_target_type` + `reward_status` | — | `(reward_user_id, reward_target_type, reward_status)` |
| `listByActivityIdAndRewardTargetTypeAndRewardUserIdAndRewardSourceUserId` (幂等查询) | `activity_id` + `reward_target_type` + `reward_user_id` + `reward_source_user_id` | — | 复合 4 列 |
| `listByBack` (后台分页) | `progress_code` / `reward_status` / `reward_user_id IN (...)` / `send_date BETWEEN` | `send_date DESC`，`GROUP BY (reward_user_id, progress_id, reward_source_user_id)` | `(send_date)` + 复合 |
| `pageUserRewardListByRewardUserIdAndRewardTargetTypeGroupByProgressId` (客户端分页) | `reward_user_id` + `reward_target_type` + `reward_status=1` | `send_date DESC`，`GROUP BY progress_id` | `(reward_user_id, reward_target_type, reward_status, send_date)` |

## 三、字段维度的设计原则

1. **租户隔离**：`reward_tenant_id` 限制奖励落到指定租户范围（避免跨租户领奖）。读操作业务层校验，不在 SQL WHERE 强制（**调用方需注意**）。
2. **不冗余手机号 / 昵称**：用户信息走 `userFeign.listByIds` 内存装配——避免冗余 + 同步问题。
3. **手动时间戳 + 手动软删**：与 jiuyu 全项目约定一致（CLAUDE.md §5）：
   - **禁** `@TableLogic`
   - 写方法显式 `setCreateDate(new Date())` / `setUpdateDate(new Date())`
4. **状态字段类型**：`rewardStatus` 用 `Long` 而非 `Integer`/`TinyInt` 的语义类型——是历史遗留，不影响功能。
5. **send_date 可为空**：未发放时为 NULL；发放后 `setSendDate(new Date())`。

## 四、未持久化的辅助实体

### ClientInviteRewardRecordGroupEntity (extends ClientInviteRewardRecordEntity)

新增字段：
- `progressRewardIdStr` (String) — 用于承载 SQL `GROUP_CONCAT(progress_reward_id SEPARATOR '_')` 结果。在 `clientGetUserRewardList` 流程中，分组后用 `String.split("_")` 还原成 `List<Long>`。

⚠️ **不要**给这个类加 `@TableName`——它没有对应物理表。如果加了，等同于第二张表 `tb_client_invite_reward_record_group_entity` 被 MyBatis-Plus 推断出来，会导致后续 `BaseMapper<Group>` 找不到表报错。

## 五、与 tb_user_device_fingerprint 的运行时关联

防刷流程会读 `tb_user_device_fingerprint`（持久化在 [[power_data]] replay-power），但本模块不直接 SQL JOIN——通过 `userDeviceFingerprintFeign.getUserDeviceFingerprintByFingerprintAndDeviceType()` Feign 调用拉取。
