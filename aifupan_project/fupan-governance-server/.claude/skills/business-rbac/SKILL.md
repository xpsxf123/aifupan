---
name: "business-rbac"
description: "权限与人员模块核心业务规则：RBAC角色生命周期、员工离职资产保护、录制账号解绑。开发 rbac 相关功能时调用。"
---

# Business Domain: RBAC & Employee (权限与人员模块)

This skill defines the business logic, lifecycle, and constraints for the RBAC and Employee module (`business.rbac`).

## 1. 角色与权限规则 (Role & Permission Rules)

本系统采用基于角色的访问控制（RBAC），针对直播业务做了深度定制。

### 1.1 权限范围 (Permission Scopes)

角色包含两个维度的权限：
1. **功能权限**：菜单、页面、按钮（关联 `Menu` 表，分为 `DIRECTORY`, `MENU`, `FUNCTION`）。
2. **数据权限**：1-仅看个人，2-看本小组，3-看本部门，4-看本公司，5-看全公司。
- **并集规则**：一个员工如果关联多个角色，其功能权限取**并集**，数据权限取**最大范围**。

### 1.2 角色生命周期 (Role Lifecycle)

- **同名拦截**：租户内角色名称唯一。
- **安全删除**：若角色下有任何未删除的员工，**绝对禁止删除**该角色。
- **超级管理员兜底**：租户初始化时自动生成的"租户管理员"拥有全量权限，且**不可编辑、不可删除**。

## 2. 员工全生命周期管理 (Employee Lifecycle)

### 2.1 唯一性凭证

- `mobile` (手机号) 是租户内员工的唯一登录凭证，新增和修改时必须校验唯一性。

### 2.2 离职与历史资产保护 (Resignation & Asset Protection)

- **绝对禁止物理删除**：员工离职只能通过修改 `account_status`（停用）或 `is_deleted`（逻辑删除）。
- **历史业绩锁定**：员工离职或调岗前产生的**历史排班记录和业绩数据必须永久保留**，归属于当时的组织架构。
- **未来排班阻断**：操作离职/禁用/删除时，系统必须自动检测该员工在**今天及未来日期**是否还有未执行的排班。如果有，**强制阻断**操作并提示"请先进行调班或取消排班"。

### 2.3 录制账号解绑 (Replay Account Binding)

- 系统底层依赖第三方录制客户端。员工开启录制权限 (`on_rec`) 时，系统调用第三方 API 绑定账户；离职或关闭权限时，必须调用 `userAccountService.unbind` 解绑，否则会导致企业扣费。

## 3. 核心字段与字典 (Core Fields)

- `mobile`: 手机号（全局唯一凭证）。
- `account_status`: 账户状态（0-停用, 1-正常）。
- `job_type`: 就职类型（1-全职, 2-兼职）。兼职在排班时可能免除部分疲劳度预警。
- `connector_client_user_id`: 关联的底层录制系统账户 ID。

## 关联技能

- [support-name-map](../support-name-map/SKILL.md): 人员/角色等需要 ID→名称批量补全（get*NameMap），用于列表展示与数据组装。
