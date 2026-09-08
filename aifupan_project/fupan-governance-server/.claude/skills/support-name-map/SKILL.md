---
name: "support-name-map"
description: "汇总并规范 get*NameMap(List<Long>) 批量补全能力。需要把一批 ID 转成名称用于展示/导出、且要避免 N+1 查询时调用。"
---

# Support: NameMap 批量补全

## 1. 目标

当上游业务只有一批 `id`（公司/部门/小组/岗位/人员/角色/直播间/行业等），但需要展示名称时，统一通过各模块 `get*NameMap(List<Long>)` 返回 `Map<Long, String>`，用于 **批量补全**，避免在循环里逐条查库（N+1）。

## 2. 统一契约（Contract）

- 入参：`List<Long> ids`
- 出参：`Map<Long, String>`，key 为 id，value 为名称
- 空入参：应返回空 `Map`（实现里常见 `Map.of()` 或 `Collections.emptyMap()`）
- 部分缺失：允许返回结果中缺少某些 id（例如已删除/不存在的记录）
- 使用边界：仅用于"展示补全"，不要用它做权限判断或业务校验（权限与存在性校验应走 `hasId/find*` 等更强约束方法）

## 3. 现有能力清单（按模块）

### 3.1 组织架构 Org

- 子公司名称：`SubCompanyService#getNameMap(List<Long>)`
- 部门名称：`DeptService#getDeptNameMap(List<Long>)`
- 小组名称：`TeamService#getTeamNameMap(List<Long>)`
- 岗位名称：`PositionService#getPositionNameMap(List<Long>)`

### 3.2 权限与人员 RBAC

- 人员名称：`EmployeeService#getNameMap(List<Long>)`
- 角色名称：`RoleService#getRoleNameMap(List<Long>)`

### 3.3 直播间 Room

- 直播间展示名：`LiveRoomService#getLiveRoomNameMap(List<Long>)`
  - 当前拼接规则：`anchorName + "(" + anchorNumber + ")"`

### 3.4 外部依赖（OpenFeign / Replay）

- 行业名称：`TradeService#getTradeNameMap(List<Long>)`
  - 注意：该实现会远程调用 replay 服务，失败时返回空 Map（并记录错误日志）

## 4. 推荐用法（与 Complete 配合）

典型场景：分页/列表查询返回 DTO 列表后，链式补全名称字段。

## 关联技能

- [utils-usage-standard](../utils-usage-standard/SKILL.md): 定义了 `Complete + getNameMap` 的标准使用姿势与禁忌（避免 JOIN / 避免 N+1）。
- [business-org](../business-org/SKILL.md): 组织架构（公司/部门/小组）经常需要 ID→名称补全。
- [business-rbac](../business-rbac/SKILL.md): 人员/角色场景经常需要 ID→名称补全。
- [business-room](../business-room/SKILL.md): 直播间展示名需要批量补全。
