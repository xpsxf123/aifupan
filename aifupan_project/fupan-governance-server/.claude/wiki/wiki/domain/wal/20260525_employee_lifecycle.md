# [Domain] Employee Lifecycle (员工生命周期)

> Date: 2026-05-25
> Source: distilling from `.agents/llm_wiki/wiki/domain/wal/archive/employee_lifecycle.md`

## State Definitions

| 状态 | 标识字段 | 描述 |
|---|---|---|
| **初始化** | `hold_tenant = true` | 租户开通时自动创建的主账户，拥有最高权限，不可删除。 |
| **正常** | `account_status = NORMAL` | 员工的活跃状态，可参与排班、录制和业绩核算。 |
| **停用** | `account_status = DISABLED` | 禁用状态，通常由解绑外部账户或手动触发，无法登录系统。 |
| **已删除** | `is_deleted = true` | 逻辑删除状态，释放人员配额，数据保留用于历史追溯。 |

## Core Transitions

### Onboarding (入职)
- **入口**: `addEmployee`, `addForReplayAccount`, `syncSubAccount`
- **关键约束**:
  - **额度校验**: 调用 `propertyService.editTenantPropertyQuota` 检查企业版套餐的人员配额。
  - **唯一性**: 手机号在全局（跨租户）和当前租户内必须唯一。
  - **录制绑定**: 若 `on_rec = true`，通过 Feign 调用 `userAccountService.bind` 创建外部子账户，并记录 `connector_client_user_id`。
  - **组织归属**: 必须关联到具体的公司 (`company_id`)，部门/小组可选。

### Profile Update (信息变更)
- **入口**: `updateEmployee`, `updateProfile`
- **数据权限**: 修改组织归属时，通过 `@BeforePermission` 校验操作者是否有权管理目标公司/部门/小组。
- **同步机制**: 若开启了录制权限且修改了手机号，会同步调用外部接口更新子账户信息。

### Offboarding (离职/删除)
- **入口**: `delete`
- **保护机制**: 主账户 (`hold_tenant = true`) 禁止删除；租户至少需保留一名员工。
- **资源回收**:
  1. 解绑外部录制账号 (`userAccountService.unbind`)。
  2. 执行逻辑删除 (`is_deleted = true`)。
  3. 归还人员配额 (`editTenantPropertyQuota` 递减)。

## Key Business Rules

- **主账户特权**: 主账户不参与额度统计，且不能被编辑或删除。
- **录制权限联动**: `on_rec` 字段直接控制员工是否能在外部直播系统中进行录制操作。
- **角色缓存**: 员工角色关系 (`user_role`) 变更后，会同步更新 Redis 缓存 (`oauth:employee-role:{id}`)，有效期 1 小时。

## Dependencies

- **External**: `UserAccountService` (爱复盘客户端账户体系)
- **Internal**: `PropertyService` (租户配额管理), `ManagerConnectorProcessor` (组织缓存清理)
