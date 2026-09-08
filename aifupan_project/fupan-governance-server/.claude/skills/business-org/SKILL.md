---
name: "business-org"
description: "组织架构模块核心业务规则：三级树状结构、级联删除阻断、名称防重、负责人委派。开发 org 相关功能时调用。"
---

# Business Domain: Organization (组织架构模块)

This skill defines the business logic, relationships, and constraints for the Organization module (`business.org`).

## 1. 业务流程与层级关系 (Hierarchy & Flow)

企业内部采用严格的 **三级树状结构**：
- **SubCompany (子公司)** -> **Dept (部门)** -> **Team (小组)**
- **数据归属**：所有的 `Dept` 必须且只能挂载在某个 `SubCompany` 下；所有的 `Team` 必须且只能挂载在某个 `Dept` 下。
- **跨模块关联**：`Employee` (人员) 和 `LiveRoom` (直播间) 必须挂载在这一套组织架构上。

## 2. 核心业务规则 (Core Rules & Constraints)

### 2.1 级联阻断与安全删除 (Cascade Blocking)

系统采用**绝对的级联阻断机制**，不允许产生"孤儿数据"。
- **删除子公司前**：必须校验其下是否还有未删除的 `Dept`、`Employee` 或 `LiveRoom`。如果有，**强制拦截抛出错误**。
- **删除部门前**：必须校验其下是否还有未删除的 `Team`、`Employee` 或 `LiveRoom`。如果有，**强制拦截**。
- **删除小组前**：必须校验其下是否还有未删除的 `Employee` 或 `LiveRoom`。如果有，**强制拦截**。

### 2.2 同级名称防重 (Name Uniqueness)

- 同一个层级下的组织名称不能重复。例如，同一个租户下，不能有两个名为"直播一部"的部门；同一个部门下不能有两个名为"带货A组"的小组。
- 校验时必须排除已逻辑删除 (`is_deleted = 1`) 的记录。

### 2.3 部门负责人委派 (Manager Assignment)

- 支持将某位或多位 `Employee` 设置为某级组织（公司/部门/小组）的"管理员/负责人"。
- 这层关系通过 `ManagerConnector` 表（`ManagerType` 枚举区分层级）维护。
- **作用**：该身份决定了排班审批流的节点，以及数据看板（Dashboard）的默认数据透视权限。

## 3. 核心字段与字典 (Core Fields)

- `tenant_id`: 租户ID（所有查询更新的绝对前置条件）。
- `manager_user_ids`: 负责人 ID 列表（在 DTO 中传递，落库于 `manager_connector`）。

## 4. 对接与注意事项 (Integration Notes)

- 当修改组织架构层级（如把 Team 移动到另一个 Dept）时，必须联动清理 `ManagerConnector` 的缓存（调用 `connectorProcessor.clearOrgCache`）。
- **查询隔离**：列表查询和下拉接口，需要根据用户的权限动态拼接 `deptIds` 或 `companyIds` 进行数据隔离。

## 关联技能

- [support-name-map](../support-name-map/SKILL.md): 组织维度经常需要公司/部门/小组 ID→名称批量补全（get*NameMap），用于展示与导出。
